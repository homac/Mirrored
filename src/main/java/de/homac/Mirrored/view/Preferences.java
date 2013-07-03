/*
 * Preferences.java
 *
 * Part of the Mirrored app for Android
 *
 * Copyright (C) 2010 Holger Macht <holger@homac.de>
 *
 * This file is released under the GPLv3.
 *
 */

package de.homac.Mirrored.view;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import de.homac.Mirrored.common.MDebug;
import de.homac.Mirrored.common.Mirrored;
import de.homac.Mirrored.R;

public class Preferences extends PreferenceActivity {

	private Mirrored app;
	private String TAG;

	@Override
	protected void onCreate(Bundle icicle) {
		app = (Mirrored)getApplication();
		TAG = app.APP_NAME + ", " + "ArticlesList";

		super.onCreate(icicle);
		addPreferencesFromResource(R.xml.preferences);

        initActionBar();
	}

    private void initActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getActionBar().setHomeButtonEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }

    @Override
	protected void onStop() {
		super.onStop();

		if (MDebug.LOG)
			Log.d(TAG, "onStop()");

		app.setOfflineMode(app.getPreferences().getBoolean("PrefStartWithOfflineMode", false));
	}
}