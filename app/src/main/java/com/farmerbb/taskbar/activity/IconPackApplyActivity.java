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
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.service.DashboardService;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.util.U;

public class IconPackApplyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent().hasExtra(Intent.EXTRA_PACKAGE_NAME)) {
            SharedPreferences pref = U.getSharedPreferences(this);
            if(pref.getString("theme", "light").equals("dark"))
                setTheme(R.style.AppTheme_Dialog_Dark);

            @SuppressLint("InlinedApi")
            final String iconPackPackage = getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME);
            PackageManager pm = getPackageManager();

            boolean iconPackValid = true;
            try {
                pm.getPackageInfo(iconPackPackage, 0);
            } catch (PackageManager.NameNotFoundException e) {
                iconPackValid = false;
            }

            if(iconPackValid) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.apply_icon_pack)
                        .setNegativeButton(R.string.action_cancel, (dialog, which) -> finish())
                        .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                            pref.edit().putString("icon_pack", iconPackPackage).apply();

                            U.refreshPinnedIcons(IconPackApplyActivity.this);
                            restartTaskbar();

                            finish();
                        });

                try {
                    builder.setMessage(getString(R.string.apply_icon_pack_description,
                            pm.getApplicationLabel(pm.getApplicationInfo(iconPackPackage, 0))));
                } catch (PackageManager.NameNotFoundException e) { /* Gracefully fail */ }

                AlertDialog dialog = builder.create();
                dialog.show();
                dialog.setCancelable(false);
            } else {
                U.showToast(this, R.string.invalid_package_name);
                finish();
            }
        } else {
            U.showToast(this, R.string.must_specify_extra);
            finish();
        }
    }

    private void startTaskbarService(boolean fullRestart) {
        startService(new Intent(this, TaskbarService.class));
        startService(new Intent(this, StartMenuService.class));
        startService(new Intent(this, DashboardService.class));
        if(fullRestart) startService(new Intent(this, NotificationService.class));
    }

    private void stopTaskbarService(boolean fullRestart) {
        stopService(new Intent(this, TaskbarService.class));
        stopService(new Intent(this, StartMenuService.class));
        stopService(new Intent(this, DashboardService.class));
        if(fullRestart) stopService(new Intent(this, NotificationService.class));
    }

    private void restartTaskbar() {
        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean("taskbar_active", false) && !pref.getBoolean("is_hidden", false)) {
            pref.edit().putBoolean("is_restarting", true).apply();

            stopTaskbarService(true);
            startTaskbarService(true);
        } else if(U.isServiceRunning(this, StartMenuService.class)) {
            stopTaskbarService(false);
            startTaskbarService(false);
        }
    }
}
