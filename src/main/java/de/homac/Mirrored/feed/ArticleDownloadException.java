package de.homac.Mirrored.feed;

/**
 * <!--
 * created:  06.05.12 14:29  by: frank
 * -->
 */
public class ArticleDownloadException extends Exception {

    int httpCode;

    public ArticleDownloadException(int httpCode) {
        this.httpCode = httpCode;
    }

    public ArticleDownloadException(int httpCode, String detailMessage) {
        super(detailMessage);
        this.httpCode = httpCode;
    }

    public int getHttpCode() {
        return httpCode;
    }
}
