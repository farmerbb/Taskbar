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

package com.farmerbb.taskbar.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.appcompat.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.Display;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.TaskbarPosition;
import com.farmerbb.taskbar.service.DashboardService;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.ui.DashboardController;
import com.farmerbb.taskbar.ui.UIHost;
import com.farmerbb.taskbar.ui.ViewParams;
import com.farmerbb.taskbar.ui.StartMenuController;
import com.farmerbb.taskbar.ui.TaskbarController;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.DesktopIconInfo;
import com.farmerbb.taskbar.util.DisplayInfo;
import com.farmerbb.taskbar.util.FABWrapper;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.LauncherHelper;
import com.farmerbb.taskbar.util.MenuHelper;
import com.farmerbb.taskbar.util.U;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.farmerbb.taskbar.util.Constants.*;

public class HomeActivityDelegate extends AppCompatActivity implements UIHost {
    private TaskbarController taskbarController;
    private StartMenuController startMenuController;
    private DashboardController dashboardController;

    private FrameLayout layout;
    private GridLayout desktopIcons;
    private FABWrapper fab;

    private boolean forceTaskbarStart = false;
    private AlertDialog dialog;

    private WindowManager windowManager;
    private boolean shouldDelayFreeformHack;
    private int hits;

    private boolean isDesktopIconsEnabled;
    private boolean iconArrangeMode = false;
    private int startDragIndex;
    private int endDragIndex;

    private boolean isSecondaryHome;

    private GestureDetector detector;

