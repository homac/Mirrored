/*
 * ArticleList.java
 *
 * Part of the Mirrored app for Android
 *
 * Copyright (C) 2010 Holger Macht <holger@homac.de>
 *
 * This file is released under the GPLv3.
 *
 */

package de.homac.Mirrored.view;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import de.homac.Mirrored.R;
import de.homac.Mirrored.common.Helper;
import de.homac.Mirrored.common.MDebug;
import de.homac.Mirrored.common.Mirrored;
import de.homac.Mirrored.feed.ArticleContentDownloader;
import de.homac.Mirrored.feed.ArticleDownloadException;
import de.homac.Mirrored.feed.ArticleDownloadThread;
import de.homac.Mirrored.feed.Feed;
import de.homac.Mirrored.model.Article;
import de.homac.Mirrored.provider.SpiegelOnlineDownloader;

public class ArticlesList extends ListActivity {
	static final int CONTEXT_MENU_DELETE_ID = 0;
	static final int CONTEXT_MENU_SAVE_ID = 1;
    static final int CONTEXT_MENU_SHARE_ID = 2;
    static final int REQ_PICK_CATEGORY = 0;

	private boolean _internetReady = false;
    private String category = null;
    private Feed feed;

	private final String TAG = "ArticlesList";
	private Mirrored app;
    private ProgressDialog progressDialog;

    @Override
	protected void onCreate(Bundle icicle) {
        app = (Mirrored) getApplication();

        if (MDebug.LOG)
            Log.d(TAG, "onCreate()");

        super.onCreate(icicle);

        // if (_prefDarkBackground)
        // setTheme(android.R.style.Theme_Black);

        if (MDebug.LOG)
            Log.d(TAG, "Setting content view");
        setContentView(R.layout.articles_list);

        registerForContextMenu(getListView());

        ArticlesListStateHolder holder = (ArticlesListStateHolder) getLastNonConfigurationInstance();
        if (holder != null) {
            restoreState(holder);
        } else {
            initCategory();

            refresh();
        }
    }

    private void initCategory() {
        String category = app.getPreferences().getString("PrefStartWithCategory", null);
        if (category != null && category.length() != 0) {
            if (MDebug.LOG)
                Log.d(TAG, "Got feedCategory from preferences: " + category);
        } else {
            category = Mirrored.BASE_CATEGORY;
            if (MDebug.LOG)
                Log.d(TAG, "No feedCategory set, using BASE_CATEGORY: " + category);
        }
        setCategory(category);
    }

    private void setCategory(String category) {
        // currently all categories have to be lower case for the URL
        this.category = category.toLowerCase();
    }

    private void refresh() {
        _internetReady = app.online();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            invalidateOptionsMenu();
        }

        refreshTitle();

