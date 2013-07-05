package de.homac.Mirrored.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.Html;

import java.net.URL;

import de.homac.Mirrored.R;

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

    public static void shareUrl(Context context, URL url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, url.toString());

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.title_share_via)));
    }
}
