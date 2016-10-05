package com.farmerbb.taskbar.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.farmerbb.taskbar.BuildConfig;

import java.util.Map;
import java.util.WeakHashMap;

public class IconCache {
    private final Map<String, Drawable> drawables = new WeakHashMap<>();

    private static IconCache theInstance;

    private IconCache() {}

    public static IconCache getInstance() {
        if(theInstance == null) theInstance = new IconCache();

        return theInstance;
    }

    public Drawable getIcon(Context context, PackageManager pm, ActivityInfo appInfo) {
        String name = appInfo.name;

        if(!drawables.containsKey(name))
            drawables.put(name, loadIcon(context, pm, appInfo));

        return drawables.get(name);
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
        drawables.clear();
        IconPackManager.getInstance().forceReload();
        System.gc();
    }
}