        ArticleLoader loader = new ArticleLoader() {
            @Override
            protected void onPreExecute() {
                showProgressDialog(getString(R.string.progress_dialog_load_all), new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        cancel(true);
                    }
                });
            }

            @Override
            protected void onCancelled() {
                dismissProgressDialog();
            }

            @Override
            protected void onPostExecute(Long result) {
                dismissProgressDialog();

                feed = getFeed();
                List<Article> articles = feed.getArticles(category);
                setListAdapter(new IconicAdapter(ArticlesList.this, articles));
                app.setOfflineFeed(getOfflineFeed());

                if (articles.size() == 0)
                    Helper.showDialog(ArticlesList.this, getString(R.string.no_articles));
            }
        };

        boolean allowDownloadForNet = !app.getPreferences().getBoolean("PrefDownloadAllWifi", true) || app.wifiConnected();
        loader.setInternetReady(_internetReady);
        loader.setDownloadAllArticles(app.getPreferences().getBoolean("PrefDownloadAllArticles", false) && allowDownloadForNet);
        loader.setDownloadImages(app.getPreferences().getBoolean("PrefDownloadImages", true));

        loader.execute(category);
    }

    private void refreshTitle() {
        String title = category.substring(0, 1).toUpperCase() + category.substring(1);
        if (!_internetReady) {
            title += " (" + getString(R.string.caption_offline) + ")";
            if (!app.getPreferences().getBoolean("PrefStartWithOfflineMode", false)) {
                Toast.makeText(getApplicationContext(),
                        R.string.switch_to_offline_mode, Toast.LENGTH_LONG)
                        .show();
            }
        }
        setTitle(title);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (MDebug.LOG)
            Log.d(TAG, "onRetainNonConfigurationInstance");

        cancelProgressDialog();

        ArticlesListStateHolder holder = new ArticlesListStateHolder();
        holder.category = category;
        holder.feed = feed;
        return holder;
    }

    private void restoreState(ArticlesListStateHolder holder) {
        _internetReady = app.online();
        setCategory(holder.category);
        refreshTitle();
        feed = holder.feed;
        if (feed != null) {
            List<Article> articles = feed.getArticles(category);
            setListAdapter(new IconicAdapter(ArticlesList.this, articles));
        } else {
            refresh();
        }
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.articles_list, menu);
		return true;
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.setGroupVisible(R.id.group_online, _internetReady);
        menu.setGroupVisible(R.id.group_offline, !_internetReady);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;

		switch (item.getItemId()) {
			case R.id.menu_categories:
				if (MDebug.LOG)
					Log.d(TAG, "MENU_CATEGORIES clicked");

				intent = new Intent(this, CategoriesList.class);
				if (MDebug.LOG)
					Log.d(TAG, "Starting CategoriesView");
				startActivityForResult(intent, REQ_PICK_CATEGORY);

				return true;

			case R.id.menu_preferences:
				if (MDebug.LOG)
					Log.d(TAG, "MENU_PREFERENCES clicked");

				//for (Article article : _articles) {
				//	article.resetContent();
				//}

				intent = new Intent(this, Preferences.class);
				startActivity(intent);

				return true;
			case R.id.menu_saveAll:
				if (MDebug.LOG)
					Log.d(TAG, "MENU_SAVE_ALL clicked");

                app.getOfflineFeed().getArticles().addAll(feed.getArticles());

                app.saveOfflineFeed(this, null);

				return true;

			case R.id.menu_deleteAll:
				if (MDebug.LOG)
					Log.d(TAG, "MENU_DELETE_ALL clicked");

				List<Article> toRemove = new ArrayList<Article>(app.getOfflineFeed().getArticles());
                for (Article article : toRemove) {
                    // remove deleted row
                    ((IconicAdapter) getListView().getAdapter())
                            .remove(article);
                }
                app.getOfflineFeed().getArticles().clear();
                app.saveOfflineFeed(this, null);

				return true;

			case R.id.menu_offlineMode:
				if (MDebug.LOG)
					Log.d(TAG, "MENU_OFFLINE_MODE clicked");

				app.setOfflineMode(true);
                refresh();

				return true;

			case R.id.menu_onlineMode:
				if (MDebug.LOG)
					Log.d(TAG, "MENU_ONLINE_MODE clicked");

				app.setOfflineMode(false);

				if (!app.online())
					Helper.showDialog(this,
                            getString(R.string.please_check_internet));
				else {
					refresh();
				}

				return true;

			case R.id.menu_refresh:
				if (MDebug.LOG)
					Log.d(TAG, "MENU_REFRESH clicked");

				refresh();
		}

		return false;
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQ_PICK_CATEGORY) {
            setCategory(data.getStringExtra(CategoriesList.EXTRA_CATEGORY));

            refresh();
        }
    }

    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Article article = (Article) getListAdapter().getItem(info.position);

        boolean availableOffline = app.getOfflineFeed().getArticles().contains(article);
        if (_internetReady) {
            menu.add(0, CONTEXT_MENU_SHARE_ID, 0, R.string.menu_share);
        }
		if (_internetReady && !availableOffline) {
			menu.add(0, CONTEXT_MENU_SAVE_ID, 0, R.string.context_menu_save);
        }
		if (availableOffline) {
            menu.add(0, CONTEXT_MENU_DELETE_ID, 0, R.string.context_menu_delete);
        }
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Article article = (Article) getListAdapter().getItem(info.position);

		switch (item.getItemId()) {
			case CONTEXT_MENU_DELETE_ID :
				if (MDebug.LOG)
					Log.d(TAG, "CONTEXT_MENU_DELETE_ID clicked");

				if (MDebug.LOG)
					Log.d(TAG, "Removing article with title: " + article.getTitle());

				app.getOfflineFeed().getArticles().remove(article);
				// remove deleted row
				((IconicAdapter) getListView().getAdapter()).remove(article);

				app.saveOfflineFeed(this, new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        Toast.makeText(getApplicationContext(),
                                R.string.article_deleted, Toast.LENGTH_LONG).show();

                    }
                });

				return true;

			case CONTEXT_MENU_SAVE_ID :
				if (MDebug.LOG)
					Log.d(TAG, "CONTEXT_MENU_SAVE_ID clicked");

				// get the content, just to be sure it has been downloaded, give
				// false for internet state to
				// make sure article is trimmed
                if (!app.getOfflineFeed().getArticles().contains(article)) {
                    app.getOfflineFeed().getArticles().add(article);
                }

                app.saveOfflineFeed(this, new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        Toast.makeText(getApplicationContext(), R.string.article_saved,
                                Toast.LENGTH_LONG).show();

                    }
                });

				// make sure the articles is redownloaded
				if (_internetReady)
					article.resetContent();

				return true;

            case CONTEXT_MENU_SHARE_ID:
                Helper.shareUrl(this, article.getUrl());
                return true;

			default :
				return super.onContextItemSelected(item);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

        final Article article = (Article) getListAdapter().getItem(position);

        if (article.getContent() == null || article.getContent().length() == 0) {
            AsyncTask<Article, Long, Integer> async = new AsyncTask<Article, Long, Integer>() {
                public boolean downloadImages;

                @Override
                protected void onPreExecute() {
                    downloadImages = Mirrored.getInstance().getPreferences().getBoolean("PrefDownloadImages", true);

                    showProgressDialog(getString(R.string.progress_dialog_load), new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            cancel(true);
                        }
                    });
                }

                @Override
                protected Integer doInBackground(Article... articles) {
                    SpiegelOnlineDownloader loader = new SpiegelOnlineDownloader(articles[0], Mirrored.getInstance().getCacheHelper());
                    try {
                        loader.downloadContent(downloadImages);
                        return 0;
                    } catch (ArticleDownloadException e) {
                        if (MDebug.LOG)
                            Log.e(TAG, String.format("Could not fetch article '%s', statuscode was %s", articles[0].getUrl(), e.getHttpCode()));
                        return e.getHttpCode();
                    }
                }

                @Override
                protected void onCancelled() {
                    dismissProgressDialog();
                }

                @Override
                protected void onPostExecute(Integer result) {
                    dismissProgressDialog();

                    if (result == 0) {
                        openArticleViewer(article);
                    } else {
                        Spanned text = Html.fromHtml(getString(R.string.article_download_error, result));
                        Toast toast = Toast.makeText(app.getBaseContext(), text, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.TOP, 0, 0);
                        toast.show();
                    }
                }
            };
            async.execute(article);
        } else {
            openArticleViewer(article);
        }
	}

    private void openArticleViewer(Article article) {
        app.setArticle(article);

        Intent intent = new Intent(ArticlesList.this, ArticleViewer.class);
        startActivity(intent);
    }

    private void showProgressDialog(String title, DialogInterface.OnCancelListener listener) {
        cancelProgressDialog();
        progressDialog = ProgressDialog.show(this, "", title, true, true, listener);
    }

    private void cancelProgressDialog() {
        if (progressDialog != null) {
            progressDialog.cancel();
        }
        progressDialog = null;
    }

    private void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        progressDialog = null;
    }
}

