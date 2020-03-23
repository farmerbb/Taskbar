/* Copyright 2019 Braden Farmer
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

package com.farmerbb.taskbar.lib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import com.farmerbb.taskbar.activity.MainActivity;
import com.farmerbb.taskbar.activity.SecondaryHomeActivity;
import com.farmerbb.taskbar.util.U;

@Keep public class Taskbar {

    private Taskbar() {}

    /**
     * Opens the settings page for configuring Taskbar.
     * @param context Context used to start the activity
     */
    @Keep public static void openSettings(@NonNull Context context) {
        openSettings(context, null, -1);
    }

    /**
     * Opens the settings page for configuring Taskbar, using the specified title.
     * @param context Context used to start the activity
     * @param title Title to display in the top level of the settings hierarchy.
     *              If null, defaults to "Settings".
     */
    @Keep public static void openSettings(@NonNull Context context, @Nullable String title) {
        openSettings(context, title, -1);
    }

    /**
     * Opens the settings page for configuring Taskbar, using the specified theme.
     * @param context Context used to start the activity
     * @param theme Theme to apply to the settings activity. If set to -1, the activity will
     *              use the app's default theme if it is a derivative of Theme.AppCompat,
     *              or Theme.AppCompat.Light otherwise.
     */
    @Keep public static void openSettings(@NonNull Context context, @StyleRes int theme) {
        openSettings(context, null, theme);
    }

    /**
     * Opens the settings page for configuring Taskbar, using the specified title and theme.
     * @param context Context used to start the activity
     * @param title Title to display in the top level of the settings hierarchy.
     *              If null, defaults to "Settings".
     * @param theme Theme to apply to the settings activity. If set to -1, the activity will
     *              use the app's default theme if it is a derivative of Theme.AppCompat,
     *              or Theme.AppCompat.Light otherwise.
     */
    @Keep public static void openSettings(@NonNull Context context, @Nullable String title, @StyleRes int theme) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("theme", theme);
        intent.putExtra("back_arrow", true);

        if(!(context instanceof Activity))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }

    /**
     * Enables or disables Taskbar's desktop mode at runtime.
     * @param context Context used to control component state
     * @param enabled "true" to enable desktop mode, "false" to disable
     */
    @Keep public static void setEnabled(@NonNull Context context, boolean enabled) {
        U.setComponentEnabled(context, SecondaryHomeActivity.class, enabled);
    }
}