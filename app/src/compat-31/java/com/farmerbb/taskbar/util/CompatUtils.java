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

package com.farmerbb.taskbar.util;

// Utility class meant for compatibility between the Android-x86 version of Taskbar (compiled with SDK 28)
// and the Play Store version of Taskbar (compiled with SDK 34).
// TODO Do not make changes to this file without making corresponding changes to the Android-x86 version.

import android.app.Activity;
import android.os.Build;
import android.content.Intent;
import android.service.quicksettings.TileService;
import android.view.View;
import android.view.WindowInsets;

public class CompatUtils {

    private CompatUtils() {}

    public static boolean isImeVisible(View view) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && view.getRootWindowInsets().isVisible(WindowInsets.Type.ime());
    }

    public static void grantNotificationPermissionIfNeeded(Activity activity) {}

    public static boolean startActivityAndCollapse(TileService service, Intent intent) {
        return false;
    }
}