class ArticlesListStateHolder {
    String category;
    Feed feed;
}

class IconicAdapter extends ArrayAdapter<Article> {
    private static final String TAG = "IconicAdapter";
    private final List<Article> articles;

    IconicAdapter(Context context, List<Article> articles) {
        super(context, R.layout.article_row, articles);
        this.articles = articles;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;

        if (row == null) {
            if (MDebug.LOG)
                Log.d(TAG, "row is null in getView()");

            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            row = inflater.inflate(R.layout.article_row, parent, false);

				/*
				 * row.setTag(R.id.article_headline,
				 * row.findViewById(R.id.article_headline));
				 * row.setTag(R.id.article_description,
				 * row.findViewById(R.id.article_description));
				 * row.setTag(R.id.article_image,
				 * row.findViewById(R.id.article_image));
				 */
        }

        Article article = articles.get(position);

        TextView headline = (TextView) row.findViewById(R.id.article_headline);
        TextView date = (TextView) row.findViewById(R.id.article_date);
        TextView description = (TextView) row.findViewById(R.id.article_description);

        headline.setText(article.getTitle());
        description.setText(Html.fromHtml(article.getDescription()));
        if (article.getThumbnailImage() != null) {
            float ar = (float)article.getThumbnailImage().getWidth() / article.getThumbnailImage().getHeight();
            Drawable thumb = new BitmapDrawable(getContext().getResources(), article.getThumbnailImage());
            //scale bitmap to 90dp height
            int dim = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, getContext().getResources().getDisplayMetrics());
            thumb.setBounds(0, 0, dim, (int)(dim/ar));
            description.setCompoundDrawables(thumb, null, null, null);
        } else {
            description.setCompoundDrawables(null, null, null, null);
        }
        date.setText(article.pubDateString());

        return row;
    }
}

class ArticleLoader extends AsyncTask<String, Integer, Long> {
    private static final String TAG = "ArticleLoader";

    private boolean internetReady;
    private boolean downloadAllArticles;
    private boolean downloadImages;

    private Feed feed, offlineFeed;

    protected Long doInBackground(String... categories) {
        String category = categories[0];
        URL url = SpiegelOnlineDownloader.getFeedUrl(category);
        // first thread run, run only once
        feed = new Feed(url, internetReady, category);

        if (internetReady) {
            // get offline feed also if online
            offlineFeed = new Feed(url, false, category);
            if (MDebug.LOG)
                Log.d(TAG, "Offline feed has " + offlineFeed.getArticles().size() + " articles");

            downloadArticleParts(feed.getArticles());
        } else {
            offlineFeed = feed;
        }

        // we're finally done
        if (MDebug.LOG)
            Log.d(TAG, "all articles fetched, sending message");
        return 0L;
    }

    private void downloadArticleParts(List<Article> articles) {
        ArticleContentDownloader downloader = new ArticleContentDownloader(articles,
                downloadAllArticles, downloadImages && internetReady);
        downloader.download();
    }

    protected Feed getFeed() {
        return feed;
    }

    protected Feed getOfflineFeed() {
        return offlineFeed;
    }

    public void setInternetReady(boolean internetReady) {
        this.internetReady = internetReady;
    }

    public void setDownloadAllArticles(boolean downloadAllArticles) {
        this.downloadAllArticles = downloadAllArticles;
    }

    public void setDownloadImages(boolean downloadImages) {
        this.downloadImages = downloadImages;
    }
}

