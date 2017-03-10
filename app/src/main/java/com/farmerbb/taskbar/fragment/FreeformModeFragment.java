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
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.U;

public class FreeformModeFragment extends SettingsFragment implements Preference.OnPreferenceClickListener {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onActivityCreated(savedInstanceState);

        if(findPreference("dummy") == null) {
            // Add preferences
            addPreferencesFromResource(R.xml.pref_freeform_hack);

            findPreference("freeform_hack").setOnPreferenceClickListener(this);
            findPreference("freeform_mode_help").setOnPreferenceClickListener(this);
            findPreference("add_shortcut").setOnPreferenceClickListener(this);

            bindPreferenceSummaryToValue(findPreference("window_size"));

            SharedPreferences pref = U.getSharedPreferences(getActivity());
            boolean freeformHackEnabled = pref.getBoolean("freeform_hack", false);
            findPreference("launch_games_fullscreen").setEnabled(freeformHackEnabled);
            findPreference("save_window_sizes").setEnabled(freeformHackEnabled);
            findPreference("window_size").setEnabled(freeformHackEnabled);
            findPreference("add_shortcut").setEnabled(freeformHackEnabled);
            findPreference("force_new_window").setEnabled(freeformHackEnabled);
        }

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

            findPreference("launch_games_fullscreen").setEnabled(U.hasFreeformSupport(getActivity()));
            findPreference("save_window_sizes").setEnabled(U.hasFreeformSupport(getActivity()));
            findPreference("window_size").setEnabled(U.hasFreeformSupport(getActivity()));
            findPreference("add_shortcut").setEnabled(U.hasFreeformSupport(getActivity()));
            findPreference("force_new_window").setEnabled(U.hasFreeformSupport(getActivity()));

            if(U.hasFreeformSupport(getActivity())) {
                U.showToastLong(getActivity(), R.string.reboot_required);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean onPreferenceClick(final Preference p) {
        final SharedPreferences pref = U.getSharedPreferences(getActivity());

        switch(p.getKey()) {
            case "freeform_hack":
                if(((CheckBoxPreference) p).isChecked()) {
                    if(!U.hasFreeformSupport(getActivity())) {
                        ((CheckBoxPreference) p).setChecked(false);

                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(R.string.freeform_dialog_title)
                                .setMessage(R.string.freeform_dialog_message)
                                .setPositiveButton(R.string.action_developer_options, (dialogInterface, i) -> {
                                    showReminderToast = true;

                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                                    try {
                                        startActivity(intent);
                                        U.showToastLong(getActivity(), R.string.enable_force_activities_resizable);
                                    } catch (ActivityNotFoundException e) {
                                        intent = new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS);
                                        try {
                                            startActivity(intent);
                                            U.showToastLong(getActivity(), R.string.enable_developer_options);
                                        } catch (ActivityNotFoundException e2) { /* Gracefully fail */ }
                                    }
                                });

                        AlertDialog dialog = builder.create();
                        dialog.show();
                        dialog.setCancelable(false);
                    }

                    if(pref.getBoolean("taskbar_active", false)
                            && getActivity().isInMultiWindowMode()
                            && !FreeformHackHelper.getInstance().isFreeformHackActive()) {
                        U.startFreeformHack(getActivity(), false, false);
                    }
                } else {
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("com.farmerbb.taskbar.FINISH_FREEFORM_ACTIVITY"));
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("com.farmerbb.taskbar.FORCE_TASKBAR_RESTART"));
                }

                findPreference("launch_games_fullscreen").setEnabled(((CheckBoxPreference) p).isChecked());
                findPreference("save_window_sizes").setEnabled(((CheckBoxPreference) p).isChecked());
                findPreference("window_size").setEnabled(((CheckBoxPreference) p).isChecked());
                findPreference("add_shortcut").setEnabled(((CheckBoxPreference) p).isChecked());
                findPreference("force_new_window").setEnabled(((CheckBoxPreference) p).isChecked());

                break;
            case "freeform_mode_help":
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setView(View.inflate(getActivity(), R.layout.freeform_help_dialog, null))
                        .setTitle(R.string.freeform_help_dialog_title)
                        .setPositiveButton(R.string.action_close, null);

                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            case "add_shortcut":
                Intent intent = U.getShortcutIntent(getActivity());
                intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                intent.putExtra("duplicate", false);

                getActivity().sendBroadcast(intent);

                U.showToast(getActivity(), R.string.shortcut_created);
                break;
        }

        return true;
    }
}
