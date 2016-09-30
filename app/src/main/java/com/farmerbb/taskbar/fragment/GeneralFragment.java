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
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.Blacklist;
import com.farmerbb.taskbar.util.U;

public class GeneralFragment extends SettingsFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onActivityCreated(savedInstanceState);

        // Add preferences
        addPreferencesFromResource(R.xml.pref_general);

        // Set OnClickListeners for certain preferences
        findPreference("blacklist").setOnPreferenceClickListener(this);
        findPreference("icon_pack_list").setOnPreferenceClickListener(this);

        bindPreferenceSummaryToValue(findPreference("start_menu_layout"));
        bindPreferenceSummaryToValue(findPreference("show_background"));
        bindPreferenceSummaryToValue(findPreference("scrollbar"));
        bindPreferenceSummaryToValue(findPreference("position"));
        bindPreferenceSummaryToValue(findPreference("theme"));
        bindPreferenceSummaryToValue(findPreference("invisible_button"));
        bindPreferenceSummaryToValue(findPreference("anchor"));
        bindPreferenceSummaryToValue(findPreference("app_drawer_icon"));

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.pref_header_general);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        finishedLoadingPrefs = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        int size = Blacklist.getInstance(getActivity()).getBlockedApps().size();
        String summary = size == 1 ? getString(R.string.app_hidden) : getString(R.string.apps_hidden, size);

        Preference blacklistPref = findPreference("blacklist");
        if(blacklistPref != null) {
            blacklistPref.setSummary(summary);
        }

        Preference iconPackListPref = findPreference("icon_pack_list");
        if(iconPackListPref != null) {
            SharedPreferences pref = U.getSharedPreferences(getActivity());
            String iconPackPackage = pref.getString("icon_pack", BuildConfig.APPLICATION_ID);
            PackageManager pm = getActivity().getPackageManager();

            boolean iconPackValid = true;
            try {
                pm.getPackageInfo(iconPackPackage, 0);
            } catch (PackageManager.NameNotFoundException e) {
                iconPackValid = false;
            }

            if(!iconPackValid || iconPackPackage.equals(BuildConfig.APPLICATION_ID)) {
                iconPackListPref.setSummary(getString(R.string.icon_pack_none));
            } else {
                try {
                    iconPackListPref.setSummary(pm.getApplicationLabel(pm.getApplicationInfo(iconPackPackage, 0)));
                } catch (PackageManager.NameNotFoundException e) { /* Gracefully fail */ }
            }
        }
    }
}
