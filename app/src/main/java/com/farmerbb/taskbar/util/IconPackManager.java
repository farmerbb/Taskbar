/* Based on code by Richard Ginzburg
 * See http://stackoverflow.com/questions/31490630/how-to-load-icon-from-icon-pack
 *
 * Copyright 2016 Braden Farmer
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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

public class IconPackManager {

    private static IconPackManager theInstance;

    private IconPack currentIconPack;

    private IconPackManager() {}

    public static IconPackManager getInstance() {
        if(theInstance == null) theInstance = new IconPackManager();

        return theInstance;
    }

    private List<IconPack> iconPacks = null;

    public List<IconPack> getAvailableIconPacks(Context mContext, boolean forceReload) {
        if(iconPacks == null || forceReload) {
            iconPacks = new ArrayList<>();

            PackageManager pm = mContext.getPackageManager();
            List<ResolveInfo> rinfo = pm.queryIntentActivities(new Intent("org.adw.launcher.THEMES"), PackageManager.GET_META_DATA);

            for(ResolveInfo ri : rinfo) {
                IconPack ip = new IconPack();
                ip.setPackageName(ri.activityInfo.packageName);

                ApplicationInfo ai;
                try {
                    ai = pm.getApplicationInfo(ip.getPackageName(), PackageManager.GET_META_DATA);
                    ip.setName(mContext.getPackageManager().getApplicationLabel(ai).toString());
                    iconPacks.add(ip);
                } catch (PackageManager.NameNotFoundException e) { /* Gracefully fail */ }
            }
        }
        return iconPacks;
    }

    void nullify() {
        currentIconPack = null;
    }

    IconPack getIconPack(String packageName) {
        if(currentIconPack == null || !currentIconPack.getPackageName().equals(packageName)) {
            currentIconPack = new IconPack();
            currentIconPack.setPackageName(packageName);
        }

        return currentIconPack;
    }
}