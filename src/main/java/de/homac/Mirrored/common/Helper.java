package de.homac.Mirrored.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

public class Helper {
    private Helper() {
    }

    public static void showDialog(Context context, String text) {
         showDialog(context,text,false);
    }

    public static void showDialog(Context  context, String text, boolean formatted) {
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        if (formatted) {
            alertDialog.setMessage(Html.fromHtml(text));
        } else {
            alertDialog.setMessage(text);
        }
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					return;
				} });
		alertDialog.show();
	}

    public static String getBaseUrl(URL url) {
        return url.toString().substring(0, url.toString().lastIndexOf('/')+1);
    }
}
