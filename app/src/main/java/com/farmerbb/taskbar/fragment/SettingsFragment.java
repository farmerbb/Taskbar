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
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
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
import android.view.View;
import android.widget.ListView;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.MainActivity;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.HomeActivity;
import com.farmerbb.taskbar.activity.IconPackActivity;
import com.farmerbb.taskbar.activity.IconPackActivityDark;
import com.farmerbb.taskbar.activity.InvisibleActivityFreeform;
import com.farmerbb.taskbar.activity.KeyboardShortcutActivity;
import com.farmerbb.taskbar.activity.SelectAppActivity;
import com.farmerbb.taskbar.activity.SelectAppActivityDark;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.PinnedBlockedApps;
import com.farmerbb.taskbar.util.SavedWindowSizes;
import com.farmerbb.taskbar.util.U;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class SettingsFragment extends PreferenceFragment implements OnPreferenceClickListener {

    boolean finishedLoadingPrefs;
    boolean showReminderToast = false;
    int noThanksCount = 0;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Remove dividers
        View rootView = getView();
        if(rootView != null) {
            ListView list = (ListView) rootView.findViewById(android.R.id.list);
            if(list != null) list.setDivider(null);
        }

        // Set values
        setRetainInstance(true);
        setHasOptionsMenu(true);

        // On smaller-screened devices, set "Grid" as the default start menu layout
        SharedPreferences pref = U.getSharedPreferences(getActivity());
        if(getResources().getConfiguration().smallestScreenWidthDp < 600
                && pref.getString("start_menu_layout", "null").equals("null")) {
            pref.edit().putString("start_menu_layout", "grid").apply();
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if(!pref.getBoolean("freeform_hack_override", false)) {
                pref.edit()
                        .putBoolean("freeform_hack", U.hasFreeformSupport(getActivity()))
                        .putBoolean("freeform_hack_override", true)
                        .apply();
            } else if(!U.hasFreeformSupport(getActivity())) {
                pref.edit().putBoolean("freeform_hack", false).apply();

                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("com.farmerbb.taskbar.FINISH_FREEFORM_ACTIVITY"));
            }
        }
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
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

                if(finishedLoadingPrefs && preference.getKey().equals("theme")) {
                    // Restart MainActivity
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("theme_change", true);
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

    void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        if(!(preference instanceof CheckBoxPreference))
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
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
        final SharedPreferences pref = U.getSharedPreferences(getActivity());

        switch(p.getKey()) {
            case "clear_pinned_apps":
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.clear_pinned_apps)
                        .setMessage(R.string.are_you_sure)
                        .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                PinnedBlockedApps.getInstance(getActivity()).clear(getActivity());
                                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    SavedWindowSizes.getInstance(getActivity()).clear(getActivity());
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
                U.checkForUpdates(getActivity());
                break;
            case "freeform_hack":
                if(((CheckBoxPreference) p).isChecked()) {
                    if(!U.hasFreeformSupport(getActivity())) {
                        ((CheckBoxPreference) p).setChecked(false);

                        AlertDialog.Builder builder2 = new AlertDialog.Builder(getActivity());
                        builder2.setTitle(R.string.freeform_dialog_title)
                                .setMessage(R.string.freeform_dialog_message)
                                .setPositiveButton(R.string.action_developer_options, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
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
                                    }
                                });

                        AlertDialog dialog2 = builder2.create();
                        dialog2.show();
                        dialog2.setCancelable(false);
                    }

                    if(pref.getBoolean("taskbar_active", false)
                            && getActivity().isInMultiWindowMode()
                            && !FreeformHackHelper.getInstance().isFreeformHackActive()) {
                        DisplayManager dm = (DisplayManager) getActivity().getSystemService(Context.DISPLAY_SERVICE);
                        Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);

                        Intent freeformIntent = new Intent(getActivity(), InvisibleActivityFreeform.class);
                        freeformIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
                        startActivity(freeformIntent, ActivityOptions.makeBasic().setLaunchBounds(new Rect(display.getWidth(), display.getHeight(), display.getWidth() + 1, display.getHeight() + 1)).toBundle());
                    }
                } else {
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("com.farmerbb.taskbar.FINISH_FREEFORM_ACTIVITY"));
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("com.farmerbb.taskbar.FORCE_TASKBAR_RESTART"));
                }

                findPreference("open_in_fullscreen").setEnabled(((CheckBoxPreference) p).isChecked());
                findPreference("save_window_sizes").setEnabled(((CheckBoxPreference) p).isChecked());
                findPreference("window_size").setEnabled(((CheckBoxPreference) p).isChecked());
                findPreference("add_shortcut").setEnabled(((CheckBoxPreference) p).isChecked());

                break;
            case "freeform_mode_help":
                AlertDialog.Builder builder2 = new AlertDialog.Builder(getActivity());
                builder2.setView(View.inflate(getActivity(), R.layout.freeform_help_dialog, null))
                        .setTitle(R.string.freeform_help_dialog_title)
                        .setPositiveButton(R.string.action_close, null);

                AlertDialog dialog2 = builder2.create();
                dialog2.show();
                break;
            case "blacklist":
                Intent intent = null;

                switch(pref.getString("theme", "light")) {
                    case "light":
                        intent = new Intent(getActivity(), SelectAppActivity.class);
                        break;
                    case "dark":
                        intent = new Intent(getActivity(), SelectAppActivityDark.class);
                        break;
                }

                startActivity(intent);
                break;
            case "donate":
                NumberFormat format = NumberFormat.getCurrencyInstance();
                format.setCurrency(Currency.getInstance(Locale.US));

                AlertDialog.Builder builder3 = new AlertDialog.Builder(getActivity());
                builder3.setTitle(R.string.pref_title_donate)
                        .setMessage(getString(R.string.dialog_donate_message, format.format(1.99)))
                        .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.BASE_APPLICATION_ID + ".paid"));
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                try {
                                    startActivity(intent);
                                } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
                            }
                        })
                        .setNegativeButton(R.string.action_no_thanks, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                noThanksCount++;

                                if(noThanksCount == 3) {
                                    pref.edit().putBoolean("hide_donate", true).apply();
                                    findPreference("donate").setEnabled(false);
                                }
                            }
                        });

                AlertDialog dialog3 = builder3.create();
                dialog3.show();
                break;
            case "pref_screen_general":
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new GeneralFragment(), "GeneralFragment")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
                break;
            case "pref_screen_recent_apps":
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new RecentAppsFragment(), "RecentAppsFragment")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
                break;
            case "pref_screen_freeform":
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new FreeformModeFragment(), "FreeformModeFragment")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
                break;
            case "pref_screen_advanced":
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new AdvancedFragment(), "AdvancedFragment")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
                break;
            case "icon_pack_list":
                Intent intent2 = null;

                switch(pref.getString("theme", "light")) {
                    case "light":
                        intent2 = new Intent(getActivity(), IconPackActivity.class);
                        break;
                    case "dark":
                        intent2 = new Intent(getActivity(), IconPackActivityDark.class);
                        break;
                }

                startActivityForResult(intent2, 123);
                break;
            case "add_shortcut":
                Intent intent3 = U.getShortcutIntent(getActivity());
                intent3.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                intent3.putExtra("duplicate", false);

                getActivity().sendBroadcast(intent3);

                U.showToast(getActivity(), R.string.shortcut_created);
                break;
        }

        return true;
    }

    private void startTaskbarService(boolean fullRestart) {
        getActivity().startService(new Intent(getActivity(), TaskbarService.class));
        getActivity().startService(new Intent(getActivity(), StartMenuService.class));
        if(fullRestart) getActivity().startService(new Intent(getActivity(), NotificationService.class));
    }

    private void stopTaskbarService(boolean fullRestart) {
        getActivity().stopService(new Intent(getActivity(), TaskbarService.class));
        getActivity().stopService(new Intent(getActivity(), StartMenuService.class));
        if(fullRestart) getActivity().stopService(new Intent(getActivity(), NotificationService.class));
    }

    private void restartTaskbar() {
        SharedPreferences pref = U.getSharedPreferences(getActivity());
        if(pref.getBoolean("taskbar_active", false) && !pref.getBoolean("is_hidden", false)) {
            pref.edit().putBoolean("is_restarting", true).apply();

            stopTaskbarService(true);
            startTaskbarService(true);
        } else if(isServiceRunning()) {
            stopTaskbarService(false);
            startTaskbarService(false);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(getActivity());
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(StartMenuService.class.getName().equals(service.service.getClassName()))
                return true;
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 123 && resultCode == Activity.RESULT_OK) {
            U.refreshPinnedIcons(getActivity());
            restartTaskbar();
        }
    }
}