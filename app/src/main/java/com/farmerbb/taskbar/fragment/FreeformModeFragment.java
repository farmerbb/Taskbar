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

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.ShortcutActivity;
import com.farmerbb.taskbar.util.U;

public class FreeformModeFragment extends SettingsFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onActivityCreated(savedInstanceState);

        // Add preferences
        addPreferencesFromResource(R.xml.pref_freeform_hack);

        findPreference("freeform_hack").setOnPreferenceClickListener(this);
        findPreference("freeform_mode_help").setOnPreferenceClickListener(this);
        findPreference("add_shortcut").setOnPreferenceClickListener(this);

        bindPreferenceSummaryToValue(findPreference("window_size"));

        SharedPreferences pref = U.getSharedPreferences(getActivity());
        boolean freeformHackEnabled = pref.getBoolean("freeform_hack", false);
        findPreference("open_in_fullscreen").setEnabled(freeformHackEnabled);
        findPreference("save_window_sizes").setEnabled(freeformHackEnabled);
        findPreference("window_size").setEnabled(freeformHackEnabled);
        findPreference("add_shortcut").setEnabled(freeformHackEnabled);

        ComponentName component = new ComponentName(BuildConfig.APPLICATION_ID, ShortcutActivity.class.getName());
        getActivity().getPackageManager().setComponentEnabledSetting(component,
                freeformHackEnabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.pref_header_freeform);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        finishedLoadingPrefs = true;
    }


    @Override
    public void onResume() {
        super.onResume();

        if(showReminderToast) {
            showReminderToast = false;

            ((CheckBoxPreference) findPreference("freeform_hack")).setChecked(U.hasFreeformSupport(getActivity()));

            findPreference("open_in_fullscreen").setEnabled(U.hasFreeformSupport(getActivity()));
            findPreference("save_window_sizes").setEnabled(U.hasFreeformSupport(getActivity()));
            findPreference("window_size").setEnabled(U.hasFreeformSupport(getActivity()));
            findPreference("add_shortcut").setEnabled(U.hasFreeformSupport(getActivity()));

            ComponentName component = new ComponentName(BuildConfig.APPLICATION_ID, ShortcutActivity.class.getName());
            getActivity().getPackageManager().setComponentEnabledSetting(component,
                    U.hasFreeformSupport(getActivity()) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            if(U.hasFreeformSupport(getActivity())) {
                U.showToastLong(getActivity(), R.string.reboot_required);

                SharedPreferences pref = U.getSharedPreferences(getActivity());
                pref.edit().putBoolean("reboot_required", true).apply();
            }
        }
    }
}
