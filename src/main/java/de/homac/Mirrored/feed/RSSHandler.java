/*
 * RSSHandler.java
 *
 * Part of the Mirrored app for Android
 *
 * Copyright (C) 2010 Holger Macht <holger@homac.de>
 *
 * This file is released under the GPLv3.
 *
 */

package de.homac.Mirrored.feed;

import android.util.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import de.homac.Mirrored.common.Mirrored;
import de.homac.Mirrored.common.MDebug;
import de.homac.Mirrored.model.Article;

public class RSSHandler extends DefaultHandler {
    public static final DateFormat RSS822_DATE = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

	// feed variables
	protected final String TAG = "RSSHandler";

	// Feed and Article objects to use for temporary storage
	private Article _currentArticle;

	private ArrayList<Article> _articles = new ArrayList<Article>();
	private StringBuffer stringBuffer;
	private String feedCategory;

	public RSSHandler(URL url, boolean online, String feedCategory) {
		stringBuffer = new StringBuffer();
		this.feedCategory = feedCategory;

		_currentArticle = new Article();
		// fetch and parse required feed content
		try {
			if (MDebug.LOG) {
				Log.d(TAG, "Parsing feed " + url.toString());
			}
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser tParser = spf.newSAXParser();
			XMLReader tReader = tParser.getXMLReader();
			tReader.setContentHandler(this);
			if (online) {
				String feedString = Mirrored.convertStreamToString(url.openStream());
				tParser.parse(new ByteArrayInputStream(feedString.getBytes()),
						this);
			} else {
				File tFile = FeedSaver.read();
				if (tFile != null) {
					tParser.parse(tFile, this);
				}
			}
		} catch (UnknownHostException e) {
			if (MDebug.LOG) {
				Log.e(TAG, "Feed currently not available: " + e.toString());
			}
			return;
		} catch (FileNotFoundException e) {
			if (MDebug.LOG)
				Log.e(TAG, "Feed currently not available: " + e.toString());
			return;
		} catch (IOException e) {
			if (MDebug.LOG)
				Log.e(TAG, e.toString());
		} catch (SAXException e) {
			Log.e(TAG, e.toString());
		} catch (ParserConfigurationException e) {
			if (MDebug.LOG)
				Log.e(TAG, e.toString());
		}
		if (MDebug.LOG)
			Log.d(TAG, "Found " + _articles.size() + " articles");
	}

	public void startElement(String uri, String name, String qName,
			Attributes atts) {
		if (name.trim().equals("item")) {
			_currentArticle = new Article();
		} else if (name.trim().equals("enclosure")) {
			for (int i = 0; i < atts.getLength(); i++) {
				try {
					String n = atts.getLocalName(i);
					String v = atts.getValue(i);
					if (n.equals("url")) {
						_currentArticle.setThumbnailImageUrl(new URL(v));
					}
				} catch (MalformedURLException e) {
					if (MDebug.LOG)
						Log.e(TAG, e.toString());
				}
			}
		}
	}

	public void endElement(String uri, String name, String qName)
			throws SAXException {
		String tString = stringBuffer.toString().trim();
		if (name.trim().equals("title")) {
			_currentArticle.setTitle(tString);
		} else if (name.trim().equals("link")) {
			try {
				_currentArticle.setUrl(new URL(tString));
			} catch (MalformedURLException e) {
				if (MDebug.LOG) {
					Log.e(TAG, e.toString());
				}
			}
		} else if (name.trim().equals("description")) {
			_currentArticle.setDescription(tString);
		} else if (name.trim().equals("content")) {
			_currentArticle.setContent(tString);
		} else if (name.trim().equals("category")) {
			_currentArticle.setFeedCategory(tString.toLowerCase());
		} else if (name.trim().equals("guid")) {
			_currentArticle.setGuid(tString);
		} else if (name.trim().equals("pubDate")) {
            try {
                _currentArticle.setPubDate(RSS822_DATE.parse(tString));
            } catch (ParseException e) {
            }
        } else if (name.trim().equals("item")) {
			// Subfeeds e.g. netzwelt oder kultur have no category tag, so
			// calculate it from url
			if (_currentArticle.getFeedCategory() == null
					|| _currentArticle.getFeedCategory().length() == 0) {
				Log.w(TAG, "category of " + _currentArticle.getTitle()
						+ " ist empty");
				_currentArticle.setFeedCategory(feedCategory);
			}
			// feedCategory;
			// Check if looking for article, and if article is complete
			if (_currentArticle.getUrl() != null
					&& _currentArticle.getTitle().length() > 0
					&& _currentArticle.getDescription().length() > 0) {
				if (_currentArticle.getUrl() != null) {
                    addArticle(_currentArticle);
                    if (MDebug.LOG) {
                        Log.d(TAG, "SAX, added article with title: "
                                + _currentArticle.getTitle());
                    }
				}
				_currentArticle = null;
			}
		}
		stringBuffer = new StringBuffer();
	}

    protected void addArticle(Article currentArticle) {
        _articles.add(_currentArticle);
    }

    public void characters(char ch[], int start, int length) {
		stringBuffer.append(ch, start, length);
	}

	public List<Article> getArticles() {
		return _articles;
	}
}
