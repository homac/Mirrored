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
			Thread thread = new Thread(new ArticleDownloadThread(article, downloadContent, downloadImages));
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

class ArticleDownloadThread implements Runnable {
    private final String TAG = "ArticleDownloadThread";

    private Article article;
    private boolean downloadContent;
    private boolean downloadImages;

    public ArticleDownloadThread(Article article, boolean downloadContent, boolean downloadImages) {
        this.article = article;
        this.downloadContent = downloadContent;
        this.downloadImages = downloadImages;
    }

    public void run() {
        SpiegelOnlineDownloader downloader = new SpiegelOnlineDownloader(article);
        try {
            if (downloadContent) {
                downloader.downloadContent(downloadImages);
            }
            if (downloadImages) {
                downloader.downloadThumbnailImage();
            }
        } catch (ArticleDownloadException e) {
            Log.e(TAG,
                    String.format("Could not fetch article '%s', statuscode was %s", article.getUrl(),
                            e.getHttpCode()));
        }
    }

}

