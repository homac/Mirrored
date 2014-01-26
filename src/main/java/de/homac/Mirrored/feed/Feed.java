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

package de.homac.Mirrored.feed;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import de.homac.Mirrored.common.Mirrored;
import de.homac.Mirrored.model.Article;

public class Feed {
    private ArrayList<Article> articles = new ArrayList<Article>();
    private String feedCategory;
    private URL feedUrl;

	public Feed(URL feedUrl, String feedCategory) {
        this.feedUrl = feedUrl;
        this.feedCategory = feedCategory;
	}

    public List<Article> getArticles() {
        return articles;
    }

	// only return those articles with a specific feedCategory
	public List<Article> getArticles(String category) {
		if (category.equals(Mirrored.BASE_CATEGORY))
			return articles;

        List<Article> result = new ArrayList<Article>();
        for (Article article : articles)
			if (article.getFeedCategory().equals(category))
                result.add(article);
		return result;
	}

    public void addArticle(Article currentArticle) {
        // ugly hack: We currently don't support
        // "Fotostrecke and Videos:", so don't add article if it is one
        if (!currentArticle.getUrl().toString().contains("/fotostrecke/")
                && !currentArticle.getUrl().toString().contains("/video/")
                && currentArticle.getUrl().getHost().equals(feedUrl.getHost())) {
            articles.add(currentArticle);
        }
    }

    public String getFeedCategory() {
        return feedCategory;
    }

    public URL getFeedUrl() {
        return feedUrl;
    }
}
