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

package de.homac.Mirrored;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.Spanned;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.*;
import android.util.Log;
import android.util.DisplayMetrics;
import android.text.Html;
import android.os.Message;
import android.os.Handler;
import android.os.Bundle;
import android.content.res.Configuration;
import android.content.Intent;
import android.app.ProgressDialog;
import android.app.ListActivity;

public class ArticlesList extends ListActivity {
	static final int CONTEXT_MENU_DELETE_ID = 0;
	static final int CONTEXT_MENU_SAVE_ID = 1;
	static final int MENU_CATEGORIES = 0;
	static final int MENU_PREFERENCES = 1;
	static final int MENU_SAVE_ALL = 2;
	static final int MENU_DELETE_ALL = 3;
	static final int MENU_OFFLINE_MODE = 4;
	static final int MENU_ONLINE_MODE = 5;
	static final int MENU_REFRESH = 6;
    static final int REQ_PICK_CATEGORY = 0;

	private boolean _internetReady;
    private String category = null;
    private Feed feed;

	private final String TAG = "ArticlesList";
	private Mirrored app;

    @Override
	protected void onCreate(Bundle icicle) {
        app = (Mirrored) getApplication();

        if (MDebug.LOG)
            Log.d(TAG, "onCreate()");

        super.onCreate(icicle);

        _internetReady = app.online();

        // if (_prefDarkBackground)
        // setTheme(android.R.style.Theme_Black);

        if (MDebug.LOG)
            Log.d(TAG, "Setting content view");
        setContentView(R.layout.articles_list);

        initCategory();

        registerForContextMenu(getListView());

        refresh();
    }

    private void initCategory() {
        String category = app.getStringPreference("PrefStartWithCategory", null);
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
        this.category = category;

        String title = category.substring(0, 1).toUpperCase() + category.substring(1);
        if (!_internetReady) {
            title += " (" + getString(R.string.caption_offline) + ")";
            if (!app.getBooleanPreference("PrefStartWithOfflineMode", false)) {
                Toast.makeText(getApplicationContext(),
                        R.string.switch_to_offline_mode, Toast.LENGTH_LONG)
                        .show();
            }
        }
        setTitle(title);
    }

    private void refresh() {
        _internetReady = app.online();

        final ProgressDialog pdialog = ProgressDialog.show(this, "",
                getString(R.string.progress_dialog_load_all), true, false);

        ArticleLoader loader = new ArticleLoader(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                ArticleLoader ld = (ArticleLoader) msg.obj;
                feed = ld.getFeed();
                List<Article> articles = feed.getArticles(category);
                setListAdapter(new IconicAdapter(ArticlesList.this, articles));
                app.setOfflineFeed(ld.getOfflineFeed());
                pdialog.dismiss();

                if (articles.size() == 0)
                    app.showDialog(ArticlesList.this, getString(R.string.no_articles));
            }
        });
        loader.setCategory(category);
        loader.setInternetReady(_internetReady);
        loader.setDownloadAllArticles(app.getBooleanPreference("PrefDownloadAllArticles", false));
        loader.setDownloadImages(app.getBooleanPreference("PrefDownloadImages", true));

        Thread thread = new Thread(loader);
        thread.start();
    }

    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (MDebug.LOG)
			Log.d(TAG, "onConfigurationChanged()");
		setContentView(R.layout.articles_list);
		registerForContextMenu(getListView());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(Menu.NONE, MENU_CATEGORIES, Menu.NONE,
				R.string.menu_categories).setIcon(
				android.R.drawable.ic_menu_more);
		menu.add(Menu.NONE, MENU_PREFERENCES, Menu.NONE,
				R.string.menu_preferences).setIcon(
				android.R.drawable.ic_menu_preferences);

		if (_internetReady) {
			menu.add(Menu.NONE, MENU_OFFLINE_MODE, Menu.NONE,
					R.string.menu_offline_mode).setIcon(
					android.R.drawable.ic_menu_close_clear_cancel);
			menu.add(Menu.NONE, MENU_REFRESH, Menu.NONE, R.string.menu_refresh)
					.setIcon(R.drawable.ic_menu_refresh);
			menu.add(Menu.NONE, MENU_SAVE_ALL, Menu.NONE,
					R.string.menu_save_all).setIcon(
					android.R.drawable.ic_menu_save);
		} else {
			menu.add(Menu.NONE, MENU_ONLINE_MODE, Menu.NONE,
					R.string.menu_online_mode).setIcon(
					android.R.drawable.ic_menu_upload);
			if (app.getOfflineFeed().getArticles().size() > 0)
				menu.add(Menu.NONE, MENU_DELETE_ALL, Menu.NONE,
						R.string.menu_delete_all).setIcon(
						android.R.drawable.ic_menu_delete);
		}

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;

		switch (item.getItemId()) {
			case MENU_CATEGORIES :
				if (MDebug.LOG)
					Log.d(TAG, "MENU_CATEGORIES clicked");

				intent = new Intent(this, CategoriesList.class);
				if (MDebug.LOG)
					Log.d(TAG, "Starting CategoriesView");
				startActivityForResult(intent, REQ_PICK_CATEGORY);

				return true;

			case MENU_PREFERENCES :
				if (MDebug.LOG)
					Log.d(TAG, "MENU_PREFERENCES clicked");

				//for (Article article : _articles) {
				//	article.resetContent();
				//}

				intent = new Intent(this, Preferences.class);
				startActivity(intent);

				return true;
			case MENU_SAVE_ALL :
				if (MDebug.LOG)
					Log.d(TAG, "MENU_SAVE_ALL clicked");

				app.getOfflineFeed().getArticles().clear();
                app.getOfflineFeed().getArticles().addAll(feed.getArticles());

                app.saveOfflineFeed(this, null);

				return true;

			case MENU_DELETE_ALL :
				if (MDebug.LOG)
					Log.d(TAG, "MENU_DELETE_ALL clicked");

				for (Article article : app.getOfflineFeed().getArticles()) {
                    // remove deleted row
                    ((IconicAdapter) getListView().getAdapter())
                            .remove(article);
                }
                app.getOfflineFeed().getArticles().clear();
                app.saveOfflineFeed(this, null);

				return true;

			case MENU_OFFLINE_MODE :
				if (MDebug.LOG)
					Log.d(TAG, "MENU_OFFLINE_MODE clicked");

				app.setOfflineMode(true);
                refresh();

				return true;

			case MENU_ONLINE_MODE :
				if (MDebug.LOG)
					Log.d(TAG, "MENU_ONLINE_MODE clicked");

				app.setOfflineMode(false);

				if (!app.online())
					app.showDialog(this,
							getString(R.string.please_check_internet));
				else {
					refresh();
				}

				return true;

			case MENU_REFRESH :
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
					Log.d(TAG, "Removing article with title: " + article.title);

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

			default :
				return super.onContextItemSelected(item);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

        final Article article = (Article) getListAdapter().getItem(position);

        if (article.getContent() == null || article.getContent().length() == 0) {
            final ProgressDialog pdialog = ProgressDialog.show(this, "",
                    getString(R.string.progress_dialog_load), true, false);

            SingleArticleLoader loader = new SingleArticleLoader(new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    pdialog.dismiss();

                    if (msg.what == 0) {
                        openArticleViewer(article);
                    } else {
                        Spanned text = Html.fromHtml(getString(R.string.article_download_error, msg.arg1));
                        Toast toast = Toast.makeText(app.getBaseContext(), text, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.TOP, 0, 0);
                        toast.show();
                    }
                }
            }, article);
            loader.setDownloadImages(app.getBooleanPreference("PrefDownloadImages", true));

            Thread thread = new Thread(loader);
            thread.start();
        } else {
            openArticleViewer(article);
        }
	}

    private void openArticleViewer(Article article) {
        app.setArticle(article);

        Intent intent = new Intent(ArticlesList.this, ArticleViewer.class);
        startActivity(intent);
    }
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

        TextView headline = (TextView) row
                .findViewById(R.id.article_headline);
        TextView date = (TextView) row.findViewById(R.id.article_date);
        ImageView image = (ImageView) row.findViewById(R.id.article_image);
        TextView description = (TextView) row
                .findViewById(R.id.article_description);

        headline.setText(article.title);
        image.setImageBitmap(article.getThumbnailImage());
        description.setText(Html.fromHtml(article.description));
        date.setText(article.dateString());

        return row;
    }
}

