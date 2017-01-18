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

import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.PinnedBlockedApps;

import java.util.ArrayList;
import java.util.List;

public class UninstallReceiver extends BroadcastReceiver {
    @SuppressWarnings("Convert2streamapi")
    @Override
    public void onReceive(Context context, Intent intent) {
        String packageName = intent.getData().getEncodedSchemeSpecificPart();

        PinnedBlockedApps pba = PinnedBlockedApps.getInstance(context);
        List<AppEntry> pinnedApps = pba.getPinnedApps();
        List<String> componentNames = new ArrayList<>();

        for(AppEntry entry : pinnedApps) {
            if(entry.getPackageName().equals(packageName)) {
                componentNames.add(entry.getComponentName());
            }
        }

        for(String componentName : componentNames) {
            pba.removePinnedApp(context, componentName);
        }
    }
}
