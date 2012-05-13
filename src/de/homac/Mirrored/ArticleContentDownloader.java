/*
 * ArticleContentDownloader.java
 *
 * Part of the Mirrored app for Android
 *
 * Copyright (C) 2010 Holger Macht <holger@homac.de>
 *
 * This file is released under the GPLv3.
 *
 */

package de.homac.Mirrored;

import android.util.DisplayMetrics;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArticleContentDownloader {

	private List<Article> _articles = null;

	private String TAG;

    private final boolean _downloadImages;
	private final boolean _downloadContent;

	private List<Article> downloadedArticles;

	public ArticleContentDownloader(Mirrored app, DisplayMetrics dm, List<Article> articles, boolean downloadContent,
			boolean downloadImages, boolean internetReady) {

		_articles = articles;
		_downloadImages = downloadImages;
		_downloadContent = downloadContent;
		downloadedArticles = Collections.synchronizedList(new ArrayList<Article>());

        TAG = app.APP_NAME + ", " + "ArticleContentDownloader";
	}

	public List<Article> download() {
		ArrayList<Thread> threads = new ArrayList<Thread>();
		for (Article article : _articles) {
			Thread thread = new Thread(new ArticleDownloadThread(article));
			thread.start();
			threads.add(thread);
		}

		// wait for all threads to finish
		try {
			for (Thread thread : threads) {
				thread.join();
			}
		} catch (InterruptedException e) {
			if (MDebug.LOG)
				Log.e(TAG, e.toString());
		}
		return downloadedArticles;
	}

	private class ArticleDownloadThread implements Runnable {

		private Article article;

		public ArticleDownloadThread(Article article) {
			this.article = article;
		}

		public void run() {
			try {
				if (_downloadContent) {
					article.downloadContent();
					if (_downloadImages) {
						article.downloadImage();
					}
				}
				downloadedArticles.add(article);
			} catch (ArticleDownloadException e) {
				Log.e(TAG,
						String.format("Could not fetch article '%s', statuscode was %s", article.getArticleUrl(0),
								e.getHttpCode()));
			}
		}
	}
}
