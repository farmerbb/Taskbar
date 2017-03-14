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

package com.farmerbb.taskbar.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.farmerbb.taskbar.activity.DummyActivity;
import com.farmerbb.taskbar.service.DashboardService;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.util.U;

public class StartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences pref = U.getSharedPreferences(context);

        boolean taskbarNotActive = !pref.getBoolean("taskbar_active", false);
        boolean taskbarActiveButHidden = pref.getBoolean("taskbar_active", false) && pref.getBoolean("is_hidden", false);

        if(taskbarNotActive || taskbarActiveButHidden) {
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("is_hidden", false);

            if(taskbarNotActive) {
                if(pref.getBoolean("first_run", true)) {
                    editor.putBoolean("first_run", false);
                    editor.putBoolean("collapsed", true);
                }

                editor.putBoolean("taskbar_active", true);
                editor.putLong("time_of_service_start", System.currentTimeMillis());
            }

            editor.apply();

            if(taskbarActiveButHidden)
                context.stopService(new Intent(context, NotificationService.class));

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && pref.getBoolean("freeform_hack", false)) {
                Intent intent2 = new Intent(context, DummyActivity.class);
                intent2.putExtra("start_freeform_hack", true);
                intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent2);
            }

            context.startService(new Intent(context, TaskbarService.class));
            context.startService(new Intent(context, StartMenuService.class));
            context.startService(new Intent(context, DashboardService.class));
            context.startService(new Intent(context, NotificationService.class));
        }
    }
}
