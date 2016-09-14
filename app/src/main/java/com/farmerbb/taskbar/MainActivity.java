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
import android.app.ActivityManager;
import android.app.ActivityOptions;
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
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.Display;
import android.widget.CompoundButton;

import com.farmerbb.taskbar.activity.HomeActivity;
import com.farmerbb.taskbar.activity.InvisibleActivityFreeform;
import com.farmerbb.taskbar.activity.KeyboardShortcutActivity;
import com.farmerbb.taskbar.fragment.SettingsFragment;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.U;

public class MainActivity extends AppCompatActivity {

    private SwitchCompat theSwitch;

    private BroadcastReceiver switchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSwitch();
        }
    };

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

        if(pref.getBoolean("taskbar_active", false) && !isServiceRunning())
            editor.putBoolean("taskbar_active", false);

        // Ensure that components that should be enabled are enabled properly
        boolean launcherEnabled = pref.getBoolean("launcher", false) && canDrawOverlays();
        editor.putBoolean("launcher", launcherEnabled);
        if(pref.getBoolean("boot_to_freeform", false) && !launcherEnabled)
            editor.putBoolean("boot_to_freeform", false);

        editor.apply();

        ComponentName component = new ComponentName(BuildConfig.APPLICATION_ID, HomeActivity.class.getName());
        getPackageManager().setComponentEnabledSetting(component,
                launcherEnabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        ComponentName component2 = new ComponentName(BuildConfig.APPLICATION_ID, KeyboardShortcutActivity.class.getName());
        getPackageManager().setComponentEnabledSetting(component2,
                pref.getBoolean("keyboard_shortcut", false) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        setContentView(R.layout.main);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setCustomView(R.layout.switch_layout);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
        }

        theSwitch = (SwitchCompat) findViewById(R.id.the_switch);
        if(theSwitch != null) {
            theSwitch.setChecked(pref.getBoolean("taskbar_active", false));

            theSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if(b) {
                        if(canDrawOverlays())
                            startTaskbarService();
                        else {
                            U.showPermissionDialog(MainActivity.this);
                            compoundButton.setChecked(false);
                        }
                    } else {
                        if(pref.getBoolean("boot_to_freeform", false)) {
                            U.showToastLong(MainActivity.this, R.string.cannot_stop_taskbar);
                            compoundButton.setChecked(true);
                        } else
                            stopTaskbarService();
                    }
                }
            });
        }

        getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new SettingsFragment(), "SettingsFragment").commit();
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
            DisplayManager dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);
            Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);

            Intent intent = new Intent(this, InvisibleActivityFreeform.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
            startActivity(intent, ActivityOptions.makeBasic().setLaunchBounds(new Rect(display.getWidth(), display.getHeight(), display.getWidth() + 1, display.getHeight() + 1)).toBundle());
        }

        startService(new Intent(this, TaskbarService.class));
        startService(new Intent(this, StartMenuService.class));
        startService(new Intent(this, NotificationService.class));
    }

    private void stopTaskbarService() {
        SharedPreferences pref = U.getSharedPreferences(this);
        pref.edit().putBoolean("taskbar_active", false).apply();

        stopService(new Intent(this, TaskbarService.class));
        stopService(new Intent(this, StartMenuService.class));
        stopService(new Intent(this, NotificationService.class));
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(NotificationService.class.getName().equals(service.service.getClassName()))
                return true;
        }

        return false;
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
}