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
import android.os.Bundle;

import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.util.BundleScrubber;
import com.farmerbb.taskbar.util.PluginBundleManager;
import com.farmerbb.taskbar.util.U;

public final class TaskerConditionReceiver extends BroadcastReceiver {
    private Bundle lastbundle = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(U.isExternalAccessDisabled(context)) return;

        if(lastbundle != null && lastbundle.equals(intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE))) {
            // bundle hasn't changed: we can safely return
            return;
        }
        updateValues(intent);

        BundleScrubber.scrub(intent);

        BundleScrubber.scrub(lastbundle);

        if(PluginBundleManager.isBundleValid(lastbundle)) {
            String action = lastbundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE);

            if(action != null) switch(action) {
                case "tasker_on":
                    if(U.isServiceRunning(context, NotificationService.class))
                        setResultCode(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_SATISFIED);
                    else
                        setResultCode(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNSATISFIED);
                    break;
                case "tasker_off":
                    if(U.isServiceRunning(context, NotificationService.class))
                        setResultCode(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNSATISFIED);
                    else
                        setResultCode(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_SATISFIED);
                    break;
            }
        }
    }

    private void updateValues(Intent intent) {
        lastbundle = intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
    }
}