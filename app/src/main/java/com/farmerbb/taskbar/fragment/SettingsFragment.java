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
import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Display;
import android.view.MenuItem;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.MainActivity;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.HomeActivity;
import com.farmerbb.taskbar.activity.InvisibleActivityFreeform;
import com.farmerbb.taskbar.activity.KeyboardShortcutActivity;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.PinnedBlockedApps;
import com.farmerbb.taskbar.util.U;

public class SettingsFragment extends PreferenceFragment implements OnPreferenceClickListener {

    private boolean finishedLoadingPrefs;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onActivityCreated(savedInstanceState);

        // Set values
        setRetainInstance(true);
        setHasOptionsMenu(true);

        // Add preferences
        addPreferencesFromResource(R.xml.pref_general);
        addPreferencesFromResource(R.xml.pref_recent_apps);
        addPreferencesFromResource(R.xml.pref_advanced);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SharedPreferences pref = U.getSharedPreferences(getActivity());
            if(!pref.getBoolean("freeform_hack_override", false)) {
                pref.edit()
                        .putBoolean("freeform_hack", hasFreeformSupport())
                        .putBoolean("freeform_hack_override", true)
                        .apply();
            }

            addPreferencesFromResource(R.xml.pref_freeform_hack);
            findPreference("freeform_hack").setOnPreferenceClickListener(this);
        }

        addPreferencesFromResource(R.xml.pref_about);

        // Set OnClickListeners for certain preferences
        findPreference("clear_pinned_apps").setOnPreferenceClickListener(this);
        findPreference("enable_recents").setOnPreferenceClickListener(this);
        findPreference("launcher").setOnPreferenceClickListener(this);
        findPreference("keyboard_shortcut").setOnPreferenceClickListener(this);
        findPreference("about").setOnPreferenceClickListener(this);
        findPreference("about").setSummary(getString(R.string.pref_about_description, new String(Character.toChars(0x1F601))));

        bindPreferenceSummaryToValue(findPreference("start_menu_layout"));
        bindPreferenceSummaryToValue(findPreference("refresh_frequency"));
        bindPreferenceSummaryToValue(findPreference("recents_amount"));
        bindPreferenceSummaryToValue(findPreference("sort_order"));
        bindPreferenceSummaryToValue(findPreference("show_background"));
        bindPreferenceSummaryToValue(findPreference("scrollbar"));
        bindPreferenceSummaryToValue(findPreference("position"));
        bindPreferenceSummaryToValue(findPreference("theme"));
        bindPreferenceSummaryToValue(findPreference("shortcut_icon"));

        finishedLoadingPrefs = true;
    }

    private Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @SuppressLint("CommitPrefEdits")
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if(preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference
                        .setSummary(index >= 0 ? listPreference.getEntries()[index]
                                : null);

                if(finishedLoadingPrefs && preference.getKey().equals("theme")) {
                    // Restart MainActivity
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    getActivity().overridePendingTransition(0, 0);
                }

            } else if(!(preference instanceof CheckBoxPreference)) {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }

            if(finishedLoadingPrefs) restartTaskbar();

            return true;
        }
    };

    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference
                .setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        if(!(preference instanceof CheckBoxPreference)) sBindPreferenceSummaryToValueListener.onPreferenceChange(
                preference,
                PreferenceManager.getDefaultSharedPreferences(
                        preference.getContext()).getString(preference.getKey(),
                        ""));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Override default Android "up" behavior to instead mimic the back button
                getActivity().onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean onPreferenceClick(final Preference p) {
        switch(p.getKey()) {
            case "clear_pinned_apps":
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.clear_pinned_apps)
                        .setMessage(R.string.are_you_sure)
                        .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                PinnedBlockedApps.getInstance(getActivity()).clear(getActivity());
                            }
                        }).setNegativeButton(R.string.action_cancel, null);

                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            case "enable_recents":
                try {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                } catch (ActivityNotFoundException e) {
                    U.showErrorDialog(getActivity(), "GET_USAGE_STATS");
                }
                break;
            case "launcher":
                if(canDrawOverlays()) {
                    ComponentName component = new ComponentName(BuildConfig.APPLICATION_ID, HomeActivity.class.getName());
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
                ComponentName component = new ComponentName(BuildConfig.APPLICATION_ID, KeyboardShortcutActivity.class.getName());
                getActivity().getPackageManager().setComponentEnabledSetting(component,
                        ((CheckBoxPreference) p).isChecked() ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                break;
            case "about":
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
                break;
            case "freeform_hack":
                if(((CheckBoxPreference) p).isChecked()) {
                    if(!hasFreeformSupport()) {
                        ((CheckBoxPreference) p).setChecked(false);

                        AlertDialog.Builder builder2 = new AlertDialog.Builder(getActivity());
                        builder2.setTitle(R.string.freeform_dialog_title)
                                .setMessage(R.string.freeform_dialog_message)
                                .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        ((CheckBoxPreference) p).setChecked(true);
                                    }
                                });

                        AlertDialog dialog2 = builder2.create();
                        dialog2.show();
                        dialog2.setCancelable(false);
                    }

                    SharedPreferences pref = U.getSharedPreferences(getActivity());
                    if(pref.getBoolean("taskbar_active", false)
                            && getActivity().isInMultiWindowMode()
                            && !FreeformHackHelper.getInstance().isFreeformHackActive()) {
                        DisplayManager dm = (DisplayManager) getActivity().getSystemService(Context.DISPLAY_SERVICE);
                        Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);

                        Intent freeformIntent = new Intent(getActivity(), InvisibleActivityFreeform.class);
                        freeformIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
                        startActivity(freeformIntent, ActivityOptions.makeBasic().setLaunchBounds(new Rect(display.getWidth(), display.getHeight(), display.getWidth() + 1, display.getHeight() + 1)).toBundle());
                    }
                } else
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("com.farmerbb.taskbar.FINISH_FREEFORM_ACTIVITY"));
                break;
        }

        return true;
    }

    private void startTaskbarService() {
        getActivity().startService(new Intent(getActivity(), TaskbarService.class));
        getActivity().startService(new Intent(getActivity(), StartMenuService.class));
        getActivity().startService(new Intent(getActivity(), NotificationService.class));
    }

    private void stopTaskbarService() {
        getActivity().stopService(new Intent(getActivity(), TaskbarService.class));
        getActivity().stopService(new Intent(getActivity(), StartMenuService.class));
        getActivity().stopService(new Intent(getActivity(), NotificationService.class));
    }

    private void restartTaskbar() {
        SharedPreferences pref = U.getSharedPreferences(getActivity());
        if(pref.getBoolean("taskbar_active", false)) {
            stopTaskbarService();
            startTaskbarService();
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private boolean hasFreeformSupport() {
        return getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)
                || Settings.Global.getInt(getActivity().getContentResolver(), "enable_freeform_support", -1) == 1
                || Settings.Global.getInt(getActivity().getContentResolver(), "force_resizable_activities", -1) == 1;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(getActivity());
    }
}