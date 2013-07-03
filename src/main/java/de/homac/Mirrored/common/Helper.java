package de.homac.Mirrored.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Helper {
    private Helper() {
    }

    public static String convertStreamToString(InputStream is) throws IOException {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        if (is != null) {
            StringBuilder sb = new StringBuilder();
            String line;

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"), 8*1024);
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } finally {
                is.close();
            }
            return sb.toString();
        } else {
            return "";
        }
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
}
