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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.SelectAppActivity;
import com.farmerbb.taskbar.activity.dark.SelectAppActivityDark;
import com.farmerbb.taskbar.util.Blacklist;
import com.farmerbb.taskbar.util.TopApps;
import com.farmerbb.taskbar.util.U;

public class GeneralFragment extends SettingsFragment implements Preference.OnPreferenceClickListener {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onActivityCreated(savedInstanceState);

        if(findPreference("dummy") == null) {
            // Add preferences
            addPreferencesFromResource(R.xml.pref_general);

            // Set OnClickListeners for certain preferences
            findPreference("blacklist").setOnPreferenceClickListener(this);
            findPreference("notification_settings").setOnPreferenceClickListener(this);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                findPreference("hide_taskbar").setSummary(R.string.hide_taskbar_disclaimer);

            bindPreferenceSummaryToValue(findPreference("start_menu_layout"));
            bindPreferenceSummaryToValue(findPreference("scrollbar"));
            bindPreferenceSummaryToValue(findPreference("position"));
            bindPreferenceSummaryToValue(findPreference("anchor"));
            bindPreferenceSummaryToValue(findPreference("alt_button_config"));
            bindPreferenceSummaryToValue(findPreference("show_search_bar"));
            bindPreferenceSummaryToValue(findPreference("hide_when_keyboard_shown"));
        }

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

        size = TopApps.getInstance(getActivity()).getTopApps().size();
        summary = summary + "\n" + (size == 1 ? getString(R.string.top_app) : getString(R.string.top_apps, size));

        Preference blacklistPref = findPreference("blacklist");
        if(blacklistPref != null) {
            blacklistPref.setSummary(summary);
        }
    }

    @Override
    public boolean onPreferenceClick(final Preference p) {
        final SharedPreferences pref = U.getSharedPreferences(getActivity());

        switch(p.getKey()) {
            case "blacklist":
                Intent intent = null;

                switch(pref.getString("theme", "light")) {
                    case "light":
                        intent = new Intent(getActivity(), SelectAppActivity.class);
                        break;
                    case "dark":
                        intent = new Intent(getActivity(), SelectAppActivityDark.class);
                        break;
                }

                startActivity(intent);
                break;
            case "notification_settings":
                Intent intent2 = new Intent();
                intent2.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent2.putExtra("app_package", BuildConfig.APPLICATION_ID);
                intent2.putExtra("app_uid", getActivity().getApplicationInfo().uid);

                try {
                    startActivity(intent2);
                    restartNotificationService = true;
                } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
                break;
        }

        return true;
    }
}
