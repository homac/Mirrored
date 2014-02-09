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
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.homac.Mirrored.common.IOHelper;
import de.homac.Mirrored.common.MDebug;
import de.homac.Mirrored.model.Article;

public class RSSHandler extends DefaultHandler {
    public static final DateFormat RSS822_DATE = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

	// feed variables
	protected static final String TAG = "RSSHandler";

	// Feed and Article objects to use for temporary storage
    private Feed feed;
	private Article _currentArticle;
	private StringBuffer stringBuffer;

	public RSSHandler() {
    }

    public Feed download(URL url, boolean online, String feedCategory) {
        feed = new Feed(url, feedCategory);
		stringBuffer = new StringBuffer();

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
				String feedString = IOHelper.toString(url, "ISO-8859-1");
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
			return feed;
		} catch (FileNotFoundException e) {
			if (MDebug.LOG)
				Log.e(TAG, "Feed currently not available: " + e.toString());
			return feed;
		} catch (IOException e) {
			if (MDebug.LOG)
				Log.e(TAG, String.format("Failed to download feed '%s'", feed.getFeedUrl()), e);
		} catch (SAXException e) {
            if (MDebug.LOG)
			    Log.e(TAG, e.toString());
		} catch (ParserConfigurationException e) {
			if (MDebug.LOG)
				Log.e(TAG, e.toString());
		}
		if (MDebug.LOG)
			Log.d(TAG, "Found " + feed.getArticles().size() + " articles");
        return feed;
	}

    @Override
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

    @Override
	public void endElement(String uri, String name, String qName)
			throws SAXException {

		try {
			String tString = stringBuffer.toString().trim();

			if (name.trim().equals("title"))
				_currentArticle.setTitle(tString);
			else if (name.trim().equals("link"))
				_currentArticle.setUrl(new URL(tString));
			else if (name.trim().equals("description")) {
				_currentArticle.setDescription(tString);
			} else if (name.trim().equals("content")) {
				_currentArticle.setContent(tString);
			} else if (name.trim().equals("category")) {
				_currentArticle.setFeedCategory(tString.toLowerCase());
			} else if (name.trim().equals("guid")) {
				_currentArticle.setGuid(tString);
			} else if (name.trim().equals("pubDate")) {
				_currentArticle.setPubDate(RSS822_DATE.parse(tString));
			} else if (name.trim().equals("item")) {
				// Subfeeds e.g. netzwelt oder kultur have no category tag, so
				// calculate it from url
				if (_currentArticle.getFeedCategory() == null
				    || _currentArticle.getFeedCategory().length() == 0) {
					Log.w(TAG, "category of " + _currentArticle.getTitle()
					      + " ist empty");
					_currentArticle.setFeedCategory(feed.getFeedCategory());
				}
				// feedCategory;
				// Check if looking for article, and if article is complete
				if (_currentArticle.getUrl() != null
				    && _currentArticle.getTitle().length() > 0
				    && _currentArticle.getDescription().length() > 0) {
					if (_currentArticle.getUrl() != null) {
						feed.addArticle(_currentArticle);
						if (MDebug.LOG) {
							Log.d(TAG, "SAX, added article with title: "
							      + _currentArticle.getTitle());
						}
					}
					_currentArticle = null;
				}
			}
		} catch (Exception e) {
			throw new SAXException("endElement(): uri: " + uri + ", name: " + name + ", qname: " + qName +
					       ", exception: " + e.toString());
		}

		stringBuffer = new StringBuffer();
	}

	public void characters(char ch[], int start, int length) throws SAXException {
		try {
			stringBuffer.append(ch, start, length);
		} catch (OutOfMemoryError e) {
			throw new SAXException("Out of memory while adding " + length + "characters");
		}
	}
}
