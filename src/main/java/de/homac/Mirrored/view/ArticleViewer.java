/*
 * ArticleViewer.java
 *
 * Part of the Mirrored app for Android
 *
 * Copyright (C) 2010 Holger Macht <holger@homac.de>
 *
 * This file is released under the GPLv3.
 *
 */

package de.homac.Mirrored.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.view.MenuItem;
import android.view.Menu;
import android.util.Log;
import android.util.DisplayMetrics;
import android.os.Bundle;
import android.net.Uri;
import android.content.Intent;
import android.app.Activity;

import java.net.MalformedURLException;
import java.net.URL;

import de.homac.Mirrored.common.Helper;
import de.homac.Mirrored.common.MDebug;
import de.homac.Mirrored.common.Mirrored;
import de.homac.Mirrored.R;
import de.homac.Mirrored.model.Article;

public class ArticleViewer extends Activity {
    public static final String EXTRA_ARTICLE = "article";

    private final String TAG = "ArticleViewer";
    private Mirrored app;

    private boolean _online;
    private WebView _webview;
    private DisplayMetrics _dm;
    private Article article;

    @Override
    protected void onCreate(Bundle icicle) {
        app = (Mirrored) getApplication();

        super.onCreate(icicle);

        _online = app.online();

        if (MDebug.LOG)
            Log.d(TAG, "Setting content view");
        setContentView(R.layout.article_viewer);

        _webview = (WebView) findViewById(R.id.webview);

        article = app.getArticle();
        if (MDebug.LOG)
            Log.d(TAG, "Received article from application with title: " + article.getTitle());

        /* Add some debugging */
        if (article.getTitle() == null)
            Log.d(TAG, "Article title is null");

        if (article.getTitle() != null)
            setTitle(article.getTitle());
        _dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(_dm);

        // check if screen orientation changed and relead the article content
        Mirrored.Orientation newOrientation = _getOrientation(_dm);
        if (app.screenOrientation == null) { //first time
            app.screenOrientation = newOrientation;
        } else if (app.screenOrientation != newOrientation && _online) {
            if (MDebug.LOG)
                Log.d(TAG, "Screen orientation changed, redownloading article content");
            article.resetContent();
//			article.getContent(_dm, _online);
			app.screenOrientation = newOrientation;
		}

        _webview.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
        _webview.loadDataWithBaseURL(Helper.getBaseUrl(article.getUrl()), article.getContent(), "text/html", "utf-8", article.getUrl().toString());

        initActionBar();
    }

    private void initActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getActionBar().setHomeButtonEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onBackPressed() {
        if (_webview.canGoBack()) {
            _webview.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.article_viewer, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.setGroupVisible(R.id.group_online, _online);
        menu.setGroupVisible(R.id.group_legacy, Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
        return _online;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_shareArticle:
                Helper.shareUrl(this, article.getUrl());
                return true;

            case R.id.menu_saveArticle:
                if (MDebug.LOG)
                    Log.d(TAG, "MENU_SAVE_ARTICLE clicked");

                // get the content, just to be sure it has been downloaded, give false for internet state to
                // make sure article is trimmed
                if (!app.getOfflineFeed().getArticles().contains(article)) {
                    app.getOfflineFeed().getArticles().add(article);
                }
                app.saveOfflineFeed(this, null);
                return true;

            case R.id.menu_externalBrowser:
                if (MDebug.LOG) {
                    Log.d(TAG, "MENU_EXTERNAL_BROWSER clicked");
                }
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl().toString()));
                startActivity(intent);
                return true;

            case android.R.id.home:
            case R.id.menu_home:
                if (MDebug.LOG)
                    Log.d(TAG, "MENU_BACK_TO_ARTICLES_LIST clicked");
                finish();
                return true;
        }
        return false;
    }

    private Mirrored.Orientation _getOrientation(DisplayMetrics dm) {
        if (dm.heightPixels < dm.widthPixels) {
            return Mirrored.Orientation.HORIZONTAL;
        } else
            return Mirrored.Orientation.VERTICAL;
    }
}
