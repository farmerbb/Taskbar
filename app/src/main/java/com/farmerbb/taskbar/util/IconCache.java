package com.farmerbb.taskbar.util;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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

    public Drawable getIcon(Context context, PackageManager pm, ActivityInfo appInfo) {
        String name = appInfo.packageName + "/" + appInfo.name;

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

    private Drawable loadIcon(Context context, PackageManager pm, ActivityInfo appInfo) {
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
            return appInfo.loadIcon(pm);
        else {
            IconPack iconPack = iconPackManager.getIconPack(iconPackPackage);
            String componentName = new ComponentName(appInfo.packageName, appInfo.name).toString();

            if(!useMask) {
                Drawable icon = iconPack.getDrawableIconForPackage(context, componentName);
                return icon == null ? appInfo.loadIcon(pm) : icon;
            } else {
                Drawable drawable = appInfo.loadIcon(pm);
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
