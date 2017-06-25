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

package com.farmerbb.taskbar.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.farmerbb.taskbar.MainActivity;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.U;

public class NotificationService extends Service {

    private boolean isHidden = true;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    BroadcastReceiver userForegroundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startService(new Intent(context, TaskbarService.class));
            startService(new Intent(context, StartMenuService.class));
            startService(new Intent(context, DashboardService.class));
        }
    };

    BroadcastReceiver userBackgroundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopService(new Intent(context, TaskbarService.class));
            stopService(new Intent(context, StartMenuService.class));
            stopService(new Intent(context, DashboardService.class));

            IconCache.getInstance(context).clearCache();
        }
    };

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean("taskbar_active", false)) {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                isHidden = U.getSharedPreferences(this).getBoolean("is_hidden", false);

                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                Intent receiverIntent = new Intent("com.farmerbb.taskbar.SHOW_HIDE_TASKBAR");
                receiverIntent.setPackage(getPackageName());

                Intent receiverIntent2 = new Intent("com.farmerbb.taskbar.QUIT");
                receiverIntent.setPackage(getPackageName());

                PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                PendingIntent receiverPendingIntent = PendingIntent.getBroadcast(this, 0, receiverIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                PendingIntent receiverPendingIntent2 = PendingIntent.getBroadcast(this, 0, receiverIntent2, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(pref.getBoolean("app_drawer_icon", false) ? R.drawable.ic_system : R.drawable.ic_allapps)
                        .setContentIntent(contentIntent)
                        .setContentTitle(getString(R.string.taskbar_is_active))
                        .setContentText(getString(R.string.click_to_open_settings))
                        .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                        .setPriority(Notification.PRIORITY_MIN)
                        .setShowWhen(false)
                        .setOngoing(true);

                String showHideLabel;

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !U.isChromeOs(this)) {
                    String freeformLabel = getString(pref.getBoolean("freeform_hack", false) ? R.string.freeform_off : R.string.freeform_on);

                    Intent freeformIntent = new Intent("com.farmerbb.taskbar.TOGGLE_FREEFORM_MODE");
                    freeformIntent.setPackage(getPackageName());

                    PendingIntent freeformPendingIntent = PendingIntent.getBroadcast(this, 0, freeformIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    mBuilder.addAction(0, freeformLabel, freeformPendingIntent);

                    showHideLabel = getString(isHidden ? R.string.action_show_alt : R.string.action_hide_alt);
                } else
                    showHideLabel = getString(isHidden ? R.string.action_show : R.string.action_hide);

                mBuilder.addAction(0, showHideLabel, receiverPendingIntent)
                        .addAction(0, getString(R.string.action_quit), receiverPendingIntent2);

                startForeground(8675309, mBuilder.build());

                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.UPDATE_SWITCH"));

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    TileService.requestListeningState(this, new ComponentName(getPackageName(), QuickSettingsTileService.class.getName()));

                if(!isHidden) {
                    registerReceiver(userForegroundReceiver, new IntentFilter(Intent.ACTION_USER_FOREGROUND));
                    registerReceiver(userBackgroundReceiver, new IntentFilter(Intent.ACTION_USER_BACKGROUND));
                }
            } else {
                pref.edit().putBoolean("taskbar_active", false).apply();

                stopSelf();
            }
        } else stopSelf();
    }

    @Override
    public void onDestroy() {
        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean("is_restarting", false))
            pref.edit().remove("is_restarting").apply();
        else {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.UPDATE_SWITCH"));

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                TileService.requestListeningState(this, new ComponentName(getPackageName(), QuickSettingsTileService.class.getName()));

            if(!U.launcherIsDefault(this))
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.FINISH_FREEFORM_ACTIVITY"));
        }

        super.onDestroy();

        if(!isHidden) {
            unregisterReceiver(userForegroundReceiver);
            unregisterReceiver(userBackgroundReceiver);
        }
    }
}
