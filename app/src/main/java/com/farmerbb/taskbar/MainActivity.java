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

package com.farmerbb.taskbar;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;

import com.enrico.colorpicker.colorDialog;
import com.farmerbb.taskbar.activity.HomeActivity;
import com.farmerbb.taskbar.activity.ImportSettingsActivity;
import com.farmerbb.taskbar.activity.KeyboardShortcutActivity;
import com.farmerbb.taskbar.activity.ShortcutActivity;
import com.farmerbb.taskbar.activity.StartTaskbarActivity;
import com.farmerbb.taskbar.fragment.AboutFragment;
import com.farmerbb.taskbar.fragment.AppearanceFragment;
import com.farmerbb.taskbar.fragment.SettingsFragment;
import com.farmerbb.taskbar.service.DashboardService;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.LauncherHelper;
import com.farmerbb.taskbar.util.U;

import java.io.File;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements colorDialog.ColorSelectedListener {

    private SwitchCompat theSwitch;

    private BroadcastReceiver switchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSwitch();
        }
    };

    public final int BACKGROUND_TINT = 1;
    public final int ACCENT_COLOR = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this).registerReceiver(switchReceiver, new IntentFilter("com.farmerbb.taskbar.UPDATE_SWITCH"));

        final SharedPreferences pref = U.getSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();

        switch(pref.getString("theme", "light")) {
            case "light":
                setTheme(R.style.AppTheme);
                break;
            case "dark":
                setTheme(R.style.AppTheme_Dark);
                break;
        }

        if(pref.getBoolean("taskbar_active", false) && !U.isServiceRunning(this, NotificationService.class))
            editor.putBoolean("taskbar_active", false);

        // Ensure that components that should be enabled are enabled properly
        boolean launcherEnabled = pref.getBoolean("launcher", false) && canDrawOverlays();
        editor.putBoolean("launcher", launcherEnabled);

        editor.apply();

        ComponentName component = new ComponentName(this, HomeActivity.class);
        getPackageManager().setComponentEnabledSetting(component,
                launcherEnabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        ComponentName component2 = new ComponentName(this, KeyboardShortcutActivity.class);
        getPackageManager().setComponentEnabledSetting(component2,
                pref.getBoolean("keyboard_shortcut", false) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        ComponentName component3 = new ComponentName(this, ShortcutActivity.class);
        getPackageManager().setComponentEnabledSetting(component3,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        ComponentName component4 = new ComponentName(this, StartTaskbarActivity.class);
        getPackageManager().setComponentEnabledSetting(component4,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        if(!launcherEnabled)
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.KILL_HOME_ACTIVITY"));

        if(BuildConfig.APPLICATION_ID.equals(BuildConfig.BASE_APPLICATION_ID))
            proceedWithAppLaunch(savedInstanceState);
        else {
            File file = new File(getFilesDir() + File.separator + "imported_successfully");
            if(freeVersionInstalled() && !file.exists()) {
                startActivity(new Intent(this, ImportSettingsActivity.class));
                finish();
            } else {
                proceedWithAppLaunch(savedInstanceState);
            }
        }
    }

    private boolean freeVersionInstalled() {
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pInfo = pm.getPackageInfo(BuildConfig.BASE_APPLICATION_ID, 0);
            return pInfo.versionCode >= 68
                    && pm.checkSignatures(BuildConfig.BASE_APPLICATION_ID, BuildConfig.APPLICATION_ID)
                    == PackageManager.SIGNATURE_MATCH;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void proceedWithAppLaunch(Bundle savedInstanceState) {
        setContentView(R.layout.main);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setCustomView(R.layout.switch_layout);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
        }

        theSwitch = (SwitchCompat) findViewById(R.id.the_switch);
        if(theSwitch != null) {
            final SharedPreferences pref = U.getSharedPreferences(this);
            theSwitch.setChecked(pref.getBoolean("taskbar_active", false));

            theSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
                if(b) {
                    if(canDrawOverlays()) {
                        boolean firstRun = pref.getBoolean("first_run", true);
                        startTaskbarService();

                        if(firstRun && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isSystemApp()) {
                            ApplicationInfo applicationInfo = null;
                            try {
                                applicationInfo = getPackageManager().getApplicationInfo(BuildConfig.APPLICATION_ID, 0);
                            } catch (PackageManager.NameNotFoundException e) { /* Gracefully fail */ }

                            if(applicationInfo != null) {
                                AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
                                int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);

                                if(mode != AppOpsManager.MODE_ALLOWED) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setTitle(R.string.pref_header_recent_apps)
                                            .setMessage(R.string.enable_recent_apps)
                                            .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                                                try {
                                                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                                                    U.showToastLong(MainActivity.this, R.string.usage_stats_message);
                                                } catch (ActivityNotFoundException e) {
                                                    U.showErrorDialog(MainActivity.this, "GET_USAGE_STATS");
                                                }
                                            }).setNegativeButton(R.string.action_cancel, null);

                                    AlertDialog dialog = builder.create();
                                    dialog.show();
                                }
                            }
                        }
                    } else {
                        U.showPermissionDialog(MainActivity.this);
                        compoundButton.setChecked(false);
                    }
                } else
                    stopTaskbarService();
            });
        }

        if(savedInstanceState == null) {
            if(!getIntent().hasExtra("theme_change"))
                getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new AboutFragment(), "AboutFragment").commit();
            else
                getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new AppearanceFragment(), "AppearanceFragment").commit();
        }

        if(!BuildConfig.APPLICATION_ID.equals(BuildConfig.BASE_APPLICATION_ID) && freeVersionInstalled()) {
            final SharedPreferences pref = U.getSharedPreferences(this);
            if(!pref.getBoolean("dont_show_uninstall_dialog", false)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.settings_imported_successfully)
                        .setMessage(R.string.import_dialog_message)
                        .setPositiveButton(R.string.action_uninstall, (dialog, which) -> {
                            pref.edit().putBoolean("uninstall_dialog_shown", true).apply();

                            try {
                                startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + BuildConfig.BASE_APPLICATION_ID)));
                            } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
                        });

                if(pref.getBoolean("uninstall_dialog_shown", false))
                    builder.setNegativeButton(R.string.action_dont_show_again, (dialogInterface, i) -> pref.edit().putBoolean("dont_show_uninstall_dialog", true).apply());

                AlertDialog dialog = builder.create();
                dialog.show();
                dialog.setCancelable(false);
            }

            if(!pref.getBoolean("uninstall_dialog_shown", false)) {
                if(theSwitch != null) theSwitch.setChecked(false);

                SharedPreferences.Editor editor = pref.edit();

                String iconPack = pref.getString("icon_pack", BuildConfig.BASE_APPLICATION_ID);
                if(iconPack.contains(BuildConfig.BASE_APPLICATION_ID)) {
                    editor.putString("icon_pack", BuildConfig.APPLICATION_ID);
                } else {
                    U.refreshPinnedIcons(this);
                }

                editor.putBoolean("first_run", true);
                editor.putBoolean("dashboard_tutorial_shown", false);
                editor.apply();
            }
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

            if(shortcutManager.getDynamicShortcuts().size() == 0) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClass(this, StartTaskbarActivity.class);
                intent.putExtra("is_launching_shortcut", true);

                ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "start_taskbar")
                        .setShortLabel(getString(R.string.start_taskbar))
                        .setIcon(Icon.createWithResource(this, R.drawable.shortcut_icon_start))
                        .setIntent(intent)
                        .build();

                Intent intent2 = new Intent(Intent.ACTION_MAIN);
                intent2.setClass(this, ShortcutActivity.class);
                intent2.putExtra("is_launching_shortcut", true);

                ShortcutInfo shortcut2 = new ShortcutInfo.Builder(this, "freeform_mode")
                        .setShortLabel(getString(R.string.pref_header_freeform))
                        .setIcon(Icon.createWithResource(this, R.drawable.shortcut_icon_freeform))
                        .setIntent(intent2)
                        .build();

                shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut, shortcut2));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateSwitch();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(switchReceiver);
    }

    @SuppressWarnings("deprecation")
    private void startTaskbarService() {
        SharedPreferences pref = U.getSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();

        editor.putBoolean("is_hidden", false);

        if(pref.getBoolean("first_run", true)) {
            editor.putBoolean("first_run", false);
            editor.putBoolean("collapsed", true);
        }

        editor.putBoolean("taskbar_active", true);
        editor.putLong("time_of_service_start", System.currentTimeMillis());
        editor.apply();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && pref.getBoolean("freeform_hack", false)
                && isInMultiWindowMode()
                && !FreeformHackHelper.getInstance().isFreeformHackActive()) {
            U.startFreeformHack(this, false, false);
        }

        startService(new Intent(this, TaskbarService.class));
        startService(new Intent(this, StartMenuService.class));
        startService(new Intent(this, DashboardService.class));
        startService(new Intent(this, NotificationService.class));
    }

    private void stopTaskbarService() {
        SharedPreferences pref = U.getSharedPreferences(this);
        pref.edit().putBoolean("taskbar_active", false).apply();

        if(!LauncherHelper.getInstance().isOnHomeScreen()) {
            stopService(new Intent(this, TaskbarService.class));
            stopService(new Intent(this, StartMenuService.class));
            stopService(new Intent(this, DashboardService.class));

            IconCache.getInstance(this).clearCache();

            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.START_MENU_DISAPPEARING"));
        }

        stopService(new Intent(this, NotificationService.class));
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void updateSwitch() {
        if(theSwitch != null) {
            SharedPreferences pref = U.getSharedPreferences(this);
            theSwitch.setChecked(pref.getBoolean("taskbar_active", false));
        }
    }

    @Override
    public void onBackPressed() {
        if(getFragmentManager().findFragmentById(R.id.fragmentContainer) instanceof AboutFragment)
            super.onBackPressed();
        else
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new AboutFragment(), "AboutFragment")
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .commit();
    }

    private boolean isSystemApp() {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(BuildConfig.APPLICATION_ID, 0);
            int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
            return (info.flags & mask) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onColorSelection(DialogFragment dialogFragment, int color) {
        SharedPreferences pref = U.getSharedPreferences(this);
        String preferenceId = null;

        switch(Integer.parseInt(dialogFragment.getTag())) {
            case BACKGROUND_TINT:
                preferenceId = "background_tint";
                break;
            case ACCENT_COLOR:
                preferenceId = "accent_color";
                break;
        }

        pref.edit().putInt(preferenceId, color).apply();

        SettingsFragment fragment = (SettingsFragment) getFragmentManager().findFragmentById(R.id.fragmentContainer);
        colorDialog.setColorPreferenceSummary(fragment.findPreference(preferenceId + "_pref"), color, this, getResources());

        fragment.restartTaskbar();
    }
}