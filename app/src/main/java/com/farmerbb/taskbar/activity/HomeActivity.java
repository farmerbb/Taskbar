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
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.util.U;

public class HomeActivity extends Activity {

    boolean dontStopServices = false;

    private BroadcastReceiver killReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    private BroadcastReceiver toggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dontStopServices = true;
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
                LocalBroadcastManager.getInstance(HomeActivity.this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));

                try {
                    startActivity(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER), getString(R.string.set_wallpaper)));
                } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
                return false;
            }
        });

        view.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                        && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    LocalBroadcastManager.getInstance(HomeActivity.this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));

                    try {
                        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER), getString(R.string.set_wallpaper)));
                    } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
                }
                return false;
            }
        });

        setContentView(view);

        LocalBroadcastManager.getInstance(this).registerReceiver(killReceiver, new IntentFilter("com.farmerbb.taskbar.KILL_HOME_ACTIVITY"));
        LocalBroadcastManager.getInstance(this).registerReceiver(toggleReceiver, new IntentFilter("com.farmerbb.taskbar.TOGGLE_START_MENU"));
    }

    @Override
    protected void onResume() {
        super.onResume();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dontStopServices = false;
            }
        }, 100);

        if(canDrawOverlays()) {
            SharedPreferences pref = U.getSharedPreferences(this);
            pref.edit().putBoolean("on_home_screen", true).apply();

            // We always start the Taskbar and Start Menu services, even if the app isn't normally running
            startService(new Intent(this, TaskbarService.class));
            startService(new Intent(this, StartMenuService.class));

            if(pref.getBoolean("taskbar_active", false))
                startService(new Intent(this, NotificationService.class));

            // Show the Taskbar temporarily, as nothing else will be visible on screen
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    LocalBroadcastManager.getInstance(HomeActivity.this).sendBroadcast(new Intent("com.farmerbb.taskbar.TEMP_SHOW_TASKBAR"));
                }
            }, 100);
        } else {
            ComponentName component = new ComponentName(BuildConfig.APPLICATION_ID, HomeActivity.class.getName());
            getPackageManager().setComponentEnabledSetting(component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences pref = U.getSharedPreferences(this);
        pref.edit().putBoolean("on_home_screen", false).apply();

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.TEMP_HIDE_TASKBAR"));

        // Stop the Taskbar and Start Menu services if they should normally not be active
        if(!pref.getBoolean("taskbar_active", false) || pref.getBoolean("is_hidden", false)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(!dontStopServices) {
                        stopService(new Intent(HomeActivity.this, StartMenuService.class));
                        stopService(new Intent(HomeActivity.this, TaskbarService.class));
                    }
                }
            }, 100);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(killReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(toggleReceiver);
    }

    @Override
    public void onBackPressed() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }
}
