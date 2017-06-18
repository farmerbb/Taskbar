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

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.ClearDataActivity;
import com.farmerbb.taskbar.activity.NavigationBarButtonsActivity;
import com.farmerbb.taskbar.activity.dark.ClearDataActivityDark;
import com.farmerbb.taskbar.activity.HomeActivity;
import com.farmerbb.taskbar.activity.KeyboardShortcutActivity;
import com.farmerbb.taskbar.activity.dark.NavigationBarButtonsActivityDark;
import com.farmerbb.taskbar.util.U;
import com.mikepenz.iconics.Iconics;

public class AdvancedFragment extends SettingsFragment implements Preference.OnPreferenceClickListener {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onActivityCreated(savedInstanceState);

        if(findPreference("dummy") == null) {
            // Add preferences
            addPreferencesFromResource(R.xml.pref_advanced);

            // Set OnClickListeners for certain preferences
            findPreference("clear_pinned_apps").setOnPreferenceClickListener(this);
            findPreference("launcher").setOnPreferenceClickListener(this);
            findPreference("keyboard_shortcut").setOnPreferenceClickListener(this);
            findPreference("dashboard_grid_size").setOnPreferenceClickListener(this);
            findPreference("navigation_bar_buttons").setOnPreferenceClickListener(this);
            findPreference("keyboard_shortcut").setSummary(getKeyboardShortcutSummary());

            bindPreferenceSummaryToValue(findPreference("dashboard"));

            SharedPreferences pref = U.getSharedPreferences(getActivity());
            boolean lockHomeToggle = pref.getBoolean("launcher", false)
                    && (U.hasSupportLibrary(getActivity())
                    || BuildConfig.APPLICATION_ID.equals(BuildConfig.ANDROIDX86_APPLICATION_ID));

            findPreference("launcher").setEnabled(!lockHomeToggle);
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.pref_header_advanced);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        finishedLoadingPrefs = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        updateDashboardGridSize(false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public boolean onPreferenceClick(final Preference p) {
        final SharedPreferences pref = U.getSharedPreferences(getActivity());

        switch(p.getKey()) {
            case "clear_pinned_apps":
                Intent clearIntent = null;

                switch(pref.getString("theme", "light")) {
                    case "light":
                        clearIntent = new Intent(getActivity(), ClearDataActivity.class);
                        break;
                    case "dark":
                        clearIntent = new Intent(getActivity(), ClearDataActivityDark.class);
                        break;
                }

                startActivity(clearIntent);
                break;
            case "launcher":
                if(U.canDrawOverlays(getActivity())) {
                    ComponentName component = new ComponentName(getActivity(), HomeActivity.class);
                    getActivity().getPackageManager().setComponentEnabledSetting(component,
                            ((CheckBoxPreference) p).isChecked() ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                } else {
                    U.showPermissionDialog(getActivity());
                    ((CheckBoxPreference) p).setChecked(false);
                }

                if(!((CheckBoxPreference) p).isChecked())
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("com.farmerbb.taskbar.KILL_HOME_ACTIVITY"));
                break;
            case "keyboard_shortcut":
                ComponentName component = new ComponentName(getActivity(), KeyboardShortcutActivity.class);
                getActivity().getPackageManager().setComponentEnabledSetting(component,
                        ((CheckBoxPreference) p).isChecked() ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                break;
            case "dashboard_grid_size":
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LinearLayout dialogLayout = (LinearLayout) View.inflate(getActivity(), R.layout.dashboard_size_dialog, null);

                boolean isPortrait = getActivity().getApplicationContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
                boolean isLandscape = getActivity().getApplicationContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

                int editTextId = -1;
                int editText2Id = -1;

                if(isPortrait) {
                    editTextId = R.id.fragmentEditText2;
                    editText2Id = R.id.fragmentEditText1;
                }

                if(isLandscape) {
                    editTextId = R.id.fragmentEditText1;
                    editText2Id = R.id.fragmentEditText2;
                }

                final EditText editText = (EditText) dialogLayout.findViewById(editTextId);
                final EditText editText2 = (EditText) dialogLayout.findViewById(editText2Id);

                builder.setView(dialogLayout)
                        .setTitle(R.string.dashboard_grid_size)
                        .setPositiveButton(R.string.action_ok, (dialog, id) -> {
                            boolean successfullyUpdated = false;

                            String widthString = editText.getText().toString();
                            String heightString = editText2.getText().toString();

                            if(widthString.length() > 0 && heightString.length() > 0) {
                                int width = Integer.parseInt(widthString);
                                int height = Integer.parseInt(heightString);

                                if(width > 0 && height > 0) {
                                    SharedPreferences.Editor editor = pref.edit();
                                    editor.putInt("dashboard_width", width);
                                    editor.putInt("dashboard_height", height);
                                    editor.apply();

                                    updateDashboardGridSize(true);
                                    successfullyUpdated = true;
                                }
                            }

                            if(!successfullyUpdated)
                                U.showToast(getActivity(), R.string.invalid_grid_size);
                        })
                        .setNegativeButton(R.string.action_cancel, null);

                editText.setText(Integer.toString(pref.getInt("dashboard_width", getActivity().getApplicationContext().getResources().getInteger(R.integer.dashboard_width))));
                editText2.setText(Integer.toString(pref.getInt("dashboard_height", getActivity().getApplicationContext().getResources().getInteger(R.integer.dashboard_height))));

                AlertDialog dialog = builder.create();
                dialog.show();

                new Handler().post(() -> {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editText2, InputMethodManager.SHOW_IMPLICIT);
                });

                break;
            case "navigation_bar_buttons":
                Intent intent = null;

                switch(pref.getString("theme", "light")) {
                    case "light":
                        intent = new Intent(getActivity(), NavigationBarButtonsActivity.class);
                        break;
                    case "dark":
                        intent = new Intent(getActivity(), NavigationBarButtonsActivityDark.class);
                        break;
                }

                startActivity(intent);
                break;
        }

        return true;
    }

    private void updateDashboardGridSize(boolean restartTaskbar) {
        SharedPreferences pref = U.getSharedPreferences(getActivity());
        int width = pref.getInt("dashboard_width", getActivity().getApplicationContext().getResources().getInteger(R.integer.dashboard_width));
        int height = pref.getInt("dashboard_height", getActivity().getApplicationContext().getResources().getInteger(R.integer.dashboard_height));

        boolean isPortrait = getActivity().getApplicationContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        boolean isLandscape = getActivity().getApplicationContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        int first = -1;
        int second = -1;

        if(isPortrait) {
            first = height;
            second = width;
        }

        if(isLandscape) {
            first = width;
            second = height;
        }

        findPreference("dashboard_grid_size").setSummary(getString(R.string.dashboard_grid_description, first, second));

        if(restartTaskbar) U.restartTaskbar(getActivity());
    }

    private CharSequence getKeyboardShortcutSummary() {
        if(BuildConfig.APPLICATION_ID.equals(BuildConfig.ANDROIDX86_APPLICATION_ID)) {
            return getString(R.string.pref_description_keyboard_shortcut_alt);
        } else {
            return new Iconics.IconicsBuilder()
                    .ctx(getActivity())
                    .on(getString(R.string.pref_description_keyboard_shortcut))
                    .build();
        }
    }
}