    private BroadcastReceiver killReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            killHomeActivity();
        }
    };

    private BroadcastReceiver forceTaskbarStartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            forceTaskbarStart = true;
        }
    };

    private BroadcastReceiver restartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(taskbarController != null) taskbarController.onRecreateHost(HomeActivityDelegate.this);
            if(startMenuController != null) startMenuController.onRecreateHost(HomeActivityDelegate.this);
            if(dashboardController != null) dashboardController.onRecreateHost(HomeActivityDelegate.this);
        }
    };

    private BroadcastReceiver freeformToggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(isDesktopIconsEnabled == U.isDesktopIconsEnabled(HomeActivityDelegate.this))
                updateWindowFlags();
            else
                recreate();
        }
    };

    private BroadcastReceiver refreshDesktopIconsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshDesktopIcons();
        }
    };

    private BroadcastReceiver iconArrangeModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            enterIconArrangeMode();
        }
    };

    private BroadcastReceiver sortDesktopIconsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            sortDesktopIcons();
        }
    };

    private BroadcastReceiver updateMarginsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateMargins();
        }
    };

    private LauncherApps.Callback callback = new LauncherApps.Callback() {
        @Override
        public void onPackageRemoved(String packageName, UserHandle user) {
            refreshDesktopIcons();
        }

        @Override
        public void onPackageAdded(String packageName, UserHandle user) {
            refreshDesktopIcons();
        }

        @Override
        public void onPackageChanged(String packageName, UserHandle user) {
            refreshDesktopIcons();
        }

        @Override
        public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
            refreshDesktopIcons();
        }

        @Override
        public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
            refreshDesktopIcons();
        }
    };

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isSecondaryHome = this instanceof SecondaryHomeActivity;
        if(isSecondaryHome) {
            if(!U.isDesktopModeActive(this)) {
                finish();
                return;
            }

            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }

        shouldDelayFreeformHack = true;
        hits = 0;

        WindowManager.LayoutParams params = getWindow().getAttributes();
        if(U.applyDisplayCutoutModeTo(params))
            getWindow().setAttributes(params);

        SharedPreferences pref = U.getSharedPreferences(this);

        layout = new FrameLayout(this) {
            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();

                WallpaperManager wallpaperManager = (WallpaperManager) getSystemService(WALLPAPER_SERVICE);
                wallpaperManager.setWallpaperOffsets(getWindowToken(), 0.5f, 0.5f);

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    DisplayInfo display = U.getDisplayInfo(HomeActivityDelegate.this);
                    wallpaperManager.suggestDesiredDimensions(display.width, display.height);
                }

                boolean shouldStartFreeformHack = shouldDelayFreeformHack && hits > 0;
                shouldDelayFreeformHack = false;

                if(shouldStartFreeformHack)
                    startFreeformHack();
            }
        };

        isDesktopIconsEnabled = U.isDesktopIconsEnabled(this);
        if(isDesktopIconsEnabled) {
            layout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    if(savedInstanceState != null)
                        iconArrangeMode = savedInstanceState.getBoolean("icon_arrange_mode");

                    initDesktopIcons();
                }
            });
        } else {
            layout.setOnClickListener(
                    view1 -> U.sendBroadcast(this, ACTION_HIDE_START_MENU));

            layout.setOnLongClickListener(view2 -> {
                if(!pref.getBoolean("freeform_hack", false))
                    setWallpaper();

                return false;
            });

            layout.setOnGenericMotionListener((view3, motionEvent) -> {
                if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                        && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY
                        && !pref.getBoolean("freeform_hack", false))
                    setWallpaper();

                return false;
            });
        }

        layout.setFitsSystemWindows(true);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P 
                && isDesktopIconsEnabled
                && !U.isLibrary(this)) {
            detector = new GestureDetector(this, new GestureDetector.OnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return false;
                }

                @Override
                public void onShowPress(MotionEvent e) {}

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    return false;
                }

                @Override
                public void onLongPress(MotionEvent e) {}

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    return false;
                }

                @Override
                public boolean onDown(MotionEvent e) {
                    return false;
                }
            });

            detector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if(!pref.getBoolean("dont_show_double_tap_dialog", false)
                            && !isSecondaryHome) {
                        if(pref.getBoolean("double_tap_to_sleep", false)) {
                            U.lockDevice(HomeActivityDelegate.this);
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(U.wrapContext(HomeActivityDelegate.this));
                            builder.setTitle(R.string.tb_double_tap_to_sleep)
                                    .setMessage(R.string.tb_enable_double_tap_to_sleep)
                                    .setNegativeButton(pref.getBoolean("double_tap_dialog_shown", false)
                                            ? R.string.tb_action_dont_show_again
                                            : R.string.tb_action_cancel, (dialog, which) -> pref.edit().putBoolean(pref.getBoolean("double_tap_dialog_shown", false)
                                            ? "dont_show_double_tap_dialog"
                                            : "double_tap_dialog_shown", true).apply())
                                    .setPositiveButton(R.string.tb_action_ok, (dialog, which) -> {
                                        pref.edit().putBoolean("double_tap_to_sleep", true).apply();
                                        U.lockDevice(HomeActivityDelegate.this);
                                    });

                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    }

                    return false;
                }

                @Override
                public boolean onDoubleTapEvent(MotionEvent e) {
                    return false;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    return false;
                }

            });
        }

        if((this instanceof HomeActivity
                || isSecondaryHome
                || U.isLauncherPermanentlyEnabled(this))) {
            setContentView(layout);

            if(U.isChromeOs(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(U.wrapContext(this));
                    builder.setTitle(R.string.tb_permission_dialog_title)
                            .setMessage(R.string.tb_chrome_os_wallpaper)
                            .setNegativeButton(R.string.tb_action_cancel, null)
                            .setPositiveButton(R.string.tb_action_grant_permission, (dialog, which) ->
                                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 42));

                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else
                    showWallpaperOnChromeOs();
            }

            pref.edit()
                    .putBoolean("launcher", !isSecondaryHome)
                    .putBoolean("desktop_mode", U.isDesktopModeSupported(this) && isSecondaryHome)
                    .apply();
        } else
            killHomeActivity();

        updateWindowFlags();

        U.registerReceiver(this, killReceiver, ACTION_KILL_HOME_ACTIVITY);
        U.registerReceiver(this, forceTaskbarStartReceiver, ACTION_FORCE_TASKBAR_RESTART);

        U.registerReceiver(this, freeformToggleReceiver,
                ACTION_UPDATE_FREEFORM_CHECKBOX,
                ACTION_TOUCH_ABSORBER_STATE_CHANGED,
                ACTION_FREEFORM_PREF_CHANGED);

        if(isSecondaryHome) {
            U.registerReceiver(this, restartReceiver, ACTION_RESTART);
        }

        if(isDesktopIconsEnabled) {
            U.registerReceiver(this, refreshDesktopIconsReceiver, ACTION_REFRESH_DESKTOP_ICONS);
            U.registerReceiver(this, iconArrangeModeReceiver, ACTION_ENTER_ICON_ARRANGE_MODE);
            U.registerReceiver(this, sortDesktopIconsReceiver, ACTION_SORT_DESKTOP_ICONS);
            U.registerReceiver(this, updateMarginsReceiver, ACTION_UPDATE_HOME_SCREEN_MARGINS);

            LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
            launcherApps.registerCallback(callback);
        }

        U.initPrefs(this);
    }

    private void setWallpaper() {
        U.sendBroadcast(this, ACTION_TEMP_HIDE_TASKBAR);

        try {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER), getString(R.string.tb_set_wallpaper)));
        } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    protected void onResume() {
        super.onResume();

        if(U.canBootToFreeform(this)) {
            if(U.launcherIsDefault(this))
                startFreeformHack();
            else {
                U.showToastLong(this, R.string.tb_set_as_default_home);

                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    startActivity(homeIntent);
                    finish();
                } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
            }
        } else {
            overridePendingTransition(0, R.anim.close_anim);
            U.sendBroadcast(this, ACTION_TEMP_SHOW_TASKBAR);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        U.sendBroadcast(this, ACTION_HIDE_START_MENU);

        if(U.canDrawOverlays(this)) {
            if(!U.canBootToFreeform(this)) {
                setOnHomeScreen(true);

                if(forceTaskbarStart) {
                    forceTaskbarStart = false;
                    new Handler().postDelayed(() -> {
                        setOnHomeScreen(true);
                        startTaskbar();
                    }, 250);
                } else
                    startTaskbar();
            } else if(U.launcherIsDefault(this))
                startFreeformHack();
        } else
            dialog = U.showPermissionDialog(U.wrapContext(this),
                    () -> dialog = U.showErrorDialog(U.wrapContext(this), "SYSTEM_ALERT_WINDOW"),
                    null);
    }

    private void startTaskbar() {
        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean("first_run", true)) {
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("first_run", false);
            editor.putBoolean("collapsed", true);
            editor.apply();

            dialog = U.showRecentAppsDialog(U.wrapContext(this),
                    () -> dialog = U.showErrorDialog(U.wrapContext(this), "GET_USAGE_STATS"),
                    null);
        }

        if(isSecondaryHome) {
            // Stop any currently running services and switch to using HomeActivityDelegate as UI host
            stopService(new Intent(this, TaskbarService.class));
            stopService(new Intent(this, StartMenuService.class));
            stopService(new Intent(this, DashboardService.class));

            taskbarController = new TaskbarController(this);
            startMenuController = new StartMenuController(this);
            dashboardController = new DashboardController(this);

            taskbarController.onCreateHost(this);
            startMenuController.onCreateHost(this);
            dashboardController.onCreateHost(this);
        } else {
            // We always start the Taskbar and Start Menu services, even if the app isn't normally running
            try {
                startService(new Intent(this, TaskbarService.class));
                startService(new Intent(this, StartMenuService.class));
                startService(new Intent(this, DashboardService.class));
            } catch (IllegalStateException e) { /* Gracefully fail */ }
        }

        if(pref.getBoolean("taskbar_active", false) && !U.isServiceRunning(this, NotificationService.class))
            pref.edit().putBoolean("taskbar_active", false).apply();

        // Show the Taskbar temporarily, as nothing else will be visible on screen
        new Handler().postDelayed(() ->
                U.sendBroadcast(this, ACTION_TEMP_SHOW_TASKBAR), 100);
    }

    private void startFreeformHack() {
        if(shouldDelayFreeformHack)
            hits++;
        else
            U.startFreeformHack(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences pref = U.getSharedPreferences(this);
        if(!U.canBootToFreeform(this)) {
            setOnHomeScreen(false);

            if(U.shouldCollapse(this, false)) {
                U.sendBroadcast(this, ACTION_TEMP_HIDE_TASKBAR);
            }

            if(isSecondaryHome) {
                if(taskbarController != null) taskbarController.onDestroyHost(this);
                if(startMenuController != null) startMenuController.onDestroyHost(this);
                if(dashboardController != null) dashboardController.onDestroyHost(this);

                IconCache.getInstance(this).clearCache();

                // Stop using HomeActivityDelegate as UI host and restart services if needed
                if(pref.getBoolean("taskbar_active", false) && !pref.getBoolean("is_hidden", false)) {
                    startService(new Intent(this, TaskbarService.class));
                    startService(new Intent(this, StartMenuService.class));
                    startService(new Intent(this, DashboardService.class));
                }
            } else {
                // Stop the Taskbar and Start Menu services if they should normally not be active
                if(!pref.getBoolean("taskbar_active", false) || pref.getBoolean("is_hidden", false)) {
                    stopService(new Intent(this, TaskbarService.class));
                    stopService(new Intent(this, StartMenuService.class));

                    if(!pref.getBoolean("dont_stop_dashboard", false))
                        stopService(new Intent(this, DashboardService.class));

                    IconCache.getInstance(this).clearCache();
                }
            }
        }

        if(dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        U.unregisterReceiver(this, killReceiver);
        U.unregisterReceiver(this, forceTaskbarStartReceiver);
        U.unregisterReceiver(this, freeformToggleReceiver);

        if(isSecondaryHome)
            U.unregisterReceiver(this, restartReceiver);

        if(isDesktopIconsEnabled) {
            U.unregisterReceiver(this, refreshDesktopIconsReceiver);
            U.unregisterReceiver(this, iconArrangeModeReceiver);
            U.unregisterReceiver(this, sortDesktopIconsReceiver);
            U.unregisterReceiver(this, updateMarginsReceiver);

            LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
            launcherApps.unregisterCallback(callback);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("icon_arrange_mode", iconArrangeMode);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        U.sendBroadcast(this, ACTION_HIDE_START_MENU);
    }

    private void killHomeActivity() {
        setOnHomeScreen(false);

        if(isSecondaryHome) {
            if(taskbarController != null) taskbarController.onDestroyHost(this);
            if(startMenuController != null) startMenuController.onDestroyHost(this);
            if(dashboardController != null) dashboardController.onDestroyHost(this);

            IconCache.getInstance(this).clearCache();

            U.stopFreeformHack(this);

            // Stop using HomeActivityDelegate as UI host and restart services if needed
            SharedPreferences pref = U.getSharedPreferences(this);
            if(pref.getBoolean("taskbar_active", false) && !pref.getBoolean("is_hidden", false)) {
                startService(new Intent(this, TaskbarService.class));
                startService(new Intent(this, StartMenuService.class));
                startService(new Intent(this, DashboardService.class));
            }
        } else {
            // Stop the Taskbar and Start Menu services if they should normally not be active
            SharedPreferences pref = U.getSharedPreferences(this);
            if(!pref.getBoolean("taskbar_active", false) || pref.getBoolean("is_hidden", false)) {
                stopService(new Intent(this, TaskbarService.class));
                stopService(new Intent(this, StartMenuService.class));
                stopService(new Intent(this, DashboardService.class));

                IconCache.getInstance(this).clearCache();

                U.stopFreeformHack(this);
            }
        }

        finish();
    }

    private void updateWindowFlags() {
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if(FreeformHackHelper.getInstance().isTouchAbsorberActive() && U.isOverridingFreeformHack(this))
            getWindow().setFlags(flags, flags);
        else
            getWindow().clearFlags(flags);
    }

    @Override
    public void addView(View view, ViewParams params) {
        windowManager.addView(view, params.toWindowManagerParams());
    }

    @Override
    public void removeView(View view) {
        windowManager.removeView(view);
    }

    @Override
    public void terminate() {
        // no-op
    }

    private void initDesktopIcons() {
        desktopIcons = new GridLayout(this);
        fab = new FABWrapper(this);

        updateMargins();
        refreshDesktopIcons();

        fab.setImageResource(R.drawable.tb_done);
        fab.view.setOnClickListener(v -> {
            iconArrangeMode = false;
            fab.hide();
            refreshDesktopIcons();
        });

        if(!iconArrangeMode) fab.hide();

        layout.addView(desktopIcons, 0);
        layout.addView(fab.view, 1);
    }

    private void refreshDesktopIcons() {
        if(desktopIcons == null) return;

        boolean taskbarIsVertical = TaskbarPosition.isVertical(this);
        int iconSize = getResources().getDimensionPixelSize(R.dimen.tb_icon_size);
        int desktopIconSize = getResources().getDimensionPixelSize(R.dimen.tb_start_menu_grid_width);

        int columns = (layout.getWidth() - (taskbarIsVertical ? iconSize : 0)) / desktopIconSize;
        int rows = (layout.getHeight() - (!taskbarIsVertical ? iconSize : 0)) / desktopIconSize;

        desktopIcons.removeAllViews();
        desktopIcons.setOrientation(GridLayout.VERTICAL);
        desktopIcons.setColumnCount(columns);
        desktopIcons.setRowCount(rows);

        LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        UserManager userManager = (UserManager) getSystemService(USER_SERVICE);

        SparseArray<DesktopIconInfo> icons = new SparseArray<>();
        List<Integer> iconsToRemove = new ArrayList<>();

        try {
            SharedPreferences pref = U.getSharedPreferences(this);
            JSONArray jsonIcons = new JSONArray(pref.getString("desktop_icons", "[]"));

            for(int i = 0; i < jsonIcons.length(); i++) {
                DesktopIconInfo info = DesktopIconInfo.fromJson(jsonIcons.getJSONObject(i));
                if(info != null) {
                    if(launcherApps.isActivityEnabled(
                            ComponentName.unflattenFromString(info.entry.getComponentName()),
                            userManager.getUserForSerialNumber(info.entry.getUserId(this))))
                        icons.put(getIndex(info), info);
                    else
                        iconsToRemove.add(i);
                }
            }

            if(!iconsToRemove.isEmpty()) {
                for(int i : iconsToRemove) {
                    jsonIcons.remove(i);
                }

                pref.edit().putString("desktop_icons", jsonIcons.toString()).apply();
            }
        } catch (JSONException e) { /* Gracefully fail */ }

        for(int i = 0; i < columns * rows; i++) {
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f));

            params.width = 0;
            params.height = 0;

            FrameLayout iconContainer = new FrameLayout(this);
            iconContainer.setLayoutParams(params);
            iconContainer.setOnDragListener(new DesktopIconDragListener());

            int index = i;

            iconContainer.setOnClickListener(view -> {
                boolean isStartMenuOpen = MenuHelper.getInstance().isStartMenuOpen();
                U.sendBroadcast(this, ACTION_HIDE_START_MENU);

                DesktopIconInfo info = icons.get(index);
                if(!isStartMenuOpen && info != null && info.entry != null) {
                    U.launchApp(
                            this,
                            info.entry,
                            null,
                            false,
                            false,
                            view
                    );
                }
            });

            iconContainer.setOnLongClickListener(view -> {
                int[] location = new int[2];
                view.getLocationOnScreen(location);

                DesktopIconInfo info = icons.get(index);
                if(info == null) info = getDesktopIconInfo(index);

                openContextMenu(info, location);
                return true;
            });

            iconContainer.setOnGenericMotionListener((view, motionEvent) -> {
                int action = motionEvent.getAction();

                if(action == MotionEvent.ACTION_BUTTON_PRESS
                        && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    int[] location = new int[2];
                    view.getLocationOnScreen(location);

                    DesktopIconInfo info = icons.get(index);
                    if(info == null) info = getDesktopIconInfo(index);

                    openContextMenu(info, location);
                }

                return false;
            });

            iconContainer.setOnTouchListener((v, event) -> {
                if(detector != null)
                    detector.onTouchEvent(event);

                return false;
            });

            DesktopIconInfo info = icons.get(index);
            if(info != null && info.entry != null && info.column < columns && info.row < rows)
                iconContainer.addView(inflateDesktopIcon(iconContainer, info.entry));

            desktopIcons.addView(iconContainer);
        }
    }

    private void sortDesktopIcons() {
        try {
            SharedPreferences pref = U.getSharedPreferences(this);
            JSONArray jsonIcons = new JSONArray(pref.getString("desktop_icons", "[]"));

            if(jsonIcons.length() == 0) {
                U.showToast(this, R.string.tb_no_icons_to_sort);
                return;
            }

            List<DesktopIconInfo> icons = new ArrayList<>();

            for(int i = 0; i < jsonIcons.length(); i++) {
                DesktopIconInfo info = DesktopIconInfo.fromJson(jsonIcons.getJSONObject(i));
                if(info != null)
                    icons.add(info);
            }

            Collections.sort(icons, (o1, o2) -> Collator.getInstance().compare(o1.entry.getLabel(), o2.entry.getLabel()));

            jsonIcons = new JSONArray();

            for(int i = 0; i < icons.size(); i++) {
                DesktopIconInfo oldInfo = icons.get(i);
                DesktopIconInfo newInfo = getDesktopIconInfo(i);

                oldInfo.column = newInfo.column;
                oldInfo.row = newInfo.row;

                jsonIcons.put(oldInfo.toJson(this));
            }

            pref.edit().putString("desktop_icons", jsonIcons.toString()).apply();
            refreshDesktopIcons();
        } catch (JSONException e) { /* Gracefully fail */ }
    }

    private void reassignDroppedIcon() {
        if(startDragIndex == endDragIndex) return;

        try {
            SharedPreferences pref = U.getSharedPreferences(this);
            JSONArray jsonIcons = new JSONArray(pref.getString("desktop_icons", "[]"));
            int iconToRemove = -1;

            DesktopIconInfo oldInfo = getDesktopIconInfo(startDragIndex);
            DesktopIconInfo newInfo = getDesktopIconInfo(endDragIndex);

            for(int i = 0; i < jsonIcons.length(); i++) {
                DesktopIconInfo info = DesktopIconInfo.fromJson(jsonIcons.getJSONObject(i));
                if(info != null && info.column == oldInfo.column && info.row == oldInfo.row) {
                    newInfo.entry = info.entry;
                    iconToRemove = i;
                    break;
                }
            }

            if(iconToRemove > -1) {
                jsonIcons.remove(iconToRemove);
                jsonIcons.put(newInfo.toJson(this));

                pref.edit().putString("desktop_icons", jsonIcons.toString()).apply();
            }
        } catch (JSONException e) { /* Gracefully fail */ }
    }

    private void enterIconArrangeMode() {
        try {
            SharedPreferences pref = U.getSharedPreferences(this);
            JSONArray jsonIcons = new JSONArray(pref.getString("desktop_icons", "[]"));

            if(jsonIcons.length() == 0) {
                U.showToast(this, R.string.tb_no_icons_to_arrange);
                return;
            }

            fab.view.setBackgroundTintList(
                    ColorStateList.valueOf(ColorUtils.setAlphaComponent(U.getAccentColor(this), 255)));

            iconArrangeMode = true;
            fab.show();
        } catch (JSONException e) { /* Gracefully fail */ }
    }

    private void updateMargins() {
        if(desktopIcons == null || fab == null) return;

        String position = TaskbarPosition.getTaskbarPosition(this);
        int iconSize = getResources().getDimensionPixelSize(R.dimen.tb_icon_size);

        int left = 0;
        int top = 0;
        int right = 0;
        int bottom = 0;

        if (TaskbarPosition.isVerticalLeft(position)) {
            left = iconSize;
        } else if (TaskbarPosition.isVerticalRight(position)) {
            right = iconSize;
        } else if (TaskbarPosition.isBottom(position)) {
            bottom = iconSize;
        } else {
            top = iconSize;
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        params.setMargins(left, top, right, bottom);
        desktopIcons.setLayoutParams(params);

        int fabMargin = getResources().getDimensionPixelSize(R.dimen.tb_desktop_icon_fab_margin);
        left += fabMargin;
        top += fabMargin;
        right += fabMargin;
        bottom += fabMargin;

        FrameLayout.LayoutParams params2 = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );

        params2.gravity = Gravity.BOTTOM | Gravity.END;
        params2.setMargins(left, top, right, bottom);
        fab.view.setLayoutParams(params2);
    }

    private int getIndex(DesktopIconInfo info) {
        return (info.column * desktopIcons.getRowCount()) + info.row;
    }

    private DesktopIconInfo getDesktopIconInfo(int index) {
        int row = index % desktopIcons.getRowCount();

        int pos = index;
        int column = -1;

        while(pos >= 0) {
            pos -= desktopIcons.getRowCount();
            column++;
        }

        return new DesktopIconInfo(column, row, null);
    }

    private View inflateDesktopIcon(ViewGroup parent, AppEntry entry) {
        SharedPreferences pref = U.getSharedPreferences(this);
        View icon = LayoutInflater.from(this).inflate(R.layout.tb_row_alt, parent, false);

        TextView textView = icon.findViewById(R.id.name);
        textView.setText(pref.getBoolean("hide_icon_labels", false) ? "" : entry.getLabel());
        textView.setTextColor(ContextCompat.getColor(this, R.color.tb_desktop_icon_text));
        textView.setShadowLayer(10, 0, 0, R.color.tb_desktop_icon_shadow);

        ImageView imageView = icon.findViewById(R.id.icon);
        imageView.setImageDrawable(entry.getIcon(this));

        icon.setOnTouchListener(new DesktopIconTouchListener());
        return icon;
    }

    private void openContextMenu(final DesktopIconInfo info, final int[] location) {
        if(iconArrangeMode) return;

        Bundle args = new Bundle();
        args.putSerializable("app_entry", info.entry);
        args.putSerializable("desktop_icon", info);
        args.putInt("x", location[0]);
        args.putInt("y", location[1]);

        U.startContextMenuActivity(this, args);
    }

    private final class DesktopIconTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if(iconArrangeMode && motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                startDragIndex = desktopIcons.indexOfChild((ViewGroup) view.getParent());

                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                view.startDrag(data, shadowBuilder, view, 0);
                view.setVisibility(View.INVISIBLE);
                return true;
            } else
                return false;
        }
    }

    private final class DesktopIconDragListener implements View.OnDragListener {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch(event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                default:
                    // do nothing
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    FrameLayout container = (FrameLayout) v;
                    if(container.getChildCount() == 0
                            || startDragIndex == desktopIcons.indexOfChild(container)) {
                        v.setBackgroundColor(U.getAccentColor(HomeActivityDelegate.this));
                        v.setAlpha(0.5f);
                    }
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    View view = (View) event.getLocalState();
                    view.setVisibility(View.VISIBLE);
                    // fall through
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackground(null);
                    v.setAlpha(1);
                    break;
                case DragEvent.ACTION_DROP:
                    FrameLayout container2 = (FrameLayout) v;
                    if(container2.getChildCount() == 0) {
                        // Dropped, reassign View to ViewGroup
                        View view2 = (View) event.getLocalState();
                        ViewGroup owner = (ViewGroup) view2.getParent();
                        owner.removeView(view2);
                        container2.addView(view2);

                        endDragIndex = desktopIcons.indexOfChild(container2);
                        reassignDroppedIcon();
                    }
                    break;
            }
            return true;
        }
    }

    private void setOnHomeScreen(boolean value) {
        LauncherHelper helper = LauncherHelper.getInstance();

        if(isSecondaryHome) {
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display disp = wm.getDefaultDisplay();

            helper.setOnSecondaryHomeScreen(value, disp.getDisplayId());
        } else
            helper.setOnPrimaryHomeScreen(value);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            showWallpaperOnChromeOs();
    }

    private void showWallpaperOnChromeOs() {
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        layout.setBackground(wallpaperDrawable);
    }

    @Override
    public void onTopResumedActivityChanged(boolean isTopResumedActivity) {
        if(isTopResumedActivity)
            overridePendingTransition(0, R.anim.close_anim);
    }
}
