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
import android.util.Log;

import de.homac.Mirrored.feed.ArticleContentDownloader;
import de.homac.Mirrored.feed.Feed;
import de.homac.Mirrored.feed.FeedSaver;
import de.homac.Mirrored.R;
import de.homac.Mirrored.model.Article;

public class Mirrored extends Application {
    private static Mirrored instance;

	public String APP_NAME;

	private String TAG;

    private Article article;
	private Feed offlineFeed;
	private boolean _offline_mode = false;
    private CacheHelper cacheHelper;

    public enum Orientation { HORIZONTAL, VERTICAL }
	public Orientation screenOrientation = null;

	static public final String BASE_CATEGORY = "schlagzeilen";

    public static Mirrored getInstance() {
        return instance;
    }

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

    public CacheHelper getCacheHelper() {
        return cacheHelper;
    }

	@Override
	public void onCreate() {
        cacheHelper = new CacheHelper(getCacheDir());

		APP_NAME = getString(R.string.app_name);
		TAG = APP_NAME;
        MDebug.LOG = getPreferences().getBoolean("PrefEnableDebug", false);
		if (MDebug.LOG)
			Log.d(TAG, "starting");

		setOfflineMode(getPreferences().getBoolean("PrefStartWithOfflineMode", false));

        instance = this;
	}

	@Override
	public void onTerminate() {
        instance = null;
		if (MDebug.LOG)
			Log.d(TAG, "onTerminate()");
	}

	public boolean online() {
		if (_offline_mode)
			return false;

		ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
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

    public boolean wifiConnected() {
        if (_offline_mode)
            return false;

        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = (NetworkInfo)cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        boolean online = info.isConnected();
        if (MDebug.LOG)
            Log.d(TAG, "Wifi connected: " + online);
        return online;
    }

    public void setOfflineMode(boolean offline) {
		if (MDebug.LOG)
			Log.d(TAG, "Setting offline mode to "+offline);
		_offline_mode = offline;
	}

	public SharedPreferences getPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(getBaseContext());
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
            saver.setDownloadImages(getPreferences().getBoolean("PrefDownloadImages", true));
            Thread thread = new Thread(saver);
            thread.start();
        } else {
            Helper.showDialog(this, getString(R.string.error_saving));
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
