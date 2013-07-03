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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Article {
    static private final String TAG = "Mirrored," + "Article";

	private String title = "";
    private String description = "";
    private URL url;
    private URL thumbnailImageUrl;
    private Bitmap thumbnailImage = null;
    private String content = "";
    private String feedCategory = "";
    private String guid = "";
    private Date pubDate = null;

    String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    URL getUrl() {
        return url;
    }

    void setUrl(URL url) {
        this.url = url;
    }

    URL getThumbnailImageUrl() {
        return thumbnailImageUrl;
    }

    void setThumbnailImageUrl(URL thumbnailImageUrl) {
        this.thumbnailImageUrl = thumbnailImageUrl;
    }

    Bitmap getThumbnailImage() {
        return thumbnailImage;
    }

    void setThumbnailImage(Bitmap thumbnailImage) {
        this.thumbnailImage = thumbnailImage;
    }

    String getContent() {
        return content;
    }

    void setContent(String content) {
        this.content = content;
    }

    String getFeedCategory() {
        return feedCategory;
    }

    void setFeedCategory(String feedCategory) {
        this.feedCategory = feedCategory;
    }

    String getGuid() {
        return guid;
    }

    void setGuid(String guid) {
        this.guid = guid;
    }

    Date getPubDate() {
        return pubDate;
    }

    void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    public String pubDateString() {
		if (pubDate == null) {
			return "";
		}

        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
		return format.format(pubDate);
	}

	public void resetContent() {
		content = "";
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Article article = (Article) o;

        if (!guid.equals(article.guid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return guid.hashCode();
    }
}
