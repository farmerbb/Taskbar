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
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.SearchView;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.ContextMenuActivity;
import com.farmerbb.taskbar.activity.dark.ContextMenuActivityDark;
import com.farmerbb.taskbar.activity.InvisibleActivity;
import com.farmerbb.taskbar.activity.InvisibleActivityAlt;
import com.farmerbb.taskbar.adapter.StartMenuAdapter;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.Blacklist;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.LauncherHelper;
import com.farmerbb.taskbar.util.StartMenuHelper;
import com.farmerbb.taskbar.util.TopApps;
import com.farmerbb.taskbar.util.U;
import com.farmerbb.taskbar.widget.StartMenuLayout;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StartMenuService extends Service {

    private WindowManager windowManager;
    private StartMenuLayout layout;
    private GridView startMenu;
    private SearchView searchView;
    private TextView textView;
    private PackageManager pm;

    private Handler handler;
    private Thread thread;

    private boolean shouldShowSearchBox = false;
    private boolean hasSubmittedQuery = false;

    private int layoutId = R.layout.start_menu_left;

    private List<String> currentStartMenuIds = new ArrayList<>();

    private View.OnClickListener ocl = view -> toggleStartMenu(true);
    
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

    private Comparator<LauncherActivityInfo> comparator = (ai1, ai2) -> {
        String label1;
        String label2;

        try {
            label1 = ai1.getLabel().toString();
            label2 = ai2.getLabel().toString();
        } catch (OutOfMemoryError e) {
            System.gc();

            label1 = ai1.getApplicationInfo().packageName;
            label2 = ai2.getApplicationInfo().packageName;
        }

        return Collator.getInstance().compare(label1, label2);
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

        final SharedPreferences pref = U.getSharedPreferences(this);
        final boolean hasHardwareKeyboard = getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;

        switch(pref.getString("show_search_bar", "keyboard")) {
            case "always":
                shouldShowSearchBox = true;
                break;
            case "keyboard":
                shouldShowSearchBox = hasHardwareKeyboard;
                break;
            case "never":
                shouldShowSearchBox = false;
                break;
        }

        // Initialize layout params
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        U.setCachedRotation(windowManager.getDefaultDisplay().getRotation());

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                shouldShowSearchBox ? 0 : WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
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

        switch(pref.getString("theme", "light")) {
            case "light":
                theme = R.style.AppTheme;
                break;
            case "dark":
                theme = R.style.AppTheme_Dark;
                break;
        }

        ContextThemeWrapper wrapper = new ContextThemeWrapper(this, theme);
        layout = (StartMenuLayout) LayoutInflater.from(wrapper).inflate(layoutId, null);
        startMenu = (GridView) layout.findViewById(R.id.start_menu);

        if((shouldShowSearchBox && !hasHardwareKeyboard) || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1)
            layout.viewHandlesBackButton();

        boolean scrollbar = pref.getBoolean("scrollbar", false);
        startMenu.setFastScrollEnabled(scrollbar);
        startMenu.setFastScrollAlwaysVisible(scrollbar);
        startMenu.setScrollBarStyle(scrollbar ? View.SCROLLBARS_OUTSIDE_INSET : View.SCROLLBARS_INSIDE_OVERLAY);

        if(pref.getBoolean("transparent_start_menu", false))
            startMenu.setBackgroundColor(0);

        searchView = (SearchView) layout.findViewById(R.id.search);

        int backgroundTint = U.getBackgroundTint(this);

        FrameLayout startMenuFrame = (FrameLayout) layout.findViewById(R.id.start_menu_frame);
        FrameLayout searchViewLayout = (FrameLayout) layout.findViewById(R.id.search_view_layout);
        startMenuFrame.setBackgroundColor(backgroundTint);
        searchViewLayout.setBackgroundColor(backgroundTint);

        if(shouldShowSearchBox) {
            if(!hasHardwareKeyboard) searchView.setIconifiedByDefault(true);

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if(!hasSubmittedQuery) {
                        ListAdapter adapter = startMenu.getAdapter();
                        if(adapter != null) {
                            hasSubmittedQuery = true;

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
                    }
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    searchView.setIconified(false);
                    
                    View closeButton = searchView.findViewById(R.id.search_close_btn);
                    if(closeButton != null) closeButton.setVisibility(View.GONE);

                    refreshApps(newText, false);

                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                        new Handler().postDelayed(() -> {
                            EditText editText = (EditText) searchView.findViewById(R.id.search_src_text);
                            if(editText != null) {
                                editText.requestFocus();
                                editText.setSelection(editText.getText().length());
                            }
                        }, 50);
                    }

                    return true;
                }
            });

            searchView.setOnQueryTextFocusChangeListener((view, b) -> {
                if(!hasHardwareKeyboard) {
                    ViewGroup.LayoutParams params1 = startMenu.getLayoutParams();
                    params1.height = getResources().getDimensionPixelSize(
                            b && !U.isServiceRunning(this, "com.farmerbb.secondscreen.service.DisableKeyboardService")
                                    ? R.dimen.start_menu_height_half
                                    : R.dimen.start_menu_height);
                    startMenu.setLayoutParams(params1);
                }

                if(!b) {
                    if(hasHardwareKeyboard && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
                        LocalBroadcastManager.getInstance(StartMenuService.this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
                    else {
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
            });

            searchView.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);

            LinearLayout powerButton = (LinearLayout) layout.findViewById(R.id.power_button);
            powerButton.setOnClickListener(view -> {
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                openContextMenu(location);
            });

            powerButton.setOnGenericMotionListener((view, motionEvent) -> {
                if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                        && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    int[] location = new int[2];
                    view.getLocationOnScreen(location);
                    openContextMenu(location);
                }
                return false;
            });
            
            searchViewLayout.setOnClickListener(view -> searchView.setIconified(false));

            startMenu.setOnItemClickListener((parent, view, position, id) -> {
                hideStartMenu();

                AppEntry entry = (AppEntry) parent.getAdapter().getItem(position);
                U.launchApp(StartMenuService.this, entry.getPackageName(), entry.getComponentName(), entry.getUserId(StartMenuService.this), null, false, false);
            });

            if(pref.getBoolean("transparent_start_menu", false))
                layout.findViewById(R.id.search_view_child_layout).setBackgroundColor(0);
        } else
            searchViewLayout.setVisibility(View.GONE);
        
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
            @SuppressWarnings("Convert2streamapi")
            @Override
            public void run() {
                if(pm == null) pm = getPackageManager();

                UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
                LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);

                final List<UserHandle> userHandles = userManager.getUserProfiles();
                final List<LauncherActivityInfo> unfilteredList = new ArrayList<>();

                for(UserHandle handle : userHandles) {
                    unfilteredList.addAll(launcherApps.getActivityList(null, handle));
                }

                final List<LauncherActivityInfo> topAppsList = new ArrayList<>();
                final List<LauncherActivityInfo> allAppsList = new ArrayList<>();
                final List<LauncherActivityInfo> list = new ArrayList<>();

                TopApps topApps = TopApps.getInstance(StartMenuService.this);
                for(LauncherActivityInfo appInfo : unfilteredList) {
                    if(topApps.isTopApp(appInfo.getComponentName().flattenToString())
                            || topApps.isTopApp(appInfo.getName()))
                        topAppsList.add(appInfo);
                }

                Blacklist blacklist = Blacklist.getInstance(StartMenuService.this);
                for(LauncherActivityInfo appInfo : unfilteredList) {
                    if(!(blacklist.isBlocked(appInfo.getComponentName().flattenToString())
                            || blacklist.isBlocked(appInfo.getName()))
                            && !(topApps.isTopApp(appInfo.getComponentName().flattenToString())
                            || topApps.isTopApp(appInfo.getName())))
                        allAppsList.add(appInfo);
                }

                Collections.sort(topAppsList, comparator);
                Collections.sort(allAppsList, comparator);

                list.addAll(topAppsList);
                list.addAll(allAppsList);

                topAppsList.clear();
                allAppsList.clear();

                List<LauncherActivityInfo> queryList;
                if(query == null)
                    queryList = list;
                else {
                    queryList = new ArrayList<>();
                    for(LauncherActivityInfo appInfo : list) {
                        if(appInfo.getLabel().toString().toLowerCase().contains(query.toLowerCase()))
                            queryList.add(appInfo);
                    }
                }

                // Now that we've generated the list of apps,
                // we need to determine if we need to redraw the start menu or not
                boolean shouldRedrawStartMenu = false;
                List<String> finalApplicationIds = new ArrayList<>();

                if(query == null && !firstDraw) {
                    for(LauncherActivityInfo appInfo : queryList) {
                        finalApplicationIds.add(appInfo.getApplicationInfo().packageName);
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
                    for(LauncherActivityInfo appInfo : queryList) {

                        // Attempt to work around frequently reported OutOfMemoryErrors
                        String label;
                        Drawable icon;

                        try {
                            label = appInfo.getLabel().toString();
                            icon = IconCache.getInstance(StartMenuService.this).getIcon(StartMenuService.this, pm, appInfo);
                        } catch (OutOfMemoryError e) {
                            System.gc();

                            label = appInfo.getApplicationInfo().packageName;
                            icon = defaultIcon;
                        }

                        AppEntry newEntry = new AppEntry(
                                appInfo.getApplicationInfo().packageName,
                                new ComponentName(
                                        appInfo.getApplicationInfo().packageName,
                                        appInfo.getName()).flattenToString(),
                                label,
                                icon,
                                false);

                        newEntry.setUserId(userManager.getSerialNumberForUser(appInfo.getUser()));
                        entries.add(newEntry);
                    }

                    handler.post(() -> {
                        String queryText = searchView.getQuery().toString();
                        if(query == null && queryText.length() == 0
                                || query != null && query.equals(queryText)) {
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
    @TargetApi(Build.VERSION_CODES.N)
    private void showStartMenu(boolean shouldReset) {
        if(layout.getVisibility() == View.GONE) {
            if(shouldReset) startMenu.setSelection(0);

            layout.setOnClickListener(ocl);
            layout.setVisibility(View.VISIBLE);

            StartMenuHelper.getInstance().setStartMenuOpen(true);

            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.START_MENU_APPEARING"));

            boolean onHomeScreen = LauncherHelper.getInstance().isOnHomeScreen();
            boolean inFreeformMode = FreeformHackHelper.getInstance().isInFreeformWorkspace();

            if(!onHomeScreen || inFreeformMode) {
                Class clazz = inFreeformMode && !shouldShowSearchBox ? InvisibleActivityAlt.class : InvisibleActivity.class;
                Intent intent = new Intent(this, clazz);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                if(inFreeformMode) {
                    if(clazz.equals(InvisibleActivity.class))
                        U.launchAppLowerRight(this, intent);
                    else if(clazz.equals(InvisibleActivityAlt.class))
                        U.launchAppMaximized(this, intent);
                } else
                    startActivity(intent);
            }

            if(searchView.getVisibility() == View.VISIBLE) searchView.requestFocus();

            refreshApps(false);
        }
    }

    private void hideStartMenu() {
        if(layout.getVisibility() == View.VISIBLE) {
            layout.setOnClickListener(null);
            layout.setVisibility(View.INVISIBLE);

            StartMenuHelper.getInstance().setStartMenuOpen(false);

            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.START_MENU_DISAPPEARING"));

            layout.postDelayed(() -> {
                layout.setVisibility(View.GONE);
                searchView.setQuery(null, false);
                searchView.setIconified(true);
                hasSubmittedQuery = false;

                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(layout.getWindowToken(), 0);
            }, 250);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(layout != null)
            try {
                windowManager.removeView(layout);
            } catch (IllegalArgumentException e) { /* Gracefully fail */ }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(toggleReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(toggleReceiverAlt);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hideReceiver);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(layout != null) {
            try {
                windowManager.removeView(layout);
            } catch (IllegalArgumentException e) { /* Gracefully fail */ }

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))
                drawStartMenu();
            else {
                SharedPreferences pref = U.getSharedPreferences(this);
                pref.edit().putBoolean("taskbar_active", false).apply();

                stopSelf();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void openContextMenu(final int[] location) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));

        new Handler().postDelayed(() -> {
            SharedPreferences pref = U.getSharedPreferences(StartMenuService.this);
            Intent intent = null;

            switch(pref.getString("theme", "light")) {
                case "light":
                    intent = new Intent(StartMenuService.this, ContextMenuActivity.class);
                    break;
                case "dark":
                    intent = new Intent(StartMenuService.this, ContextMenuActivityDark.class);
                    break;
            }

            if(intent != null) {
                intent.putExtra("launched_from_start_menu", true);
                intent.putExtra("is_overflow_menu", true);
                intent.putExtra("x", location[0]);
                intent.putExtra("y", location[1]);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && FreeformHackHelper.getInstance().isInFreeformWorkspace()) {
                DisplayManager dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);
                Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);

                startActivity(intent, ActivityOptions.makeBasic().setLaunchBounds(new Rect(0, 0, display.getWidth(), display.getHeight())).toBundle());
            } else
                startActivity(intent);
        }, shouldDelay() ? 100 : 0);
    }

    private boolean shouldDelay() {
        SharedPreferences pref = U.getSharedPreferences(this);
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && pref.getBoolean("freeform_hack", false)
                && !FreeformHackHelper.getInstance().isFreeformHackActive();
    }
}
