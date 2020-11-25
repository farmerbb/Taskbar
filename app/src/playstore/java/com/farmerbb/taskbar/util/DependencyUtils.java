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
import android.net.Uri;
import android.os.Build;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.TaskerConditionActivity;
import com.mikepenz.foundation_icons_typeface_library.FoundationIcons;
import com.mikepenz.iconics.Iconics;

// Utility class meant for abstracting out all third-party dependencies.
// This allows the Android-x86 version of Taskbar to be built purely from AOSP source.
// TODO Do not make changes to this file without making corresponding changes to the Android-x86 version.

public class DependencyUtils {

    private DependencyUtils() {}

    public static CharSequence getKeyboardShortcutSummary(Context context) {
        Iconics.registerFont(new FoundationIcons());
        return new Iconics.IconicsBuilder()
                .ctx(context)
                .on(context.getString(R.string.tb_pref_description_keyboard_shortcut))
                .build();
    }

    static ToastInterface createToast(Context context, String message, int length) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
            return new ToastFrameworkImpl(context, message, length);
        else
            return new ToastCompatImpl(context, message, length);
    }

    public static void requestTaskerQuery(Context context) {
        Intent query = new Intent(com.twofortyfouram.locale.api.Intent.ACTION_REQUEST_QUERY);
        query.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_ACTIVITY_CLASS_NAME, TaskerConditionActivity.class.getName());
        context.sendBroadcast(query);
    }

    public static void openChromeCustomTab(Context context, Uri uri) {
        new CustomTabsIntent.Builder()
                .setToolbarColor(ContextCompat.getColor(context, R.color.tb_colorPrimary))
                .setSecondaryToolbarColor(ContextCompat.getColor(context, R.color.tb_main_activity_background))
                .setStartAnimations(context, R.anim.tb_enter_from_right, R.anim.tb_exit_to_left)
                .setExitAnimations(context, R.anim.tb_enter_from_left, R.anim.tb_exit_to_right)
                .setShowTitle(true)
                .addDefaultShareMenuItem()
                .build()
                .launchUrl(context, uri);
    }
}
