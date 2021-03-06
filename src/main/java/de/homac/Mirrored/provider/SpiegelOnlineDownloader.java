package de.homac.Mirrored.provider;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import org.apache.http.client.HttpResponseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.homac.Mirrored.common.CacheHelper;
import de.homac.Mirrored.common.IOHelper;
import de.homac.Mirrored.common.MDebug;
import de.homac.Mirrored.feed.ArticleDownloadException;
import de.homac.Mirrored.model.Article;

public class SpiegelOnlineDownloader {
    private static final String TAG = "SpiegelOnlineDownloader";

    private static final String FEED_PREFIX = "http://m.spiegel.de/";
    private static final String FEED_SUFFIX = "/index.rss";

    private static final String TEASER = "<p id=\"spIntroTeaser\"";
    private static final String CONTENT = "<div class=\"spArticleContent\"";
    private static final String IMAGE = "<div class=\"spArticleImageBox";
    private static final String VIDEO = "class=\"spVideoAsset";

    private final Article article;
    private CacheHelper cacheHelper;

    public SpiegelOnlineDownloader(Article article, CacheHelper cacheHelper) {
        this.article = article;
        this.cacheHelper = cacheHelper;
    }

    private Bitmap downloadImage(URL imageUrl) {
        Bitmap bitmap = null;

        try {
            byte[] data = cacheHelper.loadCached(imageUrl, false);
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (IOException e) {
            if (MDebug.LOG)
                Log.e(TAG, e.toString());
        }

        return bitmap;
    }

    private String downloadContentPage(int page, boolean downloadImage) throws ArticleDownloadException {
        StringBuilder sb = new StringBuilder();
        URL url = null;
        try {

            url = getArticleUrl(page);
            if (MDebug.LOG)
                Log.d(TAG, "Downloading " + url.toString());

            BufferedReader reader = new BufferedReader(new StringReader(IOHelper.toString(url, "UTF-8")), 8 * 1024);
            if (page == 0) {
                sb.append("<html>");
            }
            sb.append(extractArticleContent(reader, page > 1, downloadImage));
            String line;
            boolean couldHasNext = false;
            while ((line = reader.readLine()) != null) {
                if (line.contains("<li class=\"spMultiPagerLink\">")) {
                    couldHasNext = true;
                } else if (couldHasNext && line.contains(">WEITER</a>")) {
                    Log.d(TAG, "Downloading next page");
                    sb.append(downloadContentPage(page + 1, downloadImage));
                }
            }
        } catch (MalformedURLException e) {
            if (MDebug.LOG)
                Log.e(TAG, e.toString());
        } catch (IOException e) {
            if (MDebug.LOG)
                Log.e(TAG, String.format("Could not download url '%s'", url), e);
            throw new ArticleDownloadException((e instanceof HttpResponseException ? ((HttpResponseException) e).getStatusCode() : 500));
        }
        if (page == 1) {
            sb.append("</body></html>");
        }

        return sb.toString();
    }

    private URL getArticleUrl(int page) throws MalformedURLException {
        String base = article.getUrl().toString();
        base = base.substring(0, base.lastIndexOf(".html"));
        return new URL(base + (page > 1 ? "-" + page : "") + ".html");
    }

    private String extractArticleContent(BufferedReader reader, boolean skipTeaser, boolean downloadImage)
            throws IOException, ArticleDownloadException {
        StringBuilder text = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null && !(line.contains(CONTENT))) {
            line = line.trim();
            if (!skipTeaser) {
                if (line.contains("<head>") || line.contains("</head>")
                        || line.startsWith("<link rel=\"stylesheet\"") || line.contains("<meta")) {
                    text.append(line);
                }
            }
        }
        if (line == null) {
            throw new ArticleDownloadException(501);
        }

        if(!skipTeaser){ //write body tag
            text.append("<body>");
        }
        text.append(line.substring(line.indexOf(CONTENT)));
        boolean hasImage=false;
        while (((line = reader.readLine()) != null) && !(line.contains(TEASER))) {
            if (!skipTeaser) {
                if (!downloadImage && (line.contains(IMAGE) || line.contains(VIDEO))) {
                    hasImage=true;
                }
                if (!hasImage) {
                    text.append(line);
                }
            }
        }
        if (line == null) {
            throw new ArticleDownloadException(502);
        }
        text.append(line.substring(line.indexOf(TEASER)));

        int diffCount = 1;
        int openDivs;
        int closeDivs;
        while (((line = reader.readLine()) != null) && diffCount > 0) {
            openDivs = countTag(line, "<div");
            closeDivs = countTag(line, "</div>");

            diffCount -= closeDivs;

            if (diffCount == 1) {
                text.append(line);
            }
            if (diffCount > 0 || (openDivs > 0 && closeDivs > openDivs)) {
                diffCount += openDivs;
            }
        }
        if (line == null) {
            throw new ArticleDownloadException(503);
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

    public void downloadContent(boolean downloadImage) throws ArticleDownloadException {
        if (article.getContent() != null && article.getContent().length() != 0) {
            if (MDebug.LOG)
                Log.d(TAG, "Article already has content, returning it");
        } else {
            if (MDebug.LOG)
                Log.d(TAG, "Article doesn't have content, downloading and returning it");
            String content = downloadContentPage(1, downloadImage);
            content = cleanupBody(content);
            content = inlineAssets(content, P_IMG, true);
            content = inlineAssets(content, P_LINK, false);
            if (Thread.interrupted()) {
                throw new ArticleDownloadException(500);
            }
            article.setContent(content);
        }
    }

    private static final Pattern P_AD = Pattern.compile("<div class=\"[^\"]*spEms[^\"]*\">.*?</div></div>", Pattern.MULTILINE);

    private String cleanupBody(String content) {
        return P_AD.matcher(content).replaceAll("");
    }

    private static final Pattern P_IMG = Pattern.compile("<img\\s+src=\"([^\"]+)\"");
    private static final Pattern P_LINK = Pattern.compile("<link\\s+rel=\"stylesheet\"\\s+type=\"text/css\"\\s+href=\"([^\"]+)\"\\s*/>");

    private String inlineAssets(String content, Pattern pattern, boolean image) {
        Matcher m = pattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String assetUrl = m.group(1);
            String replacement = m.group(0);
            try {
                URL url = new URL(article.getUrl(), assetUrl);
                if (image) {
                    //convert image to data: url
                    byte[] data = IOHelper.toByteArray(url);
                    String imgData = "data:image/jpg;base64," + Base64.encodeToString(data, Base64.NO_WRAP);
                    replacement = "<img src=\"" + imgData + "\"";
                } else {
                    //insert stylesheet as-is
                    String styleContent;
                    if (cacheHelper != null) {
                        styleContent = new String(cacheHelper.loadCached(url));
                    } else {
                        styleContent = IOHelper.toString(url);
                    }
                    replacement = "<style type=\"text/css\">" + styleContent + "</style>";
                }
            } catch (MalformedURLException e) {
                if (MDebug.LOG)
                    Log.w(TAG, "Failed to inline " + assetUrl, e);
            } catch (IOException e) {
                if (MDebug.LOG)
                    Log.w(TAG, "Failed to inline " + assetUrl, e);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);

        return sb.toString();
    }

    public void downloadThumbnailImage() {
        if (article.getThumbnailImageUrl() != null && article.getThumbnailImage() == null) {
            Bitmap img = downloadImage(article.getThumbnailImageUrl());
            article.setThumbnailImage(img);
        }
    }

    public static URL getFeedUrl(String category) {
        try {
            return new URL(FEED_PREFIX + category + FEED_SUFFIX);
        } catch (MalformedURLException e) {
            if (MDebug.LOG)
                Log.e(TAG, e.toString());
            throw new RuntimeException(e);
        }
    }

}
