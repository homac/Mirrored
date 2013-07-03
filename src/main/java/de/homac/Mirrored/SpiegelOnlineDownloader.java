package de.homac.Mirrored;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class SpiegelOnlineDownloader {
    private static final String TAG = "SpiegelOnlineDownloader";

    private static final String FEED_PREFIX = "http://m.spiegel.de/";
    private static final String FEED_SUFFIX = "/index.rss";

    private static final String TEASER = "<p id=\"spIntroTeaser\">";
    private static final String CONTENT = "<div class=\"spArticleContent\"";
    private static final String IMAGE = "<div class=\"spArticleImageBox";

    private final Article article;

    public SpiegelOnlineDownloader(Article article) {
        this.article = article;
    }

    private Bitmap downloadImage(URL imageUrl) {
        Bitmap bitmap = null;

        try {
            bitmap = BitmapFactory.decodeStream(imageUrl.openStream());
        } catch (IOException e) {
            if (MDebug.LOG)
                Log.e(TAG, e.toString());
        }

        return bitmap;
    }

    private String downloadContentPage(int page, boolean downloadImage) throws ArticleDownloadException {
        StringBuilder sb = new StringBuilder();
        try {

            URL url = getArticleUrl(page);
            if (MDebug.LOG)
                Log.d(TAG, "Downloading " + url.toString());

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                Log.e(TAG, String.format("Could not download url '%s'. Errorcode is:  %s.", url, responseCode));
                throw new ArticleDownloadException(responseCode);
            }

            Log.d(TAG, String.format("Response code is %s", responseCode));
            InputStream is = urlConnection.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"), 8 * 1024);
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
            is.close();
        } catch (MalformedURLException e) {
            if (MDebug.LOG)
                Log.e("Mirrored", e.toString());
        } catch (IOException e) {
            if (MDebug.LOG)
                Log.e("Mirrored", e.toString());
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
            throws IOException {
        StringBuilder text = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null && !(line.contains(CONTENT))) {
            line = line.trim();
            if (!skipTeaser) {
                if (line.contains("<head>") || line.startsWith("<link") || line.contains("<meta")) {
                    text.append(line);
                }
            }
            continue;
        }
        boolean hasImage=false;

        if(!skipTeaser){ //write body tag
            text.append("<body>");
        }
        text.append(line.substring(line.indexOf(CONTENT)));
        while (((line = reader.readLine()) != null) && !(line.contains(TEASER))) {
            if (!skipTeaser) {
                if (!downloadImage && line.contains(IMAGE)) {
                    hasImage=true;
                }
                if (!hasImage) {
                    text.append(line);
                }
            }
            continue;
        }
        text.append(line.substring(line.indexOf(TEASER)));

        int diffCount = 1;
        while (((line = reader.readLine()) != null) && diffCount > 0) {
            diffCount -= countTag(line, "</div>");
            if (diffCount == 1) {
                // skip inner diffs -> fotostrecke, etc
                text.append(line);
            }
            if (diffCount > 0) {
                diffCount += countTag(line, "<div");
            }
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
            article.setContent(downloadContentPage(1, downloadImage));
        }
    }

    public void downloadThumbnailImage() {
        if (article.getThumbnailImageUrl() != null)
            article.setThumbnailImage(downloadImage(article.getThumbnailImageUrl()));
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
