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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;

class Article extends Object {

	public String title = "";
	public String description = "";
	public URL image_url;
	public Bitmap image = null;
	public URL url;
	public String content = "";
	public String feedCategory = "";
	public String guid = "";

	public Mirrored app;

	private Pattern idPattern = Pattern.compile("a-([0-9]+).");

	static private final String ARTICLE_URL = "http://m.spiegel.de/";
	static private final String TAG = "Mirrored," + "Article";
	private static final String TEASER = "<p id=\"spIntroTeaser\">";
	private static final String CONTENT = "<div class=\"spArticleContent\"";

	public Article(Mirrored app) {
		this.app = app;
	}

	public Article(Mirrored app, String urlString) {
		try {
			this.app = app;
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
		feedCategory = a.feedCategory;
		image = a.image;
		guid = a.guid;
		app = a.app;
	}

	public String dateString() {
		if (guid == null || guid.length() == 0)
			return null;

		if (MDebug.LOG)
			Log.d(TAG, "dateString()");

		String date = guid.substring(guid.indexOf('_') + 1);
		SimpleDateFormat format = new SimpleDateFormat("EEE MMM d HH:mm:ss 'UTC' yyyy", Locale.ENGLISH);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));

		Date d = format.parse(date, new ParsePosition(0));
		if (d == null)
			return "";

		SimpleDateFormat format2 = new SimpleDateFormat("d. MMMM yyyy, HH:mm", Locale.getDefault());
		return format2.format(d);
	}

	private String _id() {
		if (guid == null || guid.length() == 0) {
			return null;
		}
		Matcher matcher = idPattern.matcher(guid);
		if (matcher .find() && matcher.groupCount() == 1) {
            return matcher.group(1);
        }
		return null;
	}

	private String _downloadContent( int page) throws ArticleDownloadException {
		StringBuilder sb = new StringBuilder();
		try {

			URL url = new URL(getArticleUrl(page));
			if (MDebug.LOG)
				Log.d(TAG, "Downloading " + url.toString());

			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.connect();
			int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                Log.e(TAG, String.format("Could not download url '%s'. Errorcode is:  %s.", url, responseCode));
                throw new ArticleDownloadException(responseCode);
            }

			Log.d(TAG, String.format("Response code is %s", responseCode));
			InputStream is = urlConnection.getInputStream();

			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"), 8 * 1024);

			sb.append(extractArticleContent(reader, page > 1));
			String line;
			boolean couldHasNext = false;
			while ((line = reader.readLine()) != null) {
				if (line.contains("<li class=\"spMultiPagerLink\">")) {
					couldHasNext = true;
				} else if (couldHasNext && line.contains(">WEITER</a>")) {
					Log.d(TAG, "Downloading next page");
					sb.append(this.downloadContent(page + 1));
				}
			}
			is.close();
		} catch (MalformedURLException e) {
			if (MDebug.LOG)
				Log.e("Mirrored", e.toString());
		} catch (IOException e) {
			if (MDebug.LOG)
				Log.e("Mirrored", e.toString());
		}
		if (page == 1) {
			sb.append("</body></html>");
		}

		return sb.toString();
	}

	public String getArticleUrl(int page) {
		return ARTICLE_URL + _categories() + "/a-" + _id() + (page > 1 ? "-" + page : "") + ".html";
	}

	private String _categories() {
		if (guid == null || guid.length() == 0) {
			return "";
		}
		String split[] = guid.toString().split("/");
		if (split.length == 6) {
			return split[3] + "/" + split[4];
		} else if (split.length == 5) {
			return split[3];
		}
		if (MDebug.LOG)
			Log.e(TAG, "Couldn't calculate category");
		return "";
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

	private String extractArticleContent(BufferedReader reader, boolean skipTeaser) throws IOException {
		StringBuilder text = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null && !(line.contains(CONTENT))) {
			line = line.trim();
			if (!skipTeaser) {
				if (line.contains("<head>") || line.startsWith("<link") || line.contains("<meta")) {
					text.append(line);
				}
			}
			continue;
		}
		text.append(line.substring(line.indexOf(CONTENT)));

		while (((line = reader.readLine()) != null) && !(line.contains(TEASER))) {
			if (!skipTeaser) {
				text.append(line);
			}
			continue;
		}
		text.append(line.substring(line.indexOf(TEASER)));

		int diffCount = 1;
		while (((line = reader.readLine()) != null) && diffCount > 0) {
			diffCount -= countTag(line, "</div>");
			if (diffCount == 1) {
				// skip inner diffs -> fotostrecke, etc
				text.append(line);
			}
			if (diffCount > 0) {
				diffCount += countTag(line, "<div");
			}
		}
		if (line.contains("</div>")) {
			text.append(line.substring(0, line.lastIndexOf("</div>")));
		}
		text.append("</div>");
		return text.toString();
	}

	private int countTag(String line, String tag) {
		int tagCount = 0;
		String tLine = line.trim();
		while (tLine.length() > 0 && tLine.contains(tag)) {
			tagCount++;
			tLine = tLine.substring(tLine.indexOf(tag) + tag.length());
		}
		return tagCount;
	}

    public String downloadContent(int page) throws ArticleDownloadException {
		if (content != null && content.length() != 0) {
			if (MDebug.LOG)
				Log.d(TAG, "Article already has content, returning it");
			return content;
		} else {
			if (MDebug.LOG)
				Log.d(TAG, "Article doesn't have content, downloading and returning it");
			content = _downloadContent(page);
			return content;
		}
	}

	public String downloadContent() throws ArticleDownloadException {
		return downloadContent( 1);
	}

    public String getContent() {
        return content;
    }

    public Bitmap downloadImage() {
        if (image_url != null)
       				image = _downloadImage();
       			return image;
    }

	public Bitmap getImage(boolean online) {
			return image;
	}

	public void resetContent() {
		content = "";
	}

}
