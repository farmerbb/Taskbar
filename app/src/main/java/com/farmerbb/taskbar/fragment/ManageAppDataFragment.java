/* Copyright 2020 Braden Farmer
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

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.farmerbb.taskbar.R;

public class ManageAppDataFragment extends SettingsFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onCreate(savedInstanceState);

        // Add preferences
        addPreferencesFromResource(R.xml.tb_pref_manage_app_data);

        // Set OnClickListeners for certain preferences
        findPreference("backup_settings").setOnPreferenceClickListener(this);
        findPreference("restore_settings").setOnPreferenceClickListener(this);
        findPreference("clear_pinned_apps").setOnPreferenceClickListener(this);

        finishedLoadingPrefs = true;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.tb_manage_app_data);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public boolean onPreferenceClick(final Preference p) {
        switch(p.getKey()) {
            case "backup_settings":
            case "restore_settings":
                // TODO
                break;
        }

        return super.onPreferenceClick(p);
    }
}
