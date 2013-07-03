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

package de.homac.Mirrored.model;

import android.graphics.Bitmap;

import java.net.URL;
import java.text.DateFormat;
import java.util.Date;

public class Article {
	private String title = "";
    private String description = "";
    private URL url;
    private URL thumbnailImageUrl;
    private Bitmap thumbnailImage = null;
    private String content = "";
    private String feedCategory = "";
    private String guid = "";
    private Date pubDate = null;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public URL getThumbnailImageUrl() {
        return thumbnailImageUrl;
    }

    public void setThumbnailImageUrl(URL thumbnailImageUrl) {
        this.thumbnailImageUrl = thumbnailImageUrl;
    }

    public Bitmap getThumbnailImage() {
        return thumbnailImage;
    }

    public void setThumbnailImage(Bitmap thumbnailImage) {
        this.thumbnailImage = thumbnailImage;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFeedCategory() {
        return feedCategory;
    }

    public void setFeedCategory(String feedCategory) {
        this.feedCategory = feedCategory;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public Date getPubDate() {
        return pubDate;
    }

    public void setPubDate(Date pubDate) {
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
