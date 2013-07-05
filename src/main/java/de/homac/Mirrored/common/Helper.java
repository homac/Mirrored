package de.homac.Mirrored.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;

import java.net.URL;

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
