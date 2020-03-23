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

package com.farmerbb.taskbar.activity;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.content.TaskbarIntent;
import com.farmerbb.taskbar.fragment.AboutFragment;
import com.farmerbb.taskbar.fragment.AdvancedFragment;
import com.farmerbb.taskbar.fragment.AppearanceFragment;
import com.farmerbb.taskbar.fragment.DesktopModeFragment;
import com.farmerbb.taskbar.fragment.FreeformModeFragment;
import com.farmerbb.taskbar.fragment.ManageAppDataFragment;
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
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private SwitchCompat theSwitch;
    private ImageView helpButton;

    private BroadcastReceiver switchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSwitch();
        }
    };

    private boolean hasCaption = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(
                        switchReceiver,
                        new IntentFilter(TaskbarIntent.ACTION_UPDATE_SWITCH)
                );

        final SharedPreferences pref = U.getSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();

        if(!U.isLibrary(this)) {
            switch(pref.getString("theme", "light")) {
                case "light":
                    setTheme(R.style.Taskbar);
                    break;
                case "dark":
                    setTheme(R.style.Taskbar_Dark);
                    break;
            }
        } else {
            int theme = getIntent().getIntExtra("theme", -1);
            if(theme != -1)
                setTheme(theme);
        }

        if(pref.getBoolean("taskbar_active", false) && !U.isServiceRunning(this, NotificationService.class))
            editor.putBoolean("taskbar_active", false);

        // Ensure that components that should be enabled are enabled properly
        boolean launcherEnabled = (pref.getBoolean("launcher", false) && U.canDrawOverlays(this))
                || U.isLauncherPermanentlyEnabled(this);

        boolean desktopModeEnabled = U.isDesktopModeSupported(this)
                && pref.getBoolean("desktop_mode", false);

        editor.putBoolean("launcher", launcherEnabled);
        editor.putBoolean("desktop_mode", desktopModeEnabled);
        editor.apply();

        if(!U.isLibrary(this)) {
            ComponentName component = new ComponentName(this, HomeActivity.class);
            getPackageManager().setComponentEnabledSetting(component,
                    launcherEnabled && !U.isDelegatingHomeActivity(this)
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            ComponentName component2 = new ComponentName(this, KeyboardShortcutActivity.class);
            getPackageManager().setComponentEnabledSetting(component2,
                    pref.getBoolean("keyboard_shortcut", false)
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            ComponentName component3 = new ComponentName(this, ShortcutActivity.class);
            getPackageManager().setComponentEnabledSetting(component3,
                    U.enableFreeformModeShortcut(this)
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            ComponentName component4 = new ComponentName(this, StartTaskbarActivity.class);
            getPackageManager().setComponentEnabledSetting(component4,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);

            ComponentName component5 = new ComponentName(this, SecondaryHomeActivity.class);
            getPackageManager().setComponentEnabledSetting(component5,
                    desktopModeEnabled
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            ComponentName component6 = new ComponentName(this, HSLActivity.class);
            getPackageManager().setComponentEnabledSetting(component6,
                    desktopModeEnabled
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ComponentName component7 = new ComponentName(this, KeyboardShortcutActivityLockDevice.class);
                getPackageManager().setComponentEnabledSetting(component7,
                        pref.getBoolean("keyboard_shortcut", false)
                                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        }

        if (!launcherEnabled) {
            LocalBroadcastManager
                    .getInstance(this)
                    .sendBroadcast(new Intent(TaskbarIntent.ACTION_KILL_HOME_ACTIVITY));
        }
        // Update caption state
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && U.isChromeOs(this)) {
            getWindow().setRestrictedCaptionAreaListener(rect -> hasCaption = true);

            new Handler().postDelayed(() -> pref.edit().putBoolean("has_caption", hasCaption).apply(), 500);
        }

        if(getPackageName().equals(BuildConfig.PAID_APPLICATION_ID)) {
            File file = new File(getFilesDir() + File.separator + "imported_successfully");
            if(freeVersionInstalled() && !file.exists()) {
                startActivity(new Intent(this, ImportSettingsActivity.class));
                finish();
            } else
                proceedWithAppLaunch(savedInstanceState);
        } else
            proceedWithAppLaunch(savedInstanceState);
    }

    private boolean freeVersionInstalled() {
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pInfo = pm.getPackageInfo(BuildConfig.BASE_APPLICATION_ID, 0);
            return pInfo.versionCode >= 68
                    && pm.checkSignatures(BuildConfig.BASE_APPLICATION_ID, getPackageName())
                    == PackageManager.SIGNATURE_MATCH;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void proceedWithAppLaunch(Bundle savedInstanceState) {
        try {
            setContentView(R.layout.tb_main);
        } catch (IllegalStateException e) {
            setTheme(R.style.Theme_AppCompat_Light);
            setContentView(R.layout.tb_main);
        }

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null && !U.isLibrary(this)) {
            actionBar.setCustomView(R.layout.tb_switch_layout);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
        }

        theSwitch = findViewById(R.id.the_switch);
        helpButton = findViewById(R.id.help_button);

        if(theSwitch != null) {
            final SharedPreferences pref = U.getSharedPreferences(this);
            theSwitch.setChecked(pref.getBoolean("taskbar_active", false));

            theSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
                if(b) {
                    if(U.canDrawOverlays(this)) {
                        boolean firstRun = pref.getBoolean("first_run", true);
                        startTaskbarService();

                        if(firstRun)
                            U.showRecentAppsDialog(this);
                    } else {
                        U.showPermissionDialog(this);
                        compoundButton.setChecked(false);
                    }
                } else
                    stopTaskbarService();
            });
        }

        if(savedInstanceState == null) {
            U.initPrefs(this);

            File restoreInProgress = new File(getFilesDir(), "restore_in_progress");
            File restoreSuccessful = new File(getFilesDir(), "restore_successful");

            if(restoreInProgress.exists() || restoreSuccessful.exists()) {
                if(restoreInProgress.exists()) {
                    U.showToastLong(this, R.string.tb_restore_failed);
                    restoreInProgress.delete();
                }

                if(restoreSuccessful.exists()) {
                    U.showToastLong(this, R.string.tb_restore_successful);
                    restoreSuccessful.delete();
                }

                getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new ManageAppDataFragment(), "ManageAppDataFragment").commit();
            } else if(!getIntent().hasExtra("theme_change"))
                getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new AboutFragment(), "AboutFragment").commit();
            else
                getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new AppearanceFragment(), "AppearanceFragment").commit();
        }

        SharedPreferences pref = U.getSharedPreferences(this);
        if(!getPackageName().equals(BuildConfig.BASE_APPLICATION_ID) && freeVersionInstalled()) {
            if(!pref.getBoolean("dont_show_uninstall_dialog", false)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.tb_settings_imported_successfully)
                        .setMessage(R.string.tb_import_dialog_message)
                        .setPositiveButton(R.string.tb_action_uninstall, (dialog, which) -> {
                            pref.edit().putBoolean("uninstall_dialog_shown", true).apply();

                            try {
                                startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + BuildConfig.BASE_APPLICATION_ID)));
                            } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
                        });

                if(pref.getBoolean("uninstall_dialog_shown", false))
                    builder.setNegativeButton(R.string.tb_action_dont_show_again, (dialogInterface, i) -> pref.edit().putBoolean("dont_show_uninstall_dialog", true).apply());

                AlertDialog dialog = builder.create();
                dialog.show();
                dialog.setCancelable(false);
            }

            if(!pref.getBoolean("uninstall_dialog_shown", false)) {
                if(theSwitch != null) theSwitch.setChecked(false);

                SharedPreferences.Editor editor = pref.edit();

                String iconPack = pref.getString("icon_pack", BuildConfig.BASE_APPLICATION_ID);
                if(iconPack.contains(BuildConfig.BASE_APPLICATION_ID)) {
                    editor.putString("icon_pack", getPackageName());
                } else {
                    U.refreshPinnedIcons(this);
                }

                editor.putBoolean("first_run", true);
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
                        .setShortLabel(getString(R.string.tb_start_taskbar))
                        .setIcon(Icon.createWithResource(this, R.drawable.tb_shortcut_icon_start))
                        .setIntent(intent)
                        .build();

                if(U.enableFreeformModeShortcut(this)) {
                    Intent intent2 = new Intent(Intent.ACTION_MAIN);
                    intent2.setClass(this, ShortcutActivity.class);
                    intent2.putExtra("is_launching_shortcut", true);

                    ShortcutInfo shortcut2 = new ShortcutInfo.Builder(this, "freeform_mode")
                            .setShortLabel(getString(R.string.tb_pref_header_freeform))
                            .setIcon(Icon.createWithResource(this, R.drawable.tb_shortcut_icon_freeform))
                            .setIntent(intent2)
                            .build();

                    shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut, shortcut2));
                } else
                    shortcutManager.setDynamicShortcuts(Collections.singletonList(shortcut));
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(switchReceiver);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && U.isChromeOs(this))
            getWindow().setRestrictedCaptionAreaListener(null);

        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.N)
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

        if(U.hasFreeformSupport(this)
                && pref.getBoolean("freeform_hack", false)
                && !FreeformHackHelper.getInstance().isFreeformHackActive()) {
            U.startFreeformHack(this, true);
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

            LocalBroadcastManager
                    .getInstance(this)
                    .sendBroadcast(new Intent(TaskbarIntent.ACTION_START_MENU_DISAPPEARING));
        }

        stopService(new Intent(this, NotificationService.class));
    }

    private void updateSwitch() {
        if(theSwitch != null) {
            SharedPreferences pref = U.getSharedPreferences(this);
            theSwitch.setChecked(pref.getBoolean("taskbar_active", false));
        }
    }

    @Override
    public void onBackPressed() {
        Fragment oldFragment = getFragmentManager().findFragmentById(R.id.fragmentContainer);

        if(oldFragment instanceof AboutFragment)
            super.onBackPressed();
        else {
            Fragment newFragment;
            String tag;

            if(oldFragment instanceof ManageAppDataFragment) {
                newFragment = new AdvancedFragment();
                tag = "AdvancedFragment";
            } else {
                newFragment = new AboutFragment();
                tag = "AboutFragment";
            }

            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, newFragment, tag)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .commit();
        }
    }

    public String getAboutFragmentTitle() {
        if(!U.isLibrary(this))
            return getString(R.string.tb_app_name);

        String title = getIntent().getStringExtra("title");
        return title != null ? title : getString(R.string.tb_settings);
    }

    public boolean getAboutFragmentBackArrow() {
        if(!U.isLibrary(this))
            return false;

        return getIntent().getBooleanExtra("back_arrow", false);
    }

    public void updateHelpButton(SettingsFragment fragment) {
        if(fragment instanceof FreeformModeFragment) {
            helpButton.setVisibility(View.VISIBLE);
            helpButton.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setView(View.inflate(MainActivity.this, R.layout.tb_freeform_help_dialog, null))
                        .setTitle(R.string.tb_freeform_help_dialog_title)
                        .setPositiveButton(R.string.tb_action_close, null);

                AlertDialog dialog = builder.create();
                dialog.show();
            });
        } else if(fragment instanceof DesktopModeFragment) {
            helpButton.setVisibility(View.VISIBLE);
            helpButton.setOnClickListener(v -> {
                // TODO
            });
        } else {
            helpButton.setVisibility(View.INVISIBLE);
            helpButton.setOnClickListener(null);
        }
    }
}