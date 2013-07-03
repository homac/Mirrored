/*
 * CategoriesList.java
 *
 * Part of the Mirrored app for Android
 *
 * Copyright (C) 2010 Holger Macht <holger@homac.de>
 *
 * This file is released under the GPLv3.
 *
 */

package de.homac.Mirrored.view;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Arrays;
import java.util.Comparator;

import de.homac.Mirrored.common.MDebug;
import de.homac.Mirrored.common.Mirrored;
import de.homac.Mirrored.R;

public class CategoriesList extends ListActivity {
    public static final String EXTRA_CATEGORY = "category";

	private static final Comparator<String> STRING_COMPARATOR = new Comparator<String>() {
		public int compare(String pString1, String pString2) {
			return pString1.compareTo(pString2);
		}
	};
	private String TAG;
	private Mirrored app;

    @Override
	protected void onCreate(Bundle icicle) {
		app = (Mirrored) getApplication();
		TAG = app.APP_NAME + ", " + "CategoriesList";

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		if (prefs.getBoolean("PrefDarkBackground", false)) {
			if (MDebug.LOG)
				Log.d(TAG, "Setting black theme");
			setTheme(android.R.style.Theme_Black);
		}

		super.onCreate(icicle);

		if (MDebug.LOG)
			Log.d(TAG, "Loading categoriesView");

		setContentView(R.layout.categories_list);

		if (MDebug.LOG)
			Log.d(TAG, "Getting categories array resource");
        String[] _categories = getResources().getStringArray(R.array.categories);

		ArrayAdapter<String> notes = new ArrayAdapter<String>(this,
				R.layout.category_row, R.id.category_name,
				Arrays.asList(_categories));
		notes.sort(STRING_COMPARATOR);
		setListAdapter(notes);

        setTitle(getString(R.string.title_select_category));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		String category = l.getAdapter().getItem(position).toString()
				.toLowerCase();
		if (MDebug.LOG)
			Log.d(TAG, "putExtra() feedCategory: " + category);

		Intent intent = new Intent();
		intent.putExtra(EXTRA_CATEGORY, category);
        setResult(RESULT_OK, intent);
        finish();
	}
}
