/*
 * Article.java
 *
 * Part of the Mirrored app for Android
 *
 * Copyright (C) 2010 Holger Macht <holger@homac.de>
 *
 * Support for multiple page articles by Christoph Robbert <crobbert@mail.upb.de>
 *
 * This file is released under the GPLv3.
 *
 */

package de.homac.Mirrored;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;

import android.util.Log;
import android.util.DisplayMetrics;
import android.content.Context;
import android.content.ContextWrapper;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

class Article extends Object {

	public String title = "";
	public String description = "";
	public URL image_url;
	public Bitmap image = null;
	public URL url;
	public String content = "";
	public String category = "";
	public String guid = "";

	static private final String ARTICLE_URL = "http://m.spiegel.de/article.do?emvAD=";
	static private final String TAG = "Mirrored," + "Article";

	public Article() {
	}

	public Article(String urlString) {
		try {
			this.url = new URL(urlString);

		} catch (MalformedURLException e) {
			if (MDebug.LOG)
				Log.e("Mirrored", e.toString());
		}

	}

	public Article(Article a) {
		title = a.title;
		url = a.url;
		image_url = a.image_url;
		description = a.description;
		content = a.content;
		category = a.category;
		image = a.image;
		guid = a.guid;
	}

	public String dateString() {
		if (guid == null || guid.length() == 0)
			return null;

		if (MDebug.LOG)
			Log.d(TAG, "dateString()");

		String date = guid.substring(guid.indexOf('_')+1);
		SimpleDateFormat format = new SimpleDateFormat("EEE MMM d HH:mm:ss 'UTC' yyyy",
							       Locale.ENGLISH);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));

		Date d = format.parse(date, new ParsePosition(0));
		if (d == null)
			return "";

		SimpleDateFormat format2 = new SimpleDateFormat("d. MMMM yyyy, HH:mm",
								Locale.getDefault());
		return format2.format(d);
	}

	private String _id() {
		if (guid.length() == 0 || guid == null) {
			String link = url.toString();
			int start_of_id = link.lastIndexOf("id=");

			if (start_of_id == -1) {
				if (MDebug.LOG)
					Log.e(TAG, "Couldn't calculate article id");
				return null;
			}

			int end_of_id = link.lastIndexOf("&");
			if (end_of_id == -1)
				end_of_id = link.length();

			return link.substring(start_of_id+3, end_of_id);
		}

		return guid.substring(0, guid.indexOf('_'));
	}

	private String _downloadContent(DisplayMetrics dm, boolean online, int page) {
		URL url = null;
		InputStream is = null;
		String s = "";

		try {
			url = new URL(ARTICLE_URL+ dm.widthPixels + "x" + dm.heightPixels +
				      "&id=" + _id() + "&p=" + page);
			if (MDebug.LOG)
				Log.d(TAG, "Downloading " + url.toString());

			is = url.openStream();

			BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8*1024);
			StringBuilder sb = new StringBuilder();
			String line = null;
			boolean hasNextPage = false;
			while ((line = reader.readLine()) != null) {
				if (line.contains("id=" + _id() + "&#x26;p=" + (page + 1)))
					hasNextPage = true;
				sb.append(line + "\n");
			}

			if (hasNextPage) {
				if (MDebug.LOG)
					Log.d(TAG, "Downloading next page");
				sb.append(this.getContent(dm, online, page+1));
			}
			is.close();
			s = sb.toString();

		} catch (MalformedURLException e) {
			if (MDebug.LOG)
				Log.e("Mirrored", e.toString());
		} catch (IOException e) {
			if (MDebug.LOG)
				Log.e("Mirrored", e.toString());
		}

		return s;
	}

	private Bitmap _downloadImage() {
		Bitmap bitmap = null;

		try {
			bitmap = BitmapFactory.decodeStream(image_url.openStream());
		} catch (IOException e) {
			if (MDebug.LOG)
				Log.e(TAG, e.toString());
		}

		return bitmap;
	}

	public void trimContent(boolean online) {
		if (MDebug.LOG)
			Log.d(TAG, "Trimming article content");

		int start, end;

		// cut everything starting with "Zum Thema" at the bottom of most articles...
		start = content.indexOf("<div>Zum Thema:");
		if (start != -1) {
			end = content.indexOf("</div></div></div></body></html>");
			content = content.substring(0, start-1) + content.substring(end, content.length());
		}
		// cut everything until '<div class="text mode1"', mostly ads
		start = content.indexOf("<p align=\"center\">");
		if (start != -1) {
			end = content.indexOf("</p>");
			content = content.substring(0, start-1) + content.substring(end+4, content.length());
		}
		////////////
		start = content.indexOf("<strong>MEHR ");
		if (start != -1) {
			end = content.indexOf("</div></div></div></body></html>");
			content = content.substring(0, start-1) + content.substring(end, content.length());
		}
		// Multiple page articles, remove the links to next/prev page
		start = content.indexOf("<strong>1</strong>");
		if (start != -1) {
			end = content.indexOf("</div></div></div></body></html>");
			content = content.substring(0, start-1) + content.substring(end, content.length());
		}
		start = content.indexOf("ZUR&#xdc;CK</span>");
		if (start != -1) {
			end = content.indexOf("</div></div></div></body></html>");
			content = content.substring(0, start-1) + content.substring(end, content.length());
		}

		//////////////
		// only do the following when not connected to the internet
		if (!online) {
			int i = 0;
			while ((start = content.indexOf("<img")) != -1) {
				end = content.indexOf('>', start);
				content = content.substring(0, start-1) + content.substring(end, content.length());
				i++;
			}
			if (MDebug.LOG)
				Log.d(TAG, "Replaced " + i + " occurences of <img...>");
		}
		///////////////////////

		//		content = content.replaceAll("ddp", "");
		content = content.replaceAll("FOTOSTRECKE", "");
		content = content.replaceAll("padding-top: .px;padding-bottom: .px;background-color: #ececec;", "");
		content = content.replaceAll("padding-top: 8px;background-color: #ececec;", "");
		content = content.replaceAll("background-color: #ececec;", "");
		content = content.replaceAll("Video abspielen", "");
		content = content.replaceAll("separator mode1 ", "");
		//		content = content.replaceAll("global.css", "sss");
		content = content.replaceAll("border-color: #ececec;border-style: solid;border-width: ..px;", "");
	}

	public String getContent(DisplayMetrics dm, boolean online, int page) {
		if (content != null && content.length() != 0) {
			if (MDebug.LOG)
				Log.d(TAG, "Article already has content, returning it");

			if (!online)
				trimContent(online);

			return content;
		} else {
			if (MDebug.LOG)
				Log.d(TAG, "Article doesn't have content, downloading and returning it");
			content = _downloadContent(dm, online, page);

			trimContent(online);

			return content;
		}
	}

	public String getContent(DisplayMetrics dm, boolean online) {
		return getContent(dm, online, 0);
	}

	public Bitmap getImage(boolean online) {
		if (!online)
			return image;

		if (image != null) {
			if (MDebug.LOG)
				Log.d(TAG, "Article already has image, returning it");

			return image;
		} else {
			if (MDebug.LOG)
				Log.d(TAG, "Article doesn't have image, downloading and returning it");

			if (image_url != null)
				image = _downloadImage();

			return image;
		}
	}

	public void resetContent() {
		content = "";
	}

}
