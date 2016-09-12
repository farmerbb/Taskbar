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

package com.farmerbb.taskbar.service;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.support.v4.content.LocalBroadcastManager;
import android.widget.Space;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.MainActivity;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.ContextMenuActivity;
import com.farmerbb.taskbar.activity.ContextMenuActivityDark;
import com.farmerbb.taskbar.activity.InvisibleActivityFreeform;
import com.farmerbb.taskbar.adapter.TaskbarAdapter;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.PinnedBlockedApps;
import com.farmerbb.taskbar.util.U;
import com.farmerbb.taskbar.view.TaskbarGridView;

public class TaskbarService extends Service {

    private WindowManager windowManager;
    private LinearLayout layout;
    private ImageView startButton;
    private TaskbarGridView taskbar;
    private Button button;
    private View divider;
    private Space space;

    private Handler handler;
    private Thread thread;

    private boolean isShowingRecents = true;
    private boolean shouldRefreshRecents = true;
    private boolean taskbarShownTemporarily = false;
    private boolean isRefreshingRecents = false;
    private boolean isCollapsed = false;

    private int refreshInterval = -1;
    private long searchInterval = -1;
    private String sortOrder = "false";

    private int layoutId = R.layout.taskbar_left;

    private List<String> currentTaskbarIds = new ArrayList<>();
    private int numOfPinnedApps = -1;

