/* Copyright 2017 Braden Farmer
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

package com.farmerbb.taskbar.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.farmerbb.taskbar.activity.HomeActivity;
import com.farmerbb.taskbar.service.PowerMenuService;
import com.farmerbb.taskbar.util.U;

public class EnableHomeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(U.canDrawOverlays(context)) {
            SharedPreferences pref = U.getSharedPreferences(context);
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("launcher", true);

            // Customizations for Bliss-x86
            if(U.hasSupportLibrary(context)) {
                if(intent.hasExtra("enable_freeform_hack") && U.hasFreeformSupport(context)) {
                    editor.putBoolean("freeform_hack", true);
                }

                if(intent.hasExtra("enable_running_apps_only")) {
                    editor.putString("recents_amount", "running_apps_only");
                    editor.putString("refresh_frequency", "0");
                    editor.putString("max_num_of_recents", "2147483647");
                    editor.putBoolean("full_length", true);
                }

                if(intent.hasExtra("enable_navigation_bar_buttons")) {
                    editor.putBoolean("dashboard", true);
                    editor.putBoolean("button_back", true);
                    editor.putBoolean("button_home", true);
                    editor.putBoolean("button_recents", true);
                    editor.putBoolean("auto_hide_navbar", true);

                    try {
                        Settings.Secure.putString(context.getContentResolver(),
                                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                                new ComponentName(context, PowerMenuService.class).flattenToString());
                    } catch (Exception e) { /* Gracefully fail */ }
                }
            }

            editor.apply();

            ComponentName component = new ComponentName(context, HomeActivity.class);
            context.getPackageManager().setComponentEnabledSetting(component,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }
}
