package com.farmerbb.taskbar.shadow;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.UserHandle;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLauncherApps;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static org.robolectric.util.ReflectionHelpers.ClassParameter.from;

@Implements(value = LauncherApps.class, minSdk = LOLLIPOP)
public class TaskbarShadowLauncherApps extends ShadowLauncherApps {
    private Map<UserHandle, List<String>> enabledPackages = new HashMap<>();
    private Map<UserHandle, List<LauncherActivityInfo>> activityList = new HashMap<>();

    public static LauncherActivityInfo generateTestLauncherActivityInfo(Context context,
                                                                        ActivityInfo activityInfo,
                                                                        int userHandleId) {
        return
                ReflectionHelpers.callConstructor(
                        LauncherActivityInfo.class,
                        from(Context.class, context),
                        from(ActivityInfo.class, activityInfo),
                        from(UserHandle.class, UserHandle.getUserHandleForUid(userHandleId))
                );
    }

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

    /**
     * Add an {@link LauncherActivityInfo} to be got by {@link #getActivityList(String, UserHandle)}.
     *
     * @param userHandle   the user handle to be added.
     * @param activityInfo the {@link LauncherActivityInfo} to be added.
     */
    public void addActivity(UserHandle userHandle, LauncherActivityInfo activityInfo) {
        List<LauncherActivityInfo> activityInfoList = activityList.get(userHandle);
        if (activityInfoList == null) {
            activityInfoList = new ArrayList<>();
        }
        activityInfoList.remove(activityInfo);
        activityInfoList.add(activityInfo);
        activityList.put(userHandle, activityInfoList);
    }

    @Implementation
    protected boolean isPackageEnabled(String packageName, UserHandle user) {
        List<String> packages = enabledPackages.get(user);
        return packages != null && packages.contains(packageName);
    }

    @Implementation
    public List<LauncherActivityInfo> getActivityList(String packageName, UserHandle user) {
        List<LauncherActivityInfo> activityInfoList = activityList.get(user);
        if (activityInfoList == null || packageName == null) {
            return Collections.emptyList();
        }
        final Predicate<LauncherActivityInfo> predicatePackage =
                info ->
                        info.getComponentName() != null
                                && packageName.equals(info.getComponentName().getPackageName());
        return activityInfoList.stream().filter(predicatePackage).collect(Collectors.toList());
    }
}
