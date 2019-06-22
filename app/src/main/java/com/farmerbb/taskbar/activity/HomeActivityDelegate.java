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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.ui.DashboardController;
import com.farmerbb.taskbar.ui.Host;
import com.farmerbb.taskbar.ui.ViewParams;
import com.farmerbb.taskbar.ui.StartMenuController;
import com.farmerbb.taskbar.ui.TaskbarController;
import com.farmerbb.taskbar.util.CompatUtils;
import com.farmerbb.taskbar.util.DisplayInfo;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.LauncherHelper;
import com.farmerbb.taskbar.util.U;

public class HomeActivityDelegate extends Activity implements Host {
    private TaskbarController taskbarChild;
    private StartMenuController startMenuChild;
    private DashboardController dashboardChild;

    private FrameLayout layout;

    private boolean forceTaskbarStart = false;
    private AlertDialog dialog;

    private boolean shouldDelayFreeformHack;
    private int hits;

    private BroadcastReceiver killReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            killHomeActivity();
        }
    };

    private BroadcastReceiver forceTaskbarStartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            forceTaskbarStart = true;
        }
    };

    private BroadcastReceiver freeformToggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWindowFlags();
        }
    };

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shouldDelayFreeformHack = true;
        hits = 0;

        WindowManager.LayoutParams params = getWindow().getAttributes();
        if(CompatUtils.applyDisplayCutoutModeTo(params))
            getWindow().setAttributes(params);

        SharedPreferences pref = U.getSharedPreferences(this);

        layout = new FrameLayout(this) {
            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();

                WallpaperManager wallpaperManager = (WallpaperManager) getSystemService(WALLPAPER_SERVICE);
                wallpaperManager.setWallpaperOffsets(getWindowToken(), 0.5f, 0.5f);

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    DisplayInfo display = U.getDisplayInfo(HomeActivityDelegate.this);
                    wallpaperManager.suggestDesiredDimensions(display.width, display.height);
                }

                boolean shouldStartFreeformHack = shouldDelayFreeformHack && hits > 0;
                shouldDelayFreeformHack = false;

                if(shouldStartFreeformHack)
                    startFreeformHack();
            }
        };

        layout.setOnClickListener(view1 -> LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU")));

        layout.setOnLongClickListener(view2 -> {
            if(!pref.getBoolean("freeform_hack", false))
                setWallpaper();

            return false;
        });

        layout.setOnGenericMotionListener((view3, motionEvent) -> {
            if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                    && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY
                    && !pref.getBoolean("freeform_hack", false))
                setWallpaper();

            return false;
        });

        layout.setFitsSystemWindows(true);

        if((this instanceof HomeActivity || U.isLauncherPermanentlyEnabled(this))
                && !U.isChromeOs(this)) {
            setContentView(layout);
            pref.edit().putBoolean("launcher", true).apply();
        } else
            killHomeActivity();

        updateWindowFlags();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(killReceiver, new IntentFilter("com.farmerbb.taskbar.KILL_HOME_ACTIVITY"));
        lbm.registerReceiver(forceTaskbarStartReceiver, new IntentFilter("com.farmerbb.taskbar.FORCE_TASKBAR_RESTART"));

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.farmerbb.taskbar.UPDATE_FREEFORM_CHECKBOX");
        intentFilter.addAction("com.farmerbb.taskbar.TOUCH_ABSORBER_STATE_CHANGED");

        lbm.registerReceiver(freeformToggleReceiver, intentFilter);

        U.initPrefs(this);
    }

    private void setWallpaper() {
        if(U.shouldCollapse(this, true))
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.TEMP_HIDE_TASKBAR"));
        else
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));

        try {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER), getString(R.string.set_wallpaper)));
        } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.N)
    @Override
    protected void onResume() {
        super.onResume();

        if(bootToFreeform()) {
            if(U.launcherIsDefault(this))
                startFreeformHack();
            else {
                U.showToastLong(this, R.string.set_as_default_home);

                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    startActivity(homeIntent);
                    finish();
                } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
            }
        } else {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.TEMP_SHOW_TASKBAR"));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));

        if(U.canDrawOverlays(this)) {
            if(!bootToFreeform()) {
                final LauncherHelper helper = LauncherHelper.getInstance();
                helper.setOnHomeScreen(true);

                if(forceTaskbarStart) {
                    forceTaskbarStart = false;
                    new Handler().postDelayed(() -> {
                        helper.setOnHomeScreen(true);
                        startTaskbar();
                    }, 250);
                } else
                    startTaskbar();
            } else if(U.launcherIsDefault(this))
                startFreeformHack();
        } else
            dialog = U.showPermissionDialog(U.wrapContext(this),
                    () -> dialog = U.showErrorDialog(U.wrapContext(this), "SYSTEM_ALERT_WINDOW"),
                    null);
    }

    private boolean bootToFreeform() {
        SharedPreferences pref = U.getSharedPreferences(this);
        return U.hasFreeformSupport(this)
                && pref.getBoolean("freeform_hack", false)
                && !U.isOverridingFreeformHack(this);
    }

    private void startTaskbar() {
        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean("first_run", true)) {
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("first_run", false);
            editor.putBoolean("collapsed", true);
            editor.apply();

            dialog = U.showRecentAppsDialog(U.wrapContext(this),
                    () -> dialog = U.showErrorDialog(U.wrapContext(this), "GET_USAGE_STATS"),
                    null);
        }

        // We always start the Taskbar and Start Menu services, even if the app isn't normally running
        taskbarChild = new TaskbarController(this);
        startMenuChild = new StartMenuController(this);
        dashboardChild = new DashboardController(this);

        taskbarChild.onCreateHost(this);
        startMenuChild.onCreateHost(this);
        dashboardChild.onCreateHost(this);

        if(pref.getBoolean("taskbar_active", false) && !U.isServiceRunning(this, NotificationService.class))
            pref.edit().putBoolean("taskbar_active", false).apply();

        // Show the Taskbar temporarily, as nothing else will be visible on screen
        new Handler().postDelayed(() -> LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.TEMP_SHOW_TASKBAR")), 100);
    }

    private void startFreeformHack() {
        if(shouldDelayFreeformHack)
            hits++;
        else
            U.startFreeformHack(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences pref = U.getSharedPreferences(this);
        if(!bootToFreeform()) {
            LauncherHelper.getInstance().setOnHomeScreen(false);

            if(U.shouldCollapse(this, true))
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.TEMP_HIDE_TASKBAR"));
            else
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));

            // Stop the Taskbar and Start Menu services if they should normally not be active
            if(!pref.getBoolean("taskbar_active", false) || pref.getBoolean("is_hidden", false)) {
                taskbarChild.onDestroyHost(this);
                startMenuChild.onDestroyHost(this);
                dashboardChild.onDestroyHost(this);

                IconCache.getInstance(this).clearCache();
            }
        }

        if(dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(killReceiver);
        lbm.unregisterReceiver(forceTaskbarStartReceiver);
        lbm.unregisterReceiver(freeformToggleReceiver);
    }

    @Override
    public void onBackPressed() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
    }

    private void killHomeActivity() {
        LauncherHelper.getInstance().setOnHomeScreen(false);

        // Stop the Taskbar and Start Menu services if they should normally not be active
        SharedPreferences pref = U.getSharedPreferences(this);
        if(!pref.getBoolean("taskbar_active", false) || pref.getBoolean("is_hidden", false)) {
            taskbarChild.onDestroyHost(this);
            startMenuChild.onDestroyHost(this);
            dashboardChild.onDestroyHost(this);

            IconCache.getInstance(this).clearCache();

            U.stopFreeformHack(this);
        }

        finish();
    }

    private void updateWindowFlags() {
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if(FreeformHackHelper.getInstance().isTouchAbsorberActive() && U.isOverridingFreeformHack(this))
            getWindow().setFlags(flags, flags);
        else
            getWindow().clearFlags(flags);
    }

    @Override
    public void addView(View view, ViewParams params) {
        final FrameLayout.LayoutParams flParams = new FrameLayout.LayoutParams(
                params.width,
                params.height
        );

        if(params.gravity > -1)
            flParams.gravity = params.gravity;

        view.setLayoutParams(flParams);
        layout.addView(view);
    }

    @Override
    public void removeView(View view) {
        layout.removeView(view);
    }

    @Override
    public void terminate() {
        layout.removeAllViews();
    }
}
