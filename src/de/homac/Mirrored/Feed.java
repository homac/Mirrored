/*
 * Feed.java
 *
 * Part of the Mirrored app for Android
 *
 * Copyright (C) 2010 Holger Macht <holger@homac.de>
 *
 * This file is released under the GPLv2.
 *
 */

package de.homac.Mirrored;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;

import android.util.Log;

class Feed extends RSSHandler {

	public Feed(Mirrored m, URL url, boolean online) {
		super(m, url, online);
	}

	// only return those articles with a specific category
	public ArrayList getArticles(String category) {
		ArrayList<Article> articles;
		ArrayList<Article> all_articles = getArticles();

		if (category.equals(app.BASE_CATEGORY))
			return all_articles;

		if (all_articles == null) {
			Log.d(TAG, "No articles");
			return null;
		}

		articles = new ArrayList();

		for (Article article : all_articles)
			if (article.category.equals(category))
				articles.add(article);

		return articles;
	}
}
