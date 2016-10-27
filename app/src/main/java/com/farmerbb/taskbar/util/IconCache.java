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

package com.farmerbb.taskbar.util;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.util.LruCache;

import com.farmerbb.taskbar.BuildConfig;

public class IconCache {

    private final LruCache<String, BitmapDrawable> drawables;

    private static IconCache theInstance;

    private IconCache(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final int memClass = am.getMemoryClass();
        final int cacheSize = (1024 * 1024 * memClass) / 8;

        drawables = new LruCache<String, BitmapDrawable>(cacheSize) {
            @Override
            protected int sizeOf(String key, BitmapDrawable value) {
                return value.getBitmap().getByteCount();
            }
        };
    }

    public static IconCache getInstance(Context context) {
        if(theInstance == null) theInstance = new IconCache(context);

        return theInstance;
    }

    public Drawable getIcon(Context context, PackageManager pm, LauncherActivityInfo appInfo) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        String name = appInfo.getComponentName().flattenToString() + ":" + userManager.getSerialNumberForUser(appInfo.getUser());

        Drawable drawable;
        Drawable loadedIcon = null;

        synchronized (drawables) {
            drawable = drawables.get(name);
            if(drawable == null) {
                loadedIcon = loadIcon(context, pm, appInfo);

                if(loadedIcon instanceof BitmapDrawable)
                    drawables.put(name, (BitmapDrawable) loadedIcon);
            }
        }

        return drawable == null ? loadedIcon : drawable;
    }

    private Drawable loadIcon(Context context, PackageManager pm, LauncherActivityInfo appInfo) {
        SharedPreferences pref = U.getSharedPreferences(context);
        String iconPackPackage = pref.getString("icon_pack", BuildConfig.APPLICATION_ID);
        boolean useMask = pref.getBoolean("icon_pack_use_mask", false);
        IconPackManager iconPackManager = IconPackManager.getInstance();

        try {
            pm.getPackageInfo(iconPackPackage, 0);
        } catch (PackageManager.NameNotFoundException e) {
            iconPackPackage = BuildConfig.APPLICATION_ID;
            pref.edit().putString("icon_pack", iconPackPackage).apply();
            U.refreshPinnedIcons(context);
        }

        if(iconPackPackage.equals(BuildConfig.APPLICATION_ID))
            return appInfo.getBadgedIcon(0);
        else {
            IconPack iconPack = iconPackManager.getIconPack(iconPackPackage);
            String componentName = new ComponentName(appInfo.getApplicationInfo().packageName, appInfo.getName()).toString();

            if(!useMask) {
                Drawable icon = iconPack.getDrawableIconForPackage(context, componentName);
                return icon == null ? appInfo.getBadgedIcon(0) : icon;
            } else {
                Drawable drawable = appInfo.getBadgedIcon(0);
                if(drawable instanceof BitmapDrawable) {
                    return new BitmapDrawable(context.getResources(),
                            iconPack.getIconForPackage(context, componentName, ((BitmapDrawable) drawable).getBitmap()));
                } else {
                    Drawable icon = iconPack.getDrawableIconForPackage(context, componentName);
                    return icon == null ? drawable : icon;
                }
            }
        }
    }

    public void clearCache() {
        drawables.evictAll();
        IconPackManager.getInstance().nullify();
        System.gc();
    }
}
