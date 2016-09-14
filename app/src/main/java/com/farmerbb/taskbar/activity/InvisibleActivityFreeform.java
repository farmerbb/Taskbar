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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.WindowManager;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.LauncherHelper;
import com.farmerbb.taskbar.util.U;

public class InvisibleActivityFreeform extends Activity {

    boolean showTaskbar = false;
    boolean doNotHide = false;
    boolean proceedWithOnCreate = true;
    boolean finish = false;

    private BroadcastReceiver appearingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            doNotHide = true;
        }
    };

    private BroadcastReceiver disappearingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            doNotHide = false;
        }
    };

    private BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            InvisibleActivityFreeform.super.finish();
            overridePendingTransition(0, 0);

            if(!finish) {
                FreeformHackHelper helper = FreeformHackHelper.getInstance();
                helper.setFreeformHackActive(false);
                helper.setInFreeformWorkspace(false);

                finish = true;
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(FreeformHackHelper.getInstance().isFreeformHackActive()) {
            proceedWithOnCreate = false;
            super.finish();
        }

        if(getIntent().hasExtra("check_multiwindow")) {
            showTaskbar = false;

            if(!isInMultiWindowMode()) {
                proceedWithOnCreate = false;
                super.finish();
            }
        }

        if(proceedWithOnCreate) {
            // Detect outside touches, and pass them through to the underlying activity
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);

            LocalBroadcastManager.getInstance(this).registerReceiver(appearingReceiver, new IntentFilter("com.farmerbb.taskbar.START_MENU_APPEARING"));
            LocalBroadcastManager.getInstance(this).registerReceiver(appearingReceiver, new IntentFilter("com.farmerbb.taskbar.CONTEXT_MENU_APPEARING"));
            LocalBroadcastManager.getInstance(this).registerReceiver(disappearingReceiver, new IntentFilter("com.farmerbb.taskbar.START_MENU_DISAPPEARING"));
            LocalBroadcastManager.getInstance(this).registerReceiver(disappearingReceiver, new IntentFilter("com.farmerbb.taskbar.CONTEXT_MENU_DISAPPEARING"));
            LocalBroadcastManager.getInstance(this).registerReceiver(finishReceiver, new IntentFilter("com.farmerbb.taskbar.FINISH_FREEFORM_ACTIVITY"));

            FreeformHackHelper.getInstance().setFreeformHackActive(true);

            showTaskbar = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Show the taskbar when activity is resumed (no other freeform windows are active)
        if(showTaskbar)
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.SHOW_TASKBAR"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        possiblyHideTaskbar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(proceedWithOnCreate) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(appearingReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(disappearingReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(finishReceiver);

            if(!finish) {
                FreeformHackHelper helper = FreeformHackHelper.getInstance();
                helper.setFreeformHackActive(false);
                helper.setInFreeformWorkspace(false);

                finish = true;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        FreeformHackHelper.getInstance().setInFreeformWorkspace(true);

        if(bootToFreeformActive()) {
            LauncherHelper.getInstance().setOnHomeScreen(true);

            // We always start the Taskbar and Start Menu services, even if the app isn't normally running
            startService(new Intent(this, TaskbarService.class));
            startService(new Intent(this, StartMenuService.class));

            SharedPreferences pref = U.getSharedPreferences(this);
            if(pref.getBoolean("taskbar_active", false))
                startService(new Intent(this, NotificationService.class));

            // Show the taskbar when activity is started
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(showTaskbar)
                        LocalBroadcastManager.getInstance(InvisibleActivityFreeform.this).sendBroadcast(new Intent("com.farmerbb.taskbar.SHOW_TASKBAR"));
                }
            }, 100);
        }

        // Show the taskbar when activity is started
        if(showTaskbar)
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.SHOW_TASKBAR"));
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(!finish) FreeformHackHelper.getInstance().setInFreeformWorkspace(false);

        possiblyHideTaskbar();

        if(bootToFreeformActive()) {
            LauncherHelper.getInstance().setOnHomeScreen(false);

            // Stop the Taskbar and Start Menu services if they should normally not be active
            SharedPreferences pref = U.getSharedPreferences(this);
            if(!pref.getBoolean("taskbar_active", false) || pref.getBoolean("is_hidden", false)) {
                stopService(new Intent(this, TaskbarService.class));
                stopService(new Intent(this, StartMenuService.class));
            }
        }
    }

    // We don't want this activity to finish under normal circumstances
    @Override
    public void finish() {}

    private void possiblyHideTaskbar() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!doNotHide) {
                    SharedPreferences pref = U.getSharedPreferences(InvisibleActivityFreeform.this);
                    if(pref.getBoolean("hide_taskbar", true)
                            && !FreeformHackHelper.getInstance().isInFreeformWorkspace()
                            && !LauncherHelper.getInstance().isOnHomeScreen())
                        LocalBroadcastManager.getInstance(InvisibleActivityFreeform.this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_TASKBAR"));
                    else
                        LocalBroadcastManager.getInstance(InvisibleActivityFreeform.this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
                }
            }
        }, 100);
    }

    private boolean bootToFreeformActive() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo defaultLauncher = getPackageManager().resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);

        return defaultLauncher.activityInfo.packageName.equals(BuildConfig.APPLICATION_ID);
    }
}