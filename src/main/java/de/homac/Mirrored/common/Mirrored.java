/*
 * Mirrored.java
 *
 * Part of the Mirrored app for Android
 *
 * Copyright (C) 2010 Holger Macht <holger@homac.de>
 *
 * This file is released under the GPLv3.
 *
 */

package de.homac.Mirrored.common;

import android.app.Application;
import android.app.ProgressDialog;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.StringBuilder;

import de.homac.Mirrored.feed.ArticleContentDownloader;
import de.homac.Mirrored.feed.Feed;
import de.homac.Mirrored.feed.FeedSaver;
import de.homac.Mirrored.R;
import de.homac.Mirrored.model.Article;

public class Mirrored extends Application {

	public String APP_NAME;

	private String TAG;

    private Article article;
	private Feed offlineFeed;
	private boolean _offline_mode = false;

    public enum Orientation { HORIZONTAL, VERTICAL }
	public Orientation screenOrientation = null;

	static public final String BASE_CATEGORY = "schlagzeilen";

    public Article getArticle() {
        return article;
    }

    public void setArticle(Article article) {
        this.article = article;
    }

    public Feed getOfflineFeed() {
        return offlineFeed;
    }

    public void setOfflineFeed(Feed offlineFeed) {
        this.offlineFeed = offlineFeed;
    }

	@Override
	public void onCreate() {

		APP_NAME = getString(R.string.app_name);
		TAG = APP_NAME;
        MDebug.LOG = getBooleanPreference("PrefEnableDebug", false);
		if (MDebug.LOG)
			Log.d(TAG, "starting");

		setOfflineMode(getBooleanPreference("PrefStartWithOfflineMode", false));
	}

	@Override
	public void onTerminate() {
		if (MDebug.LOG)
			Log.d(TAG, "onTerminate()");
	}

	public boolean online() {
		if (_offline_mode)
			return false;

		ConnectivityManager cm = (ConnectivityManager)this
			.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = (NetworkInfo)cm.getActiveNetworkInfo();
		boolean online = true;

		if (info == null || !info.isConnectedOrConnecting()){
			online = false;
		}

		if (info != null && info.isRoaming()){
			//here is the roaming option you can change it if you want to disable
			//internet while roaming, just return false
			online = false;
		}

		if (online) {
			if (MDebug.LOG)
				Log.d(TAG, "Internet state: online");
		} else {
			if (MDebug.LOG)
				Log.d(TAG, "Internet state: offline");
		}

		return online;
	}

	public static String convertStreamToString(InputStream is) throws IOException {
		/*
		 * To convert the InputStream to String we use the BufferedReader.readLine()
		 * method. We iterate until the BufferedReader return null which means
		 * there's no more data to read. Each line will appended to a StringBuilder
		 * and returned as String.
		 */
		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"), 8*1024);
				while ((line = reader.readLine()) != null) {
					sb.append(line).append("\n");
				}
			} finally {
				is.close();
			}
			return sb.toString();
		} else {
			return "";
		}
	}

	public void setOfflineMode(boolean offline) {
		if (MDebug.LOG)
			Log.d(TAG, "Setting offline mode to "+offline);
		_offline_mode = offline;
	}

	public boolean getBooleanPreference(String pref, boolean def) {
		SharedPreferences prefs = PreferenceManager
			.getDefaultSharedPreferences(getBaseContext());

		return prefs.getBoolean(pref, def);
	}

	public String getStringPreference(String pref, String def) {
		SharedPreferences prefs = PreferenceManager
			.getDefaultSharedPreferences(getBaseContext());
		return prefs.getString(pref, def);
	}

	public int getIntPreference(String pref, int def) {
		SharedPreferences prefs = PreferenceManager
			.getDefaultSharedPreferences(getBaseContext());

		return prefs.getInt(pref, def);
	}

    public void showDialog(Context context, String text) {
         showDialog(context,text,false);
    }

	public void showDialog(Context  context, String text, boolean formatted) {
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        if (formatted) {
            alertDialog.setMessage(Html.fromHtml(text));
        } else {
            alertDialog.setMessage(text);
        }
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					return;
				} });
		alertDialog.show();
	}

    public void saveOfflineFeed(Context context, final Handler successHandler) {
        if (FeedSaver.storageReady()) {
            final ProgressDialog pdialog = ProgressDialog.show(context, "",
                    getString(R.string.progress_dialog_save_all), true,
                    false);

            ArticleSaver saver = new ArticleSaver(new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    pdialog.dismiss();
                    if (successHandler != null) {
                        successHandler.sendEmptyMessage(0);
                    }
                }
            });
            saver.setFeed(offlineFeed);
            saver.setDownloadImages(getBooleanPreference("PrefDownloadImages", true));
            Thread thread = new Thread(saver);
            thread.start();
        } else {
            showDialog(this, getString(R.string.error_saving));
        }
    }
}

class ArticleSaver implements Runnable {
    private Handler handler;
    private Feed feed;
    private boolean downloadImages;

    public ArticleSaver(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {
        ArticleContentDownloader downloader = new ArticleContentDownloader(feed.getArticles(), true, downloadImages);
        downloader.download();

        FeedSaver saver = new FeedSaver(feed.getArticles());
        saver.save();

        handler.sendEmptyMessage(0);
    }

    public void setFeed(Feed feed) {
        this.feed = feed;
    }

    public void setDownloadImages(boolean downloadImages) {
        this.downloadImages = downloadImages;
    }
}
