/*
 * Feed.java
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

class Feed extends RSSHandler {

	public Feed(URL url, boolean online) {
		super(url, online);
	}

	// only return those articles with a specific feedCategory
	public List<Article> getArticles(String category) {
		List<Article> articles = new ArrayList();
		List<Article> all_articles = getArticles();

		if (category.equals(Mirrored.BASE_CATEGORY))
			return all_articles;

		for (Article article : all_articles)
			if (article.feedCategory.equals(category))
				articles.add(article);

		return articles;
	}
}
