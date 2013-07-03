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

package de.homac.Mirrored;

import java.util.ArrayList;
import java.util.List;

import android.webkit.WebViewClient;
import android.widget.Toast;
import android.webkit.WebView;
import android.view.MenuItem;
import android.view.Menu;
import android.view.KeyEvent;
import android.view.Gravity;
import android.util.Log;
import android.util.DisplayMetrics;
import android.text.Spanned;
import android.text.Html;
import android.os.Bundle;
import android.net.Uri;
import android.content.Intent;
import android.app.Activity;

public class ArticleViewer extends Activity {
    public static final String EXTRA_ARTICLE = "article";

    private final String TAG = "ArticleViewer";
    private Mirrored app;

    static final int MENU_SAVE_ARTICLE = 0;
    static final int MENU_EXTERNAL_BROWSER = 1;
    static final int MENU_BACK_TO_ARTICLES_LIST = 2;

    private boolean _online;
    private WebView _webview;
    private DisplayMetrics _dm;
    private Article article;

    static final String BASE_URL = "http://m.spiegel.de/";

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

        _webview.loadDataWithBaseURL(BASE_URL, article.getContent(), "text/html", "utf-8", null);
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
        super.onCreateOptionsMenu(menu);

        if (_online) {
            menu.add(Menu.NONE, MENU_SAVE_ARTICLE, Menu.NONE, R.string.menu_save_article)
                    .setIcon(android.R.drawable.ic_menu_save);
            menu.add(Menu.NONE, MENU_EXTERNAL_BROWSER, Menu.NONE, R.string.menu_external_browser)
                    .setIcon(android.R.drawable.ic_menu_view);
            menu.add(Menu.NONE, MENU_BACK_TO_ARTICLES_LIST, Menu.NONE,
                    R.string.menu_back_to_articles_list).setIcon(R.drawable.ic_menu_back);
        }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case MENU_SAVE_ARTICLE:
                if (MDebug.LOG)
                    Log.d(TAG, "MENU_SAVE_ARTICLE clicked");

                // get the content, just to be sure it has been downloaded, give false for internet state to
                // make sure article is trimmed
                if (!app.getOfflineFeed().getArticles().contains(article)) {
                    app.getOfflineFeed().getArticles().add(article);
                }
                app.saveOfflineFeed(this, null);
                return true;

            case MENU_EXTERNAL_BROWSER:
                if (MDebug.LOG) {
                    Log.d(TAG, "MENU_EXTERNAL_BROWSER clicked");
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl().toString()));
                startActivity(intent);
                return true;

            case MENU_BACK_TO_ARTICLES_LIST:
                if (MDebug.LOG)
                    Log.d(TAG, "MENU_BACK_TO_ARTICLES_LIST clicked");
                this.finish();
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
