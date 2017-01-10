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
import android.support.v4.content.LocalBroadcastManager;

import com.farmerbb.taskbar.service.DashboardService;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.LauncherHelper;
import com.farmerbb.taskbar.util.U;

public class QuitReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent taskbarIntent = new Intent(context, TaskbarService.class);
        Intent startMenuIntent = new Intent(context, StartMenuService.class);
        Intent dashboardIntent = new Intent(context, DashboardService.class);
        Intent notificationIntent = new Intent(context, NotificationService.class);

        SharedPreferences pref = U.getSharedPreferences(context);
        pref.edit().putBoolean("taskbar_active", false).apply();

        if(!LauncherHelper.getInstance().isOnHomeScreen()) {
            context.stopService(taskbarIntent);
            context.stopService(startMenuIntent);
            context.stopService(dashboardIntent);

            IconCache.getInstance(context).clearCache();

            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.START_MENU_DISAPPEARING"));
        }

        context.stopService(notificationIntent);
    }
}
