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
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.farmerbb.taskbar.R;
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

        bindPreferenceSummaryToValue(findPreference("window_size"));

        SharedPreferences pref = U.getSharedPreferences(getActivity());
        boolean freeformHackEnabled = pref.getBoolean("freeform_hack", false);
        findPreference("open_in_fullscreen").setEnabled(freeformHackEnabled);
        findPreference("save_window_sizes").setEnabled(freeformHackEnabled);
        findPreference("window_size").setEnabled(freeformHackEnabled);

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

            ((CheckBoxPreference) findPreference("freeform_hack")).setChecked(hasFreeformSupport());

            if(hasFreeformSupport()) {
                U.showToastLong(getActivity(), R.string.reboot_required);

                SharedPreferences pref = U.getSharedPreferences(getActivity());
                pref.edit().putBoolean("reboot_required", true).apply();
            }
        }
    }
}
