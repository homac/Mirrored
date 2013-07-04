package de.homac.Mirrored.feed;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import de.homac.Mirrored.common.Mirrored;
import de.homac.Mirrored.model.Article;
import de.homac.Mirrored.provider.SpiegelOnlineDownloader;

public class ArticleDownloadThread implements Runnable {
    private final String TAG = "ArticleDownloadThread";

    private Handler handler;
    private Article article;
    private boolean downloadContent;
    private boolean downloadImages;

    public ArticleDownloadThread(Article article, Handler handler) {
        this.handler = handler;
        this.article = article;
    }

    @Override
    public void run() {
        SpiegelOnlineDownloader downloader = new SpiegelOnlineDownloader(article, Mirrored.getInstance().getCacheHelper());
        try {
            if (downloadContent) {
                downloader.downloadContent(downloadImages);
            }
            if (downloadImages) {
                downloader.downloadThumbnailImage();
            }
            if (handler != null)
                handler.sendEmptyMessage(0);
        } catch (ArticleDownloadException e) {
            Log.e(TAG,
                    String.format("Could not fetch article '%s', statuscode was %s", article.getUrl(),
                            e.getHttpCode()));
            if (handler != null) {
                Message msg = new Message();
                msg.what = 1;
                msg.arg1 = e.getHttpCode();
                handler.sendMessage(msg);
            }
        }
    }

    public void setDownloadContent(boolean downloadContent) {
        this.downloadContent = downloadContent;
    }

    public void setDownloadImages(boolean downloadImages) {
        this.downloadImages = downloadImages;
    }
}
