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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.SearchView;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.InvisibleActivity;
import com.farmerbb.taskbar.activity.InvisibleActivityAlt;
import com.farmerbb.taskbar.adapter.StartMenuAdapter;
import com.farmerbb.taskbar.util.TaskbarPosition;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.Blacklist;
import com.farmerbb.taskbar.helper.FreeformHackHelper;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.helper.LauncherHelper;
import com.farmerbb.taskbar.helper.MenuHelper;
import com.farmerbb.taskbar.util.TopApps;
import com.farmerbb.taskbar.util.U;
import com.farmerbb.taskbar.widget.StartMenuLayout;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.farmerbb.taskbar.util.Constants.*;

public class StartMenuController extends UIController {

    private StartMenuLayout layout;
    private GridView startMenu;
    private SearchView searchView;
    private TextView textView;
    private PackageManager pm;
    private StartMenuAdapter adapter;

    private Handler handler;
    private Thread thread;

    private boolean hasSubmittedQuery = false;
    private boolean hasHardwareKeyboard = false;
    private boolean searchViewClicked = false;

    private List<String> currentStartMenuIds = new ArrayList<>();

    private final View.OnClickListener ocl = view -> toggleStartMenu();

    private final BroadcastReceiver toggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            toggleStartMenu();
        }
    };

    private final BroadcastReceiver showSpaceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            layout.findViewById(R.id.start_menu_space).setVisibility(View.VISIBLE);
        }
    };

    private final BroadcastReceiver hideSpaceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            layout.findViewById(R.id.start_menu_space).setVisibility(View.GONE);
        }
    };

    private final BroadcastReceiver hideReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideStartMenu(true);
        }
    };

    private final BroadcastReceiver hideReceiverNoReset = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideStartMenu(false);
        }
    };

    private final BroadcastReceiver resetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startMenu.setSelection(0);
        }
    };

    private final Comparator<LauncherActivityInfo> comparator = (ai1, ai2) -> {
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

    public StartMenuController(Context context) {
        super(context);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreateHost(UIHost host) {
        hasHardwareKeyboard = context.getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;

        init(context, host, () -> drawStartMenu(host));
    }

    private void drawStartMenu(UIHost host) {
        IconCache.getInstance(context).clearCache();

        final SharedPreferences pref = U.getSharedPreferences(context);
        boolean shouldShowSearchBox = shouldShowSearchBox(pref, hasHardwareKeyboard);

        // Initialize layout params
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        TaskbarPosition.setCachedRotation(windowManager.getDefaultDisplay().getRotation());

        final ViewParams params = new ViewParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                -1,
                shouldShowSearchBox ? 0 : WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                getBottomMargin(context)
        );

        // Determine where to show the start menu on screen
        String taskbarPosition = TaskbarPosition.getTaskbarPosition(context);
        int layoutId = getStartMenuLayoutId(taskbarPosition);
        params.gravity = getStartMenuGravity(taskbarPosition);

        // Initialize views
        layout = (StartMenuLayout) LayoutInflater.from(U.wrapContext(context)).inflate(layoutId, null);
        layout.setAlpha(0);

        startMenu = layout.findViewById(R.id.start_menu);

        if((shouldShowSearchBox && !hasHardwareKeyboard) || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1)
            layout.viewHandlesBackButton();

        boolean scrollbar = pref.getBoolean(PREF_SCROLLBAR, false);
        startMenu.setFastScrollEnabled(scrollbar);
        startMenu.setFastScrollAlwaysVisible(scrollbar);
        startMenu.setScrollBarStyle(scrollbar ? View.SCROLLBARS_OUTSIDE_INSET : View.SCROLLBARS_INSIDE_OVERLAY);

        if(pref.getBoolean(PREF_TRANSPARENT_START_MENU, false))
            startMenu.setBackgroundColor(0);

        if(pref.getBoolean(PREF_VISUAL_FEEDBACK, true))
            startMenu.setRecyclerListener(view -> view.setBackgroundColor(0));

        int columns = context.getResources().getInteger(R.integer.tb_start_menu_columns);
        boolean isGrid = pref.getString(PREF_START_MENU_LAYOUT, "grid").equals("grid");

        if(isGrid) {
            ViewGroup.LayoutParams startMenuParams = startMenu.getLayoutParams();
            startMenuParams.width = (int) (startMenuParams.width * (columns / 3f));
            startMenu.setLayoutParams(startMenuParams);
        }

        searchView = layout.findViewById(R.id.search);
        searchViewClicked = false;

        int backgroundTint = U.getBackgroundTint(context);

        FrameLayout startMenuFrame = layout.findViewById(R.id.start_menu_frame);
        FrameLayout searchViewLayout = layout.findViewById(R.id.search_view_layout);
        startMenuFrame.setBackgroundColor(backgroundTint);
        searchViewLayout.setBackgroundColor(backgroundTint);

        if(shouldShowSearchBox) {
            if(!hasHardwareKeyboard) searchView.setIconifiedByDefault(true);

            searchView.setOnTouchListener((v, event) -> {
                searchViewClicked = true;
                return false;
            });

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if(!hasSubmittedQuery) {
                        ListAdapter adapter = startMenu.getAdapter();
                        if(adapter != null) {
                            hasSubmittedQuery = true;

                            if(adapter.getCount() > 0) {
                                View view = adapter.getView(0, null, startMenu);
                                LinearLayout layout = view.findViewById(R.id.entry);
                                layout.performClick();
                            } else {
                                if(U.shouldCollapse(context, true)) {
                                    U.sendBroadcast(context, ACTION_HIDE_TASKBAR);
                                } else {
                                    hideStartMenu(true);
                                }

                                Intent intent = generateQueryWebSearchIntent(query);
                                if(intent.resolveActivity(context.getPackageManager()) != null) {
                                    context.startActivity(intent);
                                } else {
                                    intent = generateQueryGoogleIntent(query);
                                    try {
                                        context.startActivity(intent);
                                    } catch (ActivityNotFoundException ignored) {}
                                }
                            }
                        }
                    }
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    searchView.setIconified(false);

                    View closeButton = searchView.findViewById(com.google.android.material.R.id.search_close_btn);
                    if(closeButton != null) closeButton.setVisibility(View.GONE);

                    refreshApps(newText, false);

                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                        U.newHandler().postDelayed(() -> {
                            EditText editText = searchView.findViewById(com.google.android.material.R.id.search_src_text);
                            if(editText != null) {
                                editText.requestFocus();
                                editText.setSelection(editText.getText().length());
                            }
                        }, 50);
                    }

                    return true;
                }
            });

            searchView.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);

            LinearLayout powerButton = layout.findViewById(R.id.power_button);
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

            startMenu.setOnItemClickListener((viewParent, view, position, id) -> {
                hideStartMenu(true);

                AppEntry entry = (AppEntry) viewParent.getAdapter().getItem(position);
                U.launchApp(context, entry, null, false, false, view);
            });

            View childLayout = layout.findViewById(R.id.search_view_child_layout);
            if(pref.getBoolean(PREF_TRANSPARENT_START_MENU, false))
                childLayout.setBackgroundColor(0);

            if(isGrid) {
                ViewGroup.LayoutParams childLayoutParams = childLayout.getLayoutParams();
                childLayoutParams.width = (int) (childLayoutParams.width * (columns / 3f));
                childLayout.setLayoutParams(childLayoutParams);
            }
        } else
            searchViewLayout.setVisibility(View.GONE);

        applyMarginFix(host, layout, params);

        textView = layout.findViewById(R.id.no_apps_found);

        U.registerReceiver(context, toggleReceiver, ACTION_TOGGLE_START_MENU);
        U.registerReceiver(context, hideReceiver, ACTION_HIDE_START_MENU);
        U.registerReceiver(context, hideReceiverNoReset, ACTION_HIDE_START_MENU_NO_RESET);
        U.registerReceiver(context, showSpaceReceiver, ACTION_SHOW_START_MENU_SPACE);
        U.registerReceiver(context, hideSpaceReceiver, ACTION_HIDE_START_MENU_SPACE);
        U.registerReceiver(context, resetReceiver, ACTION_RESET_START_MENU);

        handler = U.newHandler();
        refreshApps(true);

        host.addView(layout, params);
    }

    @VisibleForTesting
    boolean shouldShowSearchBox(SharedPreferences pref, boolean hasHardwareKeyboard) {
        boolean shouldShowSearchBox;
        switch(pref.getString(PREF_SHOW_SEARCH_BAR, "always")) {
            case "always":
                shouldShowSearchBox = true;
                break;
            case "keyboard":
                shouldShowSearchBox = hasHardwareKeyboard;
                break;
            default:
                shouldShowSearchBox = false;
                break;
        }
        return shouldShowSearchBox;
    }

    @VisibleForTesting
    int getStartMenuLayoutId(String taskbarPosition) {
        switch(taskbarPosition) {
            case POSITION_BOTTOM_LEFT:
            default:
                return R.layout.tb_start_menu_left;
            case POSITION_BOTTOM_RIGHT:
                return R.layout.tb_start_menu_right;
            case POSITION_TOP_LEFT:
                return R.layout.tb_start_menu_top_left;
            case POSITION_TOP_VERTICAL_LEFT:
            case POSITION_BOTTOM_VERTICAL_LEFT:
                return R.layout.tb_start_menu_vertical_left;
            case POSITION_TOP_RIGHT:
                return R.layout.tb_start_menu_top_right;
            case POSITION_TOP_VERTICAL_RIGHT:
            case POSITION_BOTTOM_VERTICAL_RIGHT:
                return R.layout.tb_start_menu_vertical_right;
        }
    }

    @VisibleForTesting
    @SuppressLint("RtlHardcoded")
    int getStartMenuGravity(String taskbarPosition) {
        switch(taskbarPosition) {
            case POSITION_BOTTOM_LEFT:
            case POSITION_BOTTOM_VERTICAL_LEFT:
            default:
                return Gravity.BOTTOM | Gravity.LEFT;
            case POSITION_BOTTOM_RIGHT:
            case POSITION_BOTTOM_VERTICAL_RIGHT:
                return Gravity.BOTTOM | Gravity.RIGHT;
            case POSITION_TOP_LEFT:
            case POSITION_TOP_VERTICAL_LEFT:
                return Gravity.TOP | Gravity.LEFT;
            case POSITION_TOP_RIGHT:
            case POSITION_TOP_VERTICAL_RIGHT:
                return Gravity.TOP | Gravity.RIGHT;
        }
    }

    @VisibleForTesting
    Intent generateQueryWebSearchIntent(String query) {
        Intent intent;
        if(Patterns.WEB_URL.matcher(query).matches()) {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(URLUtil.guessUrl(query)));
        } else {
            intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra(SearchManager.QUERY, query);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @VisibleForTesting
    Intent generateQueryGoogleIntent(String query) {
        Intent intent;
        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority("www.google.com")
                .path("search")
                .appendQueryParameter("q", query)
                .build();

        intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private void refreshApps(boolean firstDraw) {
        refreshApps(null, firstDraw);
    }

    private void refreshApps(final String query, final boolean firstDraw) {
        if(thread != null) thread.interrupt();

        handler = U.newHandler();
        thread = new Thread(() -> {
            if(pm == null) pm = context.getPackageManager();

            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

            final List<UserHandle> userHandles = userManager.getUserProfiles();
            final List<LauncherActivityInfo> unfilteredList = new ArrayList<>();

            for(UserHandle handle : userHandles) {
                unfilteredList.addAll(launcherApps.getActivityList(null, handle));
            }

            final List<LauncherActivityInfo> topAppsList = new ArrayList<>();
            final List<LauncherActivityInfo> allAppsList = new ArrayList<>();
            final List<LauncherActivityInfo> list = new ArrayList<>();

            TopApps topApps = TopApps.getInstance(context);
            for(LauncherActivityInfo appInfo : unfilteredList) {
                String userSuffix = ":" + userManager.getSerialNumberForUser(appInfo.getUser());
                if(topApps.isTopApp(appInfo.getComponentName().flattenToString() + userSuffix)
                        || topApps.isTopApp(appInfo.getComponentName().flattenToString())
                        || topApps.isTopApp(appInfo.getName()))
                    topAppsList.add(appInfo);
            }

            Blacklist blacklist = Blacklist.getInstance(context);
            for(LauncherActivityInfo appInfo : unfilteredList) {
                String userSuffix = ":" + userManager.getSerialNumberForUser(appInfo.getUser());
                if(!(blacklist.isBlocked(appInfo.getComponentName().flattenToString() + userSuffix)
                        || blacklist.isBlocked(appInfo.getComponentName().flattenToString())
                        || blacklist.isBlocked(appInfo.getName()))
                        && !(topApps.isTopApp(appInfo.getComponentName().flattenToString() + userSuffix)
                        || topApps.isTopApp(appInfo.getComponentName().flattenToString())
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

                final List<AppEntry> entries =
                        generateAppEntries(context, userManager, pm, queryList);

                handler.post(() -> {
                    String queryText = searchView.getQuery().toString();
                    if(query == null && queryText.length() == 0
                            || query != null && query.equals(queryText)) {

                        if(firstDraw) {
                            SharedPreferences pref = U.getSharedPreferences(context);
                            if(pref.getString(PREF_START_MENU_LAYOUT, "grid").equals("grid")) {
                                startMenu.setNumColumns(context.getResources().getInteger(R.integer.tb_start_menu_columns));
                                adapter = new StartMenuAdapter(context, R.layout.tb_row_alt, entries);
                            } else
                                adapter = new StartMenuAdapter(context, R.layout.tb_row, entries);

                            startMenu.setAdapter(adapter);
                        }

                        int position = startMenu.getFirstVisiblePosition();

                        if(!firstDraw && adapter != null)
                            adapter.updateList(entries);

                        startMenu.setSelection(position);

                        if(adapter != null && adapter.getCount() > 0)
                            textView.setText(null);
                        else if(query != null)
                            textView.setText(context.getString(Patterns.WEB_URL.matcher(query).matches() ? R.string.tb_press_enter_alt : R.string.tb_press_enter));
                        else
                            textView.setText(context.getString(R.string.tb_nothing_to_see_here));
                    }
                });
            }
        });

        thread.start();
    }

    @VisibleForTesting
    List<AppEntry> generateAppEntries(Context context,
                                      UserManager userManager,
                                      PackageManager pm,
                                      List<LauncherActivityInfo> queryList) {
        final List<AppEntry> entries = new ArrayList<>();
        Drawable defaultIcon = pm.getDefaultActivityIcon();
        for(LauncherActivityInfo appInfo : queryList) {
            // Attempt to work around frequently reported OutOfMemoryErrors
            String label;
            Drawable icon;

            try {
                label = appInfo.getLabel().toString();
                icon = IconCache.getInstance(context).getIcon(context, pm, appInfo);
            } catch (OutOfMemoryError e) {
                System.gc();

                label = appInfo.getApplicationInfo().packageName;
                icon = defaultIcon;
            }

            String packageName = appInfo.getApplicationInfo().packageName;
            ComponentName componentName = new ComponentName(packageName, appInfo.getName());
            AppEntry newEntry =
                    new AppEntry(packageName, componentName.flattenToString(), label, icon, false);

            newEntry.setUserId(userManager.getSerialNumberForUser(appInfo.getUser()));
            entries.add(newEntry);
        }
        return entries;
    }

    private void toggleStartMenu() {
        if(layout.getVisibility() == View.GONE)
            showStartMenu();
        else
            hideStartMenu(true);
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void showStartMenu() {
        if(layout.getVisibility() == View.GONE) {
            layout.setOnClickListener(ocl);
            layout.setVisibility(View.VISIBLE);

            if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1 && !hasHardwareKeyboard)
                layout.setAlpha(1);

            MenuHelper.getInstance().setStartMenuOpen(true);

            U.sendBroadcast(context, ACTION_START_MENU_APPEARING);

            boolean onHomeScreen = LauncherHelper.getInstance().isOnHomeScreen(context);
            boolean inFreeformMode = FreeformHackHelper.getInstance().isInFreeformWorkspace();

            if(!U.isChromeOs(context)
                    && U.needsInvisibleActivityHacks()
                    && (!onHomeScreen || inFreeformMode)) {
                Class<?> clazz = inFreeformMode && !U.hasBrokenSetLaunchBoundsApi()
                        ? InvisibleActivityAlt.class
                        : InvisibleActivity.class;

                Intent intent = new Intent(context, clazz);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                if(inFreeformMode) {
                    if(clazz.equals(InvisibleActivity.class))
                        U.startActivityLowerRight(context, intent);
                    else if(clazz.equals(InvisibleActivityAlt.class))
                        U.startActivityMaximized(context, intent);
                } else
                    context.startActivity(intent);
            }

            EditText editText = searchView.findViewById(com.google.android.material.R.id.search_src_text);
            if(searchView.getVisibility() == View.VISIBLE) {
                if(hasHardwareKeyboard) {
                    searchView.setIconifiedByDefault(true);

                    if(editText != null)
                        editText.setShowSoftInputOnFocus(false);
                } else
                    searchView.requestFocus();
            }

            refreshApps(false);

            U.newHandler().postDelayed(() -> {
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1 || hasHardwareKeyboard)
                    layout.setAlpha(1);

                if(hasHardwareKeyboard) {
                    searchView.setIconifiedByDefault(false);
                    if(editText != null)
                        editText.setShowSoftInputOnFocus(true);

                    searchView.requestFocus();
                }

                searchView.setOnQueryTextFocusChangeListener((view, b) -> {
                    if(!hasHardwareKeyboard) {
                        ViewGroup.LayoutParams params1 = startMenu.getLayoutParams();
                        params1.height = context.getResources().getDimensionPixelSize(
                                b && isImeFixDisabled()
                                        ? R.dimen.tb_start_menu_height_half
                                        : R.dimen.tb_start_menu_height);
                        startMenu.setLayoutParams(params1);
                    }

                    if(!b && !(U.isBlissOs(context) && Build.VERSION.SDK_INT == Build.VERSION_CODES.P)) {
                        if(hasHardwareKeyboard && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            hideStartMenu(true);
                        } else {
                            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }
                    }
                });

                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(layout.getWindowToken(), 0);
            }, 100);
        }
    }

    private void hideStartMenu(boolean shouldReset) {
        if(layout.getVisibility() == View.VISIBLE) {
            layout.setOnClickListener(null);
            layout.setAlpha(0);

            MenuHelper.getInstance().setStartMenuOpen(false);

            U.sendBroadcast(context, ACTION_START_MENU_DISAPPEARING);

            layout.postDelayed(() -> {
                layout.setVisibility(View.GONE);

                if(searchViewClicked || hasHardwareKeyboard) {
                    if(!hasHardwareKeyboard)
                        searchView.setQuery(null, false);

                    searchView.setIconified(true);
                }

                searchView.setOnQueryTextFocusChangeListener(null);
                hasSubmittedQuery = false;

                if(shouldReset) {
                    startMenu.smoothScrollBy(0, 0);
                    startMenu.setSelection(0);
                }

                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(layout.getWindowToken(), 0);
            }, 100);
        }
    }

    @Override
    public void onDestroyHost(UIHost host) {
        if(layout != null)
            try {
                host.removeView(layout);
            } catch (IllegalArgumentException ignored) {}

        U.unregisterReceiver(context, toggleReceiver);
        U.unregisterReceiver(context, hideReceiver);
        U.unregisterReceiver(context, hideReceiverNoReset);
        U.unregisterReceiver(context, showSpaceReceiver);
        U.unregisterReceiver(context, hideSpaceReceiver);
        U.unregisterReceiver(context, resetReceiver);

        U.sendBroadcast(context, ACTION_START_MENU_DISAPPEARING);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRecreateHost(UIHost host) {
        if(layout != null) {
            try {
                host.removeView(layout);
            } catch (IllegalArgumentException ignored) {}

            if(U.canDrawOverlays(context))
                drawStartMenu(host);
            else {
                SharedPreferences pref = U.getSharedPreferences(context);
                pref.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();

                host.terminate();
            }
        }
    }

    private void openContextMenu(final int[] location) {
        hideStartMenu(false);

        Bundle args = new Bundle();
        args.putBoolean("launched_from_start_menu", true);
        args.putBoolean("is_overflow_menu", true);
        args.putInt("x", location[0]);
        args.putInt("y", location[1]);

        U.newHandler().postDelayed(() -> U.startContextMenuActivity(context, args), shouldDelay() ? 100 : 0);
    }

    private boolean shouldDelay() {
        return U.hasFreeformSupport(context)
                && U.isFreeformModeEnabled(context)
                && !FreeformHackHelper.getInstance().isFreeformHackActive();
    }
}
