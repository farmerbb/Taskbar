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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.SparseArray;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
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
import com.farmerbb.taskbar.util.CompatUtils;
import com.farmerbb.taskbar.util.DesktopIconInfo;
import com.farmerbb.taskbar.util.DisplayInfo;
import com.farmerbb.taskbar.util.FeatureFlags;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.LauncherHelper;
import com.farmerbb.taskbar.util.U;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HomeActivityDelegate extends Activity implements UIHost {
    private TaskbarController taskbarController;
    private StartMenuController startMenuController;
    private DashboardController dashboardController;

    private FrameLayout layout;
    private GridLayout desktopIcons;

    private boolean forceTaskbarStart = false;
    private AlertDialog dialog;

    private boolean shouldDelayFreeformHack;
    private boolean shouldInitDesktopIcons;
    private int hits;

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
            updateWindowFlags();
        }
    };

    private BroadcastReceiver refreshDesktopIconsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshDesktopIcons();
        }
    };

    private BroadcastReceiver arrangeDesktopIconsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            arrangeDesktopIcons();
        }
    };

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shouldDelayFreeformHack = true;
        shouldInitDesktopIcons = true;
        hits = 0;

        WindowManager.LayoutParams params = getWindow().getAttributes();
        if(CompatUtils.applyDisplayCutoutModeTo(params))
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

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);

                if(shouldInitDesktopIcons && FeatureFlags.desktopIcons(HomeActivityDelegate.this)) {
                    initDesktopIcons();
                    shouldInitDesktopIcons = false;
                }
            }
        };

        if(!FeatureFlags.desktopIcons(this)) {
            layout.setOnClickListener(view1 -> LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU")));

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

        if((this instanceof HomeActivity || U.isLauncherPermanentlyEnabled(this))
                && !U.isChromeOs(this)) {
            setContentView(layout);
            pref.edit().putBoolean("launcher", true).apply();
        } else
            killHomeActivity();

        updateWindowFlags();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(killReceiver, new IntentFilter("com.farmerbb.taskbar.KILL_HOME_ACTIVITY"));
        lbm.registerReceiver(forceTaskbarStartReceiver, new IntentFilter("com.farmerbb.taskbar.FORCE_TASKBAR_RESTART"));

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.farmerbb.taskbar.UPDATE_FREEFORM_CHECKBOX");
        intentFilter.addAction("com.farmerbb.taskbar.TOUCH_ABSORBER_STATE_CHANGED");

        lbm.registerReceiver(freeformToggleReceiver, intentFilter);

        if(FeatureFlags.homeActivityUIHost())
            lbm.registerReceiver(restartReceiver, new IntentFilter("com.farmerbb.taskbar.RESTART"));

        if(FeatureFlags.desktopIcons(this)) {
            lbm.registerReceiver(refreshDesktopIconsReceiver, new IntentFilter("com.farmerbb.taskbar.REFRESH_DESKTOP_ICONS"));
            lbm.registerReceiver(arrangeDesktopIconsReceiver, new IntentFilter("com.farmerbb.taskbar.ARRANGE_DESKTOP_ICONS"));
        }

        U.initPrefs(this);
    }

    private void setWallpaper() {
        if(U.shouldCollapse(this, true))
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.TEMP_HIDE_TASKBAR"));
        else
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));

        try {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER), getString(R.string.set_wallpaper)));
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
                U.showToastLong(this, R.string.set_as_default_home);

                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    startActivity(homeIntent);
                    finish();
                } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
            }
        } else {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.TEMP_SHOW_TASKBAR"));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));

        if(U.canDrawOverlays(this, true)) {
            if(!U.canBootToFreeform(this)) {
                final LauncherHelper helper = LauncherHelper.getInstance();
                helper.setOnHomeScreen(true);

                if(forceTaskbarStart) {
                    forceTaskbarStart = false;
                    new Handler().postDelayed(() -> {
                        helper.setOnHomeScreen(true);
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

        if(FeatureFlags.homeActivityUIHost()) {
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
            startService(new Intent(this, TaskbarService.class));
            startService(new Intent(this, StartMenuService.class));
            startService(new Intent(this, DashboardService.class));
        }

        if(pref.getBoolean("taskbar_active", false) && !U.isServiceRunning(this, NotificationService.class))
            pref.edit().putBoolean("taskbar_active", false).apply();

        // Show the Taskbar temporarily, as nothing else will be visible on screen
        new Handler().postDelayed(() -> LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.TEMP_SHOW_TASKBAR")), 100);
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
            LauncherHelper.getInstance().setOnHomeScreen(false);

            if(U.shouldCollapse(this, true))
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.TEMP_HIDE_TASKBAR"));
            else
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));

            if(FeatureFlags.homeActivityUIHost()) {
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

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(killReceiver);
        lbm.unregisterReceiver(forceTaskbarStartReceiver);
        lbm.unregisterReceiver(freeformToggleReceiver);

        if(FeatureFlags.homeActivityUIHost())
            lbm.unregisterReceiver(restartReceiver);

        if(FeatureFlags.desktopIcons(this)) {
            lbm.unregisterReceiver(refreshDesktopIconsReceiver);
            lbm.unregisterReceiver(arrangeDesktopIconsReceiver);
        }
    }

    @Override
    public void onBackPressed() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
    }

    private void killHomeActivity() {
        LauncherHelper.getInstance().setOnHomeScreen(false);

        if(FeatureFlags.homeActivityUIHost()) {
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
        final FrameLayout.LayoutParams flParams = new FrameLayout.LayoutParams(
                params.width,
                params.height
        );

        if(params.gravity > -1)
            flParams.gravity = params.gravity;

        view.setLayoutParams(flParams);
        layout.addView(view);
    }

    @Override
    public void removeView(View view) {
        layout.removeView(view);
    }

    @Override
    public void terminate() {
        // no-op
    }

    private void initDesktopIcons() {
        desktopIcons = new GridLayout(this);
        refreshDesktopIcons();

        layout.addView(desktopIcons, 0);
        layout.invalidate();
    }

    private void refreshDesktopIcons() {
        int desktopIconSize = getResources().getDimensionPixelSize(R.dimen.start_menu_grid_width);

        int columns = layout.getWidth() / desktopIconSize;
        int rows = layout.getHeight() / desktopIconSize;

        desktopIcons.removeAllViews();
        desktopIcons.setOrientation(LinearLayout.VERTICAL);
        desktopIcons.setColumnCount(columns);
        desktopIcons.setRowCount(rows);

        SparseArray<DesktopIconInfo> icons = new SparseArray<>();

        try {
            SharedPreferences pref = U.getSharedPreferences(this);
            JSONArray jsonIcons = new JSONArray(pref.getString("desktop_icons", "[]"));

            for(int i = 0; i < jsonIcons.length(); i++) {
                DesktopIconInfo info = DesktopIconInfo.fromJson(jsonIcons.getJSONObject(i));
                if(info != null)
                    icons.put(getIndex(info), info);
            }
        } catch (JSONException e) { /* Gracefully fail */ }

        for(int i = 0; i < columns * rows; i++) {
            FrameLayout iconContainer = new FrameLayout(this);
            iconContainer.setLayoutParams(new GridLayout.LayoutParams(new ViewGroup.LayoutParams(desktopIconSize, desktopIconSize)));
            iconContainer.setOnDragListener(new DesktopIconDragListener());

            int index = i;

            iconContainer.setOnLongClickListener(view -> {
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                openContextMenu(null, location, getDesktopIconInfo(index));
                return true;
            });

            iconContainer.setOnGenericMotionListener((view, motionEvent) -> {
                int action = motionEvent.getAction();

                if(action == MotionEvent.ACTION_BUTTON_PRESS
                        && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    int[] location = new int[2];
                    view.getLocationOnScreen(location);
                    openContextMenu(null, location, getDesktopIconInfo(index));
                }

                return false;
            });

            DesktopIconInfo info = icons.get(index);
            if(info != null && info.entry != null)
                iconContainer.addView(inflateDesktopIcon(iconContainer, info.entry));

            desktopIcons.addView(iconContainer);
        }
    }

    private void arrangeDesktopIcons() {
        try {
            SharedPreferences pref = U.getSharedPreferences(this);
            JSONArray jsonIcons = new JSONArray(pref.getString("desktop_icons", "[]"));
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
        View icon = LayoutInflater.from(this).inflate(R.layout.row_alt, parent, false);

        TextView textView = icon.findViewById(R.id.name);
        textView.setText(entry.getLabel());
        textView.setTextColor(ContextCompat.getColor(this, R.color.desktop_icon_text));
        textView.setShadowLayer(50, 0, 0, R.color.desktop_icon_shadow);

        ImageView imageView = icon.findViewById(R.id.icon);
        imageView.setImageDrawable(entry.getIcon(this));

        LinearLayout layout = icon.findViewById(R.id.entry);
        layout.setOnClickListener(view -> U.launchApp(
                this,
                entry.getPackageName(),
                entry.getComponentName(),
                entry.getUserId(this),
                null,
                false,
                false
        ));

        icon.setOnTouchListener(new DesktopIconTouchListener());
        return icon;
    }

    private void openContextMenu(final AppEntry entry, final int[] location, DesktopIconInfo info) {
        Bundle args = new Bundle();

        if(entry != null) {
            args.putString("package_name", entry.getPackageName());
            args.putString("app_name", entry.getLabel());
            args.putString("component_name", entry.getComponentName());
            args.putLong("user_id", entry.getUserId(this));
        }

        args.putSerializable("desktop_icon", info);
        args.putInt("x", location[0]);
        args.putInt("y", location[1]);

        U.startContextMenuActivity(this, args);
    }

    private final class DesktopIconTouchListener implements View.OnTouchListener {
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                view.startDrag(data, shadowBuilder, view, 0);
                view.setVisibility(View.INVISIBLE);
                return true;
            } else {
                return false;
            }
        }
    }

    class DesktopIconDragListener implements View.OnDragListener {

        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // do nothing
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    Drawable dropTarget = ContextCompat.getDrawable(HomeActivityDelegate.this, R.drawable.drop_target);
                    if(dropTarget != null) {
                        dropTarget.setColorFilter(U.getAccentColor(HomeActivityDelegate.this), PorterDuff.Mode.DST_IN);
                        v.setBackground(dropTarget);
                    }
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                case DragEvent.ACTION_DRAG_ENDED:
                    v.setBackground(null);
                    break;
                case DragEvent.ACTION_DROP:
                    // Dropped, reassign View to ViewGroup
                    View view = (View) event.getLocalState();
                    ViewGroup owner = (ViewGroup) view.getParent();
                    owner.removeView(view);
                    FrameLayout container = (FrameLayout) v;
                    container.addView(view);
                    view.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
            return true;
        }
    }
}
