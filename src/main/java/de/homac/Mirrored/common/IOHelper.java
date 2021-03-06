package de.homac.Mirrored.common;

import org.apache.http.client.HttpResponseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.Charset;

public class IOHelper {
    private static final int EOF = -1;

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    public static final int TIMEOUT_MILLIS = 10000;

    public static String toString(InputStream input, Charset encoding) throws IOException {
        StringWriter sw = new StringWriter();
        copy(input, sw, encoding);
        return sw.toString();
    }

    public static String toString(InputStream input, String encoding)
            throws IOException {
        return toString(input, toCharset(encoding));
    }

    public static String toString(URL url) throws IOException {
        return toString(url, Charset.defaultCharset());
    }

    public static String toString(URL url, Charset encoding) throws IOException {
        if (Thread.currentThread().isInterrupted()) {
            throw new ClosedByInterruptException();
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.connect();
        if (connection.getResponseCode() != 200) {
            throw new HttpResponseException(connection.getResponseCode(), connection.getResponseMessage());
        }
        InputStream inputStream = connection.getInputStream();
        try {
            return toString(inputStream, encoding);
        } finally {
            inputStream.close();
            close(connection);
        }
    }

    public static String toString(URL url, String encoding) throws IOException {
        return toString(url, toCharset(encoding));
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output);
        return output.toByteArray();
    }

    public static byte[] toByteArray(URL url) throws IOException {
        if (Thread.currentThread().isInterrupted()) {
            throw new ClosedByInterruptException();
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.connect();
        if (connection.getResponseCode() != 200) {
            throw new HttpResponseException(connection.getResponseCode(), connection.getResponseMessage());
        }
        InputStream inputStream = connection.getInputStream();
        try {
            return toByteArray(inputStream);
        } finally {
            inputStream.close();
            close(connection);
        }
    }

    public static int copy(InputStream input, OutputStream output) throws IOException {
        long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }

    public static void copy(InputStream input, Writer output, Charset encoding) throws IOException {
        InputStreamReader in = new InputStreamReader(input, toCharset(encoding));
        copy(in, output);
    }

    public static int copy(Reader input, Writer output) throws IOException {
        long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }

    public static long copyLarge(InputStream input, OutputStream output)
            throws IOException {
        return copyLarge(input, output, new byte[DEFAULT_BUFFER_SIZE]);
    }

    public static long copyLarge(InputStream input, OutputStream output, byte[] buffer)
            throws IOException {
        long count = 0;
        int n = 0;
        while (!Thread.currentThread().isInterrupted() && EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new ClosedByInterruptException();
        }
        return count;
    }

    public static long copyLarge(Reader input, Writer output) throws IOException {
        return copyLarge(input, output, new char[DEFAULT_BUFFER_SIZE]);
    }

    public static long copyLarge(Reader input, Writer output, char [] buffer) throws IOException {
        long count = 0;
        int n = 0;
        while (!Thread.currentThread().isInterrupted() && EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new ClosedByInterruptException();
        }
        return count;
    }

    private static void close(URLConnection conn) {
        if (conn instanceof HttpURLConnection) {
            ((HttpURLConnection) conn).disconnect();
        }
    }
    private static Charset toCharset(Charset charset) {
        return charset == null ? Charset.defaultCharset() : charset;
    }
    private static Charset toCharset(String charset) {
        return charset == null ? Charset.defaultCharset() : Charset.forName(charset);
    }
}
