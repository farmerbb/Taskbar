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
import android.support.v4.content.LocalBroadcastManager;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.DummyActivity;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.util.U;

public class ToggleFreeformModeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent notificationIntent = new Intent(context, NotificationService.class);

        SharedPreferences pref = U.getSharedPreferences(context);
        if(pref.getBoolean("freeform_hack", false)) {
            pref.edit().putBoolean("freeform_hack", false).apply();

            context.stopService(notificationIntent);
            context.startService(notificationIntent);

            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.FINISH_FREEFORM_ACTIVITY"));
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.UPDATE_FREEFORM_CHECKBOX"));
        } else if(U.hasFreeformSupport(context)) {
            pref.edit().putBoolean("freeform_hack", true).apply();

            context.stopService(notificationIntent);

            Intent intent2 = new Intent(context, DummyActivity.class);
            intent2.putExtra("start_freeform_hack", true);
            intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent2);
            context.startService(notificationIntent);

            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.UPDATE_FREEFORM_CHECKBOX"));
        } else
            U.showToastLong(context, R.string.no_freeform_support);
    }
}
