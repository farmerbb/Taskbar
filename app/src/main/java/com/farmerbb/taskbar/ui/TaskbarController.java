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

package com.farmerbb.taskbar.ui;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.speech.RecognizerIntent;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.support.v4.content.LocalBroadcastManager;
import android.widget.LinearLayout;
import android.widget.Space;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.activity.HomeActivityDelegate;
import com.farmerbb.taskbar.activity.MainActivity;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.HomeActivity;
import com.farmerbb.taskbar.activity.InvisibleActivityFreeform;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.DisplayInfo;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.LauncherHelper;
import com.farmerbb.taskbar.util.PinnedBlockedApps;
import com.farmerbb.taskbar.util.MenuHelper;
import com.farmerbb.taskbar.util.U;

public class TaskbarController implements UIController {

    private Context context;
    private LinearLayout layout;
    private ImageView startButton;
    private LinearLayout taskbar;
    private FrameLayout scrollView;
    private Button button;
    private Space space;
    private FrameLayout dashboardButton;
    private LinearLayout navbarButtons;

    private Handler handler;
    private Handler handler2;
    private Thread thread;
    private Thread thread2;

    private boolean isShowingRecents = true;
    private boolean shouldRefreshRecents = true;
    private boolean taskbarShownTemporarily = false;
    private boolean taskbarHiddenTemporarily = false;
    private boolean isRefreshingRecents = false;
    private boolean isFirstStart = true;

    private boolean startThread2 = false;
    private boolean stopThread2 = false;

    private int refreshInterval = -1;
    private long searchInterval = -1;
    private String sortOrder = "false";
    private boolean runningAppsOnly = false;

    private int layoutId = R.layout.taskbar_left;
    private int currentTaskbarPosition = 0;
    private boolean showHideAutomagically = false;
    private boolean positionIsVertical = false;
    private boolean dashboardEnabled = false;
    private boolean navbarButtonsEnabled = false;

    private List<String> currentTaskbarIds = new ArrayList<>();
    private int numOfPinnedApps = -1;

    private View.OnClickListener ocl = view -> {
        Intent intent = new Intent("com.farmerbb.taskbar.TOGGLE_START_MENU");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    };

