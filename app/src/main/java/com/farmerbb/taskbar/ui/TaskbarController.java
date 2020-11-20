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
import android.bluetooth.BluetoothAdapter;
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
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;

import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.activity.HomeActivityDelegate;
import com.farmerbb.taskbar.activity.MainActivity;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.HomeActivity;
import com.farmerbb.taskbar.activity.InvisibleActivityFreeform;
import com.farmerbb.taskbar.activity.SecondaryHomeActivity;
import com.farmerbb.taskbar.util.TaskbarPosition;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.DisplayInfo;
import com.farmerbb.taskbar.helper.FreeformHackHelper;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.helper.LauncherHelper;
import com.farmerbb.taskbar.util.PinnedBlockedApps;
import com.farmerbb.taskbar.helper.MenuHelper;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class TaskbarController extends UIController {

    private LinearLayout layout;
    private ImageView startButton;
    private LinearLayout taskbar;
    private FrameLayout scrollView;
    private Button button;
    private Space space;
    private FrameLayout dashboardButton;
    private LinearLayout navbarButtons;
    private LinearLayout sysTrayLayout;
    private FrameLayout sysTrayParentLayout;
    private TextView time;
    private ImageView notificationCountCircle;
    private TextView notificationCountText;

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

    private int currentTaskbarPosition = 0;
    private boolean showHideAutomagically = false;
    private boolean positionIsVertical = false;
    private boolean dashboardEnabled = false;
    private boolean navbarButtonsEnabled = false;
    private boolean sysTrayEnabled = false;

    private List<String> currentTaskbarIds = new ArrayList<>();
    private int numOfPinnedApps = -1;

    private int cellStrength = -1;
    private int notificationCount = 0;
    private int numOfSysTrayIcons = 0;

    private boolean matchParent;
    private Runnable updateParamsRunnable;

    private final Map<Integer, Boolean> sysTrayIconStates = new HashMap<>();

    private final View.OnClickListener ocl = view ->
            U.sendBroadcast(context, ACTION_TOGGLE_START_MENU);

    private final BroadcastReceiver showReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showTaskbar(true);
        }
    };

    private final BroadcastReceiver hideReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideTaskbar(true);
        }
    };

    private final BroadcastReceiver tempShowReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tempShowTaskbar();
        }
    };

    private final BroadcastReceiver tempHideReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tempHideTaskbar(false);
        }
    };

    private final BroadcastReceiver startMenuAppearReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(startButton.getVisibility() == View.GONE
                    && (!LauncherHelper.getInstance().isOnHomeScreen(context) || FreeformHackHelper.getInstance().isInFreeformWorkspace()))
                layout.setVisibility(View.GONE);
        }
    };

    private final BroadcastReceiver startMenuDisappearReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(startButton.getVisibility() == View.GONE)
                layout.setVisibility(View.VISIBLE);
        }
    };

    private final BroadcastReceiver notificationCountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            notificationCount = intent.getIntExtra(EXTRA_COUNT, 0);
        }
    };

    @TargetApi(Build.VERSION_CODES.M)
    private final PhoneStateListener listener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            try {
                cellStrength = signalStrength.getLevel();
            } catch (SecurityException e) {
                cellStrength = -1;
            }
        }
    };

    public TaskbarController(Context context) {
        super(context);
    }

    @Override
    public void onCreateHost(UIHost host) {
        init(context, host, () -> drawTaskbar(host));
    }

    private void drawTaskbar(UIHost host) {
        IconCache.getInstance(context).clearCache();

        // Initialize layout params
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        TaskbarPosition.setCachedRotation(windowManager.getDefaultDisplay().getRotation());

        final ViewParams params = new ViewParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                -1,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                getBottomMargin(context, host)
        );

        // Determine where to show the taskbar on screen
        String taskbarPosition = TaskbarPosition.getTaskbarPosition(context);
        params.gravity = getTaskbarGravity(taskbarPosition);
        int layoutId = getTaskbarLayoutId(taskbarPosition);
        positionIsVertical = TaskbarPosition.isVertical(taskbarPosition);

        // Initialize views
        SharedPreferences pref = U.getSharedPreferences(context);
        boolean altButtonConfig = pref.getBoolean(PREF_ALT_BUTTON_CONFIG, false);

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
        drawStartButton(context, startButton, pref, accentColor);

        refreshInterval = (int) (Float.parseFloat(pref.getString(PREF_REFRESH_FREQUENCY, "1")) * 1000);
        if(refreshInterval == 0)
            refreshInterval = 100;

        sortOrder = pref.getString(PREF_SORT_ORDER, "false");
        runningAppsOnly =
                PREF_RECENTS_AMOUNT_RUNNING_APPS_ONLY
                        .equals(pref.getString(PREF_RECENTS_AMOUNT, PREF_RECENTS_AMOUNT_PAST_DAY));
        searchInterval = getSearchInterval(pref);

        U.sendBroadcast(context, ACTION_HIDE_START_MENU);
        U.sendBroadcast(context, ACTION_UPDATE_HOME_SCREEN_MARGINS);

        if(altButtonConfig) {
            button = layout.findViewById(R.id.hide_taskbar_button_alt);
            layout.findViewById(R.id.hide_taskbar_button).setVisibility(View.GONE);
        } else {
            button = layout.findViewById(R.id.hide_taskbar_button);
            layout.findViewById(R.id.hide_taskbar_button_alt).setVisibility(View.GONE);
        }

        try {
            button.setTypeface(Typeface.createFromFile("/system/fonts/Roboto-Regular.ttf"));
        } catch (RuntimeException ignored) {}

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
        dashboardEnabled = drawDashboardButton(context, layout, dashboardButton, accentColor);
        navbarButtonsEnabled = drawNavbarButtons(context, layout, pref, accentColor);
        if(!navbarButtonsEnabled)
            navbarButtons.setVisibility(View.GONE);

        sysTrayEnabled = U.isSystemTrayEnabled(context);

        if(sysTrayEnabled) {
            drawSysTray(context, layoutId, layout);
        }

        layout.setBackgroundColor(backgroundTint);
        layout.findViewById(R.id.divider).setBackgroundColor(accentColor);
        button.setTextColor(accentColor);

        applyMarginFix(host, layout, params);

        if(isFirstStart && FreeformHackHelper.getInstance().isInFreeformWorkspace())
            showTaskbar(false);
        else if(!pref.getBoolean(PREF_COLLAPSED, false) && pref.getBoolean(PREF_TASKBAR_ACTIVE, false))
            toggleTaskbar(false);

        if(pref.getBoolean(PREF_AUTO_HIDE_NAVBAR, false))
            U.showHideNavigationBar(context, false);

        if(FreeformHackHelper.getInstance().isTouchAbsorberActive()) {
            U.sendBroadcast(context, ACTION_FINISH_FREEFORM_ACTIVITY);

            new Handler().postDelayed(() -> U.startTouchAbsorberActivity(context), 500);
        }

        U.registerReceiver(context, showReceiver, ACTION_SHOW_TASKBAR);
        U.registerReceiver(context, hideReceiver, ACTION_HIDE_TASKBAR);
        U.registerReceiver(context, tempShowReceiver, ACTION_TEMP_SHOW_TASKBAR);
        U.registerReceiver(context, tempHideReceiver, ACTION_TEMP_HIDE_TASKBAR);
        U.registerReceiver(context, startMenuAppearReceiver, ACTION_START_MENU_APPEARING);
        U.registerReceiver(context, startMenuDisappearReceiver, ACTION_START_MENU_DISAPPEARING);

        if(sysTrayEnabled) {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            manager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

            U.registerReceiver(context, notificationCountReceiver, ACTION_NOTIFICATION_COUNT_CHANGED);
            U.sendBroadcast(context, ACTION_REQUEST_NOTIFICATION_COUNT);
        }

        matchParent = false;
        updateParamsRunnable = () -> {
            ViewParams newParams;
            if(TaskbarPosition.isVertical(context)) {
                newParams = matchParent
                        ? params.updateHeight(WindowManager.LayoutParams.MATCH_PARENT)
                        : params.updateHeight(WindowManager.LayoutParams.WRAP_CONTENT);
            } else {
                newParams = matchParent
                        ? params.updateWidth(WindowManager.LayoutParams.MATCH_PARENT)
                        : params.updateWidth(WindowManager.LayoutParams.WRAP_CONTENT);
            }

            host.updateViewLayout(layout, newParams);
        };

        startRefreshingRecents();

        host.addView(layout, params);

        isFirstStart = false;
    }

    @SuppressLint("RtlHardcoded")
    @VisibleForTesting
    int getTaskbarGravity(String taskbarPosition) {
        int gravity = Gravity.BOTTOM | Gravity.LEFT;
        switch(taskbarPosition) {
            case POSITION_BOTTOM_LEFT:
            case POSITION_BOTTOM_VERTICAL_LEFT:
                gravity = Gravity.BOTTOM | Gravity.LEFT;
                break;
            case POSITION_BOTTOM_RIGHT:
            case POSITION_BOTTOM_VERTICAL_RIGHT:
                gravity = Gravity.BOTTOM | Gravity.RIGHT;
                break;
            case POSITION_TOP_LEFT:
            case POSITION_TOP_VERTICAL_LEFT:
                gravity = Gravity.TOP | Gravity.LEFT;
                break;
            case POSITION_TOP_RIGHT:
            case POSITION_TOP_VERTICAL_RIGHT:
                gravity = Gravity.TOP | Gravity.RIGHT;
                break;
        }
        return gravity;
    }

    @VisibleForTesting
    int getTaskbarLayoutId(String taskbarPosition) {
        int layoutId = R.layout.tb_taskbar_left;
        switch(taskbarPosition) {
            case POSITION_BOTTOM_LEFT:
            case POSITION_TOP_LEFT:
                layoutId = R.layout.tb_taskbar_left;
                break;
            case POSITION_BOTTOM_VERTICAL_LEFT:
            case POSITION_BOTTOM_VERTICAL_RIGHT:
                layoutId = R.layout.tb_taskbar_vertical;
                break;
            case POSITION_BOTTOM_RIGHT:
            case POSITION_TOP_RIGHT:
                layoutId = R.layout.tb_taskbar_right;
                break;
            case POSITION_TOP_VERTICAL_LEFT:
            case POSITION_TOP_VERTICAL_RIGHT:
                layoutId = R.layout.tb_taskbar_top_vertical;
                break;
        }
        return layoutId;
    }

    @VisibleForTesting
    void drawStartButton(Context context, ImageView startButton, SharedPreferences pref, int accentColor) {
        Drawable allAppsIcon = ContextCompat.getDrawable(context, R.drawable.tb_all_apps_button_icon);
        int padding = 0;

        switch(pref.getString(PREF_START_BUTTON_IMAGE, U.getDefaultStartButtonImage(context))) {
            case PREF_START_BUTTON_IMAGE_DEFAULT:
                startButton.setImageDrawable(allAppsIcon);
                padding = context.getResources().getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding);
                break;
            case PREF_START_BUTTON_IMAGE_APP_LOGO:
                Drawable drawable;

                if(U.isAndroidGeneric(context)) {
                    try {
                        String bdPackageName = "com.boringdroid.systemui";
                        Resources res = context.getPackageManager().getResourcesForApplication(bdPackageName);
                        int id = res.getIdentifier("bt_all_apps", "drawable", bdPackageName);
                        drawable = ResourcesCompat.getDrawable(res, id, null);
                    } catch (Exception e) {
                        drawable = ContextCompat.getDrawable(context, R.drawable.tb_bliss);
                        drawable.setTint(accentColor);
                    }
                } else {
                    LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                    LauncherActivityInfo info = launcherApps.getActivityList(context.getPackageName(), Process.myUserHandle()).get(0);
                    drawable = IconCache.getInstance(context).getIcon(context, context.getPackageManager(), info);
                }

                startButton.setImageDrawable(drawable);
                padding = context.getResources().getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding_alt);
                break;
            case PREF_START_BUTTON_IMAGE_CUSTOM:
                U.applyCustomImage(context, "custom_image", startButton, allAppsIcon);
                padding = context.getResources().getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding);
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
    }

    @VisibleForTesting
    boolean drawDashboardButton(Context context,
                                LinearLayout layout,
                                FrameLayout dashboardButton,
                                int accentColor) {
        boolean dashboardEnabled = U.getBooleanPrefWithDefault(context, PREF_DASHBOARD);
        if(dashboardEnabled) {
            layout.findViewById(R.id.square1).setBackgroundColor(accentColor);
            layout.findViewById(R.id.square2).setBackgroundColor(accentColor);
            layout.findViewById(R.id.square3).setBackgroundColor(accentColor);
            layout.findViewById(R.id.square4).setBackgroundColor(accentColor);
            layout.findViewById(R.id.square5).setBackgroundColor(accentColor);
            layout.findViewById(R.id.square6).setBackgroundColor(accentColor);

            dashboardButton.setOnClickListener(v -> U.sendBroadcast(context, ACTION_TOGGLE_DASHBOARD));
            dashboardButton.setVisibility(View.VISIBLE);
        } else
            dashboardButton.setVisibility(View.GONE);

        return dashboardEnabled;
    }

    @VisibleForTesting
    boolean drawNavbarButtons(Context context,
                              LinearLayout layout,
                              SharedPreferences pref,
                              int accentColor) {
        boolean navbarButtonsEnabled = false;
        if(pref.getBoolean(PREF_BUTTON_BACK, false)) {
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

        if(pref.getBoolean(PREF_BUTTON_HOME, false)) {
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
                } catch (ActivityNotFoundException ignored) {}

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
                    } catch (ActivityNotFoundException ignored) {}

                    if(U.shouldCollapse(context, false))
                        hideTaskbar(true);
                }
                return true;
            });
        }

        if(pref.getBoolean(PREF_BUTTON_RECENTS, false)) {
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
        return navbarButtonsEnabled;
    }

    @VisibleForTesting
    long getSearchInterval(SharedPreferences pref) {
        long searchInterval = -1;
        switch(pref.getString(PREF_RECENTS_AMOUNT, PREF_RECENTS_AMOUNT_PAST_DAY)) {
            case PREF_RECENTS_AMOUNT_PAST_DAY:
                searchInterval = System.currentTimeMillis() - AlarmManager.INTERVAL_DAY;
                break;
            case PREF_RECENTS_AMOUNT_APP_START:
                long appStartTime = pref.getLong(PREF_TIME_OF_SERVICE_START, System.currentTimeMillis());
                long deviceStartTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();
                searchInterval = Math.max(deviceStartTime, appStartTime);
                break;
            case PREF_RECENTS_AMOUNT_SHOW_ALL:
                searchInterval = 0;
                break;
        }
        return searchInterval;
    }

    @VisibleForTesting
    void drawSysTray(Context context, int layoutId, LinearLayout layout) {
        sysTrayLayout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.tb_system_tray, null);

        FrameLayout.LayoutParams sysTrayParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                context.getResources().getDimensionPixelSize(R.dimen.tb_icon_size)
        );

        if(layoutId == R.layout.tb_taskbar_right) {
            time = sysTrayLayout.findViewById(R.id.time_left);
            sysTrayParams.gravity = Gravity.START;
            sysTrayLayout.findViewById(R.id.space_right).setVisibility(View.VISIBLE);
        } else {
            time = sysTrayLayout.findViewById(R.id.time_right);
            sysTrayParams.gravity = Gravity.END;
            sysTrayLayout.findViewById(R.id.space_left).setVisibility(View.VISIBLE);
        }

        time.setVisibility(View.VISIBLE);
        sysTrayLayout.setLayoutParams(sysTrayParams);

        if(!U.isLibrary(context)) {
            sysTrayLayout.setOnClickListener(v -> {
                U.sendAccessibilityAction(context, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, () -> {
                    if(LauncherHelper.getInstance().isOnSecondaryHomeScreen(context)) {
                        U.showToast(context, R.string.tb_opening_notification_tray);
                        U.sendBroadcast(context, ACTION_UNDIM_SCREEN);
                    }
                });

                if(U.shouldCollapse(context, false))
                    hideTaskbar(true);
            });

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                sysTrayLayout.setOnLongClickListener(v -> {
                    U.sendAccessibilityAction(context, AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS, () -> {
                        if(LauncherHelper.getInstance().isOnSecondaryHomeScreen(context)) {
                            U.showToast(context, R.string.tb_opening_quick_settings);
                            U.sendBroadcast(context, ACTION_UNDIM_SCREEN);
                        }
                    });

                    if(U.shouldCollapse(context, false))
                        hideTaskbar(true);

                    return true;
                });

                sysTrayLayout.setOnGenericMotionListener((view, motionEvent) -> {
                    if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                            && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                        U.sendAccessibilityAction(context, AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS, () -> {
                            if(LauncherHelper.getInstance().isOnSecondaryHomeScreen(context)) {
                                U.showToast(context, R.string.tb_opening_quick_settings);
                                U.sendBroadcast(context, ACTION_UNDIM_SCREEN);
                            }
                        });

                        if(U.shouldCollapse(context, false))
                            hideTaskbar(true);
                    }
                    return true;
                });
            }
        }

        notificationCountCircle = sysTrayLayout.findViewById(R.id.notification_count_circle);
        notificationCountText = sysTrayLayout.findViewById(R.id.notification_count_text);

        sysTrayParentLayout = layout.findViewById(R.id.add_systray_here);
        sysTrayParentLayout.setVisibility(View.VISIBLE);
        sysTrayParentLayout.addView(sysTrayLayout);

        sysTrayIconStates.clear();
        sysTrayIconStates.put(R.id.cellular, false);
        sysTrayIconStates.put(R.id.bluetooth, false);
        sysTrayIconStates.put(R.id.wifi, false);
        sysTrayIconStates.put(R.id.battery, false);
        sysTrayIconStates.put(R.id.notification_count, false);
    }

    private void startRefreshingRecents() {
        if(thread != null) thread.interrupt();
        stopThread2 = true;

        SharedPreferences pref = U.getSharedPreferences(context);
        showHideAutomagically = pref.getBoolean(PREF_HIDE_WHEN_KEYBOARD_SHOWN, false);

        currentTaskbarIds.clear();

        handler = new Handler();
        thread = new Thread(() -> {
            updateRecentApps(true);

            if(!isRefreshingRecents) {
                isRefreshingRecents = true;

                while(shouldRefreshRecents) {
                    updateRecentApps(false);

                    if(showHideAutomagically && !positionIsVertical && !MenuHelper.getInstance().isStartMenuOpen()) {
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

                    SystemClock.sleep(refreshInterval);
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

        updateSystemTray();

        SharedPreferences pref = U.getSharedPreferences(context);
        final PackageManager pm = context.getPackageManager();
        final List<AppEntry> entries = new ArrayList<>();
        List<LauncherActivityInfo> launcherAppCache = new ArrayList<>();
        int maxNumOfEntries = firstRefresh ? 0 : U.getMaxNumOfEntries(context);
        boolean fullLength = pref.getBoolean(PREF_FULL_LENGTH, true);

        PinnedBlockedApps pba = PinnedBlockedApps.getInstance(context);
        List<AppEntry> pinnedApps = pba.getPinnedApps();
        List<AppEntry> blockedApps = pba.getBlockedApps();
        List<String> applicationIdsToRemove = new ArrayList<>();

        // Filter out anything on the pinned/blocked apps lists
        int realNumOfPinnedApps = filterRealPinnedApps(context, pinnedApps, entries, applicationIdsToRemove);

        if(blockedApps.size() > 0) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized(blockedApps) {
                for(AppEntry entry : blockedApps) {
                    applicationIdsToRemove.add(entry.getPackageName());
                }
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
                            && !packageInfo.getPackageName().equals(defaultLauncher.activityInfo.packageName)
                            && (!(U.launcherIsDefault(context) && pref.getBoolean(PREF_DESKTOP_MODE, false))
                            || !packageInfo.getPackageName().equals(pref.getString(PREF_HSL_ID, "null"))))
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
                filterForegroundApp(context, pref, searchInterval, applicationIdsToRemove);

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
                if(needToReverseOrder(context, sortOrder)) {
                    Collections.reverse(usageStatsList6);
                }

                // Generate the AppEntries for the recent apps list
                int number = usageStatsList6.size() == maxNumOfEntries
                        ? usageStatsList6.size() - realNumOfPinnedApps
                        : usageStatsList6.size();

                generateAppEntries(context, number, usageStatsList6, entries, launcherAppCache);
            }

            while(entries.size() > maxNumOfEntries) {
                try {
                    entries.remove(entries.size() - 1);
                    launcherAppCache.remove(launcherAppCache.size() - 1);
                } catch (ArrayIndexOutOfBoundsException ignored) {}
            }

            // Determine if we need to reverse the order again
            if(TaskbarPosition.isVertical(context)) {
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

            int realNumOfSysTrayIcons = 0;
            for(Integer key : sysTrayIconStates.keySet()) {
                if(sysTrayIconStates.get(key))
                    realNumOfSysTrayIcons++;
            }

            if(finalApplicationIds.size() != currentTaskbarIds.size()
                    || numOfPinnedApps != realNumOfPinnedApps
                    || numOfSysTrayIcons != realNumOfSysTrayIcons)
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
                numOfSysTrayIcons = realNumOfSysTrayIcons;

                populateAppEntries(context, pm, entries, launcherAppCache);

                final int numOfEntries = Math.min(entries.size(), maxNumOfEntries);

                handler.post(() -> {
                    if(numOfEntries > 0 || fullLength) {
                        ViewGroup.LayoutParams params = scrollView.getLayoutParams();
                        calculateScrollViewParams(context, pref, params, fullLength, numOfEntries);
                        scrollView.setLayoutParams(params);

                        for(Integer key : sysTrayIconStates.keySet()) {
                            sysTrayLayout.findViewById(key).setVisibility(
                                    sysTrayIconStates.get(key) ? View.VISIBLE : View.GONE
                            );
                        }

                        taskbar.removeAllViews();
                        for(int i = 0; i < entries.size(); i++) {
                            taskbar.addView(getView(entries, i));
                        }

                        if(runningAppsOnly)
                            updateRunningAppIndicators(pinnedApps, usageStatsList, entries);

                        isShowingRecents = true;
                        if(shouldRefreshRecents && scrollView.getVisibility() != View.VISIBLE) {
                            if(firstRefresh)
                                scrollView.setVisibility(View.INVISIBLE);
                            else
                                scrollView.setVisibility(View.VISIBLE);
                        }

                        if(firstRefresh && scrollView.getVisibility() != View.VISIBLE) {
                            new Handler().post(
                                    () -> scrollTaskbar(
                                            scrollView,
                                            taskbar,
                                            TaskbarPosition.getTaskbarPosition(context),
                                            sortOrder,
                                            shouldRefreshRecents
                                    )
                            );
                        }
                    } else {
                        isShowingRecents = false;
                        scrollView.setVisibility(View.GONE);
                    }
                });
            } else if(runningAppsOnly)
                handler.post(() -> updateRunningAppIndicators(pinnedApps, usageStatsList, entries));
        } else if(firstRefresh || currentTaskbarIds.size() > 0) {
            currentTaskbarIds.clear();
            handler.post(() -> {
                isShowingRecents = false;
                scrollView.setVisibility(View.GONE);
            });
        }
    }

    @VisibleForTesting
    void calculateScrollViewParams(Context context,
                                   SharedPreferences pref,
                                   ViewGroup.LayoutParams params,
                                   boolean fullLength,
                                   int numOfEntries) {
        DisplayInfo display = U.getDisplayInfo(context, true);
        int recentsSize = context.getResources().getDimensionPixelSize(R.dimen.tb_icon_size) * numOfEntries;
        float maxRecentsSize = fullLength ? Float.MAX_VALUE : recentsSize;
        int maxScreenSize;

        float baseStart = U.getBaseTaskbarSizeStart(context);
        float baseEnd = U.getBaseTaskbarSizeEnd(context, sysTrayIconStates);
        int baseTotal = Math.round(baseStart + baseEnd);

        int diff = Math.round(Math.max(baseStart, baseEnd) - Math.min(baseStart, baseEnd));
        boolean startIsBigger = Math.max(baseStart, baseEnd) == baseStart;

        if(TaskbarPosition.isVertical(context)) {
            maxScreenSize = Math.max(0, display.height
                    - U.getStatusBarHeight(context)
                    - baseTotal);

            params.height = (int) Math.min(maxRecentsSize, maxScreenSize)
                    + context.getResources().getDimensionPixelSize(R.dimen.tb_divider_size);

            if(fullLength) {
                try {
                    Space whitespaceStart = layout.findViewById(R.id.whitespace_start);
                    Space whitespaceEnd = layout.findViewById(R.id.whitespace_end);
                    int height = maxScreenSize - recentsSize;

                    if(pref.getBoolean(PREF_CENTERED_ICONS, false)) {
                        int startHeight = (height / 2) + (diff / (startIsBigger ? -2 : 2));
                        int endHeight = (height / 2) + (diff / (startIsBigger ? 2 : -2));
                        
                        if(startHeight < 0) {
                            startHeight = 0;
                            endHeight = height;
                        }

                        if(endHeight < 0) {
                            startHeight = height;
                            endHeight = 0;
                        }
                        
                        ViewGroup.LayoutParams startParams = whitespaceStart.getLayoutParams();
                        startParams.height = startHeight;
                        whitespaceStart.setLayoutParams(startParams);

                        ViewGroup.LayoutParams endParams = whitespaceEnd.getLayoutParams();
                        endParams.height = endHeight;
                        whitespaceEnd.setLayoutParams(endParams);
                    } else {
                        ViewGroup.LayoutParams endParams = whitespaceEnd.getLayoutParams();
                        endParams.height = height;
                        whitespaceEnd.setLayoutParams(endParams);
                    }
                } catch (NullPointerException ignored) {}
            }
        } else {
            maxScreenSize = Math.max(0, display.width - baseTotal);

            params.width = (int) Math.min(maxRecentsSize, maxScreenSize)
                    + context.getResources().getDimensionPixelSize(R.dimen.tb_divider_size);

            if(fullLength) {
                try {
                    Space whitespaceStart = layout.findViewById(R.id.whitespace_start);
                    Space whitespaceEnd = layout.findViewById(R.id.whitespace_end);
                    int width = maxScreenSize - recentsSize;

                    if(pref.getBoolean(PREF_CENTERED_ICONS, false)) {
                        int startWidth = (width / 2) + (diff / (startIsBigger ? -2 : 2));
                        int endWidth = (width / 2) + (diff / (startIsBigger ? 2 : -2));

                        if(startWidth < 0) {
                            startWidth = 0;
                            endWidth = width;
                        }
                        
                        if(endWidth < 0) {
                            startWidth = width;
                            endWidth = 0;
                        }
                        
                        ViewGroup.LayoutParams startParams = whitespaceStart.getLayoutParams();
                        startParams.width = startWidth;
                        whitespaceStart.setLayoutParams(startParams);

                        ViewGroup.LayoutParams endParams = whitespaceEnd.getLayoutParams();
                        endParams.width = endWidth;
                        whitespaceEnd.setLayoutParams(endParams);
                    } else {
                        ViewGroup.LayoutParams endParams = whitespaceEnd.getLayoutParams();
                        endParams.width = width;
                        whitespaceEnd.setLayoutParams(endParams);
                    }
                } catch (NullPointerException ignored) {}
            }
        }

        boolean realMatchParent = maxRecentsSize >= maxScreenSize
                && pref.getBoolean(PREF_COLLAPSED, false)
                && !(TaskbarPosition.isVertical(context) && U.isChromeOs(context));

        if(realMatchParent != matchParent) {
            matchParent = realMatchParent;
            new Handler().post(updateParamsRunnable);
        }
    }

    @VisibleForTesting
    void scrollTaskbar(FrameLayout scrollView,
                       LinearLayout taskbar,
                       String taskbarPosition,
                       String sortOrder,
                       boolean shouldRefreshRecents) {
        if(TaskbarPosition.isVertical(taskbarPosition)) {
            if(sortOrder.contains("false")) {
                scrollView.scrollTo(taskbar.getWidth(), taskbar.getHeight());
            } else if(sortOrder.contains("true")) {
                scrollView.scrollTo(0, 0);
            }
        } else {
            if(sortOrder.contains("false")) {
                scrollView.scrollTo(0, 0);
            } else if(sortOrder.contains("true")) {
                scrollView.scrollTo(taskbar.getWidth(), taskbar.getHeight());
            }
        }

        if(shouldRefreshRecents) {
            scrollView.setVisibility(View.VISIBLE);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @VisibleForTesting
    void filterForegroundApp(Context context,
                             SharedPreferences pref,
                             long searchInterval,
                             List<String> applicationIdsToRemove) {
        if(pref.getBoolean(PREF_HIDE_FOREGROUND, false)) {
            UsageStatsManager mUsageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            UsageEvents events = mUsageStatsManager.queryEvents(searchInterval, System.currentTimeMillis());
            UsageEvents.Event eventCache = new UsageEvents.Event();
            String currentForegroundApp = null;

            while (events.hasNextEvent()) {
                events.getNextEvent(eventCache);

                if(eventCache.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    if(!(eventCache.getPackageName().contains(BuildConfig.BASE_APPLICATION_ID)
                            && !eventCache.getClassName().equals(MainActivity.class.getCanonicalName())
                            && !eventCache.getClassName().equals(HomeActivity.class.getCanonicalName())
                            && !eventCache.getClassName().equals(HomeActivityDelegate.class.getCanonicalName())
                            && !eventCache.getClassName().equals(SecondaryHomeActivity.class.getCanonicalName())
                            && !eventCache.getClassName().equals(InvisibleActivityFreeform.class.getCanonicalName()))) {
                        currentForegroundApp = eventCache.getPackageName();
                    }
                }
            }

            if(!applicationIdsToRemove.contains(currentForegroundApp)) {
                applicationIdsToRemove.add(currentForegroundApp);
            }
        }
    }

    @VisibleForTesting
    boolean needToReverseOrder(Context context, String sortOrder) {
        switch(TaskbarPosition.getTaskbarPosition(context)) {
            case POSITION_BOTTOM_RIGHT:
            case POSITION_TOP_RIGHT:
                return sortOrder.contains("false");
            default:
                return sortOrder.contains("true");
        }
    }

    @VisibleForTesting
    int filterRealPinnedApps(Context context,
                             List<AppEntry> pinnedApps,
                             List<AppEntry> entries,
                             List<String> applicationIdsToRemove) {
        int realNumOfPinnedApps = 0;
        if(pinnedApps.size() > 0) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized(pinnedApps) {
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
        }

        return realNumOfPinnedApps;
    }

    @VisibleForTesting
    void generateAppEntries(Context context,
                            int number,
                            List<AppEntry> usageStatsList6,
                            List<AppEntry> entries,
                            List<LauncherActivityInfo> launcherAppCache) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        LauncherApps launcherApps =
                (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

        final List<UserHandle> userHandles = userManager.getUserProfiles();

        final String googleSearchBoxPackage = "com.google.android.googlequicksearchbox";
        final String googleSearchBoxActivity =
                "com.google.android.googlequicksearchbox.SearchActivity";
        for(int i = 0; i < number; i++) {
            for(UserHandle handle : userHandles) {
                String packageName = usageStatsList6.get(i).getPackageName();
                long lastTimeUsed = usageStatsList6.get(i).getLastTimeUsed();
                List<LauncherActivityInfo> list = launcherApps.getActivityList(packageName, handle);
                if(!list.isEmpty()) {
                    // Google App workaround
                    if(!packageName.equals(googleSearchBoxPackage)) {
                        launcherAppCache.add(list.get(0));
                    } else {
                        boolean added = false;
                        for(LauncherActivityInfo info : list) {
                            if(info.getName().equals(googleSearchBoxActivity)) {
                                launcherAppCache.add(info);
                                added = true;
                            }
                        }

                        if(!added) {
                            launcherAppCache.add(list.get(0));
                        }
                    }

                    AppEntry newEntry = new AppEntry(packageName, null, null, null, false);

                    newEntry.setUserId(userManager.getSerialNumberForUser(handle));
                    newEntry.setLastTimeUsed(lastTimeUsed);
                    entries.add(newEntry);

                    break;
                }
            }
        }
    }

    @SuppressWarnings("SuspiciousListRemoveInLoop")
    @VisibleForTesting
    void populateAppEntries(Context context,
                            PackageManager pm,
                            List<AppEntry> entries,
                            List<LauncherActivityInfo> launcherAppCache) {
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
    }

    private void updateRunningAppIndicators(List<AppEntry> pinnedApps, List<AppEntry> usageStatsList, List<AppEntry> entries) {
        if(taskbar.getChildCount() != entries.size())
            return;

        List<String> pinnedPackageList = new ArrayList<>();
        List<String> runningPackageList = new ArrayList<>();

        for(AppEntry entry : pinnedApps)
            pinnedPackageList.add(entry.getPackageName());

        for(AppEntry entry : usageStatsList)
            runningPackageList.add(entry.getPackageName());

        for(int i = 0; i < taskbar.getChildCount(); i++) {
            View convertView = taskbar.getChildAt(i);
            String packageName = entries.get(i).getPackageName();

            ImageView runningAppIndicator = convertView.findViewById(R.id.running_app_indicator);
            if(pinnedPackageList.contains(packageName) && !runningPackageList.contains(packageName))
                runningAppIndicator.setVisibility(View.GONE);
            else {
                runningAppIndicator.setVisibility(View.VISIBLE);
                runningAppIndicator.setColorFilter(U.getAccentColor(context));
            }
        }
    }

    private void toggleTaskbar(boolean userInitiated) {
        if(userInitiated && Build.BRAND.equalsIgnoreCase("essential")) {
            SharedPreferences pref = U.getSharedPreferences(context);
            LauncherHelper helper = LauncherHelper.getInstance();

            if(!pref.getBoolean(PREF_GRIP_REJECTION_TOAST_SHOWN, false)
                    && !helper.isOnSecondaryHomeScreen(context)) {
                U.showToastLong(context, R.string.tb_essential_phone_grip_rejection);
                pref.edit().putBoolean(PREF_GRIP_REJECTION_TOAST_SHOWN, true).apply();
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

            if(sysTrayEnabled)
                sysTrayParentLayout.setVisibility(View.VISIBLE);

            shouldRefreshRecents = true;
            startRefreshingRecents();

            SharedPreferences pref = U.getSharedPreferences(context);
            pref.edit().putBoolean(PREF_COLLAPSED, true).apply();

            updateButton(false);

            new Handler().post(() -> U.sendBroadcast(context, ACTION_SHOW_START_MENU_SPACE));
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

            if(isShowingRecents)
                scrollView.setVisibility(View.GONE);

            if(sysTrayEnabled)
                sysTrayParentLayout.setVisibility(View.GONE);

            shouldRefreshRecents = false;
            if(thread != null) thread.interrupt();

            SharedPreferences pref = U.getSharedPreferences(context);
            pref.edit().putBoolean(PREF_COLLAPSED, false).apply();

            updateButton(true);

            if(clearVariables) {
                U.sendBroadcast(context, ACTION_HIDE_START_MENU);
                U.sendBroadcast(context, ACTION_HIDE_DASHBOARD);
            }

            if(matchParent) {
                matchParent = false;
                new Handler().post(updateParamsRunnable);
            }

            new Handler().post(() -> U.sendBroadcast(context, ACTION_HIDE_START_MENU_SPACE));
        }
    }

    private void tempShowTaskbar() {
        if(!taskbarHiddenTemporarily) {
            SharedPreferences pref = U.getSharedPreferences(context);
            if(!pref.getBoolean(PREF_COLLAPSED, false)) taskbarShownTemporarily = true;
        }

        showTaskbar(false);

        if(taskbarHiddenTemporarily)
            taskbarHiddenTemporarily = false;
    }

    private void tempHideTaskbar(boolean monitorPositionChanges) {
        if(!taskbarShownTemporarily) {
            SharedPreferences pref = U.getSharedPreferences(context);
            if(pref.getBoolean(PREF_COLLAPSED, false)) taskbarHiddenTemporarily = true;
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
            } catch (IllegalArgumentException ignored) {}

        SharedPreferences pref = U.getSharedPreferences(context);
        if(pref.getBoolean(PREF_SKIP_AUTO_HIDE_NAVBAR, false)) {
            pref.edit().remove(PREF_SKIP_AUTO_HIDE_NAVBAR).apply();
        } else if(pref.getBoolean(PREF_AUTO_HIDE_NAVBAR, false))
            U.showHideNavigationBar(context, true);

        U.unregisterReceiver(context, showReceiver);
        U.unregisterReceiver(context, hideReceiver);
        U.unregisterReceiver(context, tempShowReceiver);
        U.unregisterReceiver(context, tempHideReceiver);
        U.unregisterReceiver(context, startMenuAppearReceiver);
        U.unregisterReceiver(context, startMenuDisappearReceiver);

        if(sysTrayEnabled) {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            manager.listen(listener, PhoneStateListener.LISTEN_NONE);

            U.unregisterReceiver(context, notificationCountReceiver);
        }

        isFirstStart = true;
    }

    private void openContextMenu() {
        SharedPreferences pref = U.getSharedPreferences(context);

        Bundle args = new Bundle();
        args.putBoolean("dont_show_quit",
                LauncherHelper.getInstance().isOnHomeScreen(context)
                        && !pref.getBoolean(PREF_TASKBAR_ACTIVE, false));
        args.putBoolean("is_start_button", true);

        U.startContextMenuActivity(context, args);
    }

    private void updateButton(boolean isCollapsed) {
        SharedPreferences pref = U.getSharedPreferences(context);
        boolean hide = pref.getBoolean(PREF_INVISIBLE_BUTTON, false);

        if(button != null) button.setText(context.getString(isCollapsed ? R.string.tb_right_arrow : R.string.tb_left_arrow));
        if(layout != null) layout.setAlpha(isCollapsed && hide ? 0 : 1);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRecreateHost(UIHost host) {
        if(layout != null) {
            try {
                host.removeView(layout);
            } catch (IllegalArgumentException ignored) {}

            currentTaskbarPosition = 0;

            if(U.canDrawOverlays(context))
                drawTaskbar(host);
            else {
                SharedPreferences pref = U.getSharedPreferences(context);
                pref.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();

                host.terminate();
            }
        }
    }

    private View getView(List<AppEntry> list, int position) {
        View convertView = View.inflate(context, R.layout.tb_icon, null);

        final AppEntry entry = list.get(position);
        final SharedPreferences pref = U.getSharedPreferences(context);

        ImageView imageView = convertView.findViewById(R.id.icon);
        ImageView imageView2 = convertView.findViewById(R.id.shortcut_icon);
        imageView.setImageDrawable(entry.getIcon(context));
        imageView2.setBackgroundColor(U.getAccentColor(context));

        String taskbarPosition = TaskbarPosition.getTaskbarPosition(context);
        if(pref.getBoolean(PREF_SHORTCUT_ICON, true)) {
            boolean shouldShowShortcutIcon;
            if(taskbarPosition.contains("vertical"))
                shouldShowShortcutIcon = position >= list.size() - numOfPinnedApps;
            else
                shouldShowShortcutIcon = position < numOfPinnedApps;

            if(shouldShowShortcutIcon) imageView2.setVisibility(View.VISIBLE);
        }

        if(POSITION_BOTTOM_RIGHT.equals(taskbarPosition) || POSITION_TOP_RIGHT.equals(taskbarPosition)) {
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

            if(action == MotionEvent.ACTION_SCROLL && pref.getBoolean(PREF_VISUAL_FEEDBACK, true))
                view.setBackgroundColor(0);

            return false;
        });

        if(pref.getBoolean(PREF_VISUAL_FEEDBACK, true)) {
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
            return getAppEntriesUsingActivityManager(Integer.parseInt(pref.getString(PREF_MAX_NUM_OF_RECENTS, "10")));
        else
            return getAppEntriesUsingUsageStats();
    }

    @SuppressWarnings({"deprecation", "JavaReflectionMemberAccess"})
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

                U.allowReflection();
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

    @SuppressLint("SetTextI18n")
    private void updateSystemTray() {
        if(!sysTrayEnabled) return;

        handler.post(() -> {
            Map<Integer, Drawable> drawables = new HashMap<>();
            drawables.put(R.id.battery, getBatteryDrawable());
            drawables.put(R.id.wifi, getWifiDrawable());
            drawables.put(R.id.bluetooth, getBluetoothDrawable());
            drawables.put(R.id.cellular, getCellularDrawable());

            for(Integer key : drawables.keySet()) {
                ImageView view = sysTrayLayout.findViewById(key);
                Drawable drawable = drawables.get(key);

                if(drawable != null) view.setImageDrawable(drawable);
                sysTrayIconStates.put(key, drawable != null);
            }

            if(notificationCount > 0) {
                int color = ColorUtils.setAlphaComponent(U.getBackgroundTint(context), 255);
                notificationCountText.setTextColor(color);

                Drawable drawable = ContextCompat.getDrawable(context, R.drawable.tb_circle);
                drawable.setTint(U.getAccentColor(context));

                notificationCountCircle.setImageDrawable(drawable);
                notificationCountText.setText(Integer.toString(notificationCount));
                sysTrayIconStates.put(R.id.notification_count, true);
            } else
                sysTrayIconStates.put(R.id.notification_count, false);

            time.setText(context.getString(R.string.tb_systray_clock,
                    DateFormat.getTimeFormat(context).format(new Date()),
                    DateFormat.getDateFormat(context).format(new Date())));
            time.setTextColor(U.getAccentColor(context));
        });
    }

    @TargetApi(Build.VERSION_CODES.M)
    private Drawable getBatteryDrawable() {
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        if(batLevel == Integer.MIN_VALUE)
            return null;

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        String batDrawable;
        if(batLevel < 10 && !isCharging)
            batDrawable = "alert";
        else if(batLevel < 25)
            batDrawable = "20";
        else if(batLevel < 40)
            batDrawable = "30";
        else if(batLevel < 55)
            batDrawable = "50";
        else if(batLevel < 70)
            batDrawable = "60";
        else if(batLevel < 85)
            batDrawable = "80";
        else if(batLevel < 95)
            batDrawable = "90";
        else
            batDrawable = "full";

        String charging;
        if(isCharging)
            charging = "charging_";
        else
            charging = "";

        String batRes = "tb_battery_" + charging + batDrawable;
        int id = getResourceIdFor(batRes);

        return getDrawableForSysTray(id);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private Drawable getWifiDrawable() {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo ethernet = manager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        if(ethernet != null && ethernet.isConnected())
            return getDrawableForSysTray(R.drawable.tb_settings_ethernet);

        NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(wifi == null || !wifi.isConnected())
            return null;

        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int numberOfLevels = 5;

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numberOfLevels);

        String wifiRes = "tb_signal_wifi_" + level + "_bar";
        int id = getResourceIdFor(wifiRes);

        return getDrawableForSysTray(id);
    }

    private Drawable getBluetoothDrawable() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter != null && adapter.isEnabled())
            return getDrawableForSysTray(R.drawable.tb_bluetooth);

        return null;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private Drawable getCellularDrawable() {
        if(Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0)
            return getDrawableForSysTray(R.drawable.tb_airplanemode_active);

        if(cellStrength == -1)
            return null;

        String cellRes = "tb_signal_cellular_" + cellStrength + "_bar";
        int id = getResourceIdFor(cellRes);

        return getDrawableForSysTray(id);
    }

    private Drawable getDrawableForSysTray(int id) {
        Drawable drawable = null;
        try {
            drawable = ContextCompat.getDrawable(context, id);
        } catch (Resources.NotFoundException ignored) {}

        if(drawable == null) return null;

        drawable.setTint(U.getAccentColor(context));
        return drawable;
    }

    private int getResourceIdFor(String name) {
        String packageName = context.getResources().getResourcePackageName(R.drawable.tb_dummy);
        return context.getResources().getIdentifier(name, "drawable", packageName);
    }
}
