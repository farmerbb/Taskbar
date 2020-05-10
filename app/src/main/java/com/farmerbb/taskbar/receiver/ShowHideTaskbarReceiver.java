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
import com.farmerbb.taskbar.activity.DummyActivity;
import com.farmerbb.taskbar.service.DashboardService;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.LauncherHelper;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class ShowHideTaskbarReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences pref = U.getSharedPreferences(context);
        if(!pref.getBoolean("taskbar_active", false))
            return;

        Intent taskbarIntent = new Intent(context, TaskbarService.class);
        Intent startMenuIntent = new Intent(context, StartMenuService.class);
        Intent dashboardIntent = new Intent(context, DashboardService.class);
        Intent notificationIntent = new Intent(context, NotificationService.class);

        if(pref.getBoolean("is_hidden", false)) {
            pref.edit().putBoolean("is_hidden", false).apply();

            context.stopService(notificationIntent);

            if(U.hasFreeformSupport(context) && U.isFreeformModeEnabled(context)) {
                Intent intent2 = new Intent(context, DummyActivity.class);
                intent2.putExtra("start_freeform_hack", true);
                intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent2);
            }

            notificationIntent.putExtra("start_services", true);

            U.startForegroundService(context, notificationIntent);
        } else {
            pref.edit().putBoolean("is_hidden", true).apply();

            context.stopService(notificationIntent);

            if(!LauncherHelper.getInstance().isOnHomeScreen()) {
                context.stopService(taskbarIntent);
                context.stopService(startMenuIntent);
                context.stopService(dashboardIntent);

                IconCache.getInstance(context).clearCache();

                U.sendBroadcast(context, ACTION_START_MENU_DISAPPEARING);
            }

            U.startForegroundService(context, notificationIntent);
        }
    }
}
