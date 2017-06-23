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

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.DummyActivity;
import com.farmerbb.taskbar.activity.InvisibleActivityFreeform;
import com.farmerbb.taskbar.activity.ShortcutActivity;
import com.farmerbb.taskbar.activity.StartTaskbarActivity;
import com.farmerbb.taskbar.receiver.LockDeviceReceiver;
import com.farmerbb.taskbar.service.DashboardService;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.service.PowerMenuService;
import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.service.TaskbarService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class U {

    private U() {}

    private static SharedPreferences pref;
    private static Integer cachedRotation;

    private static final int MAXIMIZED = 0;
    private static final int LEFT = -1;
    private static final int RIGHT = 1;

    public static final int HIDDEN = 0;
    public static final int TOP_APPS = 1;

    // From android.app.ActivityManager.StackId
    private static final int FULLSCREEN_WORKSPACE_STACK_ID = 1;
    private static final int FREEFORM_WORKSPACE_STACK_ID = 2;

    public static SharedPreferences getSharedPreferences(Context context) {
        if(pref == null) pref = context.getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_PRIVATE);
        return pref;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static AlertDialog showPermissionDialog(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_dialog_message)
                .setPositiveButton(R.string.action_grant_permission, (dialog, which) -> {
                    try {
                        context.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + BuildConfig.APPLICATION_ID)));
                    } catch (ActivityNotFoundException e) {
                        showErrorDialog(context, "SYSTEM_ALERT_WINDOW");
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setCancelable(false);

        return dialog;
    }

    public static void showErrorDialog(final Context context, String appopCmd) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.error_dialog_title)
                .setMessage(context.getString(R.string.error_dialog_message, BuildConfig.APPLICATION_ID, appopCmd))
                .setPositiveButton(R.string.action_ok, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void lockDevice(Context context) {
        ComponentName component = new ComponentName(context, LockDeviceReceiver.class);
        context.getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        DevicePolicyManager mDevicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if(mDevicePolicyManager.isAdminActive(component))
            mDevicePolicyManager.lockNow();
        else {
            launchApp(context, () -> {
                Intent intent = new Intent(context, DummyActivity.class);
                intent.putExtra("device_admin", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            });
        }
    }

    public static void sendAccessibilityAction(Context context, int action) {
        ComponentName component = new ComponentName(context, PowerMenuService.class);
        context.getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        if(isAccessibilityServiceEnabled(context)) {
            Intent intent = new Intent("com.farmerbb.taskbar.ACCESSIBILITY_ACTION");
            intent.putExtra("action", action);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } else {
            launchApp(context, () -> {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    context.startActivity(intent);
                    showToastLong(context, R.string.enable_accessibility);
                } catch (ActivityNotFoundException e) {
                    showToast(context, R.string.lock_device_not_supported);
                }
            });
        }
    }

    private static boolean isAccessibilityServiceEnabled(Context context) {
        String accessibilityServices = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        ComponentName component = new ComponentName(context, PowerMenuService.class);

        return accessibilityServices != null
                && (accessibilityServices.contains(component.flattenToString())
                || accessibilityServices.contains(component.flattenToShortString()));
    }

    public static void showToast(Context context, int message) {
        showToast(context, context.getString(message), Toast.LENGTH_SHORT);
    }

    public static void showToastLong(Context context, int message) {
        showToast(context, context.getString(message), Toast.LENGTH_LONG);
    }

    public static void showToast(Context context, String message, int length) {
        cancelToast();

        ToastInterface toast;
        if(BuildConfig.APPLICATION_ID.equals(BuildConfig.ANDROIDX86_APPLICATION_ID))
            toast = new ToastFrameworkImpl(context, message, length);
        else
            toast = new ToastCompatImpl(context, message, length);

        toast.show();

        ToastHelper.getInstance().setLastToast(toast);
    }

    private static void cancelToast() {
        ToastInterface toast = ToastHelper.getInstance().getLastToast();
        if(toast != null) toast.cancel();
    }

    public static void startShortcut(Context context, String packageName, String componentName, ShortcutInfo shortcut) {
        launchApp(context,
                packageName,
                componentName,
                0,
                null,
                false,
                false,
                shortcut);
    }

    public static void launchApp(final Context context,
                                 final String packageName,
                                 final String componentName,
                                 final long userId, final String windowSize,
                                 final boolean launchedFromTaskbar,
                                 final boolean openInNewWindow) {
        launchApp(context,
                packageName,
                componentName,
                userId,
                windowSize,
                launchedFromTaskbar,
                openInNewWindow,
                null);
    }

    private static void launchApp(final Context context,
                                 final String packageName,
                                 final String componentName,
                                 final long userId, final String windowSize,
                                 final boolean launchedFromTaskbar,
                                 final boolean openInNewWindow,
                                 final ShortcutInfo shortcut) {
        launchApp(context, launchedFromTaskbar, () -> continueLaunchingApp(context, packageName, componentName, userId,
                windowSize, launchedFromTaskbar, openInNewWindow, shortcut));
    }

    public static void launchApp(Context context, Runnable runnable) {
        launchApp(context, true, runnable);
    }

    private static void launchApp(Context context, boolean launchedFromTaskbar, Runnable runnable) {
        SharedPreferences pref = getSharedPreferences(context);
        FreeformHackHelper helper = FreeformHackHelper.getInstance();

        boolean specialLaunch = isOPreview() && FreeformHackHelper.getInstance().isInFreeformWorkspace()
                && MenuHelper.getInstance().isContextMenuOpen();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && pref.getBoolean("freeform_hack", false)
                && (!helper.isInFreeformWorkspace() || specialLaunch)) {
            new Handler().postDelayed(() -> {
                startFreeformHack(context, true, launchedFromTaskbar);

                new Handler().postDelayed(runnable, helper.isFreeformHackActive() ? 0 : 100);
            }, launchedFromTaskbar ? 0 : 100);
        } else
            runnable.run();
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.N)
    public static void startFreeformHack(Context context, boolean checkMultiWindow, boolean launchedFromTaskbar) {
        Intent freeformHackIntent = new Intent(context, InvisibleActivityFreeform.class);
        freeformHackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);

        if(checkMultiWindow)
            freeformHackIntent.putExtra("check_multiwindow", true);

        if(launchedFromTaskbar) {
            SharedPreferences pref = getSharedPreferences(context);
            if(pref.getBoolean("disable_animations", false))
                freeformHackIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }

        if(canDrawOverlays(context))
            launchAppLowerRight(context, freeformHackIntent);
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static void continueLaunchingApp(Context context,
                                             String packageName,
                                             String componentName,
                                             long userId,
                                             String windowSize,
                                             boolean launchedFromTaskbar,
                                             boolean openInNewWindow,
                                             ShortcutInfo shortcut) {
        SharedPreferences pref = getSharedPreferences(context);
        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(componentName));
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if(FreeformHackHelper.getInstance().isInFreeformWorkspace()
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1
                && !isOPreview())
            intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);

        if(launchedFromTaskbar) {
            if(pref.getBoolean("disable_animations", false))
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }

        if(openInNewWindow || pref.getBoolean("force_new_window", false)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

            ActivityInfo activityInfo = intent.resolveActivityInfo(context.getPackageManager(), 0);
            if(activityInfo != null) {
                switch(activityInfo.launchMode) {
                    case ActivityInfo.LAUNCH_SINGLE_TASK:
                    case ActivityInfo.LAUNCH_SINGLE_INSTANCE:
                        intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
                        break;
                }
            }
        }

        ApplicationType type = getApplicationType(context, packageName);

        if(windowSize == null)
            windowSize = SavedWindowSizes.getInstance(context).getWindowSize(context, packageName);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                || !pref.getBoolean("freeform_hack", false)
                || windowSize.equals("standard")) {
            launchStandard(context, intent, userId, shortcut, type);
        } else switch(windowSize) {
            case "large":
                launchMode1(context, intent, userId, shortcut, type);
                break;
            case "fullscreen":
                launchMode2(context, intent, MAXIMIZED, userId, shortcut, type);
                break;
            case "half_left":
                launchMode2(context, intent, LEFT, userId, shortcut, type);
                break;
            case "half_right":
                launchMode2(context, intent, RIGHT, userId, shortcut, type);
                break;
            case "phone_size":
                launchMode3(context, intent, userId, shortcut, type);
                break;
        }

        if(pref.getBoolean("hide_taskbar", true) && !FreeformHackHelper.getInstance().isInFreeformWorkspace())
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_TASKBAR"));
        else
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
    }
    
    private static void launchStandard(Context context, Intent intent, long userId, ShortcutInfo shortcut, ApplicationType type) {
        Bundle bundle = Build.VERSION.SDK_INT < Build.VERSION_CODES.N ? null : getActivityOptions(type).toBundle();
        if(shortcut == null) {
            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            if(userId == userManager.getSerialNumberForUser(Process.myUserHandle())) {
                try {
                    context.startActivity(intent, bundle);
                } catch (ActivityNotFoundException e) {
                    launchAndroidForWork(context, intent.getComponent(), bundle, userId);
                } catch (IllegalArgumentException e) { /* Gracefully fail */ }
            } else
                launchAndroidForWork(context, intent.getComponent(), bundle, userId);
        } else
            launchShortcut(context, shortcut, bundle);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.N)
    private static void launchMode1(Context context, Intent intent, long userId, ShortcutInfo shortcut, ApplicationType type) {
        DisplayMetrics metrics = getRealDisplayMetrics(context);

        int width1 = metrics.widthPixels / 8;
        int width2 = metrics.widthPixels - width1;
        int height1 = metrics.heightPixels / 8;
        int height2 = metrics.heightPixels - height1;

        Bundle bundle = getActivityOptions(type).setLaunchBounds(new Rect(
                width1,
                height1,
                width2,
                height2
        )).toBundle();

        if(shortcut == null) {
            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            if(userId == userManager.getSerialNumberForUser(Process.myUserHandle())) {
                try {
                    context.startActivity(intent, bundle);
                } catch (ActivityNotFoundException e) {
                    launchAndroidForWork(context, intent.getComponent(), bundle, userId);
                } catch (IllegalArgumentException e) { /* Gracefully fail */ }
            } else
                launchAndroidForWork(context, intent.getComponent(), bundle, userId);
        } else
            launchShortcut(context, shortcut, bundle);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.N)
    private static void launchMode2(Context context, Intent intent, int launchType, long userId, ShortcutInfo shortcut, ApplicationType type) {
        DisplayMetrics metrics = getRealDisplayMetrics(context);
        
        int statusBarHeight = getStatusBarHeight(context);
        String position = getTaskbarPosition(context);

        boolean isPortrait = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        boolean isLandscape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        int left = launchType == RIGHT && isLandscape
                ? metrics.widthPixels / 2
                : 0;

        int top = launchType == RIGHT && isPortrait
                ? metrics.heightPixels / 2
                : statusBarHeight;

        int right = launchType == LEFT && isLandscape
                ? metrics.widthPixels / 2
                : metrics.widthPixels;

        int bottom = launchType == LEFT && isPortrait
                ? metrics.heightPixels / 2
                : metrics.heightPixels;

        int iconSize = context.getResources().getDimensionPixelSize(R.dimen.icon_size);

        if(position.contains("vertical_left")) {
            if(launchType != RIGHT || isPortrait) left = left + iconSize;
        } else if(position.contains("vertical_right")) {
            if(launchType != LEFT || isPortrait) right = right - iconSize;
        } else if(position.contains("bottom")) {
            if(isLandscape || (launchType != LEFT && isPortrait))
                bottom = bottom - iconSize;
        } else if(isLandscape || (launchType != RIGHT && isPortrait))
            top = top + iconSize;

        Bundle bundle = getActivityOptions(type).setLaunchBounds(new Rect(
                left,
                top,
                right,
                bottom
        )).toBundle();

        if(shortcut == null) {
            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            if(userId == userManager.getSerialNumberForUser(Process.myUserHandle())) {
                try {
                    context.startActivity(intent, bundle);
                } catch (ActivityNotFoundException e) {
                    launchAndroidForWork(context, intent.getComponent(), bundle, userId);
                } catch (IllegalArgumentException e) { /* Gracefully fail */ }
            } else
                launchAndroidForWork(context, intent.getComponent(), bundle, userId);
        } else
            launchShortcut(context, shortcut, bundle);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.N)
    private static void launchMode3(Context context, Intent intent, long userId, ShortcutInfo shortcut, ApplicationType type) {
        DisplayMetrics metrics = getRealDisplayMetrics(context);

        int width1 = metrics.widthPixels / 2;
        int width2 = context.getResources().getDimensionPixelSize(R.dimen.phone_size_width) / 2;
        int height1 = metrics.heightPixels / 2;
        int height2 = context.getResources().getDimensionPixelSize(R.dimen.phone_size_height) / 2;

        Bundle bundle = getActivityOptions(type).setLaunchBounds(new Rect(
                width1 - width2,
                height1 - height2,
                width1 + width2,
                height1 + height2
        )).toBundle();

        if(shortcut == null) {
            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            if(userId == userManager.getSerialNumberForUser(Process.myUserHandle())) {
                try {
                    context.startActivity(intent, bundle);
                } catch (ActivityNotFoundException e) {
                    launchAndroidForWork(context, intent.getComponent(), bundle, userId);
                } catch (IllegalArgumentException e) { /* Gracefully fail */ }
            } else
                launchAndroidForWork(context, intent.getComponent(), bundle, userId);
        } else
            launchShortcut(context, shortcut, bundle);
    }

    private static void launchAndroidForWork(Context context, ComponentName componentName, Bundle bundle, long userId) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

        try {
            launcherApps.startMainActivity(componentName, userManager.getUserForSerialNumber(userId), null, bundle);
        } catch (ActivityNotFoundException | NullPointerException e) { /* Gracefully fail */ }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private static void launchShortcut(Context context, ShortcutInfo shortcut, Bundle bundle) {
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

        if(launcherApps.hasShortcutHostPermission()) {
            try {
                launcherApps.startShortcut(shortcut, null, bundle);
            } catch (ActivityNotFoundException | NullPointerException e) { /* Gracefully fail */ }
        }
    }

    public static void launchAppMaximized(Context context, Intent intent) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        long userId = userManager.getSerialNumberForUser(Process.myUserHandle());

        launchMode2(context, intent, MAXIMIZED, userId, null, ApplicationType.CONTEXT_MENU);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.N)
    public static void launchAppLowerRight(Context context, Intent intent) {
        DisplayMetrics metrics = getRealDisplayMetrics(context);
        try {
            context.startActivity(intent, getActivityOptions(ApplicationType.FREEFORM_HACK).setLaunchBounds(new Rect(
                    metrics.widthPixels,
                    metrics.heightPixels,
                    metrics.widthPixels + 1,
                    metrics.heightPixels + 1
            )).toBundle());
        } catch (IllegalArgumentException e) { /* Gracefully fail */ }
    }

    public static void checkForUpdates(Context context) {
        if(!BuildConfig.APPLICATION_ID.equals(BuildConfig.ANDROIDX86_APPLICATION_ID)) {
            if(!BuildConfig.DEBUG) {
                String url;
                try {
                    context.getPackageManager().getPackageInfo("com.android.vending", 0);
                    url = "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID;
                } catch (PackageManager.NameNotFoundException e) {
                    url = "https://f-droid.org/repository/browse/?fdid=" + BuildConfig.BASE_APPLICATION_ID;
                }

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
            } else
                showToast(context, R.string.debug_build);
        }
    }

    public static boolean launcherIsDefault(Context context) {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo defaultLauncher = context.getPackageManager().resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);

        return defaultLauncher.activityInfo.packageName.equals(BuildConfig.APPLICATION_ID);
    }

    public static void setCachedRotation(int cachedRotation) {
        U.cachedRotation = cachedRotation;
    }

    public static String getTaskbarPosition(Context context) {
        SharedPreferences pref = getSharedPreferences(context);
        String position = pref.getString("position", "bottom_left");

        if(pref.getBoolean("anchor", false)) {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            int rotation = cachedRotation != null ? cachedRotation : windowManager.getDefaultDisplay().getRotation();

            switch(position) {
                case "bottom_left":
                    switch(rotation) {
                        case Surface.ROTATION_0:
                            return "bottom_left";
                        case Surface.ROTATION_90:
                            return "bottom_vertical_right";
                        case Surface.ROTATION_180:
                            return "top_right";
                        case Surface.ROTATION_270:
                            return "top_vertical_left";
                    }
                    break;
                case "bottom_vertical_left":
                    switch(rotation) {
                        case Surface.ROTATION_0:
                            return "bottom_vertical_left";
                        case Surface.ROTATION_90:
                            return "bottom_right";
                        case Surface.ROTATION_180:
                            return "top_vertical_right";
                        case Surface.ROTATION_270:
                            return "top_left";
                    }
                    break;
                case "bottom_right":
                    switch(rotation) {
                        case Surface.ROTATION_0:
                            return "bottom_right";
                        case Surface.ROTATION_90:
                            return "top_vertical_right";
                        case Surface.ROTATION_180:
                            return "top_left";
                        case Surface.ROTATION_270:
                            return "bottom_vertical_left";
                    }
                    break;
                case "bottom_vertical_right":
                    switch(rotation) {
                        case Surface.ROTATION_0:
                            return "bottom_vertical_right";
                        case Surface.ROTATION_90:
                            return "top_right";
                        case Surface.ROTATION_180:
                            return "top_vertical_left";
                        case Surface.ROTATION_270:
                            return "bottom_left";
                    }
                    break;
                case "top_left":
                    switch(rotation) {
                        case Surface.ROTATION_0:
                            return "top_left";
                        case Surface.ROTATION_90:
                            return "bottom_vertical_left";
                        case Surface.ROTATION_180:
                            return "bottom_right";
                        case Surface.ROTATION_270:
                            return "top_vertical_right";
                    }
                    break;
                case "top_vertical_left":
                    switch(rotation) {
                        case Surface.ROTATION_0:
                            return "top_vertical_left";
                        case Surface.ROTATION_90:
                            return "bottom_left";
                        case Surface.ROTATION_180:
                            return "bottom_vertical_right";
                        case Surface.ROTATION_270:
                            return "top_right";
                    }
                    break;
                case "top_right":
                    switch(rotation) {
                        case Surface.ROTATION_0:
                            return "top_right";
                        case Surface.ROTATION_90:
                            return "top_vertical_left";
                        case Surface.ROTATION_180:
                            return "bottom_left";
                        case Surface.ROTATION_270:
                            return "bottom_vertical_right";
                    }
                    break;
                case "top_vertical_right":
                    switch(rotation) {
                        case Surface.ROTATION_0:
                            return "top_vertical_right";
                        case Surface.ROTATION_90:
                            return "top_left";
                        case Surface.ROTATION_180:
                            return "bottom_vertical_left";
                        case Surface.ROTATION_270:
                            return "bottom_right";
                    }
                    break;
            }
        }

        return position;
    }

    private static int getMaxNumOfColumns(Context context) {
        SharedPreferences pref = getSharedPreferences(context);
        DisplayMetrics metrics = getRealDisplayMetrics(context);
        float baseTaskbarSize = getBaseTaskbarSizeFloat(context) / metrics.density;
        int numOfColumns = 0;

        float maxScreenSize = getTaskbarPosition(context).contains("vertical")
                ? (metrics.heightPixels - getStatusBarHeight(context)) / metrics.density
                : metrics.widthPixels / metrics.density;

        float iconSize = context.getResources().getDimension(R.dimen.icon_size) / metrics.density;

        int userMaxNumOfColumns = Integer.valueOf(pref.getString("max_num_of_recents", "10"));

        while(baseTaskbarSize + iconSize < maxScreenSize
                && numOfColumns < userMaxNumOfColumns) {
            baseTaskbarSize = baseTaskbarSize + iconSize;
            numOfColumns++;
        }

        return numOfColumns;
    }

    public static int getMaxNumOfEntries(Context context) {
        SharedPreferences pref = getSharedPreferences(context);
        return pref.getBoolean("disable_scrolling_list", false)
                ? getMaxNumOfColumns(context)
                : Integer.valueOf(pref.getString("max_num_of_recents", "10"));
    }

    public static int getStatusBarHeight(Context context) {
        int statusBarHeight = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if(resourceId > 0)
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);

        return statusBarHeight;
    }

    public static void refreshPinnedIcons(Context context) {
        IconCache.getInstance(context).clearCache();

        PinnedBlockedApps pba = PinnedBlockedApps.getInstance(context);
        List<AppEntry> pinnedAppsList = new ArrayList<>(pba.getPinnedApps());
        List<AppEntry> blockedAppsList = new ArrayList<>(pba.getBlockedApps());
        PackageManager pm = context.getPackageManager();

        pba.clear(context);

        for(AppEntry entry : pinnedAppsList) {
            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

            final List<UserHandle> userHandles = userManager.getUserProfiles();
            LauncherActivityInfo appInfo = null;

            for(UserHandle handle : userHandles) {
                List<LauncherActivityInfo> list = launcherApps.getActivityList(entry.getPackageName(), handle);
                if(!list.isEmpty()) {
                    // Google App workaround
                    if(!entry.getPackageName().equals("com.google.android.googlequicksearchbox"))
                        appInfo = list.get(0);
                    else {
                        boolean added = false;
                        for(LauncherActivityInfo info : list) {
                            if(info.getName().equals("com.google.android.googlequicksearchbox.SearchActivity")) {
                                appInfo = info;
                                added = true;
                            }
                        }

                        if(!added) appInfo = list.get(0);
                    }

                    break;
                }
            }

            if(appInfo != null) {
                AppEntry newEntry = new AppEntry(
                        entry.getPackageName(),
                        entry.getComponentName(),
                        entry.getLabel(),
                        IconCache.getInstance(context).getIcon(context, pm, appInfo),
                        true);

                newEntry.setUserId(entry.getUserId(context));
                pba.addPinnedApp(context, newEntry);
            }
        }

        for(AppEntry entry : blockedAppsList) {
            pba.addBlockedApp(context, entry);
        }
    }

    public static Intent getShortcutIntent(Context context) {
        Intent shortcutIntent = new Intent(context, ShortcutActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        shortcutIntent.putExtra("is_launching_shortcut", true);

        BitmapDrawable drawable = (BitmapDrawable) context.getDrawable(R.mipmap.ic_freeform_mode);

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        if(drawable != null) intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, drawable.getBitmap());
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getString(R.string.pref_header_freeform));

        return intent;
    }

    public static Intent getStartStopIntent(Context context) {
        Intent shortcutIntent = new Intent(context, StartTaskbarActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        shortcutIntent.putExtra("is_launching_shortcut", true);

        BitmapDrawable drawable = (BitmapDrawable) context.getDrawable(R.mipmap.ic_launcher);

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        if(drawable != null) intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, drawable.getBitmap());
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getString(R.string.start_taskbar));

        return intent;
    }

    public static boolean hasFreeformSupport(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)
                || Settings.Global.getInt(context.getContentResolver(), "enable_freeform_support", -1) == 1
                || (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1
                && Settings.Global.getInt(context.getContentResolver(), "force_resizable_activities", -1) == 1));
    }

    public static boolean hasPartialFreeformSupport() {
         return Build.MANUFACTURER.equalsIgnoreCase("Samsung");
    }

    public static boolean isServiceRunning(Context context, Class<? extends Service> cls) {
        return isServiceRunning(context, cls.getName());
    }

    public static boolean isServiceRunning(Context context, String className) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(className.equals(service.service.getClassName()))
                return true;
        }

        return false;
    }

    public static int getBackgroundTint(Context context) {
        SharedPreferences pref = getSharedPreferences(context);

        // Import old background tint preference
        if(pref.contains("show_background")) {
            SharedPreferences.Editor editor = pref.edit();

            if(!pref.getBoolean("show_background", true))
                editor.putInt("background_tint", Color.TRANSPARENT).apply();

            editor.remove("show_background");
            editor.apply();
        }

        return pref.getInt("background_tint", context.getResources().getInteger(R.integer.translucent_gray));
    }

    public static int getAccentColor(Context context) {
        SharedPreferences pref = getSharedPreferences(context);
        return pref.getInt("accent_color", context.getResources().getInteger(R.integer.translucent_white));
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean canDrawOverlays(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    public static boolean isGame(Context context, String packageName) {
        SharedPreferences pref = getSharedPreferences(context);
        if(pref.getBoolean("launch_games_fullscreen", true)) {
            PackageManager pm = context.getPackageManager();

            try {
                ApplicationInfo info = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                return (info.flags & ApplicationInfo.FLAG_IS_GAME) != 0 || (info.metaData != null && info.metaData.getBoolean("isGame", false));
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        } else
            return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static ActivityOptions getActivityOptions(ApplicationType applicationType) {
        ActivityOptions options = ActivityOptions.makeBasic();
        Integer stackId = null;

        switch(applicationType) {
            case APPLICATION:
                if(!FreeformHackHelper.getInstance().isFreeformHackActive())
                    stackId = FULLSCREEN_WORKSPACE_STACK_ID;
                break;
            case GAME:
                stackId = FULLSCREEN_WORKSPACE_STACK_ID;
                break;
            case FREEFORM_HACK:
                stackId = FREEFORM_WORKSPACE_STACK_ID;
                break;
            case CONTEXT_MENU:
                if(isOPreview()) stackId = FULLSCREEN_WORKSPACE_STACK_ID;
        }

        if(stackId != null) {
            try {
                Method method = ActivityOptions.class.getMethod("setLaunchStackId", int.class);
                method.invoke(options, stackId);
            } catch (Exception e) { /* Gracefully fail */ }
        }

        return options;
    }

    private static ApplicationType getApplicationType(Context context, String packageName) {
        return isGame(context, packageName) ? ApplicationType.GAME : ApplicationType.APPLICATION;
    }

    public static boolean isSystemApp(Context context) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(BuildConfig.APPLICATION_ID, 0);
            int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
            return (info.flags & mask) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean isOPreview() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) || (Build.VERSION.RELEASE.equals("O") && Build.VERSION.PREVIEW_SDK_INT > 0);
    }

    public static boolean isChromeOs(Context context) {
        return context.getPackageManager().hasSystemFeature("org.chromium.arc");
    }

    public static boolean hasSupportLibrary(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(BuildConfig.SUPPORT_APPLICATION_ID, 0);
            return pm.checkSignatures(BuildConfig.SUPPORT_APPLICATION_ID, BuildConfig.APPLICATION_ID) == PackageManager.SIGNATURE_MATCH
                    && BuildConfig.APPLICATION_ID.equals(BuildConfig.BASE_APPLICATION_ID)
                    && isSystemApp(context);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static int getBaseTaskbarSize(Context context) {
        return Math.round(getBaseTaskbarSizeFloat(context));
    }

    private static float getBaseTaskbarSizeFloat(Context context) {
        SharedPreferences pref = getSharedPreferences(context);
        float baseTaskbarSize = context.getResources().getDimension(R.dimen.base_taskbar_size);
        boolean navbarButtonsEnabled = false;

        if(pref.getBoolean("dashboard", false))
            baseTaskbarSize += context.getResources().getDimension(R.dimen.dashboard_button_size);

        if(pref.getBoolean("button_back", false)) {
            navbarButtonsEnabled = true;
            baseTaskbarSize += context.getResources().getDimension(R.dimen.icon_size);
        }

        if(pref.getBoolean("button_home", false)) {
            navbarButtonsEnabled = true;
            baseTaskbarSize += context.getResources().getDimension(R.dimen.icon_size);
        }

        if(pref.getBoolean("button_recents", false)) {
            navbarButtonsEnabled = true;
            baseTaskbarSize += context.getResources().getDimension(R.dimen.icon_size);
        }

        if(navbarButtonsEnabled)
            baseTaskbarSize += context.getResources().getDimension(R.dimen.navbar_buttons_margin);

        return baseTaskbarSize;
    }

    private static void startTaskbarService(Context context, boolean fullRestart) {
        context.startService(new Intent(context, TaskbarService.class));
        context.startService(new Intent(context, StartMenuService.class));
        context.startService(new Intent(context, DashboardService.class));
        if(fullRestart) context.startService(new Intent(context, NotificationService.class));
    }

    private static void stopTaskbarService(Context context, boolean fullRestart) {
        context.stopService(new Intent(context, TaskbarService.class));
        context.stopService(new Intent(context, StartMenuService.class));
        context.stopService(new Intent(context, DashboardService.class));
        if(fullRestart) context.stopService(new Intent(context, NotificationService.class));
    }

    public static void restartTaskbar(Context context) {
        SharedPreferences pref = getSharedPreferences(context);
        if(pref.getBoolean("taskbar_active", false) && !pref.getBoolean("is_hidden", false)) {
            pref.edit()
                    .putBoolean("is_restarting", true)
                    .putBoolean("skip_auto_hide_navbar", true)
                    .apply();

            stopTaskbarService(context, true);
            startTaskbarService(context, true);
        } else if(isServiceRunning(context, StartMenuService.class)) {
            pref.edit().putBoolean("skip_auto_hide_navbar", true).apply();

            stopTaskbarService(context, false);
            startTaskbarService(context, false);
        }
    }

    public static void restartNotificationService(Context context) {
        if(isServiceRunning(context, NotificationService.class)) {
            SharedPreferences pref = getSharedPreferences(context);
            pref.edit().putBoolean("is_restarting", true).apply();

            Intent intent = new Intent(context, NotificationService.class);
            context.stopService(intent);
            context.startService(intent);
        }
    }

    public static void showHideNavigationBar(Context context, boolean show) {
        // Show or hide the system navigation bar on Bliss-x86
        try {
            Settings.System.putInt(context.getContentResolver(), "navigation_bar_show", show ? 1 : 0);
        } catch (Exception e) { /* Gracefully fail */ }
    }

    public static void initPrefs(Context context) {
        // On smaller-screened devices, set "Grid" as the default start menu layout
        SharedPreferences pref = getSharedPreferences(context);
        if(context.getApplicationContext().getResources().getConfiguration().smallestScreenWidthDp < 600
                && pref.getString("start_menu_layout", "null").equals("null")) {
            pref.edit().putString("start_menu_layout", "grid").apply();
        }

        // Enable freeform hack automatically on supported devices
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if(!pref.getBoolean("freeform_hack_override", false)) {
                pref.edit()
                        .putBoolean("freeform_hack", hasFreeformSupport(context) && !hasPartialFreeformSupport())
                        .putBoolean("save_window_sizes", false)
                        .putBoolean("freeform_hack_override", true)
                        .apply();
            } else if(!hasFreeformSupport(context)) {
                pref.edit().putBoolean("freeform_hack", false).apply();

                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.FINISH_FREEFORM_ACTIVITY"));
            }
        }

        // Customizations for Android-x86 devices (non-Bliss)
        if(BuildConfig.APPLICATION_ID.equals(BuildConfig.ANDROIDX86_APPLICATION_ID)
                && isSystemApp(context)
                && !pref.getBoolean("android_x86_prefs", false)) {
            pref.edit()
                    .putString("recents_amount", "running_apps_only")
                    .putString("refresh_frequency", "0")
                    .putString("max_num_of_recents", "2147483647")
                    .putBoolean("full_length", true)
                    .putBoolean("dashboard", true)
                    .putBoolean("android_x86_prefs", true)
                    .apply();
        }
    }

    public static DisplayMetrics getRealDisplayMetrics(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display disp = wm.getDefaultDisplay();

        if(isChromeOs(context))
            disp.getRealMetrics(metrics);
        else
            disp.getMetrics(metrics);

        return metrics;
    }
}
