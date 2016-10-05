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
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.PinnedBlockedApps;
import com.farmerbb.taskbar.util.SavedWindowSizes;
import com.farmerbb.taskbar.util.U;

public class ContextMenuActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener {

    String packageName;
    String componentName;
    String appName;

    boolean showStartMenu = false;
    boolean shouldHideTaskbar = false;
    boolean isStartButton = false;
    boolean openInNewWindow = false;

    @SuppressLint("RtlHardcoded")
    @SuppressWarnings("deprecation")
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.CONTEXT_MENU_APPEARING"));

        showStartMenu = getIntent().getBooleanExtra("launched_from_start_menu", false);
        isStartButton = !getIntent().hasExtra("package_name") && !getIntent().hasExtra("app_name");

        // Determine where to position the dialog on screen
        WindowManager.LayoutParams params = getWindow().getAttributes();
        SharedPreferences pref = U.getSharedPreferences(this);

        DisplayManager dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);

        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if(resourceId > 0)
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);

        if(showStartMenu) {
            int x = getIntent().getIntExtra("x", 0);
            int y = getIntent().getIntExtra("y", 0);
            int offset = getResources().getDimensionPixelSize(R.dimen.context_menu_offset);

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

        // Generate options to show on the menu, depending on which icon was clicked
        if(isStartButton) {
            addPreferencesFromResource(R.xml.pref_context_menu_open_settings);
            findPreference("open_taskbar_settings").setOnPreferenceClickListener(this);

            if(!getIntent().getBooleanExtra("dont_show_quit", false)) {
                addPreferencesFromResource(R.xml.pref_context_menu_quit);
                findPreference("quit_taskbar").setOnPreferenceClickListener(this);
            }
        } else {
            appName = getIntent().getStringExtra("app_name");
            packageName = getIntent().getStringExtra("package_name");
            componentName = getIntent().getStringExtra("component_name");

            if(getResources().getConfiguration().screenWidthDp >= 600
                    && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
                setTitle(appName);
            else {
                addPreferencesFromResource(R.xml.pref_context_menu_header);
                findPreference("header").setTitle(appName);
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    && pref.getBoolean("freeform_hack", false)
                    && isInMultiWindowMode()
                    && FreeformHackHelper.getInstance().isFreeformHackActive()) {
                addPreferencesFromResource(R.xml.pref_context_menu_show_window_sizes);
                findPreference("show_window_sizes").setOnPreferenceClickListener(this);
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
                    final int MAX_NUM_OF_COLUMNS = U.getMaxNumOfColumns(this);

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
    @Override
    public boolean onPreferenceClick(Preference p) {
        boolean appIsValid = true;

        if(!isStartButton) {
            try {
                getPackageManager().getPackageInfo(getIntent().getStringExtra("package_name"), 0);
            } catch(PackageManager.NameNotFoundException e) {
                appIsValid = false;
            }
        }

        boolean dontFinish = false;

        if(appIsValid) switch(p.getKey()) {
            case "app_info":
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getIntent().getStringExtra("package_name")));
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode())
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startFreeformActivity();
                startActivity(intent);

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "uninstall":
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode()) {
                    Intent intent2 = new Intent(ContextMenuActivity.this, DummyActivity.class);
                    intent2.putExtra("uninstall", getIntent().getStringExtra("package_name"));

                    startFreeformActivity();
                    startActivity(intent2);
                } else {
                    startFreeformActivity();
                    startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + getIntent().getStringExtra("package_name"))));
                }

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "open_taskbar_settings":
                startFreeformActivity();
                startActivity(new Intent(this, MainActivity.class));

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
                    Intent throwaway = new Intent();
                    throwaway.setComponent(ComponentName.unflattenFromString(componentName));

                    pba.addPinnedApp(this, new AppEntry(
                            packageName,
                            componentName,
                            appName,
                            IconCache.getInstance().getIcon(this, getPackageManager(), throwaway.resolveActivityInfo(getPackageManager(), 0)),
                            true));
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

                String windowSizePref = SavedWindowSizes.getInstance(this).getWindowSize(this, packageName);

                addPreferencesFromResource(R.xml.pref_context_menu_window_size_list);
                findPreference("window_size_standard").setOnPreferenceClickListener(this);
                findPreference("window_size_large").setOnPreferenceClickListener(this);
                findPreference("window_size_fullscreen").setOnPreferenceClickListener(this);
                findPreference("window_size_half_left").setOnPreferenceClickListener(this);
                findPreference("window_size_half_right").setOnPreferenceClickListener(this);
                findPreference("window_size_phone_size").setOnPreferenceClickListener(this);

                CharSequence title = findPreference("window_size_" + windowSizePref).getTitle();
                findPreference("window_size_" + windowSizePref).setTitle('\u2713' + " " + title);

                openInNewWindow = true;
                dontFinish = true;
                break;
            case "window_size_standard":
                SavedWindowSizes.getInstance(this).setWindowSize(this, packageName, "standard");

                U.launchStandard(this, generateIntent());

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "window_size_large":
                SavedWindowSizes.getInstance(this).setWindowSize(this, packageName, "large");

                U.launchLarge(this, generateIntent());

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "window_size_fullscreen":
                SavedWindowSizes.getInstance(this).setWindowSize(this, packageName, "fullscreen");

                U.launchFullscreen(this, generateIntent(), false);

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "window_size_half_left":
                SavedWindowSizes.getInstance(this).setWindowSize(this, packageName, "half_left");

                U.launchHalfLeft(this, generateIntent(), false);

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "window_size_half_right":
                SavedWindowSizes.getInstance(this).setWindowSize(this, packageName, "half_right");

                U.launchHalfRight(this, generateIntent(), false);

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
            case "window_size_phone_size":
                SavedWindowSizes.getInstance(this).setWindowSize(this, packageName, "phone_size");

                U.launchPhoneSize(this, generateIntent());

                showStartMenu = false;
                shouldHideTaskbar = true;
                break;
        }

        if(!dontFinish) finish();
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

        if(showStartMenu)
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.TOGGLE_START_MENU_ALT"));
        else if(shouldHideTaskbar) {
            SharedPreferences pref = U.getSharedPreferences(this);
            if(pref.getBoolean("hide_taskbar", true) && !FreeformHackHelper.getInstance().isInFreeformWorkspace())
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_TASKBAR"));
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
            DisplayManager dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);
            Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);

            Intent intent = new Intent(this, InvisibleActivityFreeform.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
            startActivity(intent, ActivityOptions.makeBasic().setLaunchBounds(new Rect(display.getWidth(), display.getHeight(), display.getWidth() + 1, display.getHeight() + 1)).toBundle());
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private Intent generateIntent() {
        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(componentName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if(openInNewWindow) {
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

            switch(intent.resolveActivityInfo(getPackageManager(), 0).launchMode) {
                case ActivityInfo.LAUNCH_SINGLE_TASK:
                case ActivityInfo.LAUNCH_SINGLE_INSTANCE:
                    intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
                    break;
            }
        }

        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean("disable_animations", false) && !showStartMenu)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        return intent;
    }
}