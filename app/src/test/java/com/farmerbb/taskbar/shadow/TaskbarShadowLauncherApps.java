package com.farmerbb.taskbar.shadow;

import android.content.pm.LauncherApps;
import android.os.UserHandle;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLauncherApps;

import java.util.ArrayList;
import java.util.List;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

@Implements(value = LauncherApps.class, minSdk = LOLLIPOP)
public class TaskbarShadowLauncherApps extends ShadowLauncherApps {
    private static final List<String> enabledPackages = new ArrayList<>();

    public static void addEnabledPackages(String packageName) {
        if (!enabledPackages.contains(packageName)) {
            enabledPackages.add(packageName);
        }
    }

    public static void removeEnabledPackage(String packageName) {
        enabledPackages.remove(packageName);
    }

    public static void reset() {
        enabledPackages.clear();
    }

    @Implementation
    protected boolean isPackageEnabled(String packageName, UserHandle user) {
        return enabledPackages.contains(packageName);
    }
}
