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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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

    private BroadcastReceiver checkBoxReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            CheckBoxPreference preference = (CheckBoxPreference) findPreference("freeform_hack");
            if(preference != null) {
                SharedPreferences pref = U.getSharedPreferences(getActivity());
                preference.setChecked(pref.getBoolean("freeform_hack", false));
            }
        }
    };

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
            findPreference("window_size").setOnPreferenceClickListener(this);

            bindPreferenceSummaryToValue(findPreference("window_size"));

            SharedPreferences pref = U.getSharedPreferences(getActivity());
            boolean lockFreeformToggle = pref.getBoolean("freeform_hack", false)
                    && U.isChromeOs(getActivity());

            if(!lockFreeformToggle) {
                findPreference("save_window_sizes").setDependency("freeform_hack");
                findPreference("force_new_window").setDependency("freeform_hack");
                findPreference("launch_games_fullscreen").setDependency("freeform_hack");
                findPreference("window_size").setDependency("freeform_hack");
                findPreference("add_shortcut").setDependency("freeform_hack");
            }

            findPreference("freeform_hack").setEnabled(!lockFreeformToggle);
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.pref_header_freeform);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        // Dialog shown on devices which seem to not work correctly with freeform mode
        if(U.hasPartialFreeformSupport()) {
            SharedPreferences pref = U.getSharedPreferences(getActivity());
            if(!pref.getBoolean("samsung_dialog_shown", false)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.samsung_freeform_title)
                        .setMessage(R.string.samsung_freeform_message)
                        .setPositiveButton(R.string.action_ok, (dialog, which) -> pref.edit().putBoolean("samsung_dialog_shown", true).apply());

                AlertDialog dialog = builder.create();
                dialog.show();
                dialog.setCancelable(false);
            }
        }

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(checkBoxReceiver, new IntentFilter("com.farmerbb.taskbar.UPDATE_FREEFORM_CHECKBOX"));

        finishedLoadingPrefs = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(showReminderToast) {
            showReminderToast = false;

            freeformSetupComplete();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(checkBoxReceiver);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean onPreferenceClick(final Preference p) {
        final SharedPreferences pref = U.getSharedPreferences(getActivity());

        switch(p.getKey()) {
            case "freeform_hack":
                if(((CheckBoxPreference) p).isChecked()) {
                    if(!U.hasFreeformSupport(getActivity())) {
                        try {
                            Settings.Global.putInt(getActivity().getContentResolver(), "enable_freeform_support", 1);
                            U.showToastLong(getActivity(), R.string.reboot_required);
                        } catch (Exception e) {
                            ((CheckBoxPreference) p).setChecked(false);

                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                                builder.setTitle(R.string.freeform_dialog_title)
                                        .setMessage(R.string.freeform_dialog_message_alt)
                                        .setPositiveButton(R.string.action_ok, (dialogInterface, i) -> freeformSetupComplete());
                            } else {
                                builder.setTitle(R.string.freeform_dialog_title)
                                        .setMessage(R.string.freeform_dialog_message)
                                        .setPositiveButton(R.string.action_developer_options, (dialogInterface, i) -> {
                                            showReminderToast = true;

                                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                                            try {
                                                startActivity(intent);
                                                U.showToastLong(getActivity(), R.string.enable_force_activities_resizable);
                                            } catch (ActivityNotFoundException e1) {
                                                intent = new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS);
                                                try {
                                                    startActivity(intent);
                                                    U.showToastLong(getActivity(), R.string.enable_developer_options);
                                                } catch (ActivityNotFoundException e2) { /* Gracefully fail */ }
                                            }
                                        });
                            }

                            AlertDialog dialog = builder.create();
                            dialog.show();
                            dialog.setCancelable(false);
                        }
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

                U.restartNotificationService(getActivity());
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

                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                ResolveInfo defaultLauncher = getActivity().getPackageManager().resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);

                intent.setPackage(defaultLauncher.activityInfo.packageName);
                getActivity().sendBroadcast(intent);

                U.showToast(getActivity(), R.string.shortcut_created);
                break;
            case "window_size":
                if(U.isOPreview()) {
                    U.showToast(getActivity(), R.string.window_sizes_not_available);
                }

                break;
        }

        return true;
    }

    private void freeformSetupComplete() {
        ((CheckBoxPreference) findPreference("freeform_hack")).setChecked(U.hasFreeformSupport(getActivity()));

        if(U.hasFreeformSupport(getActivity())) {
            U.showToastLong(getActivity(), R.string.reboot_required);
        }
    }
}
