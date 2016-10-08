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
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.LauncherHelper;
import com.farmerbb.taskbar.util.U;

public class HomeActivity extends Activity {

    private boolean forceTaskbarStop = false;
    private boolean setWallpaper = false;

    private BroadcastReceiver killReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LauncherHelper.getInstance().setOnHomeScreen(false);

            // Stop the Taskbar and Start Menu services if they should normally not be active
            SharedPreferences pref = U.getSharedPreferences(HomeActivity.this);
            if(!pref.getBoolean("taskbar_active", false) || pref.getBoolean("is_hidden", false)) {
                stopService(new Intent(HomeActivity.this, TaskbarService.class));
                stopService(new Intent(HomeActivity.this, StartMenuService.class));

                IconCache.getInstance(context).clearCache();

                LocalBroadcastManager.getInstance(HomeActivity.this).sendBroadcast(new Intent("com.farmerbb.taskbar.FINISH_FREEFORM_ACTIVITY"));
            }

            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        View view = new View(this);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocalBroadcastManager.getInstance(HomeActivity.this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
            }
        });

        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                setWallpaper();
                return false;
            }
        });

        view.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                        && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    setWallpaper();
                }
                return false;
            }
        });

        setContentView(view);

        LocalBroadcastManager.getInstance(this).registerReceiver(killReceiver, new IntentFilter("com.farmerbb.taskbar.KILL_HOME_ACTIVITY"));
    }

    private void setWallpaper() {
        setWallpaper = true;
        LocalBroadcastManager.getInstance(HomeActivity.this).sendBroadcast(new Intent("com.farmerbb.taskbar.TEMP_HIDE_TASKBAR"));

        try {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER), getString(R.string.set_wallpaper)));
        } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(setWallpaper) {
            setWallpaper = false;

            LocalBroadcastManager.getInstance(HomeActivity.this).sendBroadcast(new Intent("com.farmerbb.taskbar.TEMP_SHOW_TASKBAR"));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onStart() {
        super.onStart();

        if(canDrawOverlays()) {
            SharedPreferences pref = U.getSharedPreferences(this);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    && pref.getBoolean("freeform_hack", false)
                    && hasFreeformSupport()
                    && !LauncherHelper.getInstance().shouldForceTaskbarRestart()) {

                if(U.bootToFreeformActive(this)) {
                    DisplayManager dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);
                    Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);

                    Intent intent = new Intent(this, InvisibleActivityFreeform.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
                    startActivity(intent, ActivityOptions.makeBasic().setLaunchBounds(new Rect(display.getWidth(), display.getHeight(), display.getWidth() + 1, display.getHeight() + 1)).toBundle());
                } else {
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
                final LauncherHelper helper = LauncherHelper.getInstance();
                helper.setOnHomeScreen(true);

                if(helper.shouldForceTaskbarRestart()) {
                    helper.setForceTaskbarRestart(false);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            helper.setOnHomeScreen(true);
                            startTaskbar();
                            forceTaskbarStop = true;
                        }
                    }, 100);
                } else
                    startTaskbar();
            }
        } else {
            ComponentName component = new ComponentName(BuildConfig.APPLICATION_ID, HomeActivity.class.getName());
            getPackageManager().setComponentEnabledSetting(component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            finish();
        }
    }

    private void startTaskbar() {
        // We always start the Taskbar and Start Menu services, even if the app isn't normally running
        startService(new Intent(this, TaskbarService.class));
        startService(new Intent(this, StartMenuService.class));

        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean("taskbar_active", false))
            startService(new Intent(this, NotificationService.class));

        // Show the Taskbar temporarily, as nothing else will be visible on screen
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                LocalBroadcastManager.getInstance(HomeActivity.this).sendBroadcast(new Intent("com.farmerbb.taskbar.TEMP_SHOW_TASKBAR"));
            }
        }, 100);
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences pref = U.getSharedPreferences(this);
        if(!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && pref.getBoolean("freeform_hack", false)
                && hasFreeformSupport())) {
            LauncherHelper.getInstance().setOnHomeScreen(false);

            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.TEMP_HIDE_TASKBAR"));

            // Stop the Taskbar and Start Menu services if they should normally not be active
            if(!pref.getBoolean("taskbar_active", false) || pref.getBoolean("is_hidden", false)) {
                stopService(new Intent(this, TaskbarService.class));
                stopService(new Intent(this, StartMenuService.class));

                IconCache.getInstance(this).clearCache();
            }
        } else if(forceTaskbarStop) {
            forceTaskbarStop = false;
            stopService(new Intent(this, TaskbarService.class));
            stopService(new Intent(this, StartMenuService.class));

            IconCache.getInstance(this).clearCache();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(killReceiver);
        LauncherHelper.getInstance().setForceTaskbarRestart(false);
    }

    @Override
    public void onBackPressed() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    @TargetApi(Build.VERSION_CODES.N)
    private boolean hasFreeformSupport() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)
                || Settings.Global.getInt(getContentResolver(), "enable_freeform_support", -1) == 1
                || Settings.Global.getInt(getContentResolver(), "force_resizable_activities", -1) == 1;
    }
}
