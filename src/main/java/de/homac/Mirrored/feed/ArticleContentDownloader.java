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

package de.homac.Mirrored.feed;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.homac.Mirrored.common.MDebug;
import de.homac.Mirrored.model.Article;
import de.homac.Mirrored.provider.SpiegelOnlineDownloader;

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
        if (MDebug.LOG)
            Log.d(TAG, "Loading " + articles.size() + " articles; downloadContent = " + downloadContent + ", downloadImages = " + downloadImages);
		ArrayList<Thread> threads = new ArrayList<Thread>();
		for (Article article : articles) {
            ArticleDownloadThread loader = new ArticleDownloadThread(article, null);
            loader.setDownloadImages(downloadImages);
            loader.setDownloadContent(downloadContent);

			Thread thread = new Thread(loader);
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
}
