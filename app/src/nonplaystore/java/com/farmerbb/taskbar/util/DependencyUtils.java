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
import android.net.Uri;

import com.farmerbb.taskbar.R;

public class DependencyUtils {

    private DependencyUtils() {}

    public static CharSequence getKeyboardShortcutSummary(Context context) {
        return context.getString(R.string.tb_pref_description_keyboard_shortcut_alt);
    }

    static ToastInterface createToast(Context context, String message, int length) {
        return new ToastFrameworkImpl(context, message, length);
    }

    public static void requestTaskerQuery(Context context) {}

    public static void openChromeCustomTab(Context context, Uri uri) {}
}
