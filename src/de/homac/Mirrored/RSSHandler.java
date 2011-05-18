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

package de.homac.Mirrored;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.util.Log;

import java.io.InputStream;
import java.io.StringReader;
import java.io.ByteArrayInputStream;

public class RSSHandler extends DefaultHandler {

	private boolean _inItem = false;
	private boolean _inTitle = false;
	private boolean _inLink = false;
	private boolean _inDescription = false;
 	private boolean _inContent = false;
	private boolean _inCategory = false;
	private boolean _inGuid = false;

	// feed variables
	public String feedTitle = "";
	private URL _feedUrl;
	private String _article_url_string = "";

	protected Mirrored app;
	protected String TAG;

	// Feed and Article objects to use for temporary storage
	private Article _currentArticle;

	// The possible values for targetFlag
	private static final int TARGET_FEED = 0;
	private static final int TARGET_ARTICLES = 1;

	// A flag to know if looking for Articles or Feed name
	private int _targetFlag;
	private String _url;

	private ArrayList<Article> _articles = new ArrayList<Article>();

	public RSSHandler(Mirrored m, URL url, boolean online) {
		String feedString = null;
		InputStream i = null;
		InputSource is = null;
		File f = null;

		app = m;
		TAG = app.APP_NAME + ", " + "RSSHandler";

		_currentArticle = new Article(app);

		_feedUrl = url;

		// fetch and parse required feed content
		try {
			_targetFlag = TARGET_FEED;

			if (MDebug.LOG)
				Log.d(TAG, "Parsing feed " + url.toString());

			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			xr.setContentHandler(this);

			if (online) {
				i = (InputStream)url.openStream();
				// copy this inputstream so we only have to download the feed once
				feedString = app.convertStreamToString(i);
				is = new InputSource(new ByteArrayInputStream(feedString.getBytes()));
				sp.parse(is, this);

			} else {
				f = FeedSaver.read();
				if (f != null)
					sp.parse(f, this);
			}

		} catch (UnknownHostException e) {
			if (MDebug.LOG)
				Log.e(TAG, "Feed currently not available: " + e.toString());
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

		// fetch and parse articles
		try {
			if (MDebug.LOG)
				Log.d(TAG, "Feed title: "+feedTitle);
			_targetFlag = TARGET_ARTICLES;

			if (MDebug.LOG)
				Log.d(TAG, "Parsing articles");

			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			xr.setContentHandler(this);

			if (online) {
				is = new InputSource(new ByteArrayInputStream(feedString.getBytes()));
				sp.parse(is, this);
			} else {
				if (f != null)
					sp.parse(f, this);
			}
			
		} catch (IOException e) {
			if (MDebug.LOG)
				Log.e(TAG, e.toString());
		} catch (SAXException e) {
			if (MDebug.LOG)
				Log.e(TAG, e.toString());
		} catch (ParserConfigurationException e) {
			if (MDebug.LOG)
				Log.e(TAG, e.toString());
		}

		if (MDebug.LOG)
			Log.d(TAG, "Found " + _articles.size() + " articles");
	}

	public void startElement(String uri, String name, String qName, Attributes atts) {

		if (name.trim().equals("title"))
			_inTitle = true;
		else if (name.trim().equals("item"))
			_inItem = true;
		else if (name.trim().equals("link"))
			_inLink = true;
		else if (name.trim().equals("description"))
			_inDescription = true;
		else if (name.trim().equals("enclosure")) {

			for (int i = 0; i < atts.getLength(); i++) {
				try {
					String n = atts.getLocalName(i);
					String v = atts.getValue(i); 
					if (n == "url")
						_currentArticle.image_url = new URL(v);

				} catch (MalformedURLException e) {
					if (MDebug.LOG)
						Log.e(TAG, e.toString());
				}
			}
		} else if (name.trim().equals("content"))
			_inContent = true;
		else if (name.trim().equals("category"))
			_inCategory = true;
		else if (name.trim().equals("guid"))
			_inGuid = true;
	}

	public void endElement(String uri, String name, String qName)
		throws SAXException {

		if (name.trim().equals("title"))
			_inTitle = false;
		else if (name.trim().equals("item"))
			_inItem = false;
		else if (name.trim().equals("link"))
			_inLink = false;
		else if (name.trim().equals("description"))
			_inDescription = false;
		else if (name.trim().equals("content"))
			_inContent = false;
		else if (name.trim().equals("category"))
			_inCategory = false;
		else if (name.trim().equals("guid"))
			_inGuid = false;

		// Check if looking for feed, and if feed is complete // not needed csurrently
		if (_targetFlag == TARGET_FEED && _feedUrl != null && feedTitle.length() > 0) {

			if (MDebug.LOG)
				Log.d(TAG, "Feed parsing finished");
			throw new SAXException();
		}

		// Check if looking for article, and if article is complete
		if (_targetFlag == TARGET_ARTICLES && _article_url_string.length() > 0
		    && _currentArticle.title.length() > 0 && !_inItem && _currentArticle.description.length() > 0) {

			try {
				_currentArticle.url = new URL(_article_url_string);

			} catch (MalformedURLException e) {
				if (MDebug.LOG)
					Log.e(TAG, e.toString());
			}

			Article article = new Article(_currentArticle);
			if (_currentArticle.category == null || _currentArticle.category.length() == 0)
				article.category = category();

			_articles.add(article);
			if (MDebug.LOG)
				Log.d(TAG, "SAX, added article with title: " + _currentArticle.title);

			// reset current Article
			_currentArticle.title = "";
			_currentArticle.url = null;
			_currentArticle.image_url = null;
			_currentArticle.description = "";
			_currentArticle.guid = "";
			// don't change to null, otherwise the first word in the article will be "null"
			_currentArticle.content = "";
			_currentArticle.category = "";
			_article_url_string = "";

		}
	}

	public void characters(char ch[], int start, int length) {

		String chars = (new String(ch).substring(start, start + length));

			if (!_inItem) {
				if (_inTitle)
					feedTitle += chars;
			} else {
				if (_inLink) {
					_article_url_string += chars;
				}if (_inTitle) {
					_currentArticle.title += chars;
				}
				if (_inDescription)
					_currentArticle.description += chars;
				if (_inContent) {
					// this comes in chunks, so append to content
					_currentArticle.content += chars;
				}
				if (_inCategory) {
					_currentArticle.category += chars.toLowerCase();
				}
				if (_inGuid) {
					_currentArticle.guid += chars;
				}
			}
	}

	public ArrayList getArticles() {
		return _articles;
	}

	public String category() {
		String[] tSplit = _feedUrl.toString().split("/");
		if (tSplit.length != 5)
			return app.BASE_CATEGORY;
		Log.i(TAG, "category = " + tSplit[3]);
		return tSplit[3];
	}
}
