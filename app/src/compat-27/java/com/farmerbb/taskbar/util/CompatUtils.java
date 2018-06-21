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

package com.farmerbb.taskbar.util;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.view.WindowManager;

import com.farmerbb.taskbar.R;

// Utility class meant for compatibility between the Android-x86 version of Taskbar (targeting API 25)
// and the Play Store version of Taskbar (targeting API 27, with additional dependencies).
// Do not make changes to this file without making corresponding changes to the Android-x86 version.

@TargetApi(Build.VERSION_CODES.O)
public class CompatUtils {

    private CompatUtils() {}

    public static String ACTION_APP_NOTIFICATION_SETTINGS = Settings.ACTION_APP_NOTIFICATION_SETTINGS;
    public static String EXTRA_APP_PACKAGE = Settings.EXTRA_APP_PACKAGE;

    public static void pinAppShortcut(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager mShortcutManager = context.getSystemService(ShortcutManager.class);

            if(mShortcutManager.isRequestPinShortcutSupported()) {
                ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(context, "freeform_mode").build();
                mShortcutManager.requestPinShortcut(pinShortcutInfo, null);
            } else
                U.showToastLong(context, R.string.pin_shortcut_not_supported);
        } else
            U.pinAppShortcut(context);
    }

    public static NotificationCompat.Builder getNotificationBuilder(Context context) {
        String id = "taskbar_notification_channel";

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_MIN;

            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.createNotificationChannel(new NotificationChannel(id, name, importance));
        }

        return new NotificationCompat.Builder(context, id);
    }

    public static void startForegroundService(Context context, Intent intent) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(Settings.canDrawOverlays(context))
                context.startForegroundService(intent);
        } else
            context.startService(intent);
    }

    public static int getOverlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }
}
