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
import android.app.SearchManager;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.SearchView;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.InvisibleActivity;
import com.farmerbb.taskbar.adapter.StartMenuAdapter;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.Blacklist;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.LauncherHelper;
import com.farmerbb.taskbar.util.TopApps;
import com.farmerbb.taskbar.util.U;
import com.farmerbb.taskbar.view.TaskbarGridView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StartMenuService extends Service {

    private WindowManager windowManager;
    private LinearLayout layout;
    private TaskbarGridView startMenu;
    private SearchView searchView;
    private TextView textView;
    private PackageManager pm;

    private Handler handler;
    private Thread thread;

    private boolean hasSubmittedQuery = false;

    private int layoutId = R.layout.start_menu_left;

    private List<String> currentStartMenuIds = new ArrayList<>();

    private View.OnClickListener ocl = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            toggleStartMenu(true);
        }
    };
    
    private BroadcastReceiver toggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            toggleStartMenu(true);
        }
    };

    private BroadcastReceiver toggleReceiverAlt = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            toggleStartMenu(false);
        }
    };
    
    private BroadcastReceiver hideReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideStartMenu();
        }
    };

    private Comparator<ResolveInfo> comparator = new Comparator<ResolveInfo>() {
        @Override
        public int compare(ResolveInfo ai1, ResolveInfo ai2) {
            String label1;
            String label2;

            try {
                label1 = ai1.activityInfo.loadLabel(pm).toString();
                label2 = ai2.activityInfo.loadLabel(pm).toString();
            } catch (OutOfMemoryError e) {
                System.gc();

                label1 = ai1.activityInfo.packageName;
                label2 = ai2.activityInfo.packageName;
            }

            return Collator.getInstance().compare(label1, label2);
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
        if(pref.getBoolean("taskbar_active", false) || LauncherHelper.getInstance().isOnHomeScreen()) {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))
                drawStartMenu();
            else {
                pref.edit().putBoolean("taskbar_active", false).apply();

                stopSelf();
            }
        } else stopSelf();
    }

    @SuppressLint("RtlHardcoded")
    private void drawStartMenu() {
        IconCache.getInstance(this).clearCache();

        boolean shouldShowSearchBox = getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;

        int focusableFlag = shouldShowSearchBox
                ? WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                : WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;

        // Initialize layout params
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        U.setCachedRotation(windowManager.getDefaultDisplay().getRotation());

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                focusableFlag,
                PixelFormat.TRANSLUCENT);

        // Determine where to show the start menu on screen
        switch(U.getTaskbarPosition(this)) {
            case "bottom_left":
                layoutId = R.layout.start_menu_left;
                params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                break;
            case "bottom_vertical_left":
                layoutId = R.layout.start_menu_vertical_left;
                params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                break;
            case "bottom_right":
                layoutId = R.layout.start_menu_right;
                params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                break;
            case "bottom_vertical_right":
                layoutId = R.layout.start_menu_vertical_right;
                params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                break;
            case "top_left":
                layoutId = R.layout.start_menu_top_left;
                params.gravity = Gravity.TOP | Gravity.LEFT;
                break;
            case "top_vertical_left":
                layoutId = R.layout.start_menu_vertical_left;
                params.gravity = Gravity.TOP | Gravity.LEFT;
                break;
            case "top_right":
                layoutId = R.layout.start_menu_top_right;
                params.gravity = Gravity.TOP | Gravity.RIGHT;
                break;
            case "top_vertical_right":
                layoutId = R.layout.start_menu_vertical_right;
                params.gravity = Gravity.TOP | Gravity.RIGHT;
                break;
        }

        // Initialize views
        int theme = 0;

        final SharedPreferences pref = U.getSharedPreferences(this);
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
        startMenu = (TaskbarGridView) layout.findViewById(R.id.start_menu);

        boolean scrollbar = pref.getBoolean("scrollbar", false);
        startMenu.setFastScrollEnabled(scrollbar);
        startMenu.setFastScrollAlwaysVisible(scrollbar);
        startMenu.setScrollBarStyle(scrollbar ? View.SCROLLBARS_OUTSIDE_INSET : View.SCROLLBARS_INSIDE_OVERLAY);

        searchView = (SearchView) layout.findViewById(R.id.search);
        if(shouldShowSearchBox) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if(!hasSubmittedQuery) {
                        hasSubmittedQuery = true;

                        ListAdapter adapter = startMenu.getAdapter();
                        if(adapter.getCount() > 0) {
                            View view = adapter.getView(0, null, startMenu);
                            LinearLayout layout = (LinearLayout) view.findViewById(R.id.entry);
                            layout.performClick();
                        } else {
                            if(pref.getBoolean("hide_taskbar", true) && !FreeformHackHelper.getInstance().isInFreeformWorkspace())
                                LocalBroadcastManager.getInstance(StartMenuService.this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_TASKBAR"));
                            else
                                LocalBroadcastManager.getInstance(StartMenuService.this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));

                            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                            intent.putExtra(SearchManager.QUERY, query);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if(intent.resolveActivity(getPackageManager()) != null)
                                startActivity(intent);
                            else {
                                Uri uri = new Uri.Builder()
                                        .scheme("https")
                                        .authority("www.google.com")
                                        .path("search")
                                        .appendQueryParameter("q", query)
                                        .build();

                                intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                try {
                                    startActivity(intent);
                                } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
                            }
                        }
                    }
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    View closeButton = searchView.findViewById(R.id.search_close_btn);
                    if(closeButton != null) closeButton.setVisibility(View.GONE);

                    refreshApps(newText, false);
                    return true;
                }
            });

            searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if(!b) LocalBroadcastManager.getInstance(StartMenuService.this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
                }
            });

            searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);

            LinearLayout powerButton = (LinearLayout) layout.findViewById(R.id.power_button);
            powerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(pref.getBoolean("hide_taskbar", true) && !FreeformHackHelper.getInstance().isInFreeformWorkspace())
                        LocalBroadcastManager.getInstance(StartMenuService.this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_TASKBAR"));
                    else
                        LocalBroadcastManager.getInstance(StartMenuService.this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));

                    U.lockDevice(StartMenuService.this);
                }
            });
        } else {
            FrameLayout searchViewLayout = (FrameLayout) layout.findViewById(R.id.search_view_layout);
            searchViewLayout.setVisibility(View.GONE);
        }

        textView = (TextView) layout.findViewById(R.id.no_apps_found);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(toggleReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(toggleReceiverAlt);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hideReceiver);

        LocalBroadcastManager.getInstance(this).registerReceiver(toggleReceiver, new IntentFilter("com.farmerbb.taskbar.TOGGLE_START_MENU"));
        LocalBroadcastManager.getInstance(this).registerReceiver(toggleReceiverAlt, new IntentFilter("com.farmerbb.taskbar.TOGGLE_START_MENU_ALT"));
        LocalBroadcastManager.getInstance(this).registerReceiver(hideReceiver, new IntentFilter("com.farmerbb.taskbar.HIDE_START_MENU"));

        handler = new Handler();
        refreshApps(true);

        windowManager.addView(layout, params);
    }
    
    private void refreshApps(boolean firstDraw) {
        refreshApps(null, firstDraw);
    }

    private void refreshApps(final String query, final boolean firstDraw) {
        if(thread != null) thread.interrupt();

        handler = new Handler();
        thread = new Thread() {
            @Override
            public void run() {
                if(pm == null) pm = getPackageManager();

                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);

                final List<ResolveInfo> unfilteredList = pm.queryIntentActivities(intent, 0);
                final List<ResolveInfo> topAppsList = new ArrayList<>();
                final List<ResolveInfo> allAppsList = new ArrayList<>();
                final List<ResolveInfo> list = new ArrayList<>();

                TopApps topApps = TopApps.getInstance(StartMenuService.this);
                for(ResolveInfo appInfo : unfilteredList) {
                    if(topApps.isTopApp(appInfo.activityInfo.name))
                        topAppsList.add(appInfo);
                }

                Blacklist blacklist = Blacklist.getInstance(StartMenuService.this);
                for(ResolveInfo appInfo : unfilteredList) {
                    if(!blacklist.isBlocked(appInfo.activityInfo.name)
                            && !topApps.isTopApp(appInfo.activityInfo.name))
                        allAppsList.add(appInfo);
                }

                Collections.sort(topAppsList, comparator);
                Collections.sort(allAppsList, comparator);

                list.addAll(topAppsList);
                list.addAll(allAppsList);

                topAppsList.clear();
                allAppsList.clear();

                List<ResolveInfo> queryList;
                if(query == null)
                    queryList = list;
                else {
                    queryList = new ArrayList<>();
                    for(ResolveInfo appInfo : list) {
                        if(appInfo.loadLabel(pm).toString().toLowerCase().contains(query.toLowerCase()))
                            queryList.add(appInfo);
                    }
                }

                // Now that we've generated the list of apps,
                // we need to determine if we need to redraw the start menu or not
                boolean shouldRedrawStartMenu = false;
                List<String> finalApplicationIds = new ArrayList<>();

                if(query == null && !firstDraw) {
                    for(ResolveInfo appInfo : queryList) {
                        finalApplicationIds.add(appInfo.activityInfo.packageName);
                    }

                    if(finalApplicationIds.size() != currentStartMenuIds.size())
                        shouldRedrawStartMenu = true;
                    else {
                        for(int i = 0; i < finalApplicationIds.size(); i++) {
                            if(!finalApplicationIds.get(i).equals(currentStartMenuIds.get(i))) {
                                shouldRedrawStartMenu = true;
                                break;
                            }
                        }
                    }
                } else shouldRedrawStartMenu = true;

                if(shouldRedrawStartMenu) {
                    if(query == null) currentStartMenuIds = finalApplicationIds;

                    Drawable defaultIcon = pm.getDefaultActivityIcon();

                    final List<AppEntry> entries = new ArrayList<>();
                    for(ResolveInfo appInfo : queryList) {

                        // Attempt to work around frequently reported OutOfMemoryErrors
                        String label;
                        Drawable icon;

                        try {
                            label = appInfo.loadLabel(pm).toString();
                            icon = IconCache.getInstance(StartMenuService.this).getIcon(StartMenuService.this, pm, appInfo.activityInfo);
                        } catch (OutOfMemoryError e) {
                            System.gc();

                            label = appInfo.activityInfo.packageName;
                            icon = defaultIcon;
                        }

                        entries.add(new AppEntry(
                                appInfo.activityInfo.packageName,
                                new ComponentName(
                                        appInfo.activityInfo.packageName,
                                        appInfo.activityInfo.name).flattenToString(),
                                label,
                                icon,
                                false));
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            StartMenuAdapter adapter;
                            SharedPreferences pref = U.getSharedPreferences(StartMenuService.this);
                            if(pref.getString("start_menu_layout", "list").equals("grid")) {
                                startMenu.setNumColumns(3);
                                adapter = new StartMenuAdapter(StartMenuService.this, R.layout.row_alt, entries);
                            } else
                                adapter = new StartMenuAdapter(StartMenuService.this, R.layout.row, entries);

                            int position = startMenu.getFirstVisiblePosition();
                            startMenu.setAdapter(adapter);
                            startMenu.setSelection(position);

                            if(adapter.getCount() > 0)
                                textView.setText(null);
                            else if(query != null)
                                textView.setText(getString(R.string.press_enter));
                            else
                                textView.setText(getString(R.string.nothing_to_see_here));
                        }
                    });
                }
            }
        };

        thread.start();
    }
    
    private void toggleStartMenu(boolean shouldReset) {
        if(layout.getVisibility() == View.GONE)
            showStartMenu(shouldReset);
        else
            hideStartMenu();
    }

    @SuppressWarnings("deprecation")
    private void showStartMenu(boolean shouldReset) {
        if(shouldReset) startMenu.setSelection(0);

        layout.setOnClickListener(ocl);
        layout.setVisibility(View.VISIBLE);

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.START_MENU_APPEARING"));

        boolean onHomeScreen = LauncherHelper.getInstance().isOnHomeScreen();
        if(!onHomeScreen || FreeformHackHelper.getInstance().isInFreeformWorkspace()) {
            Intent intent = new Intent(this, InvisibleActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

            SharedPreferences pref = U.getSharedPreferences(this);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && pref.getBoolean("freeform_hack", false)) {
                DisplayManager dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);
                Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);

                startActivity(intent, ActivityOptions.makeBasic().setLaunchBounds(new Rect(display.getWidth(), display.getHeight(), display.getWidth() + 1, display.getHeight() + 1)).toBundle());
            } else
                startActivity(intent);
        }

        if(searchView.getVisibility() == View.VISIBLE) searchView.requestFocus();

        refreshApps(false);
    }

    private void hideStartMenu() {
        layout.setOnClickListener(null);
        layout.setVisibility(View.INVISIBLE);

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.START_MENU_DISAPPEARING"));

        layout.postDelayed(new Runnable() {
            @Override
            public void run() {
                layout.setVisibility(View.GONE);
                searchView.setQuery(null, false);
                hasSubmittedQuery = false;
            }
        }, 250);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(layout != null) windowManager.removeView(layout);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(toggleReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(toggleReceiverAlt);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hideReceiver);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(layout != null) {
            windowManager.removeView(layout);

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))
                drawStartMenu();
            else {
                SharedPreferences pref = U.getSharedPreferences(this);
                pref.edit().putBoolean("taskbar_active", false).apply();

                stopSelf();
            }
        }
    }
}
