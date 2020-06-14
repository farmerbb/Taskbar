package com.farmerbb.taskbar.shadow;

import android.content.pm.LauncherApps;
import android.os.UserHandle;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLauncherApps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

@Implements(value = LauncherApps.class, minSdk = LOLLIPOP)
public class TaskbarShadowLauncherApps extends ShadowLauncherApps {
    private Map<UserHandle, List<String>> enabledPackages = new HashMap<>();

    public void reset() {
        enabledPackages.clear();
    }

    /**
     * Adds an enabled package to be checked by {@link #isPackageEnabled(String, UserHandle)}.
     *
     * @param userHandle  the user handle to be added.
     * @param packageName the package name to be added.
     */
    public void addEnabledPackage(UserHandle userHandle, String packageName) {
        List<String> packages = enabledPackages.get(userHandle);
        if (packages == null) {
            packages = new ArrayList<>();
        }
        if (!packages.contains(packageName)) {
            packages.add(packageName);
        }
        enabledPackages.put(userHandle, packages);
    }

    @Implementation
    protected boolean isPackageEnabled(String packageName, UserHandle user) {
        List<String> packages = enabledPackages.get(user);
        return packages != null && packages.contains(packageName);
    }
}