    private BroadcastReceiver showReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showTaskbar(true);
        }
    };

    private BroadcastReceiver hideReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideTaskbar(true);
        }
    };

    private BroadcastReceiver tempShowReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tempShowTaskbar();
        }
    };

    private BroadcastReceiver tempHideReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tempHideTaskbar(false);
        }
    };

    private BroadcastReceiver startMenuAppearReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(startButton.getVisibility() == View.GONE
                    && (!LauncherHelper.getInstance().isOnHomeScreen() || FreeformHackHelper.getInstance().isInFreeformWorkspace()))
                layout.setVisibility(View.GONE);
        }
    };

    private BroadcastReceiver startMenuDisappearReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(startButton.getVisibility() == View.GONE)
                layout.setVisibility(View.VISIBLE);
        }
    };

    public TaskbarController(Context context) {
        this.context = context;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreateHost(UIHost host) {
        SharedPreferences pref = U.getSharedPreferences(context);
        if(pref.getBoolean("taskbar_active", false) || LauncherHelper.getInstance().isOnHomeScreen()) {
            if(U.canDrawOverlays(context, host instanceof HomeActivityDelegate))
                drawTaskbar(host);
            else {
                pref.edit().putBoolean("taskbar_active", false).apply();

                host.terminate();
            }
        } else host.terminate();
    }

    @SuppressLint("RtlHardcoded")
    private void drawTaskbar(UIHost host) {
        IconCache.getInstance(context).clearCache();

        // Initialize layout params
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        U.setCachedRotation(windowManager.getDefaultDisplay().getRotation());

        final ViewParams params = new ViewParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                -1,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        );

        // Determine where to show the taskbar on screen
        switch(U.getTaskbarPosition(context)) {
            case "bottom_left":
                layoutId = R.layout.taskbar_left;
                params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                positionIsVertical = false;
                break;
            case "bottom_vertical_left":
                layoutId = R.layout.taskbar_vertical;
                params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                positionIsVertical = true;
                break;
            case "bottom_right":
                layoutId = R.layout.taskbar_right;
                params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                positionIsVertical = false;
                break;
            case "bottom_vertical_right":
                layoutId = R.layout.taskbar_vertical;
                params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                positionIsVertical = true;
                break;
            case "top_left":
                layoutId = R.layout.taskbar_left;
                params.gravity = Gravity.TOP | Gravity.LEFT;
                positionIsVertical = false;
                break;
            case "top_vertical_left":
                layoutId = R.layout.taskbar_top_vertical;
                params.gravity = Gravity.TOP | Gravity.LEFT;
                positionIsVertical = true;
                break;
            case "top_right":
                layoutId = R.layout.taskbar_right;
                params.gravity = Gravity.TOP | Gravity.RIGHT;
                positionIsVertical = false;
                break;
            case "top_vertical_right":
                layoutId = R.layout.taskbar_top_vertical;
                params.gravity = Gravity.TOP | Gravity.RIGHT;
                positionIsVertical = true;
                break;
        }

        // Initialize views
        SharedPreferences pref = U.getSharedPreferences(context);
        boolean altButtonConfig = pref.getBoolean("alt_button_config", false);

        layout = (LinearLayout) LayoutInflater.from(U.wrapContext(context)).inflate(layoutId, null);
        taskbar = layout.findViewById(R.id.taskbar);
        scrollView = layout.findViewById(R.id.taskbar_scrollview);

        int backgroundTint = U.getBackgroundTint(context);
        int accentColor = U.getAccentColor(context);

        if(altButtonConfig) {
            space = layout.findViewById(R.id.space_alt);
            layout.findViewById(R.id.space).setVisibility(View.GONE);
        } else {
            space = layout.findViewById(R.id.space);
            layout.findViewById(R.id.space_alt).setVisibility(View.GONE);
        }

        space.setOnClickListener(v -> toggleTaskbar(true));

        startButton = layout.findViewById(R.id.start_button);
        int padding = 0;

        switch(pref.getString("start_button_image", U.getDefaultStartButtonImage(context))) {
            case "default":
                startButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.all_apps_button_icon));
                padding = context.getResources().getDimensionPixelSize(R.dimen.app_drawer_icon_padding);
                break;
            case "app_logo":
                Drawable drawable;

                if(U.isBlissOs(context)) {
                    drawable = ContextCompat.getDrawable(context, R.drawable.bliss);
                    drawable.setTint(accentColor);
                } else
                    drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher);

                startButton.setImageDrawable(drawable);
                padding = context.getResources().getDimensionPixelSize(R.dimen.app_drawer_icon_padding_alt);
                break;
            case "custom":
                File file = new File(context.getFilesDir(), "custom_image");
                if(file.exists()) {
                    try {
                        startButton.setImageURI(Uri.fromFile(file));
                    } catch (Exception e) {
                        U.showToastLong(context, R.string.error_reading_custom_start_image);
                        startButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.all_apps_button_icon));
                    }
                } else
                    startButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.all_apps_button_icon));

                padding = context.getResources().getDimensionPixelSize(R.dimen.app_drawer_icon_padding);
                break;
        }

        startButton.setPadding(padding, padding, padding, padding);
        startButton.setOnClickListener(ocl);
        startButton.setOnLongClickListener(view -> {
            openContextMenu();
            return true;
        });

        startButton.setOnGenericMotionListener((view, motionEvent) -> {
            if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                    && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY)
                openContextMenu();

            return false;
        });

        refreshInterval = (int) (Float.parseFloat(pref.getString("refresh_frequency", "2")) * 1000);
        if(refreshInterval == 0)
            refreshInterval = 100;

        sortOrder = pref.getString("sort_order", "false");
        runningAppsOnly = pref.getString("recents_amount", "past_day").equals("running_apps_only");

        switch(pref.getString("recents_amount", "past_day")) {
            case "past_day":
                searchInterval = System.currentTimeMillis() - AlarmManager.INTERVAL_DAY;
                break;
            case "app_start":
                long appStartTime = pref.getLong("time_of_service_start", System.currentTimeMillis());
                long deviceStartTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();

                searchInterval = deviceStartTime > appStartTime ? deviceStartTime : appStartTime;
                break;
            case "show_all":
                searchInterval = 0;
                break;
        }

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
        lbm.sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
        lbm.sendBroadcast(new Intent("com.farmerbb.taskbar.UPDATE_HOME_SCREEN_MARGINS"));

        if(altButtonConfig) {
            button = layout.findViewById(R.id.hide_taskbar_button_alt);
            layout.findViewById(R.id.hide_taskbar_button).setVisibility(View.GONE);
        } else {
            button = layout.findViewById(R.id.hide_taskbar_button);
            layout.findViewById(R.id.hide_taskbar_button_alt).setVisibility(View.GONE);
        }

        try {
            button.setTypeface(Typeface.createFromFile("/system/fonts/Roboto-Regular.ttf"));
        } catch (RuntimeException e) { /* Gracefully fail */ }

        updateButton(false);
        button.setOnClickListener(v -> toggleTaskbar(true));

        LinearLayout buttonLayout = layout.findViewById(altButtonConfig
                ? R.id.hide_taskbar_button_layout_alt
                : R.id.hide_taskbar_button_layout);
        if(buttonLayout != null) buttonLayout.setOnClickListener(v -> toggleTaskbar(true));

        LinearLayout buttonLayoutToHide = layout.findViewById(altButtonConfig
                ? R.id.hide_taskbar_button_layout
                : R.id.hide_taskbar_button_layout_alt);
        if(buttonLayoutToHide != null) buttonLayoutToHide.setVisibility(View.GONE);

        dashboardButton = layout.findViewById(R.id.dashboard_button);
        navbarButtons = layout.findViewById(R.id.navbar_buttons);

        dashboardEnabled = pref.getBoolean("dashboard", false);
        if(dashboardEnabled) {
            layout.findViewById(R.id.square1).setBackgroundColor(accentColor);
            layout.findViewById(R.id.square2).setBackgroundColor(accentColor);
            layout.findViewById(R.id.square3).setBackgroundColor(accentColor);
            layout.findViewById(R.id.square4).setBackgroundColor(accentColor);
            layout.findViewById(R.id.square5).setBackgroundColor(accentColor);
            layout.findViewById(R.id.square6).setBackgroundColor(accentColor);

            dashboardButton.setOnClickListener(v -> LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.TOGGLE_DASHBOARD")));
        } else
            dashboardButton.setVisibility(View.GONE);

        if(pref.getBoolean("button_back", false)) {
            navbarButtonsEnabled = true;

            ImageView backButton = layout.findViewById(R.id.button_back);
            backButton.setColorFilter(accentColor);
            backButton.setVisibility(View.VISIBLE);
            backButton.setOnClickListener(v -> {
                U.sendAccessibilityAction(context, AccessibilityService.GLOBAL_ACTION_BACK);
                if(U.shouldCollapse(context, false))
                    hideTaskbar(true);
            });

            backButton.setOnLongClickListener(v -> {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showInputMethodPicker();

                if(U.shouldCollapse(context, false))
                    hideTaskbar(true);

                return true;
            });

            backButton.setOnGenericMotionListener((view13, motionEvent) -> {
                if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                        && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showInputMethodPicker();

                    if(U.shouldCollapse(context, false))
                        hideTaskbar(true);
                }
                return true;
            });
        }

        if(pref.getBoolean("button_home", false)) {
            navbarButtonsEnabled = true;

            ImageView homeButton = layout.findViewById(R.id.button_home);
            homeButton.setColorFilter(accentColor);
            homeButton.setVisibility(View.VISIBLE);
            homeButton.setOnClickListener(v -> {
                U.sendAccessibilityAction(context, AccessibilityService.GLOBAL_ACTION_HOME);
                if(U.shouldCollapse(context, false))
                    hideTaskbar(true);
            });

            homeButton.setOnLongClickListener(v -> {
                Intent voiceSearchIntent = new Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE);
                voiceSearchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    context.startActivity(voiceSearchIntent);
                } catch (ActivityNotFoundException e) { /* Gracefully fail */ }

                if(U.shouldCollapse(context, false))
                    hideTaskbar(true);

                return true;
            });

            homeButton.setOnGenericMotionListener((view13, motionEvent) -> {
                if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                        && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    Intent voiceSearchIntent = new Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE);
                    voiceSearchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    try {
                        context.startActivity(voiceSearchIntent);
                    } catch (ActivityNotFoundException e) { /* Gracefully fail */ }

                    if(U.shouldCollapse(context, false))
                        hideTaskbar(true);
                }
                return true;
            });
        }

        if(pref.getBoolean("button_recents", false)) {
            navbarButtonsEnabled = true;

            ImageView recentsButton = layout.findViewById(R.id.button_recents);
            recentsButton.setColorFilter(accentColor);
            recentsButton.setVisibility(View.VISIBLE);
            recentsButton.setOnClickListener(v -> {
                U.sendAccessibilityAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS);
                if(U.shouldCollapse(context, false))
                    hideTaskbar(true);
            });

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                recentsButton.setOnLongClickListener(v -> {
                    U.sendAccessibilityAction(context, AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
                    if(U.shouldCollapse(context, false))
                        hideTaskbar(true);

                    return true;
                });

                recentsButton.setOnGenericMotionListener((view13, motionEvent) -> {
                    if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                            && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                        U.sendAccessibilityAction(context, AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
                        if(U.shouldCollapse(context, false))
                            hideTaskbar(true);
                    }
                    return true;
                });
            }
        }

        if(!navbarButtonsEnabled)
            navbarButtons.setVisibility(View.GONE);

        layout.setBackgroundColor(backgroundTint);
        layout.findViewById(R.id.divider).setBackgroundColor(accentColor);
        button.setTextColor(accentColor);

        if(isFirstStart && FreeformHackHelper.getInstance().isInFreeformWorkspace())
            showTaskbar(false);
        else if(!pref.getBoolean("collapsed", false) && pref.getBoolean("taskbar_active", false))
            toggleTaskbar(false);

        if(pref.getBoolean("auto_hide_navbar", false))
            U.showHideNavigationBar(context, false);

        if(FreeformHackHelper.getInstance().isTouchAbsorberActive()) {
            lbm.sendBroadcast(new Intent("com.farmerbb.taskbar.FINISH_FREEFORM_ACTIVITY"));

            new Handler().postDelayed(() -> U.startTouchAbsorberActivity(context), 500);
        }

        lbm.unregisterReceiver(showReceiver);
        lbm.unregisterReceiver(hideReceiver);
        lbm.unregisterReceiver(tempShowReceiver);
        lbm.unregisterReceiver(tempHideReceiver);
        lbm.unregisterReceiver(startMenuAppearReceiver);
        lbm.unregisterReceiver(startMenuDisappearReceiver);

        lbm.registerReceiver(showReceiver, new IntentFilter("com.farmerbb.taskbar.SHOW_TASKBAR"));
        lbm.registerReceiver(hideReceiver, new IntentFilter("com.farmerbb.taskbar.HIDE_TASKBAR"));
        lbm.registerReceiver(tempShowReceiver, new IntentFilter("com.farmerbb.taskbar.TEMP_SHOW_TASKBAR"));
        lbm.registerReceiver(tempHideReceiver, new IntentFilter("com.farmerbb.taskbar.TEMP_HIDE_TASKBAR"));
        lbm.registerReceiver(startMenuAppearReceiver, new IntentFilter("com.farmerbb.taskbar.START_MENU_APPEARING"));
        lbm.registerReceiver(startMenuDisappearReceiver, new IntentFilter("com.farmerbb.taskbar.START_MENU_DISAPPEARING"));

        startRefreshingRecents();

        host.addView(layout, params);

        isFirstStart = false;
    }

    private void startRefreshingRecents() {
        if(thread != null) thread.interrupt();
        stopThread2 = true;

        SharedPreferences pref = U.getSharedPreferences(context);
        showHideAutomagically = pref.getBoolean("hide_when_keyboard_shown", false);

        currentTaskbarIds.clear();

        handler = new Handler();
        thread = new Thread(() -> {
            updateRecentApps(true);

            if(!isRefreshingRecents) {
                isRefreshingRecents = true;

                while(shouldRefreshRecents) {
                    SystemClock.sleep(refreshInterval);
                    updateRecentApps(false);

                    if(showHideAutomagically && !positionIsVertical && !MenuHelper.getInstance().isStartMenuOpen())
                        handler.post(() -> {
                            if(layout != null) {
                                int[] location = new int[2];
                                layout.getLocationOnScreen(location);

                                if(location[1] != 0) {
                                    if(location[1] > currentTaskbarPosition) {
                                        currentTaskbarPosition = location[1];
                                    } else if(location[1] < currentTaskbarPosition) {
                                        if(currentTaskbarPosition - location[1] == getNavBarSize())
                                            currentTaskbarPosition = location[1];
                                        else if(!startThread2) {
                                            startThread2 = true;
                                            tempHideTaskbar(true);
                                        }
                                    }
                                }
                            }
                        });
                }

                isRefreshingRecents = false;
            }
        });

        thread.start();
    }

    @SuppressWarnings("Convert2streamapi")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private void updateRecentApps(final boolean firstRefresh) {
        if(isScreenOff()) return;

        SharedPreferences pref = U.getSharedPreferences(context);
        final PackageManager pm = context.getPackageManager();
        final List<AppEntry> entries = new ArrayList<>();
        List<LauncherActivityInfo> launcherAppCache = new ArrayList<>();
        int maxNumOfEntries = U.getMaxNumOfEntries(context);
        int realNumOfPinnedApps = 0;
        boolean fullLength = pref.getBoolean("full_length", false);

        PinnedBlockedApps pba = PinnedBlockedApps.getInstance(context);
        List<AppEntry> pinnedApps = pba.getPinnedApps();
        List<AppEntry> blockedApps = pba.getBlockedApps();
        List<String> applicationIdsToRemove = new ArrayList<>();

        // Filter out anything on the pinned/blocked apps lists
        if(pinnedApps.size() > 0) {
            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

            for(AppEntry entry : pinnedApps) {
                boolean packageEnabled = launcherApps.isPackageEnabled(entry.getPackageName(),
                        userManager.getUserForSerialNumber(entry.getUserId(context)));

                if(packageEnabled)
                    entries.add(entry);
                else
                    realNumOfPinnedApps--;

                applicationIdsToRemove.add(entry.getPackageName());
            }

            realNumOfPinnedApps = realNumOfPinnedApps + pinnedApps.size();
        }

        if(blockedApps.size() > 0) {
            for(AppEntry entry : blockedApps) {
                applicationIdsToRemove.add(entry.getPackageName());
            }
        }

        // Get list of all recently used apps
        List<AppEntry> usageStatsList = realNumOfPinnedApps < maxNumOfEntries ? getAppEntries() : new ArrayList<>();
        if(usageStatsList.size() > 0 || realNumOfPinnedApps > 0 || fullLength) {
            if(realNumOfPinnedApps < maxNumOfEntries) {
                List<AppEntry> usageStatsList2 = new ArrayList<>();
                List<AppEntry> usageStatsList3 = new ArrayList<>();
                List<AppEntry> usageStatsList4 = new ArrayList<>();
                List<AppEntry> usageStatsList5 = new ArrayList<>();
                List<AppEntry> usageStatsList6;

                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                ResolveInfo defaultLauncher = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);

                // Filter out apps without a launcher intent
                // Also filter out the current launcher, and Taskbar itself
                for(AppEntry packageInfo : usageStatsList) {
                    if(hasLauncherIntent(packageInfo.getPackageName())
                            && !packageInfo.getPackageName().contains(BuildConfig.BASE_APPLICATION_ID)
                            && !packageInfo.getPackageName().equals(defaultLauncher.activityInfo.packageName))
                        usageStatsList2.add(packageInfo);
                }

                // Filter out apps that don't fall within our current search interval
                for(AppEntry stats : usageStatsList2) {
                    if(stats.getLastTimeUsed() > searchInterval || runningAppsOnly)
                        usageStatsList3.add(stats);
                }

                // Sort apps by either most recently used, or most time used
                if(!runningAppsOnly && sortOrder.contains("most_used")) {
                    Collections.sort(usageStatsList3, (us1, us2) -> Long.compare(us2.getTotalTimeInForeground(), us1.getTotalTimeInForeground()));
                } else {
                    Collections.sort(usageStatsList3, (us1, us2) -> Long.compare(us2.getLastTimeUsed(), us1.getLastTimeUsed()));
                }

                // Filter out any duplicate entries
                List<String> applicationIds = new ArrayList<>();
                for(AppEntry stats : usageStatsList3) {
                    if(!applicationIds.contains(stats.getPackageName())) {
                        usageStatsList4.add(stats);
                        applicationIds.add(stats.getPackageName());
                    }
                }

                // Filter out the currently running foreground app, if requested by the user
                if(pref.getBoolean("hide_foreground", false)) {
                    UsageStatsManager mUsageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
                    UsageEvents events = mUsageStatsManager.queryEvents(searchInterval, System.currentTimeMillis());
                    UsageEvents.Event eventCache = new UsageEvents.Event();
                    String currentForegroundApp = null;

                    while(events.hasNextEvent()) {
                        events.getNextEvent(eventCache);

                        if(eventCache.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                            if(!(eventCache.getPackageName().contains(BuildConfig.BASE_APPLICATION_ID)
                                    && !eventCache.getClassName().equals(MainActivity.class.getCanonicalName())
                                    && !eventCache.getClassName().equals(HomeActivity.class.getCanonicalName())
                                    && !eventCache.getClassName().equals(InvisibleActivityFreeform.class.getCanonicalName())))
                                currentForegroundApp = eventCache.getPackageName();
                        }
                    }

                    if(!applicationIdsToRemove.contains(currentForegroundApp))
                        applicationIdsToRemove.add(currentForegroundApp);
                }

                for(AppEntry stats : usageStatsList4) {
                    if(!applicationIdsToRemove.contains(stats.getPackageName())) {
                        usageStatsList5.add(stats);
                    }
                }

                // Truncate list to a maximum length
                if(usageStatsList5.size() > maxNumOfEntries)
                    usageStatsList6 = usageStatsList5.subList(0, maxNumOfEntries);
                else
                    usageStatsList6 = usageStatsList5;

                // Determine if we need to reverse the order
                boolean needToReverseOrder;
                switch(U.getTaskbarPosition(context)) {
                    case "bottom_right":
                    case "top_right":
                        needToReverseOrder = sortOrder.contains("false");
                        break;
                    default:
                        needToReverseOrder = sortOrder.contains("true");
                        break;
                }

                if(needToReverseOrder) {
                    Collections.reverse(usageStatsList6);
                }

                // Generate the AppEntries for the recent apps list
                int number = usageStatsList6.size() == maxNumOfEntries
                        ? usageStatsList6.size() - realNumOfPinnedApps
                        : usageStatsList6.size();

                UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
                LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

                final List<UserHandle> userHandles = userManager.getUserProfiles();

                for(int i = 0; i < number; i++) {
                    for(UserHandle handle : userHandles) {
                        String packageName = usageStatsList6.get(i).getPackageName();
                        long lastTimeUsed = usageStatsList6.get(i).getLastTimeUsed();
                        List<LauncherActivityInfo> list = launcherApps.getActivityList(packageName, handle);
                        if(!list.isEmpty()) {
                            // Google App workaround
                            if(!packageName.equals("com.google.android.googlequicksearchbox"))
                                launcherAppCache.add(list.get(0));
                            else {
                                boolean added = false;
                                for(LauncherActivityInfo info : list) {
                                    if(info.getName().equals("com.google.android.googlequicksearchbox.SearchActivity")) {
                                        launcherAppCache.add(info);
                                        added = true;
                                    }
                                }

                                if(!added) launcherAppCache.add(list.get(0));
                            }

                            AppEntry newEntry = new AppEntry(
                                    packageName,
                                    null,
                                    null,
                                    null,
                                    false
                            );

                            newEntry.setUserId(userManager.getSerialNumberForUser(handle));
                            newEntry.setLastTimeUsed(lastTimeUsed);
                            entries.add(newEntry);

                            break;
                        }
                    }
                }
            }

            while(entries.size() > maxNumOfEntries) {
                try {
                    entries.remove(entries.size() - 1);
                    launcherAppCache.remove(launcherAppCache.size() - 1);
                } catch (ArrayIndexOutOfBoundsException e) { /* Gracefully fail */ }
            }

            // Determine if we need to reverse the order again
            if(U.getTaskbarPosition(context).contains("vertical")) {
                Collections.reverse(entries);
                Collections.reverse(launcherAppCache);
            }

            // Now that we've generated the list of apps,
            // we need to determine if we need to redraw the Taskbar or not
            boolean shouldRedrawTaskbar = firstRefresh;

            List<String> finalApplicationIds = new ArrayList<>();
            for(AppEntry entry : entries) {
                finalApplicationIds.add(entry.getPackageName());
            }

            if(finalApplicationIds.size() != currentTaskbarIds.size()
                    || numOfPinnedApps != realNumOfPinnedApps)
                shouldRedrawTaskbar = true;
            else {
                for(int i = 0; i < finalApplicationIds.size(); i++) {
                    if(!finalApplicationIds.get(i).equals(currentTaskbarIds.get(i))) {
                        shouldRedrawTaskbar = true;
                        break;
                    }
                }
            }

            if(shouldRedrawTaskbar) {
                currentTaskbarIds = finalApplicationIds;
                numOfPinnedApps = realNumOfPinnedApps;

                UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);

                int launcherAppCachePos = -1;
                for(int i = 0; i < entries.size(); i++) {
                    if(entries.get(i).getComponentName() == null) {
                        launcherAppCachePos++;
                        LauncherActivityInfo appInfo = launcherAppCache.get(launcherAppCachePos);
                        String packageName = entries.get(i).getPackageName();
                        long lastTimeUsed = entries.get(i).getLastTimeUsed();

                        entries.remove(i);

                        AppEntry newEntry = new AppEntry(
                                packageName,
                                appInfo.getComponentName().flattenToString(),
                                appInfo.getLabel().toString(),
                                IconCache.getInstance(context).getIcon(context, pm, appInfo),
                                false);

                        newEntry.setUserId(userManager.getSerialNumberForUser(appInfo.getUser()));
                        newEntry.setLastTimeUsed(lastTimeUsed);
                        entries.add(i, newEntry);
                    }
                }

                final int numOfEntries = Math.min(entries.size(), maxNumOfEntries);

                handler.post(() -> {
                    if(numOfEntries > 0 || fullLength) {
                        ViewGroup.LayoutParams params = scrollView.getLayoutParams();
                        DisplayInfo display = U.getDisplayInfo(context);
                        int recentsSize = context.getResources().getDimensionPixelSize(R.dimen.icon_size) * numOfEntries;
                        float maxRecentsSize = fullLength ? Float.MAX_VALUE : recentsSize;

                        if(U.getTaskbarPosition(context).contains("vertical")) {
                            int maxScreenSize = display.height
                                    - U.getStatusBarHeight(context)
                                    - U.getBaseTaskbarSize(context);

                            params.height = (int) Math.min(maxRecentsSize, maxScreenSize)
                                    + context.getResources().getDimensionPixelSize(R.dimen.divider_size);

                            if(fullLength && U.getTaskbarPosition(context).contains("bottom")) {
                                try {
                                    Space whitespace = layout.findViewById(R.id.whitespace);
                                    ViewGroup.LayoutParams params2 = whitespace.getLayoutParams();
                                    params2.height = maxScreenSize - recentsSize;
                                    whitespace.setLayoutParams(params2);
                                } catch (NullPointerException e) { /* Gracefully fail */ }
                            }
                        } else {
                            int maxScreenSize = display.width - U.getBaseTaskbarSize(context);

                            params.width = (int) Math.min(maxRecentsSize, maxScreenSize)
                                    + context.getResources().getDimensionPixelSize(R.dimen.divider_size);

                            if(fullLength && U.getTaskbarPosition(context).contains("right")) {
                                try {
                                    Space whitespace = layout.findViewById(R.id.whitespace);
                                    ViewGroup.LayoutParams params2 = whitespace.getLayoutParams();
                                    params2.width = maxScreenSize - recentsSize;
                                    whitespace.setLayoutParams(params2);
                                } catch (NullPointerException e) { /* Gracefully fail */ }
                            }
                        }

                        scrollView.setLayoutParams(params);

                        taskbar.removeAllViews();
                        for(int i = 0; i < entries.size(); i++) {
                            taskbar.addView(getView(entries, i));
                        }

                        isShowingRecents = true;
                        if(shouldRefreshRecents && scrollView.getVisibility() != View.VISIBLE) {
                            if(firstRefresh)
                                scrollView.setVisibility(View.INVISIBLE);
                            else
                                scrollView.setVisibility(View.VISIBLE);
                        }

                        if(firstRefresh && scrollView.getVisibility() != View.VISIBLE)
                            new Handler().post(() -> {
                                switch(U.getTaskbarPosition(context)) {
                                    case "bottom_left":
                                    case "bottom_right":
                                    case "top_left":
                                    case "top_right":
                                        if(sortOrder.contains("false"))
                                            scrollView.scrollTo(0, 0);
                                        else if(sortOrder.contains("true"))
                                            scrollView.scrollTo(taskbar.getWidth(), taskbar.getHeight());
                                        break;
                                    case "bottom_vertical_left":
                                    case "bottom_vertical_right":
                                    case "top_vertical_left":
                                    case "top_vertical_right":
                                        if(sortOrder.contains("false"))
                                            scrollView.scrollTo(taskbar.getWidth(), taskbar.getHeight());
                                        else if(sortOrder.contains("true"))
                                            scrollView.scrollTo(0, 0);
                                        break;
                                }

                                if(shouldRefreshRecents) {
                                    scrollView.setVisibility(View.VISIBLE);
                                }
                            });
                    } else {
                        isShowingRecents = false;
                        scrollView.setVisibility(View.GONE);
                    }
                });
            }
        } else if(firstRefresh || currentTaskbarIds.size() > 0) {
            currentTaskbarIds.clear();
            handler.post(() -> {
                isShowingRecents = false;
                scrollView.setVisibility(View.GONE);
            });
        }
    }

    private void toggleTaskbar(boolean userInitiated) {
        if(userInitiated && Build.BRAND.equalsIgnoreCase("essential")) {
            SharedPreferences pref = U.getSharedPreferences(context);
            if(!pref.getBoolean("grip_rejection_toast_shown", false)) {
                U.showToastLong(context, R.string.essential_phone_grip_rejection);
                pref.edit().putBoolean("grip_rejection_toast_shown", true).apply();
            }
        }

        if(startButton.getVisibility() == View.GONE)
            showTaskbar(true);
        else
            hideTaskbar(true);
    }

    private void showTaskbar(boolean clearVariables) {
        if(clearVariables) {
            taskbarShownTemporarily = false;
            taskbarHiddenTemporarily = false;
        }

        if(startButton.getVisibility() == View.GONE) {
            startButton.setVisibility(View.VISIBLE);
            space.setVisibility(View.VISIBLE);

            if(dashboardEnabled)
                dashboardButton.setVisibility(View.VISIBLE);

            if(navbarButtonsEnabled)
                navbarButtons.setVisibility(View.VISIBLE);

            if(isShowingRecents && scrollView.getVisibility() == View.GONE)
                scrollView.setVisibility(View.INVISIBLE);

            shouldRefreshRecents = true;
            startRefreshingRecents();

            SharedPreferences pref = U.getSharedPreferences(context);
            pref.edit().putBoolean("collapsed", true).apply();

            updateButton(false);

            new Handler().post(() -> LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.SHOW_START_MENU_SPACE")));
        }
    }

    private void hideTaskbar(boolean clearVariables) {
        if(clearVariables) {
            taskbarShownTemporarily = false;
            taskbarHiddenTemporarily = false;
        }

        if(startButton.getVisibility() == View.VISIBLE) {
            startButton.setVisibility(View.GONE);
            space.setVisibility(View.GONE);

            if(dashboardEnabled)
                dashboardButton.setVisibility(View.GONE);

            if(navbarButtonsEnabled)
                navbarButtons.setVisibility(View.GONE);

            if(isShowingRecents) {
                scrollView.setVisibility(View.GONE);
            }

            shouldRefreshRecents = false;
            if(thread != null) thread.interrupt();

            SharedPreferences pref = U.getSharedPreferences(context);
            pref.edit().putBoolean("collapsed", false).apply();

            updateButton(true);

            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_DASHBOARD"));

            new Handler().post(() -> LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU_SPACE")));
        }
    }

    private void tempShowTaskbar() {
        if(!taskbarHiddenTemporarily) {
            SharedPreferences pref = U.getSharedPreferences(context);
            if(!pref.getBoolean("collapsed", false)) taskbarShownTemporarily = true;
        }

        showTaskbar(false);

        if(taskbarHiddenTemporarily)
            taskbarHiddenTemporarily = false;
    }

    private void tempHideTaskbar(boolean monitorPositionChanges) {
        if(!taskbarShownTemporarily) {
            SharedPreferences pref = U.getSharedPreferences(context);
            if(pref.getBoolean("collapsed", false)) taskbarHiddenTemporarily = true;
        }

        hideTaskbar(false);

        if(taskbarShownTemporarily)
            taskbarShownTemporarily = false;

        if(monitorPositionChanges && showHideAutomagically && !positionIsVertical) {
            if(thread2 != null) thread2.interrupt();

            handler2 = new Handler();
            thread2 = new Thread(() -> {
                stopThread2 = false;

                while(!stopThread2) {
                    SystemClock.sleep(refreshInterval);

                    handler2.post(() -> stopThread2 = checkPositionChange());
                }

                startThread2 = false;
            });

            thread2.start();
        }
    }

    private boolean checkPositionChange() {
        if(!isScreenOff() && layout != null) {
            int[] location = new int[2];
            layout.getLocationOnScreen(location);

            if(location[1] == 0) {
                return true;
            } else {
                if(location[1] > currentTaskbarPosition) {
                    currentTaskbarPosition = location[1];
                    if(taskbarHiddenTemporarily) {
                        tempShowTaskbar();
                        return true;
                    }
                } else if(location[1] == currentTaskbarPosition && taskbarHiddenTemporarily) {
                    tempShowTaskbar();
                    return true;
                } else if(location[1] < currentTaskbarPosition
                        && currentTaskbarPosition - location[1] == getNavBarSize()) {
                    currentTaskbarPosition = location[1];
                }
            }
        }

        return false;
    }

    private int getNavBarSize() {
        Point size = new Point();
        Point realSize = new Point();

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getSize(size);
        display.getRealSize(realSize);

        return realSize.y - size.y;
    }

    @Override
    public void onDestroyHost(UIHost host) {
        shouldRefreshRecents = false;

        if(layout != null)
            try {
                host.removeView(layout);
            } catch (IllegalArgumentException e) { /* Gracefully fail */ }

        SharedPreferences pref = U.getSharedPreferences(context);
        if(pref.getBoolean("skip_auto_hide_navbar", false)) {
            pref.edit().remove("skip_auto_hide_navbar").apply();
        } else if(pref.getBoolean("auto_hide_navbar", false))
            U.showHideNavigationBar(context, true);

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);

        lbm.unregisterReceiver(showReceiver);
        lbm.unregisterReceiver(hideReceiver);
        lbm.unregisterReceiver(tempShowReceiver);
        lbm.unregisterReceiver(tempHideReceiver);
        lbm.unregisterReceiver(startMenuAppearReceiver);
        lbm.unregisterReceiver(startMenuDisappearReceiver);

        isFirstStart = true;
    }

    private void openContextMenu() {
        SharedPreferences pref = U.getSharedPreferences(context);

        Bundle args = new Bundle();
        args.putBoolean("dont_show_quit",
                LauncherHelper.getInstance().isOnHomeScreen()
                        && !pref.getBoolean("taskbar_active", false));
        args.putBoolean("is_start_button", true);

        U.startContextMenuActivity(context, args);
    }

    private void updateButton(boolean isCollapsed) {
        SharedPreferences pref = U.getSharedPreferences(context);
        boolean hide = pref.getBoolean("invisible_button", false);

        if(button != null) button.setText(context.getString(isCollapsed ? R.string.right_arrow : R.string.left_arrow));
        if(layout != null) layout.setAlpha(isCollapsed && hide ? 0 : 1);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRecreateHost(UIHost host) {
        if(layout != null) {
            try {
                host.removeView(layout);
            } catch (IllegalArgumentException e) { /* Gracefully fail */ }

            currentTaskbarPosition = 0;

            if(U.canDrawOverlays(context, host instanceof HomeActivityDelegate))
                drawTaskbar(host);
            else {
                SharedPreferences pref = U.getSharedPreferences(context);
                pref.edit().putBoolean("taskbar_active", false).apply();

                host.terminate();
            }
        }
    }

    private View getView(List<AppEntry> list, int position) {
        View convertView = View.inflate(context, R.layout.icon, null);

        final AppEntry entry = list.get(position);
        final SharedPreferences pref = U.getSharedPreferences(context);

        ImageView imageView = convertView.findViewById(R.id.icon);
        ImageView imageView2 = convertView.findViewById(R.id.shortcut_icon);
        imageView.setImageDrawable(entry.getIcon(context));
        imageView2.setBackgroundColor(pref.getInt("accent_color", context.getResources().getInteger(R.integer.translucent_white)));

        String taskbarPosition = U.getTaskbarPosition(context);
        if(pref.getBoolean("shortcut_icon", true)) {
            boolean shouldShowShortcutIcon;
            if(taskbarPosition.contains("vertical"))
                shouldShowShortcutIcon = position >= list.size() - numOfPinnedApps;
            else
                shouldShowShortcutIcon = position < numOfPinnedApps;

            if(shouldShowShortcutIcon) imageView2.setVisibility(View.VISIBLE);
        }

        if(taskbarPosition.equals("bottom_right") || taskbarPosition.equals("top_right")) {
            imageView.setRotationY(180);
            imageView2.setRotationY(180);
        }

        FrameLayout layout = convertView.findViewById(R.id.entry);
        layout.setOnClickListener(view -> U.launchApp(
                context,
                entry,
                null,
                true,
                false,
                view
        ));

        layout.setOnLongClickListener(view -> {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            openContextMenu(entry, location);
            return true;
        });

        layout.setOnGenericMotionListener((view, motionEvent) -> {
            int action = motionEvent.getAction();

            if(action == MotionEvent.ACTION_BUTTON_PRESS
                    && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                openContextMenu(entry, location);
            }

            if(action == MotionEvent.ACTION_SCROLL && pref.getBoolean("visual_feedback", true))
                view.setBackgroundColor(0);

            return false;
        });

        if(pref.getBoolean("visual_feedback", true)) {
            layout.setOnHoverListener((v, event) -> {
                if(event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                    int accentColor = U.getAccentColor(context);
                    accentColor = ColorUtils.setAlphaComponent(accentColor, Color.alpha(accentColor) / 2);
                    v.setBackgroundColor(accentColor);
                }

                if(event.getAction() == MotionEvent.ACTION_HOVER_EXIT)
                    v.setBackgroundColor(0);

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    v.setPointerIcon(PointerIcon.getSystemIcon(context, PointerIcon.TYPE_DEFAULT));

                return false;
            });

            layout.setOnTouchListener((v, event) -> {
                v.setAlpha(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE ? 0.5f : 1);
                return false;
            });
        }

        if(runningAppsOnly) {
            ImageView runningAppIndicator = convertView.findViewById(R.id.running_app_indicator);
            if(entry.getLastTimeUsed() > 0) {
                runningAppIndicator.setVisibility(View.VISIBLE);
                runningAppIndicator.setColorFilter(U.getAccentColor(context));
            } else
                runningAppIndicator.setVisibility(View.GONE);
        }

        return convertView;
    }

    private void openContextMenu(AppEntry entry, int[] location) {
        Bundle args = new Bundle();
        args.putSerializable("app_entry", entry);
        args.putInt("x", location[0]);
        args.putInt("y", location[1]);

        U.startContextMenuActivity(context, args);
    }

    private List<AppEntry> getAppEntries() {
        SharedPreferences pref = U.getSharedPreferences(context);
        if(runningAppsOnly)
            return getAppEntriesUsingActivityManager(Integer.parseInt(pref.getString("max_num_of_recents", "10")));
        else
            return getAppEntriesUsingUsageStats();
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.M)
    private List<AppEntry> getAppEntriesUsingActivityManager(int maxNum) {
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RecentTaskInfo> usageStatsList = mActivityManager.getRecentTasks(maxNum, 0);
        List<AppEntry> entries = new ArrayList<>();

        for(int i = 0; i < usageStatsList.size(); i++) {
            ActivityManager.RecentTaskInfo recentTaskInfo = usageStatsList.get(i);
            if(recentTaskInfo.id != -1) {
                String packageName = recentTaskInfo.baseActivity.getPackageName();
                AppEntry newEntry = new AppEntry(
                        packageName,
                        null,
                        null,
                        null,
                        false
                );

                try {
                    Field field = ActivityManager.RecentTaskInfo.class.getField("firstActiveTime");
                    newEntry.setLastTimeUsed(field.getLong(recentTaskInfo));
                } catch (Exception e) {
                    newEntry.setLastTimeUsed(i);
                }

                entries.add(newEntry);
            }
        }

        return entries;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private List<AppEntry> getAppEntriesUsingUsageStats() {
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        List<UsageStats> usageStatsList = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, searchInterval, System.currentTimeMillis());
        List<AppEntry> entries = new ArrayList<>();

        for(UsageStats usageStats : usageStatsList) {
            AppEntry newEntry = new AppEntry(
                    usageStats.getPackageName(),
                    null,
                    null,
                    null,
                    false
            );

            newEntry.setTotalTimeInForeground(usageStats.getTotalTimeInForeground());
            newEntry.setLastTimeUsed(usageStats.getLastTimeUsed());
            entries.add(newEntry);
        }

        return entries;
    }

    private boolean hasLauncherIntent(String packageName) {
        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER);
        intentToResolve.setPackage(packageName);

        List<ResolveInfo> ris = context.getPackageManager().queryIntentActivities(intentToResolve, 0);
        return ris != null && ris.size() > 0;
    }

    private boolean isScreenOff() {
        if(U.isChromeOs(context))
            return false;

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return !pm.isInteractive();
    }
}
