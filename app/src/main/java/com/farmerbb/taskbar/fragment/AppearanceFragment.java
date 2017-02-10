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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.enrico.colorpicker.colorDialog;
import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.MainActivity;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.IconPackActivity;
import com.farmerbb.taskbar.activity.dark.IconPackActivityDark;
import com.farmerbb.taskbar.util.U;

public class AppearanceFragment extends SettingsFragment implements Preference.OnPreferenceClickListener {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onActivityCreated(savedInstanceState);

        if(findPreference("dummy") == null) {
            // Add preferences
            addPreferencesFromResource(R.xml.pref_appearance);

            // Set OnClickListeners for certain preferences
            findPreference("icon_pack_list").setOnPreferenceClickListener(this);
            findPreference("reset_colors").setOnPreferenceClickListener(this);
            findPreference("background_tint_pref").setOnPreferenceClickListener(this);
            findPreference("accent_color_pref").setOnPreferenceClickListener(this);

            bindPreferenceSummaryToValue(findPreference("theme"));
            bindPreferenceSummaryToValue(findPreference("invisible_button"));
            bindPreferenceSummaryToValue(findPreference("app_drawer_icon"));
            bindPreferenceSummaryToValue(findPreference("icon_pack_use_mask"));
            bindPreferenceSummaryToValue(findPreference("visual_feedback"));
            bindPreferenceSummaryToValue(findPreference("shortcut_icon"));
            bindPreferenceSummaryToValue(findPreference("transparent_start_menu"));

            colorDialog.setColorPreferenceSummary(findPreference("background_tint_pref"), U.getBackgroundTint(getActivity()), getActivity(), getResources());
            colorDialog.setColorPreferenceSummary(findPreference("accent_color_pref"), U.getAccentColor(getActivity()), getActivity(), getResources());
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.pref_header_appearance);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        finishedLoadingPrefs = true;
    }

    @Override
    public void onResume() {
        super.onResume();

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

    @Override
    public boolean onPreferenceClick(final Preference p) {
        final SharedPreferences pref = U.getSharedPreferences(getActivity());

        switch(p.getKey()) {
            case "icon_pack_list":
                Intent intent = null;

                switch(pref.getString("theme", "light")) {
                    case "light":
                        intent = new Intent(getActivity(), IconPackActivity.class);
                        break;
                    case "dark":
                        intent = new Intent(getActivity(), IconPackActivityDark.class);
                        break;
                }

                startActivityForResult(intent, 123);
                break;
            case "reset_colors":
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.reset_colors)
                        .setMessage(R.string.are_you_sure)
                        .setNegativeButton(R.string.action_cancel, null)
                        .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                            finishedLoadingPrefs = false;

                            pref.edit().remove("background_tint").remove("accent_color").apply();

                            colorDialog.setColorPreferenceSummary(findPreference("background_tint_pref"), U.getBackgroundTint(getActivity()), getActivity(), getResources());
                            colorDialog.setColorPreferenceSummary(findPreference("accent_color_pref"), U.getAccentColor(getActivity()), getActivity(), getResources());

                            finishedLoadingPrefs = true;
                            restartTaskbar();
                        });

                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            case "background_tint_pref":
                U.cancelToast();

                MainActivity activity = (MainActivity) getActivity();

                colorDialog.setPickerColor(activity, activity.BACKGROUND_TINT, U.getBackgroundTint(activity));
                colorDialog.showColorPicker(activity, activity.BACKGROUND_TINT);
                break;
            case "accent_color_pref":
                U.cancelToast();

                MainActivity activity2 = (MainActivity) getActivity();

                colorDialog.setPickerColor(activity2, activity2.ACCENT_COLOR, U.getAccentColor(activity2));
                colorDialog.showColorPicker(activity2, activity2.ACCENT_COLOR);
                break;
        }

        return true;
    }
}
