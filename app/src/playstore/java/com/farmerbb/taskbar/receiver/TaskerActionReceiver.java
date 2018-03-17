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

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.util.BundleScrubber;
import com.farmerbb.taskbar.util.PluginBundleManager;

public final class TaskerActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        BundleScrubber.scrub(intent);

        final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(bundle);

        if(bundle.containsKey(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE)) {
            if(PluginBundleManager.isBundleValid(bundle)) {
                String action = bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE);
                Intent startStopIntent = null;

                if(action != null) switch(action) {
                    case "tasker_on":
                        startStopIntent = new Intent("com.farmerbb.taskbar.START");
                        break;
                    case "tasker_off":
                        startStopIntent = new Intent("com.farmerbb.taskbar.QUIT");
                        break;
                }

                if(startStopIntent != null) {
                    startStopIntent.setPackage(BuildConfig.APPLICATION_ID);
                    context.sendBroadcast(startStopIntent);
                }
            }
        }
    }
}