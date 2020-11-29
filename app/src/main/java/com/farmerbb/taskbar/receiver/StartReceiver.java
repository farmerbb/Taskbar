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

package com.farmerbb.taskbar.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.farmerbb.taskbar.activity.DummyActivity;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class StartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences pref = U.getSharedPreferences(context);

        boolean taskbarNotActive = !U.isServiceRunning(context, NotificationService.class);
        boolean taskbarActiveButHidden = !taskbarNotActive && pref.getBoolean(PREF_IS_HIDDEN, false);

        if(!U.canDrawOverlays(context)) {
            U.newHandler().postDelayed(() -> {
                Intent intent2 = new Intent(context, DummyActivity.class);
                intent2.putExtra(EXTRA_SHOW_PERMISSION_DIALOG, true);
                intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent2);
            }, 250);
        } else if(taskbarNotActive || taskbarActiveButHidden) {
            U.initPrefs(context);

            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean(PREF_IS_HIDDEN, false);

            if(taskbarNotActive) {
                if(pref.getBoolean(PREF_FIRST_RUN, true)) {
                    editor.putBoolean(PREF_FIRST_RUN, false);
                    editor.putBoolean(PREF_COLLAPSED, true);

                    U.newHandler().postDelayed(() -> {
                        Intent intent2 = new Intent(context, DummyActivity.class);
                        intent2.putExtra(EXTRA_SHOW_RECENT_APPS_DIALOG, true);
                        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        context.startActivity(intent2);
                    }, 250);
                }

                editor.putBoolean(PREF_TASKBAR_ACTIVE, true);
                editor.putLong(PREF_TIME_OF_SERVICE_START, System.currentTimeMillis());
            }

            editor.apply();

            if(taskbarActiveButHidden)
                context.stopService(new Intent(context, NotificationService.class));

            if(U.hasFreeformSupport(context) && U.isFreeformModeEnabled(context)) {
                Intent intent2 = new Intent(context, DummyActivity.class);
                intent2.putExtra(EXTRA_START_FREEFORM_HACK, true);
                intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent2);
            }

            Intent notificationIntent = new Intent(context, NotificationService.class);
            notificationIntent.putExtra(EXTRA_START_SERVICES, true);

            U.startForegroundService(context, notificationIntent);
        } else if(intent.hasExtra(EXTRA_SECONDSCREEN))
            pref.edit().putBoolean(PREF_SKIP_QUIT_RECEIVER, true).apply();
    }
}
