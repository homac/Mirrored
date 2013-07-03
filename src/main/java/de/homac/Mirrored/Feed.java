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

	public Feed(URL url, boolean online, String feedCategory) {
		super(url, online, feedCategory);
	}

	// only return those articles with a specific feedCategory
	public List<Article> getArticles(String category) {
		List<Article> articles = new ArrayList();
		List<Article> all_articles = getArticles();

		if (category.equals(Mirrored.BASE_CATEGORY))
			return all_articles;

		for (Article article : all_articles)
			if (article.getFeedCategory().equals(category))
				articles.add(article);

		return articles;
	}

    @Override
    protected void addArticle(Article currentArticle) {
        // ugly hack: We currently don't support
        // "Fotostrecke and Videos:", so don't add article if it is one
        if (!currentArticle.getUrl().toString().contains("/fotostrecke/")
                && !currentArticle.getUrl().toString().contains("/video/")) {
            super.addArticle(currentArticle);
        }
    }
}
