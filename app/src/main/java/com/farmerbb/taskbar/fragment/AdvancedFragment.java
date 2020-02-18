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
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.KeyboardShortcutActivityLockDevice;
import com.farmerbb.taskbar.activity.NavigationBarButtonsActivity;
import com.farmerbb.taskbar.activity.SecondaryHomeActivity;
import com.farmerbb.taskbar.activity.HomeActivity;
import com.farmerbb.taskbar.activity.KeyboardShortcutActivity;
import com.farmerbb.taskbar.activity.dark.NavigationBarButtonsActivityDark;
import com.farmerbb.taskbar.util.DependencyUtils;
import com.farmerbb.taskbar.util.U;

public class AdvancedFragment extends SettingsFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    boolean secondScreenPrefEnabled = false;

    private BroadcastReceiver homeToggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences pref = U.getSharedPreferences(getActivity());
            CheckBoxPreference checkBox = (CheckBoxPreference) findPreference("launcher");
            checkBox.setChecked(pref.getBoolean("launcher", false));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onCreate(savedInstanceState);

        // Add preferences
        addPreferencesFromResource(R.xml.tb_pref_advanced);

        // Set OnClickListeners for certain preferences
        findPreference("dashboard_grid_size").setOnPreferenceClickListener(this);
        findPreference("keyboard_shortcut").setSummary(DependencyUtils.getKeyboardShortcutSummary(getActivity()));

        boolean isLibrary = U.isLibrary(getActivity());
        boolean isAndroidx86 = getActivity().getPackageName().equals(BuildConfig.ANDROIDX86_APPLICATION_ID);

        if(isLibrary) {
            getPreferenceScreen().removePreference(findPreference("launcher"));
            getPreferenceScreen().removePreference(findPreference("keyboard_shortcut"));
            getPreferenceScreen().removePreference(findPreference("navigation_bar_buttons"));
        } else {
            findPreference("launcher").setOnPreferenceClickListener(this);
            findPreference("keyboard_shortcut").setOnPreferenceClickListener(this);
            findPreference("navigation_bar_buttons").setOnPreferenceClickListener(this);
        }

        if(!isAndroidx86 && !isLibrary && U.isPlayStoreInstalled(getActivity()) && U.isPlayStoreRelease(getActivity())) {
            findPreference("secondscreen").setOnPreferenceClickListener(this);
            secondScreenPrefEnabled = true;
        } else
            getPreferenceScreen().removePreference(findPreference("secondscreen"));

        if(isAndroidx86 || isLibrary)
            getPreferenceScreen().removePreference(findPreference("tasker_enabled"));

        bindPreferenceSummaryToValue(findPreference("dashboard"));

        SharedPreferences pref = U.getSharedPreferences(getActivity());
        boolean lockHomeToggle = pref.getBoolean("launcher", false)
                && U.isLauncherPermanentlyEnabled(getActivity());

        if(!isLibrary)
            findPreference("launcher").setEnabled(!lockHomeToggle);

        if(getArguments() != null && getArguments().getBoolean("from_manage_app_data", false)) {
            View rootView = getView();
            if(rootView != null) {
                ListView list = rootView.findViewById(android.R.id.list);
                if(list != null) list.scrollTo(0, Integer.MAX_VALUE);
            }
        }

        if(U.isExternalAccessDisabled(getActivity())) {
            addPreferencesFromResource(R.xml.tb_pref_advanced_extra_1);
            findPreference("clear_pinned_apps").setOnPreferenceClickListener(this);
        } else {
            addPreferencesFromResource(R.xml.tb_pref_advanced_extra_2);
            findPreference("manage_app_data").setOnPreferenceClickListener(this);
        }

        finishedLoadingPrefs = true;
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
            findPreference("secondscreen").setTitle(
                    U.getSecondScreenPackageName(getActivity()) == null
                            ? R.string.tb_pref_secondscreen_title_install
                            : R.string.tb_pref_secondscreen_title_open);
        }

        updateDashboardGridSize(false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(homeToggleReceiver,
                new IntentFilter("com.farmerbb.taskbar.LAUNCHER_PREF_CHANGED"));
    }

    @Override
    public void onDetach() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(homeToggleReceiver);

        super.onDetach();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public boolean onPreferenceClick(final Preference p) {
        final SharedPreferences pref = U.getSharedPreferences(getActivity());

        switch(p.getKey()) {
            case "launcher":
                if(U.canDrawOverlays(getActivity())) {
                    ComponentName component = new ComponentName(getActivity(), HomeActivity.class);
                    getActivity().getPackageManager().setComponentEnabledSetting(component,
                            ((CheckBoxPreference) p).isChecked()
                                    ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);

                    ComponentName component2 = new ComponentName(getActivity(), SecondaryHomeActivity.class);
                    getActivity().getPackageManager().setComponentEnabledSetting(component2,
                            ((CheckBoxPreference) p).isChecked() && BuildConfig.DEBUG
                                    ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
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

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ComponentName component2 = new ComponentName(getActivity(), KeyboardShortcutActivityLockDevice.class);
                    getActivity().getPackageManager().setComponentEnabledSetting(component2,
                            ((CheckBoxPreference) p).isChecked() ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                }
                break;
            case "dashboard_grid_size":
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LinearLayout dialogLayout = (LinearLayout) View.inflate(getActivity(), R.layout.tb_dashboard_size_dialog, null);

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
                                    editor.putInt("dashboard_width", width);
                                    editor.putInt("dashboard_height", height);
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
                            pref.edit().remove("dashboard_width").remove("dashboard_height").apply();
                            updateDashboardGridSize(true);
                        });

                editText.setText(Integer.toString(pref.getInt("dashboard_width", getActivity().getApplicationContext().getResources().getInteger(R.integer.tb_dashboard_width))));
                editText2.setText(Integer.toString(pref.getInt("dashboard_height", getActivity().getApplicationContext().getResources().getInteger(R.integer.tb_dashboard_height))));

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
            case "secondscreen":
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
                    } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
                }

                break;
            case "manage_app_data":
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new ManageAppDataFragment(), "ManageAppDataFragment")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
                break;
        }

        return super.onPreferenceClick(p);
    }

    private void updateDashboardGridSize(boolean restartTaskbar) {
        SharedPreferences pref = U.getSharedPreferences(getActivity());
        int width = pref.getInt("dashboard_width", getActivity().getApplicationContext().getResources().getInteger(R.integer.tb_dashboard_width));
        int height = pref.getInt("dashboard_height", getActivity().getApplicationContext().getResources().getInteger(R.integer.tb_dashboard_height));

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

        findPreference("dashboard_grid_size").setSummary(getString(R.string.tb_dashboard_grid_description, first, second));

        if(restartTaskbar) U.restartTaskbar(getActivity());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(!U.isLibrary(getActivity()) && key.equals("tasker_enabled")) {
            boolean enabled = sharedPreferences.getBoolean(key, true);

            if(enabled) {
                getPreferenceScreen().removePreference(findPreference("clear_pinned_apps"));
                addPreferencesFromResource(R.xml.tb_pref_advanced_extra_2);
                findPreference("manage_app_data").setOnPreferenceClickListener(this);
            } else {
                getPreferenceScreen().removePreference(findPreference("manage_app_data"));
                addPreferencesFromResource(R.xml.tb_pref_advanced_extra_1);
                findPreference("clear_pinned_apps").setOnPreferenceClickListener(this);
            }
        }
    }
}
