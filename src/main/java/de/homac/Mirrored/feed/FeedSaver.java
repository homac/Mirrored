/*
 * FeedSaver.java
 *
 * Part of the Mirrored app for Android
 *
 * Copyright (C) 2010 Holger Macht <holger@homac.de>
 *
 * This file is released under the GPLv3.
 *
 */

package de.homac.Mirrored.feed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import android.util.Log;
import android.os.Environment;

import de.homac.Mirrored.common.MDebug;
import de.homac.Mirrored.model.Article;

public class FeedSaver {

	static public final String SAVE_DIR = "/Android/data/de.homac.Mirrored/";

	private List<Article> articles;

	static private String TAG = "FeedSaver";

	static private final String FILENAME = "articles.xml";

	public FeedSaver(List<Article> articles) {
		this.articles = articles;
	}

	public boolean save() {
		FileOutputStream fos = null;
		String dirname = Environment.getExternalStorageDirectory().getAbsolutePath() + SAVE_DIR;
		File directory = new File(dirname);

		if (MDebug.LOG)
			Log.d(TAG, "Saving");

		if (!directory.exists())
			directory.mkdirs();

		File f = new File(dirname + FILENAME);

		if (storageReady()) {
			if (MDebug.LOG)
				Log.d(TAG, "SD card ready");
		} else {
			if (MDebug.LOG)
				Log.d(TAG, "SD card not ready");
			return false;
		}

		try {
			f.createNewFile();
		} catch (IOException e) {
			if (MDebug.LOG)
				Log.e(TAG, e.toString());
			throw new IllegalStateException("Failed to create " + f.toString());
		}
		try {
			fos = new FileOutputStream(f);

			fos.write(_startXML().getBytes());
			if (articles != null)
				for (Article article : articles) {
					fos.write(_articleXML(article).getBytes());
				}
			fos.write(_finishXML().getBytes());

		} catch (IOException e) {
			if (MDebug.LOG)
				Log.e(TAG, e.toString());
		} finally {
			if (fos != null) {
				try {
					fos.flush();
					fos.close();
				} catch (IOException e) {
					if (MDebug.LOG)
						Log.e(TAG, e.toString());
				}
			}
		}

		return true;
	}

	static public File read() {
		// String data = null;
		String dirname = Environment.getExternalStorageDirectory().getAbsolutePath() + SAVE_DIR;
		File f = new File(dirname + FILENAME);

		if (MDebug.LOG)
			Log.d(TAG, "Reading saved articles");

		if (!f.exists())
			return null;

		return f;
	}

	private String _startXML() {
		String o;

		o = "";
		o += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		o += "<rss version=\"2.0\">\n";
		o += " <channel>\n";
		o += "  <title>Spiegel Online News</title>\n";
		return o;
	}

	private String _articleXML(Article article) {
		String o = "";

		o += "\n";
		o += " <item>\n";
		o += "  <title>" + article.getTitle() + "</title>\n";
		o += "  <guid>" + article.getGuid() + "</guid>\n";
		o += "  <link>" + article.getUrl().toString() + "</link>\n";
		o += "  <description>" + article.getDescription() + "</description>\n";

		if (article.getFeedCategory() == null)
			article.setFeedCategory("");

		o += "  <category>" + article.getFeedCategory() + "</category>\n";
        if (article.getPubDate() != null) {
            o += "  <pubDate>" + RSSHandler.RSS822_DATE.format(article.getPubDate()) + "</pubDate>\n";
        }

		o += "  <content><![CDATA[" + article.getContent() + "]]></content>\n";
		o += " </item>\n";

		return o;
	}

	private String _finishXML() {
		String end = "";
		end += " </channel>\n";
		end += "</rss>\n";
		return end;
	}

	public static boolean storageReady() {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state))
			return true;

		return false;
	}
}
