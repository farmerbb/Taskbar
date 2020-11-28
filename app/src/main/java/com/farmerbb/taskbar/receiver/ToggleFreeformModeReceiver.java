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

package com.farmerbb.taskbar.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.DummyActivity;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class ToggleFreeformModeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences pref = U.getSharedPreferences(context);
        if(!pref.getBoolean(PREF_TASKBAR_ACTIVE, false)
                || pref.getBoolean(PREF_DESKTOP_MODE, false)) return;

        Intent notificationIntent = new Intent(context, NotificationService.class);

        if(pref.getBoolean(PREF_FREEFORM_HACK, false)) {
            pref.edit().putBoolean(PREF_FREEFORM_HACK, false).apply();

            context.stopService(notificationIntent);

            U.startForegroundService(context, notificationIntent);

            U.stopFreeformHack(context);
            U.sendBroadcast(context, ACTION_UPDATE_FREEFORM_CHECKBOX);
        } else if(U.hasFreeformSupport(context)) {
            pref.edit().putBoolean(PREF_FREEFORM_HACK, true).apply();

            context.stopService(notificationIntent);

            Intent intent2 = new Intent(context, DummyActivity.class);
            intent2.putExtra(EXTRA_START_FREEFORM_HACK, true);
            intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent2);

            U.startForegroundService(context, notificationIntent);
            U.sendBroadcast(context, ACTION_UPDATE_FREEFORM_CHECKBOX);
        } else
            U.showToastLong(context, R.string.tb_no_freeform_support);
    }
}
