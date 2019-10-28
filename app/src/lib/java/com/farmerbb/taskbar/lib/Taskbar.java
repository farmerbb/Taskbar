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

import com.farmerbb.taskbar.activity.MainActivity;

public class Taskbar {

    private Taskbar() {}

    public static void openSettings(Context context) {
        openSettings(context, null);
    }

    public static void openSettings(Context context, String title) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("title", title);

        if(!(context instanceof Activity))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }
}