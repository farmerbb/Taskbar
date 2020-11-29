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
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class EnableFreeformReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences pref = U.getSharedPreferences(context);
        if(pref.getBoolean(PREF_DESKTOP_MODE, false)) return;

        boolean freeformEnabled = pref.getBoolean(PREF_FREEFORM_HACK, false);

        if(intent.hasExtra(EXTRA_SECONDSCREEN) && freeformEnabled)
            pref.edit().putBoolean(PREF_SKIP_DISABLE_FREEFORM_RECEIVER, true).apply();
        else if(U.hasFreeformSupport(context) && !freeformEnabled) {
            U.restartNotificationService(context);

            pref.edit().putBoolean(PREF_FREEFORM_HACK, true).apply();

            U.sendBroadcast(context, ACTION_UPDATE_FREEFORM_CHECKBOX);
        }
    }
}