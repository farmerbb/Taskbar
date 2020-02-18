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

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.provider.Settings;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.SelectAppActivity;
import com.farmerbb.taskbar.activity.dark.SelectAppActivityDark;
import com.farmerbb.taskbar.util.Blacklist;
import com.farmerbb.taskbar.util.TopApps;
import com.farmerbb.taskbar.util.U;

public class GeneralFragment extends SettingsFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onCreate(savedInstanceState);

        // Add preferences
        addPreferencesFromResource(R.xml.tb_pref_general);

        // Set OnClickListeners for certain preferences
        findPreference("blacklist").setOnPreferenceClickListener(this);

        if(!U.isLibrary(getActivity()))
            findPreference("notification_settings").setOnPreferenceClickListener(this);
        else {
            getPreferenceScreen().removePreference(findPreference("start_on_boot"));
            getPreferenceScreen().removePreference(findPreference("notification_settings"));
        }

        if(U.canEnableFreeform()
                && !U.isChromeOs(getActivity())
                && !U.isOverridingFreeformHack(getActivity(), false))
            findPreference("hide_taskbar").setSummary(R.string.tb_hide_taskbar_disclaimer);

        bindPreferenceSummaryToValue(findPreference("start_menu_layout"));
        bindPreferenceSummaryToValue(findPreference("scrollbar"));
        bindPreferenceSummaryToValue(findPreference("position"));
        bindPreferenceSummaryToValue(findPreference("anchor"));
        bindPreferenceSummaryToValue(findPreference("alt_button_config"));
        bindPreferenceSummaryToValue(findPreference("show_search_bar"));
        bindPreferenceSummaryToValue(findPreference("hide_when_keyboard_shown"));

        if(U.isChromeOs(getActivity()))
            bindPreferenceSummaryToValue(findPreference("chrome_os_context_menu_fix"));
        else
            getPreferenceScreen().removePreference(findPreference("chrome_os_context_menu_fix"));

        finishedLoadingPrefs = true;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.tb_pref_header_general);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        int size = Blacklist.getInstance(getActivity()).getBlockedApps().size();
        String summary = size == 1 ? getString(R.string.tb_app_hidden) : getString(R.string.tb_apps_hidden, size);

        size = TopApps.getInstance(getActivity()).getTopApps().size();
        summary = summary + "\n" + (size == 1 ? getString(R.string.tb_top_app) : getString(R.string.tb_top_apps, size));

        Preference blacklistPref = findPreference("blacklist");
        if(blacklistPref != null) {
            blacklistPref.setSummary(summary);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
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
                intent2.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);

                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1)
                    intent2.putExtra(Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
                else {
                    intent2.putExtra("app_package", getActivity().getPackageName());
                    intent2.putExtra("app_uid", getActivity().getApplicationInfo().uid);
                }

                try {
                    startActivity(intent2);
                    restartNotificationService = true;
                } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
                break;
        }

        return super.onPreferenceClick(p);
    }
}
