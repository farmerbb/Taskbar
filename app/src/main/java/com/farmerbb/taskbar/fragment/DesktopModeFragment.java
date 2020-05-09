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
import android.provider.Settings;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.HSLActivity;
import com.farmerbb.taskbar.activity.HSLConfigActivity;
import com.farmerbb.taskbar.activity.SecondaryHomeActivity;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.util.DisplayInfo;
import com.farmerbb.taskbar.util.LauncherHelper;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class DesktopModeFragment extends SettingsFragment {

    public static boolean isConfiguringHomeApp;

    private DisplayManager.DisplayListener listener = new DisplayManager.DisplayListener() {
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
    public void onCreate(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onCreate(savedInstanceState);

        // Add preferences
        addPreferencesFromResource(R.xml.tb_pref_desktop_mode);

        // Set OnClickListeners for certain preferences
        findPreference("desktop_mode").setOnPreferenceClickListener(this);
        findPreference("set_launcher_default").setOnPreferenceClickListener(this);
        findPreference("primary_launcher").setOnPreferenceClickListener(this);

        SharedPreferences pref = U.getSharedPreferences(getActivity());
        if(pref.getBoolean("launcher", false)) {
            findPreference("desktop_mode").setEnabled(false);
            U.showToastLong(getActivity(), R.string.tb_disable_home_setting);
        } else
            bindPreferenceSummaryToValue(findPreference("desktop_mode"));

        bindPreferenceSummaryToValue(findPreference("display_density"));

        DisplayManager manager = (DisplayManager) getActivity().getSystemService(Context.DISPLAY_SERVICE);
        manager.registerDisplayListener(listener, null);

        updateAdditionalSettings();

        finishedLoadingPrefs = true;
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

        if(isConfiguringHomeApp)
            startStopDesktopMode(true);

        isConfiguringHomeApp = false;

        Preference primaryLauncherPref = findPreference("primary_launcher");
        if(primaryLauncherPref != null) {
            SharedPreferences pref = U.getSharedPreferences(getActivity());
            String primaryLauncherName = pref.getString("hsl_name", "null");
            String primaryLauncherPackage = pref.getString("hsl_id", "null");

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
            case "desktop_mode":
                boolean isChecked = ((CheckBoxPreference) p).isChecked();

                U.setComponentEnabled(getActivity(), SecondaryHomeActivity.class, isChecked);
                U.setComponentEnabled(getActivity(), HSLActivity.class, isChecked);
                startStopDesktopMode(isChecked);
                updateAdditionalSettings(isChecked);

                break;
            case "set_launcher_default":
                try {
                    startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                    isConfiguringHomeApp = true;
                } catch (ActivityNotFoundException e) {
                    U.showToastLong(getActivity(), R.string.tb_unable_to_set_default_home);
                }

                break;
            case "primary_launcher":
                Intent intent = new Intent(getActivity(), HSLConfigActivity.class);
                intent.putExtra("return_to_settings", true);
                startActivity(intent);

                break;
            case "auto_hide_navbar_desktop_mode":
                if(U.isServiceRunning(getActivity(), TaskbarService.class))
                    U.showHideNavigationBar(getActivity(), !((CheckBoxPreference) p).isChecked());

                break;
        }

        return super.onPreferenceClick(p);
    }

    private void startStopDesktopMode(boolean start) {
        if(!start || !U.isDesktopModeActive(getActivity()) || !U.launcherIsDefault(getActivity()))
            U.sendBroadcast(getActivity(), ACTION_KILL_HOME_ACTIVITY);
        else if(!LauncherHelper.getInstance().isOnSecondaryHomeScreen())
            U.showToastLong(getActivity(), R.string.tb_desktop_mode_setup_complete);
    }

    private void updateAdditionalSettings() {
        SharedPreferences pref = U.getSharedPreferences(getActivity());
        updateAdditionalSettings(pref.getBoolean("desktop_mode", false));
    }

    private void updateAdditionalSettings(boolean desktopModeEnabled) {
        finishedLoadingPrefs = false;

        boolean enabled = desktopModeEnabled
                && U.hasWriteSecureSettingsPermission(getActivity())
                && U.isDesktopModeActive(getActivity());

        findPreference("display_density").setEnabled(enabled);
        findPreference("auto_hide_navbar_desktop_mode").setEnabled(enabled);
        findPreference("auto_hide_navbar_desktop_mode").setOnPreferenceClickListener(this);

        SharedPreferences pref = U.getSharedPreferences(getActivity());
        DisplayInfo info = U.getExternalDisplayInfo(getActivity());
        String densityPrefValue = info.currentDensity == info.defaultDensity
                ? "reset"
                : Integer.toString(info.currentDensity);

        pref.edit().putString("display_density", densityPrefValue).apply();

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

        ListPreference densityPref = ((ListPreference) findPreference("display_density"));
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
}