class SingleArticleLoader implements Runnable {
    private Handler handler;
    private Article article;
    private boolean downloadImages;

    public SingleArticleLoader(Handler handler, Article article) {
        this.handler = handler;
        this.article = article;
    }

    @Override
    public void run() {
        try {
            article.downloadContent(downloadImages);
            handler.sendEmptyMessage(0);
        } catch (ArticleDownloadException e) {
            Message msg = new Message();
            msg.what = 1;
            msg.arg1 = e.getHttpCode();
            handler.sendMessage(msg);
        }
    }

    public void setDownloadImages(boolean downloadImages) {
        this.downloadImages = downloadImages;
    }
}

class ArticleLoader implements Runnable {
    private static final String TAG = "ArticleLoader";

    // static final String BASE_FEED =
    // "http://www.spiegel.de/schlagzeilen/index.rss";
    static final String FEED_PREFIX = "http://www.spiegel.de/";
    static final String FEED_SUFFIX = "/index.rss";

    private Handler handler;
    private boolean internetReady;
    private String category;
    private boolean downloadAllArticles;
    private boolean downloadImages;

    private Feed feed, offlineFeed;

    public ArticleLoader(Handler handler) {
        this.handler = handler;
    }

    public void run() {
        URL url = getFeedUrl(category);
        // first thread run, run only once
        feed = new Feed(url, internetReady);

        if (internetReady) {
            // get offline feed also if online
            offlineFeed = new Feed(url, false);
            if (MDebug.LOG)
                Log.d(TAG, "Offline feed has " + offlineFeed.getArticles().size() + " articles");

            downloadArticleParts(feed.getArticles());
        } else {
            offlineFeed = feed;
        }

        // we're finally done
        if (MDebug.LOG)
            Log.d(TAG, "all articles fetched, sending message");
        Message msg = new Message();
        msg.what = 0;
        msg.obj = this;
        handler.sendMessage(msg);
    }

    private void downloadArticleParts(List<Article> articles) {
        ArticleContentDownloader downloader = new ArticleContentDownloader(articles,
                downloadAllArticles, downloadImages && internetReady);
        downloader.download();
    }

    private URL getFeedUrl(String category) {
        try {
            return new URL(FEED_PREFIX + category + FEED_SUFFIX);
        } catch (MalformedURLException e) {
            if (MDebug.LOG)
                Log.e(TAG, e.toString());
            throw new RuntimeException(e);
        }
    }

    public Feed getFeed() {
        return feed;
    }

    public Feed getOfflineFeed() {
        return offlineFeed;
    }

    void setInternetReady(boolean internetReady) {
        this.internetReady = internetReady;
    }

    void setCategory(String category) {
        this.category = category;
    }

    void setDownloadAllArticles(boolean downloadAllArticles) {
        this.downloadAllArticles = downloadAllArticles;
    }

    void setDownloadImages(boolean downloadImages) {
        this.downloadImages = downloadImages;
    }
}