    private View.OnClickListener ocl = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent("com.farmerbb.taskbar.TOGGLE_START_MENU");
            LocalBroadcastManager.getInstance(TaskbarService.this).sendBroadcast(intent);
        }
    };

    private BroadcastReceiver showReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showTaskbar();
        }
    };

    private BroadcastReceiver hideReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideTaskbar();
        }
    };

    private BroadcastReceiver tempShowReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(isCollapsed) {
                taskbarShownTemporarily = true;
                showTaskbar();
            }
        }
    };

    private BroadcastReceiver tempHideReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!isCollapsed && taskbarShownTemporarily) {
                taskbarShownTemporarily = false;
                hideTaskbar();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean("taskbar_active", false) || pref.getBoolean("on_home_screen", false)) {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))
                drawTaskbar();
            else {
                pref.edit().putBoolean("taskbar_active", false).apply();

                stopSelf();
            }
        } else stopSelf();
    }

    @SuppressLint("RtlHardcoded")
    private void drawTaskbar() {
        // Initialize layout params
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                PixelFormat.TRANSLUCENT);

        // Determine where to show the taskbar on screen
        SharedPreferences pref = U.getSharedPreferences(this);
        switch(pref.getString("position", "bottom_left")) {
            case "bottom_left":
                layoutId = R.layout.taskbar_left;
                params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                break;
            case "bottom_vertical_left":
                layoutId = R.layout.taskbar_vertical;
                params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                break;
            case "bottom_right":
                layoutId = R.layout.taskbar_right;
                params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                break;
            case "bottom_vertical_right":
                layoutId = R.layout.taskbar_vertical;
                params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                break;
        }

        // Initialize views
        int theme = 0;

        switch(pref.getString("theme", "light")) {
            case "light":
                theme = R.style.AppTheme;
                break;
            case "dark":
                theme = R.style.AppTheme_Dark;
                break;
        }

        ContextThemeWrapper wrapper = new ContextThemeWrapper(this, theme);
        layout = (LinearLayout) LayoutInflater.from(wrapper).inflate(layoutId, null);
        taskbar = (TaskbarGridView) layout.findViewById(R.id.taskbar);
        divider = layout.findViewById(R.id.divider);
        space = (Space) layout.findViewById(R.id.space);

        taskbar.setEnabled(false);

        startButton = (ImageView) layout.findViewById(R.id.start_button);
        startButton.setOnClickListener(ocl);
        startButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                openContextMenu();
                return true;
            }
        });

        startButton.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                        && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY)
                    openContextMenu();

                return false;
            }
        });

        refreshInterval = Integer.parseInt(pref.getString("refresh_frequency", "2")) * 1000;
        sortOrder = pref.getString("sort_order", "false");

        switch(pref.getString("recents_amount", "past_day")) {
            case "past_day":
                searchInterval = System.currentTimeMillis() - AlarmManager.INTERVAL_DAY;
                break;
            case "app_start":
                long oneDayAgo = System.currentTimeMillis() - AlarmManager.INTERVAL_DAY;
                long startTime = pref.getLong("time_of_service_start", System.currentTimeMillis());
                searchInterval = startTime > oneDayAgo ? startTime : oneDayAgo;
                break;
        }

        Intent intent = new Intent("com.farmerbb.taskbar.HIDE_START_MENU");
        LocalBroadcastManager.getInstance(TaskbarService.this).sendBroadcast(intent);

        button = (Button) layout.findViewById(R.id.hide_taskbar_button);
        updateButton(false);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTaskbar();
            }
        });

        LinearLayout buttonLayout = (LinearLayout) layout.findViewById(R.id.hide_taskbar_button_layout);
        if(buttonLayout != null) buttonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTaskbar();
            }
        });

        if(pref.getBoolean("show_background", true))
            layout.setBackgroundColor(ContextCompat.getColor(this, R.color.translucent_gray));

        if(!pref.getBoolean("collapsed", false) && pref.getBoolean("taskbar_active", false)) toggleTaskbar();

        LocalBroadcastManager.getInstance(this).registerReceiver(showReceiver, new IntentFilter("com.farmerbb.taskbar.SHOW_TASKBAR"));
        LocalBroadcastManager.getInstance(this).registerReceiver(hideReceiver, new IntentFilter("com.farmerbb.taskbar.HIDE_TASKBAR"));
        LocalBroadcastManager.getInstance(this).registerReceiver(tempShowReceiver, new IntentFilter("com.farmerbb.taskbar.TEMP_SHOW_TASKBAR"));
        LocalBroadcastManager.getInstance(this).registerReceiver(tempHideReceiver, new IntentFilter("com.farmerbb.taskbar.TEMP_HIDE_TASKBAR"));

        startRefreshingRecents();

        windowManager.addView(layout, params);
    }

    private void startRefreshingRecents() {
        if(thread != null) thread.interrupt();

        currentTaskbarIds.clear();

        handler = new Handler();
        thread = new Thread() {
            @Override
            public void run() {
                updateRecentApps(true);

                if(!isRefreshingRecents) {
                    isRefreshingRecents = true;
                    while(shouldRefreshRecents) {
                        SystemClock.sleep(refreshInterval);
                        updateRecentApps(false);
                    }

                    isRefreshingRecents = false;
                }
            }
        };

        thread.start();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private void updateRecentApps(boolean firstRefresh) {
        final PackageManager pm = getPackageManager();
        PinnedBlockedApps pba = PinnedBlockedApps.getInstance(this);
        final SharedPreferences pref = U.getSharedPreferences(this);
        final int MAX_NUM_OF_COLUMNS =
                pref.getString("position", "bottom_left").contains("vertical")
                        ? getResources().getInteger(R.integer.num_of_columns_vertical)
                        : getResources().getInteger(R.integer.num_of_columns);
        List<AppEntry> entries = new ArrayList<>();
        List<Intent> intentCache = new ArrayList<>();
        
        if(pba.getPinnedApps().size() > 0) {
            List<String> pinnedAppsToRemove = new ArrayList<>();

            for(AppEntry entry : pba.getPinnedApps()) {
                try {
                    pm.getPackageInfo(entry.getPackageName(), 0);
                    entries.add(entry);
                } catch (PackageManager.NameNotFoundException e) {
                    pinnedAppsToRemove.add(entry.getComponentName());
                }
            }

            for(String component : pinnedAppsToRemove) {
                pba.removePinnedApp(this, component);
            }
        }

        // Get list of all recently used apps
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        List<UsageStats> usageStatsList = pba.getPinnedApps().size() < MAX_NUM_OF_COLUMNS
                ? mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, searchInterval, System.currentTimeMillis())
                : new ArrayList<UsageStats>();

        if(usageStatsList.size() > 0 || pba.getPinnedApps().size() > 0) {
            if(pba.getPinnedApps().size() < MAX_NUM_OF_COLUMNS) {
                List<UsageStats> usageStatsList2 = new ArrayList<>();
                List<UsageStats> usageStatsList3 = new ArrayList<>();
                List<UsageStats> usageStatsList4 = new ArrayList<>();
                List<UsageStats> usageStatsList5 = new ArrayList<>();
                List<UsageStats> usageStatsList6;

                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                ResolveInfo defaultLauncher = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);

                // Filter out apps without a launcher intent
                // Also filter out the current launcher, and Taskbar itself
                for(UsageStats packageInfo : usageStatsList) {
                    if(pm.getLaunchIntentForPackage(packageInfo.getPackageName()) != null
                            && !packageInfo.getPackageName().equals(BuildConfig.APPLICATION_ID)
                            && !packageInfo.getPackageName().equals(defaultLauncher.activityInfo.packageName))
                        usageStatsList2.add(packageInfo);
                }

                // Filter out apps that don't fall within our current search interval
                for(UsageStats stats : usageStatsList2) {
                    if(stats.getLastTimeUsed() > searchInterval)
                        usageStatsList3.add(stats);
                }

                // Sort apps by either most recently used, or most time used
                if(sortOrder.contains("most_used")) {
                    Collections.sort(usageStatsList3, new Comparator<UsageStats>() {
                        @Override
                        public int compare(UsageStats us1, UsageStats us2) {
                            return Long.compare(us2.getTotalTimeInForeground(), us1.getTotalTimeInForeground());
                        }
                    });
                } else {
                    Collections.sort(usageStatsList3, new Comparator<UsageStats>() {
                        @Override
                        public int compare(UsageStats us1, UsageStats us2) {
                            return Long.compare(us2.getLastTimeUsed(), us1.getLastTimeUsed());
                        }
                    });
                }

                // Filter out any duplicate entries
                List<String> applicationIds = new ArrayList<>();
                for(UsageStats stats : usageStatsList3) {
                    if(!applicationIds.contains(stats.getPackageName())) {
                        usageStatsList4.add(stats);
                        applicationIds.add(stats.getPackageName());
                    }
                }

                // Filter out anything on the pinned/blocked apps lists
                List<String> applicationIdsToRemove = new ArrayList<>();

                for(AppEntry entry : pba.getPinnedApps()) {
                    applicationIdsToRemove.add(entry.getPackageName());
                }

                for(AppEntry entry : pba.getBlockedApps()) {
                    applicationIdsToRemove.add(entry.getPackageName());
                }

                // Filter out the currently running foreground app, if requested by the user
                if(pref.getBoolean("hide_foreground", false)) {
                    UsageEvents events = mUsageStatsManager.queryEvents(searchInterval, System.currentTimeMillis());
                    UsageEvents.Event eventCache = new UsageEvents.Event();
                    String currentForegroundApp = null;

                    while(events.hasNextEvent()) {
                        events.getNextEvent(eventCache);

                        if(eventCache.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                            if(!(eventCache.getPackageName().equals(BuildConfig.APPLICATION_ID)
                                    && !eventCache.getClassName().equals(MainActivity.class.getCanonicalName())
                                    && !eventCache.getClassName().equals(InvisibleActivityFreeform.class.getCanonicalName())))
                                currentForegroundApp = eventCache.getPackageName();
                        }
                    }

                    if(!applicationIdsToRemove.contains(currentForegroundApp))
                        applicationIdsToRemove.add(currentForegroundApp);
                }

                for(UsageStats stats : usageStatsList4) {
                    if(!applicationIdsToRemove.contains(stats.getPackageName())) {
                        usageStatsList5.add(stats);
                    }
                }

                // Truncate list to a maximum length
                if(usageStatsList5.size() > MAX_NUM_OF_COLUMNS)
                    usageStatsList6 = usageStatsList5.subList(0, MAX_NUM_OF_COLUMNS);
                else
                    usageStatsList6 = usageStatsList5;

                // Determine if we need to reverse the order
                boolean needToReverseOrder;
                switch(pref.getString("position", "bottom_left")) {
                    case "bottom_right":
                        needToReverseOrder = sortOrder.contains("false");
                        break;
                    default:
                        needToReverseOrder = sortOrder.contains("true");
                        break;
                }

                if(needToReverseOrder) {
                    Collections.reverse(usageStatsList6);
                }

                // Generate the AppEntries for TaskbarAdapter
                int number = usageStatsList6.size() == MAX_NUM_OF_COLUMNS
                        ? usageStatsList6.size() - pba.getPinnedApps().size()
                        : usageStatsList6.size();

                for(int i = 0; i < number; i++) {
                    Intent intent = pm.getLaunchIntentForPackage(usageStatsList6.get(i).getPackageName());
                    intentCache.add(intent);
                    entries.add(new AppEntry(usageStatsList6.get(i).getPackageName(), null, null, null, false));
                }
            }

            while(entries.size() > MAX_NUM_OF_COLUMNS) {
                try {
                    entries.remove(entries.size() - 1);
                    intentCache.remove(intentCache.size() - 1);
                } catch (ArrayIndexOutOfBoundsException e) { /* Gracefully fail */ }
            }

            // Determine if we need to reverse the order again
            if(pref.getString("position", "bottom_left").contains("vertical")) {
                Collections.reverse(entries);
                Collections.reverse(intentCache);
            }

            // Now that we've generated the list of apps,
            // we need to determine if we need to redraw the Taskbar or not
            boolean shouldRedrawTaskbar = firstRefresh;

            List<String> finalApplicationIds = new ArrayList<>();
            for(AppEntry entry : entries) {
                finalApplicationIds.add(entry.getPackageName());
            }

            if(finalApplicationIds.size() != currentTaskbarIds.size()
                    || numOfPinnedApps != pba.getPinnedApps().size())
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
                numOfPinnedApps = pba.getPinnedApps().size();

                int intentCachePos = -1;
                for(int i = 0; i < entries.size(); i++) {
                    if(entries.get(i).getComponentName() == null) {
                        intentCachePos++;
                        Intent intent = intentCache.get(intentCachePos);
                        String packageName = entries.get(i).getPackageName();

                        entries.remove(i);
                        entries.add(i, new AppEntry(
                            packageName,
                            intent.resolveActivity(pm).flattenToString(),
                            intent.resolveActivityInfo(pm, 0).loadLabel(pm).toString(),
                            intent.resolveActivityInfo(pm, 0).loadIcon(pm),
                            false));
                    }
                }

                final TaskbarAdapter taskbarAdapter = new TaskbarAdapter(this, R.layout.icon, entries, numOfPinnedApps);
                final int numOfEntries = entries.size();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(numOfEntries > 0) {
                            ViewGroup.LayoutParams params = taskbar.getLayoutParams();

                            if(pref.getString("position", "bottom_left").contains("vertical")) {
                                params.height = getResources().getDimensionPixelSize(R.dimen.icon_size) * numOfEntries;
                                taskbar.setNumColumns(1);
                            } else {
                                params.width = getResources().getDimensionPixelSize(R.dimen.icon_size) * numOfEntries;
                                taskbar.setNumColumns(numOfEntries);
                            }

                            taskbar.setLayoutParams(params);
                            taskbar.setAdapter(taskbarAdapter);

                            isShowingRecents = true;
                            if(shouldRefreshRecents) {
                                taskbar.setVisibility(View.VISIBLE);
                                divider.setVisibility(View.VISIBLE);
                                space.setVisibility(View.VISIBLE);
                            }
                        } else {
                            isShowingRecents = false;
                            taskbar.setVisibility(View.GONE);
                            divider.setVisibility(View.GONE);
                            space.setVisibility(View.GONE);
                        }
                    }
                });
            }
        } else if(firstRefresh || currentTaskbarIds.size() > 0) {
            currentTaskbarIds.clear();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    isShowingRecents = false;
                    taskbar.setVisibility(View.GONE);
                    divider.setVisibility(View.GONE);
                    space.setVisibility(View.GONE);
                }
            });
        }
    }

    private void toggleTaskbar() {
        if(startButton.getVisibility() == View.GONE)
            showTaskbar();
        else
            hideTaskbar();
    }

    private void showTaskbar() {
        startButton.setVisibility(View.VISIBLE);

        if(isShowingRecents) {
            if(taskbar.getVisibility() == View.GONE)
                taskbar.setVisibility(View.INVISIBLE);

            divider.setVisibility(View.VISIBLE);
            space.setVisibility(View.VISIBLE);
        }

        shouldRefreshRecents = true;
        startRefreshingRecents();

        SharedPreferences pref = U.getSharedPreferences(this);
        pref.edit().putBoolean("collapsed", true).apply();

        updateButton(false);
    }

    private void hideTaskbar() {
        startButton.setVisibility(View.GONE);

        if(isShowingRecents) {
            taskbar.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
            space.setVisibility(View.GONE);
        }

        shouldRefreshRecents = false;
        if(thread != null) thread.interrupt();

        SharedPreferences pref = U.getSharedPreferences(this);
        pref.edit().putBoolean("collapsed", false).apply();

        updateButton(true);

        Intent intent = new Intent("com.farmerbb.taskbar.HIDE_START_MENU");
        LocalBroadcastManager.getInstance(TaskbarService.this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        shouldRefreshRecents = false;

        super.onDestroy();
        if(layout != null) windowManager.removeView(layout);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(showReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hideReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(tempShowReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(tempHideReceiver);
    }

    @SuppressWarnings("deprecation")
    private void openContextMenu() {
        SharedPreferences pref = U.getSharedPreferences(this);
        Intent intent = null;

        switch(pref.getString("theme", "light")) {
            case "light":
                intent = new Intent(this, ContextMenuActivity.class);
                break;
            case "dark":
                intent = new Intent(this, ContextMenuActivityDark.class);
                break;
        }

        if(intent != null) {
            intent.putExtra("dont_show_quit", pref.getBoolean("on_home_screen", false) && !pref.getBoolean("taskbar_active", false));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && pref.getBoolean("freeform_hack", false)) {
            DisplayManager dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);
            Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);

            startActivity(intent, ActivityOptions.makeBasic().setLaunchBounds(new Rect(0, 0, display.getWidth(), display.getHeight())).toBundle());
        } else
            startActivity(intent);
    }

    private void updateButton(boolean isCollapsed) {
        this.isCollapsed = isCollapsed;

        SharedPreferences pref = U.getSharedPreferences(this);
        boolean hide = pref.getBoolean("invisible_button", false);

        if(button != null) button.setText(getString(isCollapsed ? R.string.right_arrow : R.string.left_arrow));
        if(layout != null) layout.setAlpha(isCollapsed && hide ? 0 : 1);
    }
}
