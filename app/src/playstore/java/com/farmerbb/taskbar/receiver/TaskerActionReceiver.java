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
import com.farmerbb.taskbar.util.BundleScrubber;
import com.farmerbb.taskbar.util.PluginBundleManager;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public final class TaskerActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(U.isExternalAccessDisabled(context)) return;

        BundleScrubber.scrub(intent);

        final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(bundle);

        if(bundle.containsKey(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE)
                && PluginBundleManager.isBundleValid(bundle)) {
            String action = bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE);
            String intentAction = getIntentAction(action);

            if(intentAction != null) switch(action) {
                case "tasker_on":
                case "tasker_off":
                    Intent actionIntent = new Intent(intentAction);
                    actionIntent.setPackage(context.getPackageName());
                    context.sendBroadcast(actionIntent);
                    break;
                default:
                    U.sendBroadcast(context, intentAction);
                    break;
            }
        }
    }
    
    private String getIntentAction(String action) {
        if(action != null) switch(action) {
            case "tasker_on":
                return ACTION_START;
            case "tasker_off":
                return ACTION_QUIT;
            case "show_taskbar":
                return ACTION_SHOW_TASKBAR;
            case PREF_HIDE_TASKBAR:
                return ACTION_HIDE_TASKBAR;
            case "toggle_start_menu":
                return ACTION_TOGGLE_START_MENU;
            case "toggle_dashboard":
                return ACTION_TOGGLE_DASHBOARD;
        }

        return null;
    }
}