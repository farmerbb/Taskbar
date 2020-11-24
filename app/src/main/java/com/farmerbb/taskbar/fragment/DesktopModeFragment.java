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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.provider.Settings;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.EnableAdditionalSettingsActivity;
import com.farmerbb.taskbar.activity.HSLActivity;
import com.farmerbb.taskbar.activity.HSLConfigActivity;
import com.farmerbb.taskbar.activity.SecondaryHomeActivity;
import com.farmerbb.taskbar.service.DisableKeyboardService;
import com.farmerbb.taskbar.util.DisplayInfo;
import com.farmerbb.taskbar.helper.LauncherHelper;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class DesktopModeFragment extends SettingsFragment {

    public static boolean isConfiguringHomeApp;
    private boolean isConfiguringDeveloperOptions;

    private boolean updateAdditionalSettings;

    private final DisplayManager.DisplayListener listener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
            updateAdditionalSettings();
        }

        @Override
        public void onDisplayChanged(int displayId) {
            updateAdditionalSettings();
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            updateAdditionalSettings();
        }
    };

    @Override
    protected void loadPrefs() {
        // Add preferences
        addPreferencesFromResource(R.xml.tb_pref_desktop_mode);

        // Set OnClickListeners for certain preferences
        findPreference(PREF_DESKTOP_MODE).setOnPreferenceClickListener(this);
        findPreference(PREF_SET_LAUNCHER_DEFAULT).setOnPreferenceClickListener(this);
        findPreference(PREF_PRIMARY_LAUNCHER).setOnPreferenceClickListener(this);
        findPreference(PREF_DIM_SCREEN).setOnPreferenceClickListener(this);
        findPreference(PREF_ENABLE_ADDITIONAL_SETTINGS).setOnPreferenceClickListener(this);

        if(!U.isShowHideNavbarSupported()) {
            PreferenceCategory category = (PreferenceCategory) findPreference(PREF_ADDITIONAL_SETTINGS);
            category.removePreference(findPreference(PREF_AUTO_HIDE_NAVBAR_DESKTOP_MODE));
        }

        if(U.getCurrentApiVersion() > 29.0)
            findPreference(PREF_DESKTOP_MODE_IME_FIX).setOnPreferenceClickListener(this);
        else
            getPreferenceScreen().removePreference(findPreference(PREF_DESKTOP_MODE_IME_FIX));

        SharedPreferences pref = U.getSharedPreferences(getActivity());
        if(pref.getBoolean(PREF_LAUNCHER, false)) {
            findPreference(PREF_DESKTOP_MODE).setEnabled(false);
            U.showToastLong(getActivity(), R.string.tb_disable_home_setting);
        } else
            bindPreferenceSummaryToValue(findPreference(PREF_DESKTOP_MODE));

        bindPreferenceSummaryToValue(findPreference(PREF_DISPLAY_DENSITY));

        DisplayManager manager = (DisplayManager) getActivity().getSystemService(Context.DISPLAY_SERVICE);
        manager.registerDisplayListener(listener, null);

        updateAdditionalSettings();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.tb_pref_header_desktop_mode);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        if(updateAdditionalSettings) {
            updateAdditionalSettings = false;
            updateAdditionalSettings();
        }

        if(isConfiguringHomeApp) {
            isConfiguringHomeApp = false;

            if(showReminderToast) {
                showReminderToast = false;
                desktopModeSetupComplete();
            } else
                startStopDesktopMode(true);
        }

        if(isConfiguringDeveloperOptions && !isConfiguringHomeApp) {
            isConfiguringDeveloperOptions = false;

            boolean desktopModeEnabled = U.isDesktopModePrefEnabled(getActivity());
            ((CheckBoxPreference) findPreference(PREF_DESKTOP_MODE)).setChecked(desktopModeEnabled);

            handleDesktopModePrefChange(desktopModeEnabled);

            if(desktopModeEnabled) {
                showReminderToast = true;
                configureHomeApp();
            }
        }

        Preference primaryLauncherPref = findPreference(PREF_PRIMARY_LAUNCHER);
        if(primaryLauncherPref != null) {
            SharedPreferences pref = U.getSharedPreferences(getActivity());
            String primaryLauncherName = pref.getString(PREF_HSL_NAME, "null");
            String primaryLauncherPackage = pref.getString(PREF_HSL_ID, "null");

            boolean primaryLauncherValid = true;
            try {
                getActivity().getPackageManager().getPackageInfo(primaryLauncherPackage, 0);
            } catch (PackageManager.NameNotFoundException e) {
                primaryLauncherValid = false;
            }

            primaryLauncherPref.setSummary(primaryLauncherValid
                    ? primaryLauncherName
                    : getString(R.string.tb_icon_pack_none)
            );
        }
    }

    @Override
    public void onDestroy() {
        DisplayManager manager = (DisplayManager) getActivity().getSystemService(Context.DISPLAY_SERVICE);
        manager.unregisterDisplayListener(listener);

        super.onDestroy();
    }

    @Override
    public boolean onPreferenceClick(final Preference p) {
        switch(p.getKey()) {
            case PREF_DESKTOP_MODE:
                boolean isChecked = ((CheckBoxPreference) p).isChecked();

                if(isChecked && !U.isDesktopModePrefEnabled(getActivity())) {
                    try {
                        Settings.Global.putInt(getActivity().getContentResolver(), "enable_freeform_support", 1);
                        Settings.Global.putInt(getActivity().getContentResolver(), "force_desktop_mode_on_external_displays", 1);
                        U.showToastLong(getActivity(), R.string.tb_reboot_required);
                    } catch (Exception e) {
                        ((CheckBoxPreference) p).setChecked(false);
                        isChecked = false;

                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(R.string.tb_desktop_mode_dialog_title)
                                .setMessage(R.string.tb_desktop_mode_dialog_message)
                                .setPositiveButton(R.string.tb_action_developer_options, (dialogInterface, i) -> {
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                                    try {
                                        startActivity(intent);
                                        isConfiguringDeveloperOptions = true;
                                    } catch (ActivityNotFoundException e1) {
                                        intent = new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS);
                                        try {
                                            startActivity(intent);
                                            U.showToastLong(getActivity(), R.string.tb_enable_developer_options);
                                        } catch (ActivityNotFoundException ignored) {}
                                    }
                                });

                        AlertDialog dialog = builder.create();
                        dialog.show();
                        dialog.setCancelable(false);
                    }
                }

                handleDesktopModePrefChange(isChecked);
                break;
            case PREF_SET_LAUNCHER_DEFAULT:
                configureHomeApp();
                break;
            case PREF_PRIMARY_LAUNCHER:
                Intent intent = new Intent(getActivity(), HSLConfigActivity.class);
                intent.putExtra("return_to_settings", true);
                startActivity(intent);

                break;
            case PREF_AUTO_HIDE_NAVBAR_DESKTOP_MODE:
                LauncherHelper helper = LauncherHelper.getInstance();
                if(helper.isOnSecondaryHomeScreen(getActivity()))
                    U.showHideNavigationBar(getActivity(), helper.getSecondaryDisplayId(), !((CheckBoxPreference) p).isChecked(), 0);

                break;
            case PREF_DIM_SCREEN:
                if(!((CheckBoxPreference) p).isChecked())
                    U.sendBroadcast(getActivity(), ACTION_FINISH_DIM_SCREEN_ACTIVITY);

                break;
            case PREF_ENABLE_ADDITIONAL_SETTINGS:
                updateAdditionalSettings = true;
                startActivity(U.getThemedIntent(getActivity(), EnableAdditionalSettingsActivity.class));
                break;
            case PREF_DESKTOP_MODE_IME_FIX:
                U.setComponentEnabled(getActivity(), DisableKeyboardService.class, true);

                try {
                    startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
                } catch (ActivityNotFoundException ignored) {}

                break;
        }

        return super.onPreferenceClick(p);
    }

    private void startStopDesktopMode(boolean start) {
        if(!start || !U.isDesktopModeActive(getActivity()) || !U.launcherIsDefault(getActivity()))
            U.sendBroadcast(getActivity(), ACTION_KILL_HOME_ACTIVITY);
        else if(!LauncherHelper.getInstance().isOnSecondaryHomeScreen(getActivity()))
            U.showToastLong(getActivity(), R.string.tb_desktop_mode_setup_complete);
    }

    private void updateAdditionalSettings() {
        SharedPreferences pref = U.getSharedPreferences(getActivity());
        updateAdditionalSettings(pref.getBoolean(PREF_DESKTOP_MODE, false));
    }

    private void updateAdditionalSettings(boolean desktopModeEnabled) {
        finishedLoadingPrefs = false;

        boolean enabled = desktopModeEnabled
                && U.hasWriteSecureSettingsPermission(getActivity())
                && U.isDesktopModeActive(getActivity());

        findPreference(PREF_DISPLAY_DENSITY).setEnabled(enabled);

        if(U.isShowHideNavbarSupported()) {
            findPreference(PREF_AUTO_HIDE_NAVBAR_DESKTOP_MODE).setEnabled(enabled);
            findPreference(PREF_AUTO_HIDE_NAVBAR_DESKTOP_MODE).setOnPreferenceClickListener(this);
        }

        SharedPreferences pref = U.getSharedPreferences(getActivity());
        DisplayInfo info = U.getExternalDisplayInfo(getActivity());
        String densityPrefValue = info.currentDensity == info.defaultDensity
                ? "reset"
                : Integer.toString(info.currentDensity);

        pref.edit().putString(PREF_DISPLAY_DENSITY, densityPrefValue).apply();

        String[] noDefaultList = getResources().getStringArray(R.array.tb_pref_display_density_list_alt);
        String[] noDefaultValues = getResources().getStringArray(R.array.tb_pref_display_density_list_values_alt);
        boolean useNoDefault = false;

        for(int i = 0; i < noDefaultValues.length; i++) {
            if(info.defaultDensity == Integer.parseInt(noDefaultValues[i])) {
                noDefaultList[i] = getString(R.string.tb_density_default, info.defaultDensity);
                noDefaultValues[i] = "reset";
                useNoDefault = true;
                break;
            }
        }

        ListPreference densityPref = ((ListPreference) findPreference(PREF_DISPLAY_DENSITY));
        if(useNoDefault) {
            densityPref.setEntries(noDefaultList);
            densityPref.setEntryValues(noDefaultValues);
        } else {
            densityPref.setEntries(R.array.tb_pref_display_density_list);
            densityPref.setEntryValues(R.array.tb_pref_display_density_list_values);
        }

        densityPref.setValue(densityPrefValue);

        bindPreferenceSummaryToValue(densityPref);
        if(densityPref.getSummary() == null || densityPref.getSummary().equals(""))
            densityPref.setSummary(getString(R.string.tb_density_custom, info.currentDensity));

        finishedLoadingPrefs = true;
    }

    private void handleDesktopModePrefChange(boolean isChecked) {
        U.setComponentEnabled(getActivity(), SecondaryHomeActivity.class, isChecked);
        U.setComponentEnabled(getActivity(), HSLActivity.class, isChecked);

        if(U.getCurrentApiVersion() > 29.0) {
            U.setComponentEnabled(getActivity(), DisableKeyboardService.class, isChecked);
        }

        startStopDesktopMode(isChecked);
        updateAdditionalSettings(isChecked);
    }

    private void configureHomeApp() {
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            isConfiguringHomeApp = true;
        } catch (ActivityNotFoundException e) {
            U.showToastLong(getActivity(), R.string.tb_unable_to_set_default_home);
            showReminderToast = false;
        }
    }

    private void desktopModeSetupComplete() {
        boolean desktopModeEnabled = U.isDesktopModePrefEnabled(getActivity());
        ((CheckBoxPreference) findPreference(PREF_DESKTOP_MODE)).setChecked(desktopModeEnabled);

        if(desktopModeEnabled)
            U.showToastLong(getActivity(), R.string.tb_reboot_required_alt);

        updateAdditionalSettings();
    }
}