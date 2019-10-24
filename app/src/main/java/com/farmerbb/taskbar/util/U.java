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

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.view.ContextThemeWrapper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.ContextMenuActivity;
import com.farmerbb.taskbar.activity.DummyActivity;
import com.farmerbb.taskbar.activity.InvisibleActivityFreeform;
import com.farmerbb.taskbar.activity.ShortcutActivity;
import com.farmerbb.taskbar.activity.StartTaskbarActivity;
import com.farmerbb.taskbar.activity.TouchAbsorberActivity;
import com.farmerbb.taskbar.activity.dark.ContextMenuActivityDark;
import com.farmerbb.taskbar.service.DashboardService;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.service.PowerMenuService;
import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.service.TaskbarService;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class U {

    private U() {}

    private static Integer cachedRotation;

    private static final int MAXIMIZED = 0;
    private static final int LEFT = -1;
    private static final int RIGHT = 1;

    public static final int HIDDEN = 0;
    public static final int TOP_APPS = 1;

    // From android.app.ActivityManager.StackId
    private static final int FULLSCREEN_WORKSPACE_STACK_ID = 1;
    private static final int FREEFORM_WORKSPACE_STACK_ID = 2;

    // From android.app.WindowConfiguration
    private static final int WINDOWING_MODE_FULLSCREEN = 1;
    private static final int WINDOWING_MODE_FREEFORM = 5;

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_PRIVATE);
    }

    public static void showPermissionDialog(Context context) {
        showPermissionDialog(context, null, null);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static AlertDialog showPermissionDialog(Context context, Runnable onError, Runnable onFinish) {
        Runnable finalOnFinish = onFinish == null
                ? () -> {}
                : onFinish;

        Runnable finalOnError = onError == null
                ? () -> showErrorDialog(context, "SYSTEM_ALERT_WINDOW", finalOnFinish)
                : onError;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_dialog_message)
                .setPositiveButton(R.string.action_grant_permission, (dialog, which) -> {
                    try {
                        context.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + BuildConfig.APPLICATION_ID)));

                        finalOnFinish.run();
                    } catch (ActivityNotFoundException e) {
                        finalOnError.run();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setCancelable(false);

        return dialog;
    }

    public static AlertDialog showErrorDialog(Context context, String appopCmd) {
        return showErrorDialog(context, appopCmd, null);
    }

    private static AlertDialog showErrorDialog(Context context, String appopCmd, Runnable onFinish) {
        Runnable finalOnFinish = onFinish == null
                ? () -> {}
                : onFinish;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.error_dialog_title)
                .setMessage(context.getString(R.string.error_dialog_message, BuildConfig.APPLICATION_ID, appopCmd))
                .setPositiveButton(R.string.action_ok, (dialog, which) -> finalOnFinish.run());

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setCancelable(false);

        return dialog;
    }

    public static void sendAccessibilityAction(Context context, int action) {
        sendAccessibilityAction(context, action, null);
    }

    public static void sendAccessibilityAction(Context context, int action, Runnable onComplete) {
        ComponentName component = new ComponentName(context, PowerMenuService.class);
        context.getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        boolean isAccessibilityServiceEnabled = isAccessibilityServiceEnabled(context);

        if(!isAccessibilityServiceEnabled
                && hasWriteSecureSettingsPermission(context)) {
            String services = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

            String finalServices = services == null ? "" : services;

            String powerMenuService = new ComponentName(context, PowerMenuService.class).flattenToString();

            if(!finalServices.contains(powerMenuService)) {
                try {
                    Settings.Secure.putString(context.getContentResolver(),
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                            finalServices.isEmpty()
                                    ? powerMenuService
                                    : finalServices + ":" + powerMenuService);
                } catch (Exception e) { /* Gracefully fail */ }
            }

            new Handler().postDelayed(() -> {
                Intent intent = new Intent("com.farmerbb.taskbar.ACCESSIBILITY_ACTION");
                intent.putExtra("action", action);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                try {
                    Settings.Secure.putString(context.getContentResolver(),
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                            finalServices);
                } catch (Exception e) { /* Gracefully fail */ }

                if(onComplete != null) onComplete.run();
            }, 100);
        } else if(isAccessibilityServiceEnabled) {
            Intent intent = new Intent("com.farmerbb.taskbar.ACCESSIBILITY_ACTION");
            intent.putExtra("action", action);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

            if(onComplete != null) onComplete.run();
        } else {
            launchApp(context, () -> {
                Intent intent = new Intent(context, DummyActivity.class);
                intent.putExtra("accessibility", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);

                try {
                    context.startActivity(intent, getActivityOptionsBundle(context, ApplicationType.APPLICATION, null));
                } catch (IllegalArgumentException | SecurityException e) { /* Gracefully fail */ }
            });
        }
    }

    public static boolean isAccessibilityServiceEnabled(Context context) {
        String accessibilityServices = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        ComponentName component = new ComponentName(context, PowerMenuService.class);

        return accessibilityServices != null
                && (accessibilityServices.contains(component.flattenToString())
                || accessibilityServices.contains(component.flattenToShortString()));
    }

    public static boolean hasWriteSecureSettingsPermission(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
    }

    public static void showToast(Context context, int message) {
        showToast(context, context.getString(message), Toast.LENGTH_SHORT);
    }

    public static void showToastLong(Context context, int message) {
        showToast(context, context.getString(message), Toast.LENGTH_LONG);
    }

    public static void showToast(Context context, String message, int length) {
        cancelToast();

        ToastInterface toast = DependencyUtils.createToast(context.getApplicationContext(), message, length);
        toast.show();

        ToastHelper.getInstance().setLastToast(toast);
    }

    public static void cancelToast() {
        ToastInterface toast = ToastHelper.getInstance().getLastToast();
        if(toast != null) toast.cancel();
    }

    public static void startShortcut(Context context, AppEntry entry, ShortcutInfo shortcut, View view) {
        launchApp(context,
                entry,
                null,
                false,
                false,
                shortcut,
                view);
    }

    public static void launchApp(final Context context,
                                 final AppEntry entry,
                                 final String windowSize,
                                 final boolean launchedFromTaskbar,
                                 final boolean openInNewWindow,
                                 final View view) {
        launchApp(context,
                entry,
                windowSize,
                launchedFromTaskbar,
                openInNewWindow,
                null,
                view);
    }

    private static void launchApp(final Context context,
                                  final AppEntry entry,
                                  final String windowSize,
                                  final boolean launchedFromTaskbar,
                                  final boolean openInNewWindow,
                                  final ShortcutInfo shortcut,
                                  final View view) {
        launchApp(context, launchedFromTaskbar, () -> continueLaunchingApp(context, entry,
                windowSize, openInNewWindow, shortcut, view));
    }

    public static void launchApp(Context context, Runnable runnable) {
        launchApp(context, true, runnable);
    }

    private static void launchApp(Context context, boolean launchedFromTaskbar, Runnable runnable) {
        SharedPreferences pref = getSharedPreferences(context);
        FreeformHackHelper helper = FreeformHackHelper.getInstance();

        boolean specialLaunch = hasBrokenSetLaunchBoundsApi()
                && helper.isInFreeformWorkspace()
                && MenuHelper.getInstance().isContextMenuOpen();

        boolean noAnimation = pref.getBoolean("disable_animations", false);

        if(hasFreeformSupport(context)
                && pref.getBoolean("freeform_hack", false)
                && (!helper.isInFreeformWorkspace() || specialLaunch)) {
            new Handler().postDelayed(() -> {
                startFreeformHack(context, true);

                new Handler().postDelayed(runnable, helper.isFreeformHackActive() ? 0 : 100);
            }, launchedFromTaskbar ? 0 : 100);
        } else
            new Handler().postDelayed(runnable, !launchedFromTaskbar && noAnimation ? 100 : 0);
    }

    public static void startFreeformHack(Context context) {
        startFreeformHack(context, false);
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static void startFreeformHack(Context context, boolean checkMultiWindow) {
        Intent freeformHackIntent = new Intent(context, InvisibleActivityFreeform.class);
        freeformHackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);

        if(checkMultiWindow)
            freeformHackIntent.putExtra("check_multiwindow", true);

        if(canDrawOverlays(context, false))
            startActivityLowerRight(context, freeformHackIntent);
    }

    public static void stopFreeformHack(Context context) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.FINISH_FREEFORM_ACTIVITY"));

        if(isOverridingFreeformHack(context)) {
            FreeformHackHelper helper = FreeformHackHelper.getInstance();
            helper.setFreeformHackActive(false);
            helper.setInFreeformWorkspace(false);
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static void continueLaunchingApp(Context context,
                                             AppEntry entry,
                                             String windowSize,
                                             boolean openInNewWindow,
                                             ShortcutInfo shortcut,
                                             View view) {
        SharedPreferences pref = getSharedPreferences(context);
        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(entry.getComponentName()));
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if(FreeformHackHelper.getInstance().isInFreeformWorkspace()
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1)
            intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);

        if(pref.getBoolean("disable_animations", false))
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        boolean realOpenInNewWindow = openInNewWindow || pref.getBoolean("force_new_window", false);
        if(realOpenInNewWindow) {
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

        ApplicationType type = getApplicationType(context, entry.getPackageName());

        if(windowSize == null)
            windowSize = SavedWindowSizes.getInstance(context).getWindowSize(context, entry.getPackageName());

        Bundle bundle = getActivityOptionsBundle(context, type, windowSize, view);

        prepareToStartActivity(context, realOpenInNewWindow, () -> {
            if(shortcut == null) {
                UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
                if(entry.getUserId(context) == userManager.getSerialNumberForUser(Process.myUserHandle())) {
                    try {
                        context.startActivity(intent, bundle);
                    } catch (ActivityNotFoundException e) {
                        launchAndroidForWork(context, intent.getComponent(), bundle, entry.getUserId(context));
                    } catch (IllegalArgumentException | SecurityException e) { /* Gracefully fail */ }
                } else
                    launchAndroidForWork(context, intent.getComponent(), bundle, entry.getUserId(context));
            } else
                launchShortcut(context, shortcut, bundle);
        });

        if(shouldCollapse(context, true))
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_TASKBAR"));
        else
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Bundle launchMode1(Context context, ApplicationType type, View view) {
        DisplayInfo display = getDisplayInfo(context);

        int width1 = display.width / 8;
        int width2 = display.width - width1;
        int height1 = display.height / 8;
        int height2 = display.height - height1;

        return getActivityOptions(context, type, view).setLaunchBounds(new Rect(
                width1,
                height1,
                width2,
                height2
        )).toBundle();
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Bundle launchMode2(Context context, int launchType, ApplicationType type, View view) {
        DisplayInfo display = getDisplayInfo(context);

        int statusBarHeight = getStatusBarHeight(context);
        String position = getTaskbarPosition(context);

        boolean isPortrait = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        boolean isLandscape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        int left = 0;
        int top = statusBarHeight;
        int right = display.width;
        int bottom = display.height;

        int iconSize = isOverridingFreeformHack(context) && !LauncherHelper.getInstance().isOnHomeScreen()
                ? 0
                : context.getResources().getDimensionPixelSize(R.dimen.icon_size);

        if(position.contains("vertical_left"))
            left = left + iconSize;
        else if(position.contains("vertical_right"))
            right = right - iconSize;
        else if(position.contains("bottom"))
            bottom = bottom - iconSize;
        else
            top = top + iconSize;

        int halfLandscape = (right / 2) + ((iconSize / 2) * (position.contains("vertical_left") ? 1 : 0));
        int halfPortrait = (bottom / 2) + ((iconSize / 2) * ((position.equals("top_left") || position.equals("top_right")) ? 1 : 0));

        if(launchType == RIGHT && isLandscape)
            left = halfLandscape;
        else if(launchType == RIGHT && isPortrait)
            top = halfPortrait;
        else if(launchType == LEFT && isLandscape)
            right = halfLandscape;
        else if(launchType == LEFT && isPortrait)
            bottom = halfPortrait;

        return getActivityOptions(context, type, view)
                .setLaunchBounds(new Rect(left, top, right, bottom)).toBundle();
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Bundle launchMode3(Context context, ApplicationType type, View view) {
        DisplayInfo display = getDisplayInfo(context);

        int width1 = display.width / 2;
        int width2 = context.getResources().getDimensionPixelSize(R.dimen.phone_size_width) / 2;
        int height1 = display.height / 2;
        int height2 = context.getResources().getDimensionPixelSize(R.dimen.phone_size_height) / 2;

        return getActivityOptions(context, type, view).setLaunchBounds(new Rect(
                width1 - width2,
                height1 - height2,
                width1 + width2,
                height1 + height2
        )).toBundle();
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

    private static void prepareToStartActivity(Context context, boolean openInNewWindow, Runnable runnable) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_CONTEXT_MENU"));

        if(!FreeformHackHelper.getInstance().isTouchAbsorberActive()
                && shouldLaunchTouchAbsorber(context)) {
            startTouchAbsorberActivity(context);
            new Handler().postDelayed(runnable, 100);
        } else if(openInNewWindow) {
            Intent intent = new Intent(context, DummyActivity.class);
            intent.putExtra("finish_on_pause", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityLowerRight(context, intent);

            new Handler().postDelayed(runnable, 100);
        } else
            runnable.run();
    }

    public static void startActivityMaximized(Context context, Intent intent) {
        Bundle bundle = launchMode2(context, MAXIMIZED, ApplicationType.CONTEXT_MENU, null);
        prepareToStartActivity(context, false, () -> context.startActivity(intent, bundle));
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static void startActivityLowerRight(Context context, Intent intent) {
        DisplayInfo display = getDisplayInfo(context);
        try {
            context.startActivity(intent,
                    getActivityOptions(context, ApplicationType.FREEFORM_HACK, null)
                            .setLaunchBounds(new Rect(
                                    display.width,
                                    display.height,
                                    display.width + 1,
                                    display.height + 1
                            )).toBundle());
        } catch (IllegalArgumentException | SecurityException e) { /* Gracefully fail */ }
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static void startTouchAbsorberActivity(Context context) {
        String position = getTaskbarPosition(context);
        DisplayInfo display = getDisplayInfo(context);

        int left = 0;
        int top = 0;
        int right = display.width;
        int bottom = display.height;

        int iconSize = context.getResources().getDimensionPixelSize(R.dimen.icon_size);

        if(position.contains("vertical_left"))
            right = iconSize;
        else if(position.contains("vertical_right"))
            left = right - iconSize;
        else if(position.contains("bottom"))
            top = bottom - iconSize;
        else
            bottom = iconSize;

        Intent intent = new Intent(context, TouchAbsorberActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        try {
            context.startActivity(intent,
                    getActivityOptions(context, ApplicationType.FREEFORM_HACK, null)
                            .setLaunchBounds(new Rect(left, top, right, bottom)).toBundle());
        } catch (IllegalArgumentException | SecurityException e) { /* Gracefully fail */ }
    }

    public static void startContextMenuActivity(Context context, Bundle args) {
        SharedPreferences pref = getSharedPreferences(context);
        Intent intent = null;

        switch(pref.getString("theme", "light")) {
            case "light":
                intent = new Intent(context, ContextMenuActivity.class);
                break;
            case "dark":
                intent = new Intent(context, ContextMenuActivityDark.class);
                break;
        }

        if(intent != null) {
            intent.putExtra("args", args);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        if(hasFreeformSupport(context) && FreeformHackHelper.getInstance().isInFreeformWorkspace()) {
            DisplayInfo display = getDisplayInfo(context);

            if(intent != null && hasBrokenSetLaunchBoundsApi())
                intent.putExtra("context_menu_fix", true);

            context.startActivity(intent,
                    getActivityOptions(context, ApplicationType.CONTEXT_MENU, null)
                            .setLaunchBounds(
                                    new Rect(0, 0, display.width, display.height)
                            ).toBundle());
        } else
            context.startActivity(intent);
    }

    public static void checkForUpdates(Context context) {
        String url;
        if(isPlayStoreRelease(context)) {
            if(BuildConfig.APPLICATION_ID.equals(BuildConfig.BASE_APPLICATION_ID)
                    && !isPlayStoreInstalled(context))
                url = "https://github.com/farmerbb/Taskbar/releases";
            else
                url = "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID;
        } else
            url = "https://f-droid.org/repository/browse/?fdid=" + BuildConfig.APPLICATION_ID;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
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
        DisplayInfo display = getDisplayInfo(context);
        float density = display.density / 160;
        float baseTaskbarSize = getBaseTaskbarSizeFloat(context) / density;
        int numOfColumns = 0;

        float maxScreenSize = getTaskbarPosition(context).contains("vertical")
                ? (display.height - getStatusBarHeight(context)) / density
                : display.width / density;

        float iconSize = context.getResources().getDimension(R.dimen.icon_size) / density;

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
        return getSystemDimen(context, "status_bar_height");
    }

    private static int getNavbarHeight(Context context) {
        return getSystemDimen(context, "navigation_bar_height");
    }

    private static int getSystemDimen(Context context, String id) {
        int value = 0;
        int resourceId = context.getResources().getIdentifier(id, "dimen", "android");
        if(resourceId > 0)
            value = context.getResources().getDimensionPixelSize(resourceId);

        return value;
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

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_freeform_mode));
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getString(R.string.pref_header_freeform));

        return intent;
    }

    public static Intent getStartStopIntent(Context context) {
        Intent shortcutIntent = new Intent(context, StartTaskbarActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        shortcutIntent.putExtra("is_launching_shortcut", true);

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_launcher));
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getString(R.string.start_taskbar));

        return intent;
    }

    public static boolean canEnableFreeform() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static boolean hasFreeformSupport(Context context) {
        return canEnableFreeform()
                && (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)
                || Settings.Global.getInt(context.getContentResolver(), "enable_freeform_support", 0) != 0
                || (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1
                && Settings.Global.getInt(context.getContentResolver(), "force_resizable_activities", 0) != 0));
    }

    public static boolean canBootToFreeform(Context context) {
        SharedPreferences pref = getSharedPreferences(context);
        return hasFreeformSupport(context)
                && pref.getBoolean("freeform_hack", false)
                && !isOverridingFreeformHack(context);
    }

    public static boolean isSamsungDevice() {
        return Build.MANUFACTURER.equalsIgnoreCase("Samsung");
    }

    private static boolean isNvidiaDevice() {
        return Build.MANUFACTURER.equalsIgnoreCase("NVIDIA");
    }

    public static boolean isServiceRunning(Context context, Class<? extends Service> cls) {
        if(LauncherHelper.getInstance().isOnHomeScreen(false, true)
                && (cls.equals(TaskbarService.class)
                || cls.equals(StartMenuService.class)
                || cls.equals(DashboardService.class)))
            return true;

        return isServiceRunning(context, cls.getName());
    }

    private static boolean isServiceRunning(Context context, String className) {
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

    public static boolean canDrawOverlays(Context context) {
        return canDrawOverlays(context, false);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean canDrawOverlays(Context context, boolean isSecondaryHomeScreen) {
        return isSecondaryHomeScreen
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || Settings.canDrawOverlays(context);
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

    private static ActivityOptions getActivityOptions(View view) {
        return getActivityOptions(null, null, view);
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static ActivityOptions getActivityOptions(Context context, ApplicationType applicationType, View view) {
        ActivityOptions options;
        if(view != null)
            options = ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight());
        else
            options = ActivityOptions.makeBasic();

        if(applicationType == null)
            return options;

        int stackId = -1;

        switch(applicationType) {
            case APPLICATION:
                if(FreeformHackHelper.getInstance().isFreeformHackActive())
                    stackId = getFreeformWindowModeId();
                else
                    stackId = getFullscreenWindowModeId();
                break;
            case GAME:
                stackId = getFullscreenWindowModeId();
                break;
            case FREEFORM_HACK:
                stackId = getFreeformWindowModeId();
                break;
            case CONTEXT_MENU:
                if(hasBrokenSetLaunchBoundsApi()
                        || (!isChromeOs(context) && getCurrentApiVersion() >= 28.0f))
                    stackId = getFullscreenWindowModeId();
                break;
        }

        try {
            Method method = ActivityOptions.class.getMethod(getWindowingModeMethodName(), int.class);
            method.invoke(options, stackId);
        } catch (Exception e) { /* Gracefully fail */ }

        return options;
    }

    private static int getFullscreenWindowModeId() {
        if(getCurrentApiVersion() >= 28.0f)
            return WINDOWING_MODE_FULLSCREEN;
        else
            return FULLSCREEN_WORKSPACE_STACK_ID;
    }

    private static int getFreeformWindowModeId() {
        if(getCurrentApiVersion() >= 28.0f)
            return WINDOWING_MODE_FREEFORM;
        else
            return FREEFORM_WORKSPACE_STACK_ID;
    }

    private static String getWindowingModeMethodName() {
        if(getCurrentApiVersion() >= 28.0f)
            return "setLaunchWindowingMode";
        else
            return "setLaunchStackId";
    }

    public static Bundle getActivityOptionsBundle(Context context, ApplicationType type, View view) {
        SharedPreferences pref = getSharedPreferences(context);

        return getActivityOptionsBundle(context, type, pref.getString("window_size", "standard"), view);
    }

    private static Bundle getActivityOptionsBundle(Context context, ApplicationType type, String windowSize, View view) {
        SharedPreferences pref = getSharedPreferences(context);
        if(!canEnableFreeform() || !pref.getBoolean("freeform_hack", false))
            return getActivityOptions(view).toBundle();

        switch(windowSize) {
            case "large":
                return launchMode1(context, type, view);
            case "fullscreen":
                return launchMode2(context, MAXIMIZED, type, view);
            case "half_left":
                return launchMode2(context, LEFT, type, view);
            case "half_right":
                return launchMode2(context, RIGHT, type, view);
            case "phone_size":
                return launchMode3(context, type, view);
        }

        return getActivityOptions(context, type, view).toBundle();
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

    public static boolean isChromeOs(Context context) {
        return context.getPackageManager().hasSystemFeature("org.chromium.arc");
    }

    public static boolean isBlissOs(Context context) {
        boolean validBlissOsBuildProp = false;

        String blissVersion = getSystemProperty("ro.bliss.version");
        if(blissVersion != null && !blissVersion.isEmpty())
            validBlissOsBuildProp = true;

        String buildUser = getSystemProperty("ro.build.user");
        if(buildUser != null && buildUser.equals("electrikjesus"))
            validBlissOsBuildProp = true;

        return validBlissOsBuildProp
                && BuildConfig.APPLICATION_ID.equals(BuildConfig.BASE_APPLICATION_ID)
                && isSystemApp(context);
    }

    public static boolean isLauncherPermanentlyEnabled(Context context) {
        if(BuildConfig.APPLICATION_ID.equals(BuildConfig.ANDROIDX86_APPLICATION_ID))
            return true;

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

        if(isSystemTrayEnabled(context))
            baseTaskbarSize += context.getResources().getDimension(R.dimen.systray_size);

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

        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.RESTART"));
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
            if(getCurrentApiVersion() >= 28.0f)
                Settings.Secure.putInt(context.getContentResolver(), "navigation_bar_visible", show ? 1 : 0);
            else
                Settings.System.putInt(context.getContentResolver(), "navigation_bar_show", show ? 1 : 0);
        } catch (Exception e) { /* Gracefully fail */ }
    }

    public static void initPrefs(Context context) {
        // On smaller-screened devices, set "Grid" as the default start menu layout
        SharedPreferences pref = getSharedPreferences(context);
        if(context.getApplicationContext().getResources().getConfiguration().smallestScreenWidthDp < 720
                && pref.getString("start_menu_layout", "null").equals("null")) {
            pref.edit().putString("start_menu_layout", "grid").apply();
        }

        // Enable freeform hack automatically on supported devices
        if(canEnableFreeform()) {
            if(!pref.getBoolean("freeform_hack_override", false)) {
                pref.edit()
                        .putBoolean("freeform_hack", hasFreeformSupport(context) && !isSamsungDevice())
                        .putBoolean("save_window_sizes", false)
                        .putBoolean("freeform_hack_override", true)
                        .apply();
            } else if(!hasFreeformSupport(context)) {
                pref.edit().putBoolean("freeform_hack", false).apply();

                stopFreeformHack(context);
            }
        } else {
            boolean freeformWasEnabled = pref.getBoolean("freeform_hack", false)
                    || pref.getBoolean("show_freeform_disabled_message", false);

            pref.edit()
                    .putBoolean("freeform_hack", false)
                    .putBoolean("show_freeform_disabled_message", freeformWasEnabled)
                    .apply();

            SavedWindowSizes.getInstance(context).clear(context);
            stopFreeformHack(context);
        }

        // Customizations for BlissOS
        if(isBlissOs(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !pref.getBoolean("bliss_os_prefs", false)) {
            SharedPreferences.Editor editor = pref.edit();

            if(hasFreeformSupport(context)) {
                editor.putBoolean("freeform_hack", true);
            }

            editor.putString("recents_amount", "running_apps_only");
            editor.putString("refresh_frequency", "0");
            editor.putString("max_num_of_recents", "2147483647");
            editor.putString("sort_order", "true");
            editor.putString("window_size", "phone_size");
            editor.putString("start_button_image", "app_logo");
            editor.putBoolean("full_length", true);
            editor.putBoolean("sys_tray", true);
            editor.putBoolean("dashboard", true);
            editor.putBoolean("button_back", true);
            editor.putBoolean("button_home", true);
            editor.putBoolean("button_recents", true);
            editor.putBoolean("auto_hide_navbar", true);
         // editor.putBoolean("shortcut_icon", false);
            editor.putBoolean("bliss_os_prefs", true);
            editor.apply();
        }

        // Customizations for Android-x86 devices (non-Bliss)
        if(BuildConfig.APPLICATION_ID.equals(BuildConfig.ANDROIDX86_APPLICATION_ID)
                && isSystemApp(context)
                && !pref.getBoolean("android_x86_prefs", false)) {
            pref.edit()
                    .putString("recents_amount", "running_apps_only")
                    .putString("refresh_frequency", "0")
                    .putString("max_num_of_recents", "2147483647")
                    .putString("sort_order", "true")
                    .putString("window_size", "phone_size")
                    .putBoolean("full_length", true)
                    .putBoolean("sys_tray", true)
                    .putBoolean("dashboard", true)
                    .putBoolean("shortcut_icon", false)
                    .putBoolean("android_x86_prefs", true)
                    .apply();
        }
    }

    public static DisplayInfo getDisplayInfo(Context context) {
        return getDisplayInfo(context, false);
    }

    public static DisplayInfo getDisplayInfo(Context context, boolean fromTaskbar) {
        context = context.getApplicationContext();

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display disp = wm.getDefaultDisplay();

        DisplayMetrics metrics = new DisplayMetrics();
        disp.getMetrics(metrics);

        DisplayMetrics realMetrics = new DisplayMetrics();
        disp.getRealMetrics(realMetrics);

        DisplayInfo display = new DisplayInfo(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);

        if(isChromeOs(context)) {
            SharedPreferences pref = getSharedPreferences(context);
            if(!pref.getBoolean("chrome_os_context_menu_fix", true)) {
                display.width = realMetrics.widthPixels;
                display.height = realMetrics.heightPixels;
            }

            return display;
        }

        // Workaround for incorrect display size on devices with notches in landscape mode
        if(fromTaskbar && context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            return display;

        boolean sameWidth = metrics.widthPixels == realMetrics.widthPixels;
        boolean sameHeight = metrics.heightPixels == realMetrics.heightPixels;

        if(sameWidth && !sameHeight) {
            display.width = realMetrics.widthPixels;
            display.height = realMetrics.heightPixels - getNavbarHeight(context);
        }

        if(!sameWidth && sameHeight) {
            display.width = realMetrics.widthPixels - getNavbarHeight(context);
            display.height = realMetrics.heightPixels;
        }

        return display;
    }

    public static void pinAppShortcut(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager mShortcutManager = context.getSystemService(ShortcutManager.class);

            if(mShortcutManager.isRequestPinShortcutSupported()) {
                ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(context, "freeform_mode").build();

                mShortcutManager.requestPinShortcut(pinShortcutInfo, null);
            } else
                showToastLong(context, R.string.pin_shortcut_not_supported);
        } else {
            Intent intent = getShortcutIntent(context);
            intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            intent.putExtra("duplicate", false);

            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo defaultLauncher = context.getPackageManager().resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);

            intent.setPackage(defaultLauncher.activityInfo.packageName);
            context.sendBroadcast(intent);

            showToast(context, R.string.shortcut_created);
        }
    }

    public static boolean shouldCollapse(Context context, boolean pendingAppLaunch) {
        SharedPreferences pref = getSharedPreferences(context);
        if(pref.getBoolean("hide_taskbar", true)) {
            if(isOverridingFreeformHack(context))
                return !LauncherHelper.getInstance().isOnHomeScreen();
            else {
                FreeformHackHelper helper = FreeformHackHelper.getInstance();
                if(pendingAppLaunch)
                    return !helper.isFreeformHackActive();
                else
                    return !helper.isInFreeformWorkspace();
            }
        } else
            return false;
    }

    public static boolean isOverridingFreeformHack(Context context) {
        SharedPreferences pref = getSharedPreferences(context);
        return pref.getBoolean("freeform_hack", false)
                && ((isChromeOs(context) && pref.getBoolean("chrome_os_context_menu_fix", true))
                || (!isChromeOs(context) && getCurrentApiVersion() >= 28.0f));
    }

    public static boolean isPlayStoreInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.android.vending", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static float getCurrentApiVersion() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return Float.valueOf(Build.VERSION.SDK_INT + "." + Build.VERSION.PREVIEW_SDK_INT);
        else
            return (float) Build.VERSION.SDK_INT;
    }

    public static boolean hasBrokenSetLaunchBoundsApi() {
        return getCurrentApiVersion() >= 26.0f
                && getCurrentApiVersion() < 28.0f
                && !isSamsungDevice()
                && !isNvidiaDevice();
    }

    public static String getSecondScreenPackageName(Context context) {
        return getInstalledPackage(context, Arrays.asList(
                "com.farmerbb.secondscreen.free",
                "com.farmerbb.secondscreen"));
    }

    // Returns the name of an installed package from a list of package names, in order of preference
    private static String getInstalledPackage(Context context, List<String> packageNames) {
        if(packageNames == null || packageNames.isEmpty())
            return null;

        List<String> packages = packageNames instanceof ArrayList ? packageNames : new ArrayList<>(packageNames);
        String packageName = packages.get(0);

        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return packageName;
        } catch (PackageManager.NameNotFoundException e) {
            packages.remove(0);
            return getInstalledPackage(context, packages);
        }
    }

    public static boolean visualFeedbackEnabled(Context context) {
        SharedPreferences pref = getSharedPreferences(context);
        return (getCurrentApiVersion() < 26.0f || getCurrentApiVersion() >= 28.0f)
                && pref.getBoolean("visual_feedback", true)
                && !isNvidiaDevice();
    }

    public static void showRecentAppsDialog(Context context) {
        showRecentAppsDialog(context, null, null);
    }

    public static AlertDialog showRecentAppsDialog(Context context, Runnable onError, Runnable onFinish) {
        Runnable finalOnFinish = onFinish == null
                ? () -> {}
                : onFinish;

        Runnable finalOnError = onError == null
                ? () -> showErrorDialog(context, "GET_USAGE_STATS", finalOnFinish)
                : onError;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isSystemApp(context)) {
            ApplicationInfo applicationInfo = null;
            try {
                applicationInfo = context.getPackageManager().getApplicationInfo(BuildConfig.APPLICATION_ID, 0);
            } catch (PackageManager.NameNotFoundException e) { /* Gracefully fail */ }

            if(applicationInfo != null) {
                AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);

                if(mode != AppOpsManager.MODE_ALLOWED) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.pref_header_recent_apps)
                            .setMessage(R.string.enable_recent_apps)
                            .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                                try {
                                    context.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                                    showToastLong(context, R.string.usage_stats_message);

                                    finalOnFinish.run();
                                } catch (ActivityNotFoundException e) {
                                    finalOnError.run();
                                }
                            })
                            .setNegativeButton(R.string.action_cancel, (dialog, which) -> finalOnFinish.run());

                    AlertDialog dialog = builder.create();
                    dialog.show();
                    dialog.setCancelable(false);

                    return dialog;
                }
            }
        }

        finalOnFinish.run();
        return null;
    }

    public static Context wrapContext(Context context) {
        SharedPreferences pref = getSharedPreferences(context);

        int theme = -1;
        switch(pref.getString("theme", "light")) {
            case "light":
                theme = R.style.AppTheme;
                break;
            case "dark":
                theme = R.style.AppTheme_Dark;
                break;
        }

        return theme > -1 ? new ContextThemeWrapper(context, theme) : context;
    }

    public static boolean isPlayStoreRelease(Context context) {
        return isPlayStoreRelease(context, BuildConfig.APPLICATION_ID);
    }

    @SuppressLint("PackageManagerGetSignatures")
    public static boolean isPlayStoreRelease(Context context, String packageName) {
        Signature playStoreSignature = new Signature(context.getString(R.string.signature));
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            for(Signature signature : info.signatures) {
                if(signature.equals(playStoreSignature))
                    return true;
            }
        } catch (Exception e) { /* Gracefully fail */ }

        return false;
    }

    public static boolean isExternalAccessDisabled(Context context) {
        SharedPreferences pref = getSharedPreferences(context);
        return !pref.getBoolean("tasker_enabled", true);
    }

    public static boolean enableFreeformModeShortcut(Context context) {
        return canEnableFreeform()
                && !isOverridingFreeformHack(context)
                && !isChromeOs(context);
    }

    public static void startForegroundService(Context context, Intent intent) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(Settings.canDrawOverlays(context))
                context.startForegroundService(intent);
        } else
            context.startService(intent);
    }

    public static int getOverlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    public static boolean isDelegatingHomeActivity(Context context) {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);

        final List<ResolveInfo> listOfLaunchers = context.getPackageManager().queryIntentActivities(homeIntent, 0);
        for(ResolveInfo launcher : listOfLaunchers) {
            if(launcher.activityInfo.packageName.equals(BuildConfig.SUPPORT_APPLICATION_ID))
                return true;
        }

        return false;
    }

    @SuppressLint("PrivateApi")
    private static String getSystemProperty(String key) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return cls.getMethod("get", String.class).invoke(null, key).toString();
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean importCustomStartButtonImage(Context context, Uri uri) {
        try {
            File imagesDir = new File(context.getFilesDir(), "images");
            imagesDir.mkdirs();

            File importedFile = new File(imagesDir, "custom_image_new");
            if(importedFile.exists()) importedFile.delete();

            BufferedInputStream is = new BufferedInputStream(context.getContentResolver().openInputStream(uri));
            byte[] data = new byte[is.available()];

            if(data.length > 0) {
                BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(importedFile));
                is.read(data);
                os.write(data);
                is.close();
                os.close();
            }

            File prevFile = new File(imagesDir, "custom_image");
            if(prevFile.exists()) prevFile.delete();

            importedFile.renameTo(prevFile);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static String getDefaultStartButtonImage(Context context) {
        SharedPreferences pref = getSharedPreferences(context);
        return pref.getBoolean("app_drawer_icon", false)
                ? "app_logo"
                : "default";
    }

    private static boolean shouldLaunchTouchAbsorber(Context context) {
        return isOverridingFreeformHack(context) && !isChromeOs(context) && getCurrentApiVersion() < 29.0f;
    }

    public static boolean isDesktopIconsEnabled(Context context) {
        return !canBootToFreeform(context) && !shouldLaunchTouchAbsorber(context);
    }

    public static boolean isSystemTrayEnabled(Context context) {
        SharedPreferences pref = getSharedPreferences(context);

        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && pref.getBoolean("sys_tray", false)
                && pref.getBoolean("full_length", false)
                && !getTaskbarPosition(context).contains("vertical");
    }
}
