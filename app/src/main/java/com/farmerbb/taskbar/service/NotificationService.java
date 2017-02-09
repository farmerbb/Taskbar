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
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.MainActivity;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.U;

public class NotificationService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean("taskbar_active", false)) {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                boolean isHidden = U.getSharedPreferences(this).getBoolean("is_hidden", false);
                String label = getString(isHidden ? R.string.action_show : R.string.action_hide);

                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                PendingIntent receiverIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.farmerbb.taskbar.SHOW_HIDE_TASKBAR"), PendingIntent.FLAG_UPDATE_CURRENT);
                PendingIntent receiverIntent2 = PendingIntent.getBroadcast(this, 0, new Intent("com.farmerbb.taskbar.QUIT"), PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(pref.getBoolean("app_drawer_icon", false) ? R.drawable.ic_system : R.drawable.ic_allapps)
                        .setContentIntent(contentIntent)
                        .setContentTitle(getString(R.string.taskbar_is_active))
                        .setContentText(getString(R.string.click_to_open_settings))
                        .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                        .addAction(0, label, receiverIntent)
                        .addAction(0, getString(R.string.action_quit), receiverIntent2)
                        .setPriority(Notification.PRIORITY_MIN)
                        .setShowWhen(false)
                        .setOngoing(true);

                startForeground(8675309, mBuilder.build());

                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.UPDATE_SWITCH"));

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    TileService.requestListeningState(this, new ComponentName(BuildConfig.APPLICATION_ID, QuickSettingsTileService.class.getName()));
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
                TileService.requestListeningState(this, new ComponentName(BuildConfig.APPLICATION_ID, QuickSettingsTileService.class.getName()));

            if(!U.launcherIsDefault(this))
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.FINISH_FREEFORM_ACTIVITY"));
        }

        super.onDestroy();
    }
}
