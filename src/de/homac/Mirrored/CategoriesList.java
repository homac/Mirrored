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

package de.homac.Mirrored;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class CategoriesList extends ListActivity {

	private String TAG;
	private Mirrored app;

	private String _categories[];
	private int _counter = 0;

	@Override
	protected void onCreate(Bundle icicle) {
		app = (Mirrored)getApplication();
		TAG = app.APP_NAME + ", " + "CategoriesList";

		SharedPreferences prefs = PreferenceManager
			.getDefaultSharedPreferences(getBaseContext());
		if (prefs.getBoolean("PrefDarkBackground", false)) {
			Log.d(TAG, "Setting black theme");
			setTheme(android.R.style.Theme_Black);
		}

		super.onCreate(icicle);

		Log.d(TAG, "Loading categoriesView");

		setContentView(R.layout.categories_list);

		Log.d(TAG, "Getting categories array resource");
		_categories = getResources().getStringArray(R.array.categories);

		List<String> items = new ArrayList<String>();

		for (String category : _categories)
			items.add(category);

		ArrayAdapter<String> notes =
			new ArrayAdapter<String>(this, R.layout.category_row, R.id.category_name, items);
		setListAdapter(notes);

		_counter++;
	}

	@Override
	public void onRestart() {
		super.onRestart();
		Log.d(TAG, "onStart()");
		_counter--;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		String s = _categories[position].substring(0,1).toLowerCase() + _categories[position].substring(1);
		Log.d(TAG, "putExtra() category: "+s);

		Intent intent = new Intent(this, ArticlesList.class);
		intent.putExtra(app.EXTRA_CATEGORY, s);
		intent.setAction(Intent.ACTION_VIEW);
		startActivity(intent);

		// only allow one instance of the categories list view
		Log.d(TAG, "Checking counter: "+_counter);
		if (_counter > 1) {
			Log.d(TAG, "We already have one CategoriesList, finishing this one");
			//			this.finish();
		}
	}
}
