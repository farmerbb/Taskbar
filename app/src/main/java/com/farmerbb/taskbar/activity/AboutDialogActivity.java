/* Copyright 2018 Braden Farmer
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

package com.farmerbb.taskbar.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.DependencyUtils;
import com.farmerbb.taskbar.util.U;

public class AboutDialogActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener {

    @SuppressWarnings("deprecation")
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setTitle(R.string.pref_header_about);

        if(!BuildConfig.APPLICATION_ID.equals(BuildConfig.ANDROIDX86_APPLICATION_ID)) {
            addPreferencesFromResource(R.xml.pref_about_dialog);

            findPreference("view_play_store").setOnPreferenceClickListener(this);
            findPreference("view_licenses").setOnPreferenceClickListener(this);
        } else
            addPreferencesFromResource(R.xml.pref_about_dialog_alt);

        findPreference("view_github").setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch(preference.getKey()) {
            case "view_play_store":
                U.checkForUpdates(this);
                break;
            case "view_github":
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://github.com/farmerbb/Taskbar"));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
                break;
            case "view_licenses":
                DependencyUtils.showLicenses(this);
                break;
        }

        finish();
        return true;
    }
}