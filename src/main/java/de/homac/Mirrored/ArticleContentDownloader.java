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

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArticleContentDownloader {

	private List<Article> articles = null;

	private final String TAG = "ArticleContentDownloader";

    private final boolean downloadImages;
	private final boolean downloadContent;

	public ArticleContentDownloader(List<Article> articles, boolean downloadContent,
			boolean downloadImages) {

		this.articles = articles;
		this.downloadImages = downloadImages;
		this.downloadContent = downloadContent;
	}

	public void download() {
		ArrayList<Thread> threads = new ArrayList<Thread>();
		for (Article article : articles) {
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
	}

	private class ArticleDownloadThread implements Runnable {

		private Article article;

		public ArticleDownloadThread(Article article) {
			this.article = article;
		}

		public void run() {
			try {
				if (downloadContent) {
				    article.downloadContent(downloadImages);
                }
                if (downloadImages) {
                    article.downloadThumbnailImage();
                }
            } catch (ArticleDownloadException e) {
				Log.e(TAG,
						String.format("Could not fetch article '%s', statuscode was %s", article.getArticleUrl(0),
								e.getHttpCode()));
			}
		}
	}
}
