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

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.view.WindowManager;

// Utility class meant for compatibility between the Android-x86 version of Taskbar (targeting API 25)
// and the Play Store version of Taskbar (targeting API 27, with additional dependencies).
// Do not make changes to this file without making corresponding changes to the Android-x86 version.

public class CompatUtils {

    private CompatUtils() {}

    public static String ACTION_APP_NOTIFICATION_SETTINGS = "android.settings.APP_NOTIFICATION_SETTINGS";
    public static String EXTRA_APP_PACKAGE = "android.provider.extra.APP_PACKAGE";

    public static void pinAppShortcut(Context context) {
        U.pinAppShortcut(context);
    }

    @SuppressWarnings("deprecation")
    public static NotificationCompat.Builder getNotificationBuilder(Context context) {
        return new NotificationCompat.Builder(context);
    }

    public static void startForegroundService(Context context, Intent intent) {
        context.startService(intent);
    }

    public static int getOverlayType() {
        return WindowManager.LayoutParams.TYPE_PHONE;
    }
}

