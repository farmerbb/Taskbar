/* Copyright 2020 Braden Farmer
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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.widget.Toast;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.fragment.DesktopModeFragment;
import com.farmerbb.taskbar.util.U;

import java.util.List;

import static com.farmerbb.taskbar.util.Constants.*;

public class HSLActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences pref = U.getSharedPreferences(this);

        if(DesktopModeFragment.isConfiguringHomeApp) {
            int enter = getResources().getIdentifier("activity_close_enter", "anim", "android");
            int exit = getResources().getIdentifier("activity_close_exit", "anim", "android");

            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(enter, exit);
        } else if(pref.getBoolean(PREF_DIM_SCREEN, false) && U.isDesktopModeActive(this))
            startActivity(new Intent(this, DimScreenActivity.class));
        else
            startPrimaryLauncher();

        finish();
    }

    private void startPrimaryLauncher() {
        SharedPreferences pref = U.getSharedPreferences(this);
        String activityToLaunch = pref.getString(PREF_HSL_ID, "null");

        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        PackageManager manager = getPackageManager();
        List<ResolveInfo> listOfLaunchers = manager.queryIntentActivities(homeIntent, 0);

        try {
            manager.getApplicationInfo(activityToLaunch, 0);

            for(ResolveInfo launcher : listOfLaunchers) {
                if(activityToLaunch.equals("com.google.android.googlequicksearchbox")) {
                    // Only add the Google App onto the list if Google Now Launcher is installed
                    try {
                        manager.getApplicationInfo("com.google.android.launcher", 0);
                        if(activityToLaunch.equals(launcher.activityInfo.packageName))
                            activityToLaunch = activityToLaunch + "/" + launcher.activityInfo.name;

                    } catch (PackageManager.NameNotFoundException e) {
                        activityToLaunch = "null";
                    }
                } else {
                    if(activityToLaunch.equals(launcher.activityInfo.packageName))
                        activityToLaunch = activityToLaunch + "/" + launcher.activityInfo.name;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            activityToLaunch = "null";
        }

        if(activityToLaunch.equals("null")) {
            launcherNotFound();
        } else {
            Intent intent = getIntent();
            intent.setComponent(ComponentName.unflattenFromString(activityToLaunch));

            try {
                startActivity(intent);

                // Fire the intent twice to fix launchers that specifically listen
                // for home button presses (i.e. to jump to the default panel)
                U.newHandler().post(() -> startActivity(intent));
            } catch (ActivityNotFoundException e) {
                launcherNotFound();
            }
        }
    }

    private void launcherNotFound() {
        SharedPreferences pref = U.getSharedPreferences(this);
        if(!pref.getString(PREF_HSL_NAME, "null").equals("null")) {
            U.showToast(this, getString(R.string.tb_hsl_launcher_uninstalled, pref.getString(PREF_HSL_NAME, "null")), Toast.LENGTH_LONG);
        }

        Intent intent = new Intent(this, HSLConfigActivity.class);
        startActivity(intent);
    }
}
