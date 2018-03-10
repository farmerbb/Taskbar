/* Copyright 2018 Braden Farmer
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

import com.farmerbb.taskbar.util.U;

public class DisableFreeformReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences pref = U.getSharedPreferences(context);
        boolean freeformEnabled = pref.getBoolean("freeform_hack", false);

        if(pref.getBoolean("skip_disable_freeform_receiver", false))
            pref.edit().remove("skip_disable_freeform_receiver").apply();
        else if(!U.isChromeOs(context) && freeformEnabled) {
            U.restartNotificationService(context);

            pref.edit().putBoolean("freeform_hack", false).apply();

            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.UPDATE_FREEFORM_CHECKBOX"));
        }
    }
}