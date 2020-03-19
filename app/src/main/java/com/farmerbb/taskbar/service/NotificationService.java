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
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.service.quicksettings.TileService;
import android.view.Display;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.farmerbb.taskbar.activity.MainActivity;
import com.farmerbb.taskbar.activity.SecondaryHomeActivity;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.ApplicationType;
import com.farmerbb.taskbar.util.DependencyUtils;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.LauncherHelper;
import com.farmerbb.taskbar.util.U;

public class NotificationService extends Service {

    private boolean isHidden = true;
    private boolean desktopModeStarted = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.getBooleanExtra("start_services", false)) {
            startService(new Intent(this, TaskbarService.class));
            startService(new Intent(this, StartMenuService.class));
            startService(new Intent(this, DashboardService.class));
        }

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

    DisplayManager.DisplayListener listener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
            startDesktopMode(displayId, true);
        }

        @Override
        public void onDisplayChanged(int displayId) {}

        @Override
        public void onDisplayRemoved(int displayId) {
            stopDesktopMode();
        }
    };

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean("taskbar_active", false)) {
            if(U.canDrawOverlays(this)) {
                isHidden = U.getSharedPreferences(this).getBoolean("is_hidden", false);

                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                Intent receiverIntent = new Intent("com.farmerbb.taskbar.SHOW_HIDE_TASKBAR");
                receiverIntent.setPackage(getPackageName());

                Intent receiverIntent2 = new Intent("com.farmerbb.taskbar.QUIT");
                receiverIntent2.setPackage(getPackageName());

                PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                PendingIntent receiverPendingIntent = PendingIntent.getBroadcast(this, 0, receiverIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                PendingIntent receiverPendingIntent2 = PendingIntent.getBroadcast(this, 0, receiverIntent2, PendingIntent.FLAG_UPDATE_CURRENT);

                String id = "taskbar_notification_channel";

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    CharSequence name = getString(R.string.tb_app_name);
                    int importance = NotificationManager.IMPORTANCE_MIN;

                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.createNotificationChannel(new NotificationChannel(id, name, importance));
                }

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, id)
                        .setSmallIcon(pref.getString("start_button_image", U.getDefaultStartButtonImage(this)).equals("app_logo")
                                ? R.drawable.tb_system
                                : R.drawable.tb_allapps)
                        .setContentIntent(contentIntent)
                        .setContentTitle(getString(R.string.tb_taskbar_is_active))
                        .setContentText(getString(R.string.tb_click_to_open_settings))
                        .setColor(ContextCompat.getColor(this, R.color.tb_colorPrimary))
                        .setPriority(Notification.PRIORITY_MIN)
                        .setShowWhen(false)
                        .setOngoing(true);

                String showHideLabel;

                if(U.canEnableFreeform() && !U.isChromeOs(this)) {
                    String freeformLabel = getString(pref.getBoolean("freeform_hack", false) ? R.string.tb_freeform_off : R.string.tb_freeform_on);

                    Intent freeformIntent = new Intent("com.farmerbb.taskbar.TOGGLE_FREEFORM_MODE");
                    freeformIntent.setPackage(getPackageName());

                    PendingIntent freeformPendingIntent = PendingIntent.getBroadcast(this, 0, freeformIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    mBuilder.addAction(0, freeformLabel, freeformPendingIntent);

                    showHideLabel = getString(isHidden ? R.string.tb_action_show_alt : R.string.tb_action_hide_alt);
                } else
                    showHideLabel = getString(isHidden ? R.string.tb_action_show : R.string.tb_action_hide);

                mBuilder.addAction(0, showHideLabel, receiverPendingIntent)
                        .addAction(0, getString(R.string.tb_action_quit), receiverPendingIntent2);

                startForeground(8675309, mBuilder.build());

                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.UPDATE_SWITCH"));

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    TileService.requestListeningState(this, new ComponentName(getPackageName(), QuickSettingsTileService.class.getName()));

                DependencyUtils.requestTaskerQuery(this);

                if(!isHidden) {
                    registerReceiver(userForegroundReceiver, new IntentFilter(Intent.ACTION_USER_FOREGROUND));
                    registerReceiver(userBackgroundReceiver, new IntentFilter(Intent.ACTION_USER_BACKGROUND));
                }

                if(U.shouldStartDesktopMode(this)) {
                    startDesktopMode(U.getExternalDisplayID(this), false);

                    DisplayManager manager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
                    manager.registerDisplayListener(listener, null);
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

            DependencyUtils.requestTaskerQuery(this);

            if(!U.launcherIsDefault(this) || U.isChromeOs(this))
                U.stopFreeformHack(this);
        }

        super.onDestroy();

        if(!isHidden) {
            unregisterReceiver(userForegroundReceiver);
            unregisterReceiver(userBackgroundReceiver);
        }

        if(desktopModeStarted) {
            DisplayManager manager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
            manager.unregisterDisplayListener(listener);

            stopDesktopMode();
        }
    }

    private void startDesktopMode(int displayId, boolean shouldDelay) {
        LauncherHelper helper = LauncherHelper.getInstance();
        if(displayId == Display.DEFAULT_DISPLAY || helper.isOnSecondaryHomeScreen()) return;

        Runnable desktopModeLaunch = () -> {
            helper.setOnSecondaryHomeScreen(true, displayId);

            Intent intent = new Intent(this, SecondaryHomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent, U.getActivityOptions(this, ApplicationType.APP_FULLSCREEN, null).toBundle());
        };

        if(shouldDelay)
            new Handler().postDelayed(desktopModeLaunch, 500);
        else
            desktopModeLaunch.run();

        desktopModeStarted = true;
    }

    private void stopDesktopMode() {
        LocalBroadcastManager.getInstance(NotificationService.this).sendBroadcast(new Intent("com.farmerbb.taskbar.KILL_HOME_ACTIVITY"));
        desktopModeStarted = false;
    }
}
