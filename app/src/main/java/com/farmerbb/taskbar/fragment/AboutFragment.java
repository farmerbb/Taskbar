/* Copyright 2016 Braden Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.farmerbb.taskbar.fragment;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference.OnPreferenceClickListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.U;

public class AboutFragment extends SettingsFragment implements OnPreferenceClickListener {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onActivityCreated(savedInstanceState);

        // Add preferences
        addPreferencesFromResource(R.xml.pref_base);

        boolean playStoreInstalled = true;
        try {
            getActivity().getPackageManager().getPackageInfo("com.android.vending", 0);
        } catch (PackageManager.NameNotFoundException e) {
            playStoreInstalled = false;
        }

        SharedPreferences pref = U.getSharedPreferences(getActivity());
        if(BuildConfig.APPLICATION_ID.equals(BuildConfig.BASE_APPLICATION_ID)
                && playStoreInstalled
                && !pref.getBoolean("hide_donate", false)) {
            addPreferencesFromResource(R.xml.pref_about_donate);
            findPreference("donate").setOnPreferenceClickListener(this);
        } else
            addPreferencesFromResource(R.xml.pref_about);

        // Set OnClickListeners for certain preferences
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            findPreference("pref_screen_freeform").setOnPreferenceClickListener(this);

        findPreference("pref_screen_general").setOnPreferenceClickListener(this);
        findPreference("pref_screen_appearance").setOnPreferenceClickListener(this);
        findPreference("pref_screen_recent_apps").setOnPreferenceClickListener(this);
        findPreference("pref_screen_advanced").setOnPreferenceClickListener(this);
        findPreference("about").setOnPreferenceClickListener(this);
        findPreference("about").setSummary(getString(R.string.pref_about_description, new String(Character.toChars(0x1F601))));

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.app_name);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(false);

        finishedLoadingPrefs = true;
    }
}