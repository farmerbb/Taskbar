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
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.Settings;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.helper.FreeformHackHelper;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class FreeformModeFragment extends SettingsFragment {

    private final BroadcastReceiver checkBoxReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            CheckBoxPreference preference = (CheckBoxPreference) findPreference(PREF_FREEFORM_HACK);
            if(preference != null) {
                SharedPreferences pref = U.getSharedPreferences(getActivity());
                preference.setChecked(pref.getBoolean(PREF_FREEFORM_HACK, false));
            }
        }
    };

    @Override
    protected void loadPrefs() {
        // Add preferences
        addPreferencesFromResource(R.xml.tb_pref_freeform_hack);

        findPreference(PREF_FREEFORM_HACK).setOnPreferenceClickListener(this);
        findPreference(PREF_WINDOW_SIZE).setOnPreferenceClickListener(this);

        boolean enableFreeformModeShortcut = U.enableFreeformModeShortcut(getActivity());
        if(enableFreeformModeShortcut)
            findPreference(PREF_ADD_SHORTCUT).setOnPreferenceClickListener(this);
        else
            getPreferenceScreen().removePreference(findPreference(PREF_ADD_SHORTCUT));

        bindPreferenceSummaryToValue(findPreference(PREF_WINDOW_SIZE));

        SharedPreferences pref = U.getSharedPreferences(getActivity());
        boolean isLibrary = U.isLibrary(getActivity());
        boolean freeformHackEnabled = pref.getBoolean(PREF_FREEFORM_HACK, false);
        boolean lockFreeformToggle = pref.getBoolean(PREF_DESKTOP_MODE, false)
                || (freeformHackEnabled && U.isChromeOs(getActivity())
                || isLibrary);

        if(!lockFreeformToggle) {
            findPreference(PREF_SAVE_WINDOW_SIZES).setDependency(PREF_FREEFORM_HACK);
            findPreference(PREF_FORCE_NEW_WINDOW).setDependency(PREF_FREEFORM_HACK);
            findPreference(PREF_LAUNCH_GAMES_FULLSCREEN).setDependency(PREF_FREEFORM_HACK);
            findPreference(PREF_WINDOW_SIZE).setDependency(PREF_FREEFORM_HACK);

            if(enableFreeformModeShortcut)
                findPreference(PREF_ADD_SHORTCUT).setDependency(PREF_FREEFORM_HACK);
        } else {
            ((CheckBoxPreference) findPreference(PREF_FREEFORM_HACK)).setChecked(true);
            pref.edit().putBoolean(PREF_FREEFORM_HACK, freeformHackEnabled).apply();
        }

        if(!isLibrary)
            findPreference(PREF_FREEFORM_HACK).setEnabled(!lockFreeformToggle);
        else
            getPreferenceScreen().removePreference(findPreference(PREF_FREEFORM_HACK));

        // Dialog shown on devices which seem to not work correctly with freeform mode
        if(U.isSamsungDevice() && !pref.getBoolean(PREF_SAMSUNG_DIALOG_SHOWN, false)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.tb_samsung_freeform_title)
                    .setMessage(R.string.tb_samsung_freeform_message)
                    .setPositiveButton(R.string.tb_action_ok, (dialog, which) -> pref.edit().putBoolean(PREF_SAMSUNG_DIALOG_SHOWN, true).apply());

            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.setCancelable(false);
        }

        U.registerReceiver(getActivity(), checkBoxReceiver, ACTION_UPDATE_FREEFORM_CHECKBOX);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.tb_pref_header_freeform);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
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

        U.unregisterReceiver(getActivity(), checkBoxReceiver);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean onPreferenceClick(final Preference p) {
        final SharedPreferences pref = U.getSharedPreferences(getActivity());

        switch(p.getKey()) {
            case PREF_FREEFORM_HACK:
                if(((CheckBoxPreference) p).isChecked()) {
                    if(!U.hasFreeformSupport(getActivity())) {
                        try {
                            Settings.Global.putInt(getActivity().getContentResolver(), "enable_freeform_support", 1);
                            U.showToastLong(getActivity(), R.string.tb_reboot_required);
                        } catch (Exception e) {
                            ((CheckBoxPreference) p).setChecked(false);

                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1
                                    && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                                builder.setTitle(R.string.tb_freeform_dialog_title)
                                        .setMessage(R.string.tb_freeform_dialog_message_alt)
                                        .setPositiveButton(R.string.tb_action_continue, (dialogInterface, i) -> freeformSetupComplete());
                            } else {
                                String settingName = Build.VERSION.SDK_INT > Build.VERSION_CODES.P
                                        ? getString(R.string.tb_enable_freeform_windows)
                                        : getString(R.string.tb_force_activities_resizable);

                                builder.setTitle(R.string.tb_freeform_dialog_title)
                                        .setMessage(getString(R.string.tb_freeform_dialog_message, settingName))
                                        .setPositiveButton(R.string.tb_action_developer_options, (dialogInterface, i) -> {
                                            showReminderToast = true;

                                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                                            try {
                                                startActivity(intent);
                                                U.showToast(getActivity(), getString(R.string.tb_enable_force_activities_resizable, settingName), Toast.LENGTH_LONG);
                                            } catch (ActivityNotFoundException e1) {
                                                intent = new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS);
                                                try {
                                                    startActivity(intent);
                                                    U.showToastLong(getActivity(), R.string.tb_enable_developer_options);
                                                } catch (ActivityNotFoundException ignored) {}
                                            }
                                        });
                            }

                            AlertDialog dialog = builder.create();
                            dialog.show();
                            dialog.setCancelable(false);
                        }
                    }

                    if(pref.getBoolean(PREF_TASKBAR_ACTIVE, false)
                            && !FreeformHackHelper.getInstance().isFreeformHackActive()) {
                        U.startFreeformHack(getActivity(), true);
                    }
                } else {
                    U.stopFreeformHack(getActivity());
                    U.sendBroadcast(getActivity(), ACTION_FORCE_TASKBAR_RESTART);
                }

                U.restartNotificationService(getActivity());
                U.sendBroadcast(getActivity(), ACTION_FREEFORM_PREF_CHANGED);
                break;
            case PREF_ADD_SHORTCUT:
                U.pinAppShortcut(getActivity());
                break;
            case PREF_WINDOW_SIZE:
                if(U.hasBrokenSetLaunchBoundsApi())
                    U.showToastLong(getActivity(), R.string.tb_window_sizes_not_available);
                break;
        }

        return super.onPreferenceClick(p);
    }

    private void freeformSetupComplete() {
        ((CheckBoxPreference) findPreference(PREF_FREEFORM_HACK)).setChecked(U.hasFreeformSupport(getActivity()));

        if(U.hasFreeformSupport(getActivity())) {
            U.showToastLong(getActivity(), R.string.tb_reboot_required);
        }
    }
}
