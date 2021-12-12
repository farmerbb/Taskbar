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
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.KeyboardShortcutActivityLockDevice;
import com.farmerbb.taskbar.activity.NavigationBarButtonsActivity;
import com.farmerbb.taskbar.activity.HomeActivity;
import com.farmerbb.taskbar.activity.KeyboardShortcutActivity;
import com.farmerbb.taskbar.util.DependencyUtils;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class AdvancedFragment extends SettingsFragment {

    boolean secondScreenPrefEnabled = false;

    private final BroadcastReceiver homeToggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences pref = U.getSharedPreferences(getActivity());
            CheckBoxPreference checkBox = (CheckBoxPreference) findPreference(PREF_LAUNCHER);
            checkBox.setChecked(pref.getBoolean(PREF_LAUNCHER, false));
        }
    };

    @Override
    protected void addPrefsToSanitize() {
        prefsToSanitize.put(PREF_DASHBOARD, R.bool.class);
    }

    @Override
    protected void loadPrefs() {
        // Add preferences
        addPreferencesFromResource(R.xml.tb_pref_advanced);

        // Set OnClickListeners for certain preferences
        findPreference(PREF_DASHBOARD_GRID_SIZE).setOnPreferenceClickListener(this);
        findPreference(PREF_KEYBOARD_SHORTCUT).setSummary(DependencyUtils.getKeyboardShortcutSummary(getActivity()));

        boolean isLibrary = U.isLibrary(getActivity());
        boolean isAndroidx86 = getActivity().getPackageName().equals(BuildConfig.ANDROIDX86_APPLICATION_ID);

        SharedPreferences pref = U.getSharedPreferences(getActivity());
        boolean lockHomeToggle = (pref.getBoolean(PREF_LAUNCHER, false)
                && U.isLauncherPermanentlyEnabled(getActivity()))
                || pref.getBoolean(PREF_DESKTOP_MODE, false);

        if(isLibrary) {
            getPreferenceScreen().removePreference(findPreference(PREF_TASKER_ENABLED));
            getPreferenceScreen().removePreference(findPreference(PREF_LAUNCHER));
            getPreferenceScreen().removePreference(findPreference(PREF_KEYBOARD_SHORTCUT));
            getPreferenceScreen().removePreference(findPreference(PREF_NAVIGATION_BAR_BUTTONS));
            getPreferenceScreen().removePreference(findPreference(PREF_MANAGE_APP_DATA));

            findPreference(PREF_CLEAR_PINNED_APPS).setOnPreferenceClickListener(this);
        } else {
            findPreference(PREF_LAUNCHER).setEnabled(!lockHomeToggle);
            findPreference(PREF_LAUNCHER).setOnPreferenceClickListener(this);
            findPreference(PREF_NAVIGATION_BAR_BUTTONS).setOnPreferenceClickListener(this);
            findPreference(PREF_MANAGE_APP_DATA).setOnPreferenceClickListener(this);

            if(!U.isChromeOs(getActivity()))
                findPreference(PREF_KEYBOARD_SHORTCUT).setOnPreferenceClickListener(this);
            else
                getPreferenceScreen().removePreference(findPreference(PREF_KEYBOARD_SHORTCUT));

            getPreferenceScreen().removePreference(findPreference(PREF_CLEAR_PINNED_APPS));
        }

        if(!isAndroidx86 && !isLibrary
                && U.isPlayStoreInstalled(getActivity())
                && U.isPlayStoreRelease(getActivity())) {
            findPreference(PREF_SECONDSCREEN).setOnPreferenceClickListener(this);
            secondScreenPrefEnabled = true;

            if(U.isDesktopModeSupported(getActivity()))
                findPreference(PREF_SECONDSCREEN).setSummary(R.string.tb_pref_secondscreen_description_alt);
        } else
            getPreferenceScreen().removePreference(findPreference(PREF_SECONDSCREEN));

        bindPreferenceSummaryToValue(findPreference(PREF_DASHBOARD));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !isLibrary
                && !U.canEnableFreeform(getActivity(), false)) {
            bindPreferenceSummaryToValue(findPreference(PREF_OVERRIDE_FREEFORM_UNSUPPORTED));
        } else
            getPreferenceScreen().removePreference(findPreference(PREF_OVERRIDE_FREEFORM_UNSUPPORTED));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.tb_pref_header_advanced);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        if(secondScreenPrefEnabled) {
            findPreference(PREF_SECONDSCREEN).setTitle(
                    U.getSecondScreenPackageName(getActivity()) == null
                            ? R.string.tb_pref_secondscreen_title_install
                            : R.string.tb_pref_secondscreen_title_open);
        }

        updateDashboardGridSize(false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        U.registerReceiver(getActivity(), homeToggleReceiver, ACTION_LAUNCHER_PREF_CHANGED);
    }

    @Override
    public void onDetach() {
        U.unregisterReceiver(getActivity(), homeToggleReceiver);

        super.onDetach();
    }

    @SuppressLint("SetTextI18n")
    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceClick(final Preference p) {
        final SharedPreferences pref = U.getSharedPreferences(getActivity());

        switch(p.getKey()) {
            case PREF_LAUNCHER:
                if(U.canDrawOverlays(getActivity())) {
                    U.setComponentEnabled(getActivity(), HomeActivity.class,
                            ((CheckBoxPreference) p).isChecked());
                } else {
                    U.showPermissionDialog(getActivity());
                    ((CheckBoxPreference) p).setChecked(false);
                }

                if(!((CheckBoxPreference) p).isChecked()) {
                    U.sendBroadcast(getActivity(), ACTION_KILL_HOME_ACTIVITY);
                }
                break;
            case PREF_KEYBOARD_SHORTCUT:
                U.setComponentEnabled(getActivity(), KeyboardShortcutActivity.class,
                        ((CheckBoxPreference) p).isChecked());

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    U.setComponentEnabled(getActivity(), KeyboardShortcutActivityLockDevice.class,
                            ((CheckBoxPreference) p).isChecked());
                }
                break;
            case PREF_DASHBOARD_GRID_SIZE:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LinearLayout dialogLayout = (LinearLayout) View.inflate(getActivity(), R.layout.tb_dashboard_size_dialog, null);

                int orientation = U.getDisplayOrientation(getActivity());
                boolean isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT;
                boolean isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE;

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

                final EditText editText = dialogLayout.findViewById(editTextId);
                final EditText editText2 = dialogLayout.findViewById(editText2Id);

                builder.setView(dialogLayout)
                        .setTitle(R.string.tb_dashboard_grid_size)
                        .setPositiveButton(R.string.tb_action_ok, (dialog, id) -> {
                            boolean successfullyUpdated = false;

                            String widthString = editText.getText().toString();
                            String heightString = editText2.getText().toString();

                            if(widthString.length() > 0 && heightString.length() > 0) {
                                int width = Integer.parseInt(widthString);
                                int height = Integer.parseInt(heightString);

                                if(width > 0 && height > 0) {
                                    SharedPreferences.Editor editor = pref.edit();
                                    editor.putInt(PREF_DASHBOARD_WIDTH, width);
                                    editor.putInt(PREF_DASHBOARD_HEIGHT, height);
                                    editor.putBoolean(PREF_DASHBOARD_WIDTH + isModified, true);
                                    editor.putBoolean(PREF_DASHBOARD_HEIGHT + isModified, true);
                                    editor.apply();

                                    updateDashboardGridSize(true);
                                    successfullyUpdated = true;
                                }
                            }

                            if(!successfullyUpdated)
                                U.showToast(getActivity(), R.string.tb_invalid_grid_size);
                        })
                        .setNegativeButton(R.string.tb_action_cancel, null)
                        .setNeutralButton(R.string.tb_use_default, (dialog, id) -> {
                            SharedPreferences.Editor editor = pref.edit();
                            editor.remove(PREF_DASHBOARD_WIDTH);
                            editor.remove(PREF_DASHBOARD_HEIGHT);
                            editor.remove(PREF_DASHBOARD_WIDTH + isModified);
                            editor.remove(PREF_DASHBOARD_HEIGHT + isModified);
                            editor.apply();

                            updateDashboardGridSize(true);
                        });

                editText.setText(Integer.toString(U.getIntPrefWithDefault(getActivity(), PREF_DASHBOARD_WIDTH)));
                editText2.setText(Integer.toString(U.getIntPrefWithDefault(getActivity(), PREF_DASHBOARD_HEIGHT)));

                AlertDialog dialog = builder.create();
                dialog.show();

                U.newHandler().post(() -> {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editText2, InputMethodManager.SHOW_IMPLICIT);
                });

                break;
            case PREF_NAVIGATION_BAR_BUTTONS:
                Intent intent = U.getThemedIntent(getActivity(), NavigationBarButtonsActivity.class);
                startActivity(intent);
                break;
            case PREF_SECONDSCREEN:
                PackageManager packageManager = getActivity().getPackageManager();
                String packageName = U.getSecondScreenPackageName(getActivity());
                Intent intent2;

                if(packageName == null) {
                    intent2 = new Intent(Intent.ACTION_VIEW);
                    intent2.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.farmerbb.secondscreen.free"));
                } else
                    intent2 = packageManager.getLaunchIntentForPackage(packageName);

                if(intent2 != null) {
                    intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    try {
                        startActivity(intent2);
                    } catch (ActivityNotFoundException ignored) {}
                }

                break;
            case PREF_MANAGE_APP_DATA:
                navigateTo(new ManageAppDataFragment());
                break;
        }

        return super.onPreferenceClick(p);
    }

    private void updateDashboardGridSize(boolean restartTaskbar) {
        int width = U.getIntPrefWithDefault(getActivity(), PREF_DASHBOARD_WIDTH);
        int height = U.getIntPrefWithDefault(getActivity(), PREF_DASHBOARD_HEIGHT);

        int orientation = U.getDisplayOrientation(getActivity());
        boolean isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        boolean isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE;

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

        findPreference(PREF_DASHBOARD_GRID_SIZE).setSummary(getString(R.string.tb_dashboard_grid_description, first, second));

        if(restartTaskbar) U.restartTaskbar(getActivity());
    }
}
