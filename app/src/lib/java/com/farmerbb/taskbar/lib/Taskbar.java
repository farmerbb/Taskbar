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
import android.support.annotation.Keep;

import com.farmerbb.taskbar.activity.MainActivity;

@Keep public class Taskbar {

    private Taskbar() {}

    @Keep public static void openSettings(Context context) {
        openSettings(context, null, -1);
    }

    @Keep public static void openSettings(Context context, String title) {
        openSettings(context, title, -1);
    }
    @Keep public static void openSettings(Context context, int theme) {
        openSettings(context, null, theme);
    }

    @Keep public static void openSettings(Context context, String title, int theme) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("theme", theme);
        intent.putExtra("back_arrow", true);

        if(!(context instanceof Activity))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }
}