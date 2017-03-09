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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.MainActivity;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.dark.SelectAppActivityDark;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.PinnedBlockedApps;
import com.farmerbb.taskbar.util.SavedWindowSizes;
import com.farmerbb.taskbar.util.U;

import java.util.List;

public class ContextMenuActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener {

    String packageName;
    String componentName;
    String appName;
    long userId = 0;

    boolean showStartMenu = false;
    boolean shouldHideTaskbar = false;
    boolean isStartButton = false;
    boolean isOverflowMenu = false;
    boolean secondaryMenu = false;
    boolean dashboardOrStartMenuAppearing = false;

    List<ShortcutInfo> shortcuts;

    private BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dashboardOrStartMenuAppearing = true;
            finish();
        }
    };

    @SuppressLint("RtlHardcoded")
    @SuppressWarnings("deprecation")
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.CONTEXT_MENU_APPEARING"));

        boolean isNonAppMenu = !getIntent().hasExtra("package_name") && !getIntent().hasExtra("app_name");
        showStartMenu = getIntent().getBooleanExtra("launched_from_start_menu", false);
        isStartButton = isNonAppMenu && getIntent().getBooleanExtra("is_start_button", false);
        isOverflowMenu = isNonAppMenu && getIntent().getBooleanExtra("is_overflow_menu", false);

        // Determine where to position the dialog on screen
        WindowManager.LayoutParams params = getWindow().getAttributes();
        DisplayManager dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);

        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if(resourceId > 0)
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);

        if(showStartMenu) {
            int x = getIntent().getIntExtra("x", 0);
            int y = getIntent().getIntExtra("y", 0);
            int offset = getResources().getDimensionPixelSize(isOverflowMenu ? R.dimen.context_menu_offset_overflow : R.dimen.context_menu_offset);

            switch(U.getTaskbarPosition(this)) {
                case "bottom_left":
                case "bottom_vertical_left":
                    params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                    params.x = x;
                    params.y = display.getHeight() - y - offset;
                    break;
                case "bottom_right":
                case "bottom_vertical_right":
                    params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                    params.x = x - getResources().getDimensionPixelSize(R.dimen.context_menu_width) + offset + offset;
                    params.y = display.getHeight() - y - offset;
                    break;
                case "top_left":
                case "top_vertical_left":
                    params.gravity = Gravity.TOP | Gravity.LEFT;
                    params.x = x;
                    params.y = y - offset + statusBarHeight;
                    break;
                case "top_right":
                case "top_vertical_right":
                    params.gravity = Gravity.TOP | Gravity.LEFT;
                    params.x = x - getResources().getDimensionPixelSize(R.dimen.context_menu_width) + offset + offset;
                    params.y = y - offset + statusBarHeight;
                    break;
            }
        } else {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));

            int x = getIntent().getIntExtra("x", display.getWidth());
            int y = getIntent().getIntExtra("y", display.getHeight());
            int offset = getResources().getDimensionPixelSize(R.dimen.icon_size);

            switch(U.getTaskbarPosition(this)) {
                case "bottom_left":
                    params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                    params.x = isStartButton ? 0 : x;
                    params.y = offset;
                    break;
                case "bottom_vertical_left":
                    params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                    params.x = offset;
                    params.y = display.getHeight() - y - (isStartButton ? 0 : offset);
                    break;
                case "bottom_right":
                    params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                    params.x = display.getWidth() - x;
                    params.y = offset;
                    break;
                case "bottom_vertical_right":
                    params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                    params.x = offset;
                    params.y = display.getHeight() - y - (isStartButton ? 0 : offset);
                    break;
                case "top_left":
                    params.gravity = Gravity.TOP | Gravity.LEFT;
                    params.x = isStartButton ? 0 : x;
                    params.y = offset;
                    break;
                case "top_vertical_left":
                    params.gravity = Gravity.TOP | Gravity.LEFT;
                    params.x = offset;
                    params.y = isStartButton ? 0 : y - statusBarHeight;
                    break;
                case "top_right":
                    params.gravity = Gravity.TOP | Gravity.RIGHT;
                    params.x = display.getWidth() - x;
                    params.y = offset;
                    break;
                case "top_vertical_right":
                    params.gravity = Gravity.TOP | Gravity.RIGHT;
                    params.x = offset;
                    params.y = isStartButton ? 0 : y - statusBarHeight;
                    break;
            }
        }

        params.width = getResources().getDimensionPixelSize(R.dimen.context_menu_width);
        params.dimAmount = 0;

        getWindow().setAttributes(params);

        View view = findViewById(android.R.id.list);
        if(view != null) view.setPadding(0, 0, 0, 0);

        generateMenu();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.farmerbb.taskbar.START_MENU_APPEARING");
        intentFilter.addAction("com.farmerbb.taskbar.DASHBOARD_APPEARING");

        LocalBroadcastManager.getInstance(this).registerReceiver(finishReceiver, intentFilter);
    }

    @SuppressWarnings("deprecation")
    private void generateMenu() {
        if(isStartButton) {
            addPreferencesFromResource(R.xml.pref_context_menu_open_settings);
            findPreference("open_taskbar_settings").setOnPreferenceClickListener(this);
            findPreference("start_menu_apps").setOnPreferenceClickListener(this);

            if(U.launcherIsDefault(this) && FreeformHackHelper.getInstance().isInFreeformWorkspace()) {
                addPreferencesFromResource(R.xml.pref_context_menu_change_wallpaper);
                findPreference("change_wallpaper").setOnPreferenceClickListener(this);
            }

            if(!getIntent().getBooleanExtra("dont_show_quit", false)) {
                addPreferencesFromResource(R.xml.pref_context_menu_quit);
                findPreference("quit_taskbar").setOnPreferenceClickListener(this);
            }
        } else if(isOverflowMenu) {
            if(getResources().getConfiguration().screenWidthDp >= 600
                    && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
                setTitle(R.string.tools);
            else {
                addPreferencesFromResource(R.xml.pref_context_menu_header);
                findPreference("header").setTitle(R.string.tools);
            }

            addPreferencesFromResource(R.xml.pref_context_menu_overflow);
            findPreference("volume").setOnPreferenceClickListener(this);
            findPreference("system_settings").setOnPreferenceClickListener(this);
            findPreference("lock_device").setOnPreferenceClickListener(this);
            findPreference("power_menu").setOnPreferenceClickListener(this);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                findPreference("file_manager").setOnPreferenceClickListener(this);
        } else {
            appName = getIntent().getStringExtra("app_name");
            packageName = getIntent().getStringExtra("package_name");
            componentName = getIntent().getStringExtra("component_name");
            userId = getIntent().getLongExtra("user_id", 0);

            if(getResources().getConfiguration().screenWidthDp >= 600
                    && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
                setTitle(appName);
            else {
                addPreferencesFromResource(R.xml.pref_context_menu_header);
                findPreference("header").setTitle(appName);
            }

            SharedPreferences pref = U.getSharedPreferences(this);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    && pref.getBoolean("freeform_hack", false)
                    && !U.isGame(this, packageName)) {
                addPreferencesFromResource(R.xml.pref_context_menu_show_window_sizes);
                findPreference("show_window_sizes").setOnPreferenceClickListener(this);
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                int shortcutCount = getLauncherShortcuts();

                if(shortcutCount > 1) {
                    addPreferencesFromResource(R.xml.pref_context_menu_shortcuts);
                    findPreference("app_shortcuts").setOnPreferenceClickListener(this);
                } else if(shortcutCount == 1)
                    generateShortcuts();
            }

            final PackageManager pm = getPackageManager();
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo defaultLauncher = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);

            if(!packageName.contains(BuildConfig.BASE_APPLICATION_ID)
                    && !packageName.equals(defaultLauncher.activityInfo.packageName)) {
                PinnedBlockedApps pba = PinnedBlockedApps.getInstance(this);

                if(pba.isPinned(componentName)) {
                    addPreferencesFromResource(R.xml.pref_context_menu_pin);
                    findPreference("pin_app").setOnPreferenceClickListener(this);
                    findPreference("pin_app").setTitle(R.string.unpin_app);
                } else if(pba.isBlocked(componentName)) {
                    addPreferencesFromResource(R.xml.pref_context_menu_block);
                    findPreference("block_app").setOnPreferenceClickListener(this);
                    findPreference("block_app").setTitle(R.string.unblock_app);
                } else {
                    final int MAX_NUM_OF_COLUMNS = U.getMaxNumOfEntries(this);

                    if(pba.getPinnedApps().size() < MAX_NUM_OF_COLUMNS) {
                        addPreferencesFromResource(R.xml.pref_context_menu_pin);
                        findPreference("pin_app").setOnPreferenceClickListener(this);
                        findPreference("pin_app").setTitle(R.string.pin_app);
                    }

                    addPreferencesFromResource(R.xml.pref_context_menu_block);
                    findPreference("block_app").setOnPreferenceClickListener(this);
                    findPreference("block_app").setTitle(R.string.block_app);
                }
            }

            addPreferencesFromResource(R.xml.pref_context_menu);

            findPreference("app_info").setOnPreferenceClickListener(this);
            findPreference("uninstall").setOnPreferenceClickListener(this);
        }
    }

    @SuppressWarnings("deprecation")
    private void generateShortcuts() {
        addPreferencesFromResource(R.xml.pref_context_menu_shortcut_list);
        switch(shortcuts.size()) {
            case 5:
                findPreference("shortcut_5").setTitle(getShortcutTitle(shortcuts.get(4)));
                findPreference("shortcut_5").setOnPreferenceClickListener(this);
            case 4:
                findPreference("shortcut_4").setTitle(getShortcutTitle(shortcuts.get(3)));
                findPreference("shortcut_4").setOnPreferenceClickListener(this);
            case 3:
                findPreference("shortcut_3").setTitle(getShortcutTitle(shortcuts.get(2)));
                findPreference("shortcut_3").setOnPreferenceClickListener(this);
            case 2:
                findPreference("shortcut_2").setTitle(getShortcutTitle(shortcuts.get(1)));
                findPreference("shortcut_2").setOnPreferenceClickListener(this);
            case 1:
                findPreference("shortcut_1").setTitle(getShortcutTitle(shortcuts.get(0)));
                findPreference("shortcut_1").setOnPreferenceClickListener(this);
                break;
        }

        switch(shortcuts.size()) {
            case 1:
                getPreferenceScreen().removePreference(findPreference("shortcut_2"));
            case 2:
                getPreferenceScreen().removePreference(findPreference("shortcut_3"));
            case 3:
                getPreferenceScreen().removePreference(findPreference("shortcut_4"));
            case 4:
                getPreferenceScreen().removePreference(findPreference("shortcut_5"));
                break;
        }
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.N_MR1)
    @Override
    public boolean onPreferenceClick(Preference p) {
        UserManager userManager = (UserManager) getSystemService(USER_SERVICE);
        LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        boolean appIsValid = isStartButton || isOverflowMenu ||
                !launcherApps.getActivityList(getIntent().getStringExtra("package_name"),
                        userManager.getUserForSerialNumber(userId)).isEmpty();

        if(appIsValid) switch(p.getKey()) {
            case "app_info":
                startFreeformActivity();
                launcherApps.startAppDetailsActivity(ComponentName.unflattenFromString(componentName), userManager.getUserForSerialNumber(userId), null, null);

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "uninstall":
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode()) {
                    Intent intent2 = new Intent(ContextMenuActivity.this, DummyActivity.class);
                    intent2.putExtra("uninstall", packageName);
                    intent2.putExtra("user_id", userId);

                    startFreeformActivity();
                    startActivity(intent2);
                } else {
                    startFreeformActivity();

                    Intent intent2 = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + packageName));
                    intent2.putExtra(Intent.EXTRA_USER, userManager.getUserForSerialNumber(userId));

                    try {
                        startActivity(intent2);
                    } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
                }

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "open_taskbar_settings":
                startFreeformActivity();

                Intent intent2 = new Intent(this, MainActivity.class);
                intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent2);

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "quit_taskbar":
                sendBroadcast(new Intent("com.farmerbb.taskbar.QUIT"));

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "pin_app":
                PinnedBlockedApps pba = PinnedBlockedApps.getInstance(this);
                if(pba.isPinned(componentName))
                    pba.removePinnedApp(this, componentName);
                else {
                    Intent intent = new Intent();
                    intent.setComponent(ComponentName.unflattenFromString(componentName));

                    LauncherActivityInfo appInfo = launcherApps.resolveActivity(intent, userManager.getUserForSerialNumber(userId));
                    if(appInfo != null) {
                        AppEntry newEntry = new AppEntry(
                                packageName,
                                componentName,
                                appName,
                                IconCache.getInstance(this).getIcon(this, getPackageManager(), appInfo),
                                true);

                        newEntry.setUserId(userId);
                        pba.addPinnedApp(this, newEntry);
                    }
                }
                break;
            case "block_app":
                PinnedBlockedApps pba2 = PinnedBlockedApps.getInstance(this);
                if(pba2.isBlocked(componentName))
                    pba2.removeBlockedApp(this, componentName);
                else {
                    pba2.addBlockedApp(this, new AppEntry(
                            packageName,
                            componentName,
                            appName,
                            null,
                            false));
                }
                break;
            case "show_window_sizes":
                getPreferenceScreen().removeAll();

                addPreferencesFromResource(R.xml.pref_context_menu_window_size_list);
                findPreference("window_size_standard").setOnPreferenceClickListener(this);
                findPreference("window_size_large").setOnPreferenceClickListener(this);
                findPreference("window_size_fullscreen").setOnPreferenceClickListener(this);
                findPreference("window_size_half_left").setOnPreferenceClickListener(this);
                findPreference("window_size_half_right").setOnPreferenceClickListener(this);
                findPreference("window_size_phone_size").setOnPreferenceClickListener(this);

                SharedPreferences pref = U.getSharedPreferences(this);
                if(pref.getBoolean("save_window_sizes", true)) {
                    String windowSizePref = SavedWindowSizes.getInstance(this).getWindowSize(this, packageName);
                    CharSequence title = findPreference("window_size_" + windowSizePref).getTitle();
                    findPreference("window_size_" + windowSizePref).setTitle('\u2713' + " " + title);
                }

                secondaryMenu = true;
                break;
            case "window_size_standard":
            case "window_size_large":
            case "window_size_fullscreen":
            case "window_size_half_left":
            case "window_size_half_right":
            case "window_size_phone_size":
                String windowSize = p.getKey().replace("window_size_", "");

                SharedPreferences pref2 = U.getSharedPreferences(this);
                if(pref2.getBoolean("save_window_sizes", true)) {
                    SavedWindowSizes.getInstance(this).setWindowSize(this, packageName, windowSize);
                }

                startFreeformActivity();
                U.launchApp(getApplicationContext(), packageName, componentName, userId, windowSize, false, true);

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "app_shortcuts":
                getPreferenceScreen().removeAll();
                generateShortcuts();

                secondaryMenu = true;
                break;
            case "shortcut_1":
            case "shortcut_2":
            case "shortcut_3":
            case "shortcut_4":
            case "shortcut_5":
                U.startShortcut(getApplicationContext(), packageName, componentName, shortcuts.get(Integer.parseInt(p.getKey().replace("shortcut_", "")) - 1));

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "start_menu_apps":
                startFreeformActivity();

                Intent intent = null;

                SharedPreferences pref3 = U.getSharedPreferences(this);
                switch(pref3.getString("theme", "light")) {
                    case "light":
                        intent = new Intent(this, SelectAppActivity.class);
                        break;
                    case "dark":
                        intent = new Intent(this, SelectAppActivityDark.class);
                        break;
                }

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                        && pref3.getBoolean("freeform_hack", false)
                        && intent != null && isInMultiWindowMode()) {
                    intent.putExtra("no_shadow", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);

                    U.launchAppMaximized(getApplicationContext(), intent);
                } else
                    startActivity(intent);

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "volume":
                AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
                audio.adjustSuggestedStreamVolume(AudioManager.ADJUST_SAME, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI);

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "file_manager":
                Intent fileManagerIntent;

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    startFreeformActivity();
                    fileManagerIntent = new Intent("android.provider.action.BROWSE");
                } else {
                    fileManagerIntent = new Intent("android.provider.action.BROWSE_DOCUMENT_ROOT");
                    fileManagerIntent.setComponent(ComponentName.unflattenFromString("com.android.documentsui/.DocumentsActivity"));
                }

                fileManagerIntent.addCategory(Intent.CATEGORY_DEFAULT);
                fileManagerIntent.setData(Uri.parse("content://com.android.externalstorage.documents/root/primary"));

                try {
                    startActivity(fileManagerIntent);
                } catch (ActivityNotFoundException e) {
                    U.showToast(this, R.string.lock_device_not_supported);
                }

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "system_settings":
                startFreeformActivity();

                Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);

                try {
                    startActivity(settingsIntent);
                } catch (ActivityNotFoundException e) {
                    U.showToast(this, R.string.lock_device_not_supported);
                }

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "lock_device":
                U.lockDevice(this);

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "power_menu":
                U.showPowerMenu(this);

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "change_wallpaper":
                Intent intent3 = Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER), getString(R.string.set_wallpaper));
                intent3.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                U.launchAppMaximized(getApplicationContext(), intent3);

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
        }

        if(!secondaryMenu) finish();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(!isFinishing()) finish();
    }

    @Override
    public void finish() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.CONTEXT_MENU_DISAPPEARING"));

        if(!dashboardOrStartMenuAppearing) {
            if(showStartMenu)
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.TOGGLE_START_MENU_ALT"));
            else if(shouldHideTaskbar) {
                SharedPreferences pref = U.getSharedPreferences(this);
                if(pref.getBoolean("hide_taskbar", true) && !FreeformHackHelper.getInstance().isInFreeformWorkspace())
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_TASKBAR"));
            }
        }

        super.finish();
    }

    @SuppressWarnings("deprecation")
    private void startFreeformActivity() {
        SharedPreferences pref = U.getSharedPreferences(this);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && pref.getBoolean("taskbar_active", false)
                && pref.getBoolean("freeform_hack", false)
                && isInMultiWindowMode()
                && !FreeformHackHelper.getInstance().isFreeformHackActive()) {
            U.startFreeformHack(this, false, false);
        }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private int getLauncherShortcuts() {
        LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        if(launcherApps.hasShortcutHostPermission()) {
            UserManager userManager = (UserManager) getSystemService(USER_SERVICE);

            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
            query.setActivity(ComponentName.unflattenFromString(componentName));
            query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                    | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                    | LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);

            shortcuts = launcherApps.getShortcuts(query, userManager.getUserForSerialNumber(userId));
            if(shortcuts != null)
                return shortcuts.size();
        }

        return 0;
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private CharSequence getShortcutTitle(ShortcutInfo shortcut) {
        CharSequence longLabel = shortcut.getLongLabel();
        if(longLabel != null && longLabel.length() > 0 && longLabel.length() <= 20)
            return longLabel;
        else
            return shortcut.getShortLabel();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if(secondaryMenu) {
            secondaryMenu = false;

            getPreferenceScreen().removeAll();
            generateMenu();
        } else
            super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(finishReceiver);
    }
}