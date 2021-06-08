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

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.TaskbarPosition;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.ApplicationType;
import com.farmerbb.taskbar.util.DesktopIconInfo;
import com.farmerbb.taskbar.util.DisplayInfo;
import com.farmerbb.taskbar.helper.FreeformHackHelper;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.helper.LauncherHelper;
import com.farmerbb.taskbar.helper.MenuHelper;
import com.farmerbb.taskbar.util.PinnedBlockedApps;
import com.farmerbb.taskbar.util.SavedWindowSizes;
import com.farmerbb.taskbar.util.U;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.List;

import static com.farmerbb.taskbar.util.Constants.*;

public class ContextMenuActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener {

    private AppEntry entry;
    private DesktopIconInfo desktopIcon;

    boolean showStartMenu = false;
    boolean shouldHideTaskbar = false;
    boolean isStartButton = false;
    boolean isOverflowMenu = false;
    boolean secondaryMenu = false;
    boolean dashboardOrStartMenuAppearing = false;
    boolean contextMenuFix = false;
    boolean showQuitOption = false;

    List<ShortcutInfo> shortcuts;

    private final BroadcastReceiver dashboardOrStartMenuAppearingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dashboardOrStartMenuAppearing = true;
            finish();
        }
    };

    private final BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };


    @SuppressLint("RtlHardcoded")
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        U.sendBroadcast(this, ACTION_CONTEXT_MENU_APPEARING);
        MenuHelper.getInstance().setContextMenuOpen(true);

        Bundle args = getIntent().getBundleExtra("args");
        entry = (AppEntry) args.getSerializable("app_entry");
        desktopIcon = (DesktopIconInfo) args.getSerializable("desktop_icon");

        showStartMenu = args.getBoolean("launched_from_start_menu", false);
        isStartButton = entry == null && args.getBoolean("is_start_button", false);
        isOverflowMenu = entry == null && args.getBoolean("is_overflow_menu", false);
        contextMenuFix = args.containsKey(EXTRA_CONTEXT_MENU_FIX);
        showQuitOption = !args.getBoolean("dont_show_quit", false);

        // Determine where to position the dialog on screen
        WindowManager.LayoutParams params = getWindow().getAttributes();
        
        if(args.containsKey("x") && args.containsKey("y"))
            U.applyDisplayCutoutModeTo(params);

        DisplayInfo display = U.getDisplayInfo(this);

        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if(resourceId > 0)
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);

        int contextMenuWidth = getResources().getDimensionPixelSize(R.dimen.tb_context_menu_width);

        if(showStartMenu || desktopIcon != null) {
            int x = args.getInt("x", 0);
            int y = args.getInt("y", 0);
            int offsetResourceId = isOverflowMenu ? R.dimen.tb_context_menu_offset_overflow : R.dimen.tb_context_menu_offset;
            int offset = getResources().getDimensionPixelSize(offsetResourceId);

            switch(TaskbarPosition.getTaskbarPosition(this)) {
                case POSITION_BOTTOM_LEFT:
                case POSITION_BOTTOM_VERTICAL_LEFT:
                    params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                    params.x = x;
                    params.y = display.height - y - offset;
                    break;
                case POSITION_BOTTOM_RIGHT:
                case POSITION_BOTTOM_VERTICAL_RIGHT:
                    params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                    params.x = x - contextMenuWidth + offset + offset;
                    params.y = display.height - y - offset;
                    break;
                case POSITION_TOP_LEFT:
                case POSITION_TOP_VERTICAL_LEFT:
                    params.gravity = Gravity.TOP | Gravity.LEFT;
                    params.x = x;
                    params.y = y - offset + statusBarHeight;
                    break;
                case POSITION_TOP_RIGHT:
                case POSITION_TOP_VERTICAL_RIGHT:
                    params.gravity = Gravity.TOP | Gravity.LEFT;
                    params.x = x - contextMenuWidth + offset + offset;
                    params.y = y - offset + statusBarHeight;
                    break;
            }
        } else {
            U.sendBroadcast(this, ACTION_HIDE_START_MENU);

            int x = args.getInt("x", display.width);
            int y = args.getInt("y", display.height);
            int offset = getResources().getDimensionPixelSize(R.dimen.tb_icon_size);

            switch(TaskbarPosition.getTaskbarPosition(this)) {
                case POSITION_BOTTOM_LEFT:
                    params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                    params.x = isStartButton ? 0 : x;
                    params.y = offset;
                    break;
                case POSITION_BOTTOM_VERTICAL_LEFT:
                    params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                    params.x = offset;
                    params.y = display.height - y - (isStartButton ? 0 : offset);
                    break;
                case POSITION_BOTTOM_RIGHT:
                    params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                    params.x = display.width - x;
                    params.y = offset;
                    break;
                case POSITION_BOTTOM_VERTICAL_RIGHT:
                    params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                    params.x = offset;
                    params.y = display.height - y - (isStartButton ? 0 : offset);
                    break;
                case POSITION_TOP_LEFT:
                    params.gravity = Gravity.TOP | Gravity.LEFT;
                    params.x = isStartButton ? 0 : x;
                    params.y = offset;
                    break;
                case POSITION_TOP_VERTICAL_LEFT:
                    params.gravity = Gravity.TOP | Gravity.LEFT;
                    params.x = offset;
                    params.y = isStartButton ? 0 : y - statusBarHeight;
                    break;
                case POSITION_TOP_RIGHT:
                    params.gravity = Gravity.TOP | Gravity.RIGHT;
                    params.x = display.width - x;
                    params.y = offset;
                    break;
                case POSITION_TOP_VERTICAL_RIGHT:
                    params.gravity = Gravity.TOP | Gravity.RIGHT;
                    params.x = offset;
                    params.y = isStartButton ? 0 : y - statusBarHeight;
                    break;
            }

            if(!TaskbarPosition.isVertical(this) && (params.x > display.width / 2))
                params.x = params.x - contextMenuWidth + offset;
        }

        params.width = getResources().getDimensionPixelSize(R.dimen.tb_context_menu_width);
        params.dimAmount = 0;

        if(U.isChromeOs(this)
                && TaskbarPosition.isBottom(this)) {
            SharedPreferences pref = U.getSharedPreferences(this);

            if(U.getChromeOsContextMenuFix(this)
                    && !pref.getBoolean(PREF_HAS_CAPTION, false))
                params.y = params.y - getResources().getDimensionPixelSize(R.dimen.tb_caption_offset);
        }

        getWindow().setAttributes(params);

        if(U.isChromeOs(this)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1
                && U.getCurrentApiVersion() < 30.0f) {
            getWindow().setElevation(0);
        }

        View view = findViewById(android.R.id.list);
        if(view != null) view.setPadding(0, 0, 0, 0);

        generateMenu();

        U.registerReceiver(this, dashboardOrStartMenuAppearingReceiver,
                ACTION_START_MENU_APPEARING,
                ACTION_DASHBOARD_APPEARING);

        U.registerReceiver(this, finishReceiver, ACTION_HIDE_CONTEXT_MENU);
    }

    @SuppressWarnings("deprecation")
    private void generateMenu() {
        if(isStartButton) {
            addPreferencesFromResource(R.xml.tb_pref_context_menu_open_settings);
            findPreference(PREF_OPEN_TASKBAR_SETTINGS).setOnPreferenceClickListener(this);
            findPreference(PREF_START_MENU_APPS).setOnPreferenceClickListener(this);

            if(U.isFreeformModeEnabled(this)
                    && !U.isDesktopIconsEnabled(this)
                    && ((U.launcherIsDefault(this)
                    && !U.isOverridingFreeformHack(this, false)
                    && FreeformHackHelper.getInstance().isInFreeformWorkspace())
                    || (U.isOverridingFreeformHack(this, false)
                    && LauncherHelper.getInstance().isOnHomeScreen(this)))) {
                addPreferencesFromResource(R.xml.tb_pref_context_menu_change_wallpaper);
                findPreference(PREF_CHANGE_WALLPAPER).setOnPreferenceClickListener(this);
            }

            if(showQuitOption) {
                addPreferencesFromResource(R.xml.tb_pref_context_menu_quit);
                findPreference(PREF_QUIT_TASKBAR).setOnPreferenceClickListener(this);
            }
        } else if(isOverflowMenu) {
            if(getResources().getConfiguration().screenWidthDp >= 600
                    && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
                setTitle(R.string.tb_tools);
            else {
                addPreferencesFromResource(R.xml.tb_pref_context_menu_header);
                findPreference(PREF_HEADER).setTitle(R.string.tb_tools);
            }

            addPreferencesFromResource(R.xml.tb_pref_context_menu_overflow);
            findPreference(PREF_VOLUME).setOnPreferenceClickListener(this);
            findPreference(PREF_SYSTEM_SETTINGS).setOnPreferenceClickListener(this);

            if(!U.isLibrary(this))
                findPreference(PREF_POWER_MENU).setOnPreferenceClickListener(this);
            else
                getPreferenceScreen().removePreference(findPreference(PREF_POWER_MENU));

            if(!U.isLibrary(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                findPreference(PREF_LOCK_DEVICE).setOnPreferenceClickListener(this);
            else
                getPreferenceScreen().removePreference(findPreference(PREF_LOCK_DEVICE));

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                findPreference(PREF_FILE_MANAGER).setOnPreferenceClickListener(this);
            else
                getPreferenceScreen().removePreference(findPreference(PREF_FILE_MANAGER));
        } else if(desktopIcon != null && entry == null) {
            addPreferencesFromResource(R.xml.tb_pref_context_menu_desktop_icons);
            findPreference(PREF_ADD_ICON_TO_DESKTOP).setOnPreferenceClickListener(this);
            findPreference(PREF_ARRANGE_ICONS).setOnPreferenceClickListener(this);
            findPreference(PREF_SORT_BY_NAME).setOnPreferenceClickListener(this);
            findPreference(PREF_CHANGE_WALLPAPER).setOnPreferenceClickListener(this);
        } else {
            if(getResources().getConfiguration().screenWidthDp >= 600
                    && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
                setTitle(entry.getLabel());
            else {
                addPreferencesFromResource(R.xml.tb_pref_context_menu_header);
                findPreference(PREF_HEADER).setTitle(entry.getLabel());
            }

            if(U.hasFreeformSupport(this)
                    && U.isFreeformModeEnabled(this)
                    && !U.isGame(this, entry.getPackageName())) {
                addPreferencesFromResource(R.xml.tb_pref_context_menu_show_window_sizes);
                findPreference(PREF_SHOW_WINDOW_SIZES).setOnPreferenceClickListener(this);
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                int shortcutCount = getLauncherShortcuts();

                if(shortcutCount > 1) {
                    addPreferencesFromResource(R.xml.tb_pref_context_menu_shortcuts);
                    findPreference(PREF_APP_SHORTCUTS).setOnPreferenceClickListener(this);
                } else if(shortcutCount == 1)
                    generateShortcuts();
            }

            final PackageManager pm = getPackageManager();
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo defaultLauncher = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);

            if(desktopIcon != null) {
                addPreferencesFromResource(R.xml.tb_pref_context_menu_remove_desktop_icon);
                findPreference(PREF_ARRANGE_ICONS).setOnPreferenceClickListener(this);
                findPreference(PREF_REMOVE_DESKTOP_ICON).setOnPreferenceClickListener(this);
            } else if(!entry.getPackageName().contains(BuildConfig.BASE_APPLICATION_ID)
                    && !entry.getPackageName().equals(defaultLauncher.activityInfo.packageName)) {
                PinnedBlockedApps pba = PinnedBlockedApps.getInstance(this);

                if(pba.isPinned(entry.getComponentName())) {
                    addPreferencesFromResource(R.xml.tb_pref_context_menu_pin);
                    findPreference(PREF_PIN_APP).setOnPreferenceClickListener(this);
                    findPreference(PREF_PIN_APP).setTitle(R.string.tb_unpin_app);
                } else if(pba.isBlocked(entry.getComponentName())) {
                    addPreferencesFromResource(R.xml.tb_pref_context_menu_block);
                    findPreference(PREF_BLOCK_APP).setOnPreferenceClickListener(this);
                    findPreference(PREF_BLOCK_APP).setTitle(R.string.tb_unblock_app);
                } else {
                    final int MAX_NUM_OF_COLUMNS = U.getMaxNumOfEntries(this);

                    if(pba.getPinnedApps().size() < MAX_NUM_OF_COLUMNS) {
                        addPreferencesFromResource(R.xml.tb_pref_context_menu_pin);
                        findPreference(PREF_PIN_APP).setOnPreferenceClickListener(this);
                        findPreference(PREF_PIN_APP).setTitle(R.string.tb_pin_app);
                    }

                    addPreferencesFromResource(R.xml.tb_pref_context_menu_block);
                    findPreference(PREF_BLOCK_APP).setOnPreferenceClickListener(this);
                    findPreference(PREF_BLOCK_APP).setTitle(R.string.tb_block_app);
                }
            }

            addPreferencesFromResource(R.xml.tb_pref_context_menu);

            findPreference(PREF_APP_INFO).setOnPreferenceClickListener(this);
            findPreference(PREF_UNINSTALL).setOnPreferenceClickListener(this);
        }
    }

    @SuppressWarnings("deprecation")
    private void generateShortcuts() {
        addPreferencesFromResource(R.xml.tb_pref_context_menu_shortcut_list);
        switch(shortcuts.size()) {
            case 5:
                findPreference(PREF_SHORTCUT_5).setTitle(getShortcutTitle(shortcuts.get(4)));
                findPreference(PREF_SHORTCUT_5).setOnPreferenceClickListener(this);
            case 4:
                findPreference(PREF_SHORTCUT_4).setTitle(getShortcutTitle(shortcuts.get(3)));
                findPreference(PREF_SHORTCUT_4).setOnPreferenceClickListener(this);
            case 3:
                findPreference(PREF_SHORTCUT_3).setTitle(getShortcutTitle(shortcuts.get(2)));
                findPreference(PREF_SHORTCUT_3).setOnPreferenceClickListener(this);
            case 2:
                findPreference(PREF_SHORTCUT_2).setTitle(getShortcutTitle(shortcuts.get(1)));
                findPreference(PREF_SHORTCUT_2).setOnPreferenceClickListener(this);
            case 1:
                findPreference(PREF_SHORTCUT_1).setTitle(getShortcutTitle(shortcuts.get(0)));
                findPreference(PREF_SHORTCUT_1).setOnPreferenceClickListener(this);
                break;
        }

        switch(shortcuts.size()) {
            case 1:
                getPreferenceScreen().removePreference(findPreference(PREF_SHORTCUT_2));
            case 2:
                getPreferenceScreen().removePreference(findPreference(PREF_SHORTCUT_3));
            case 3:
                getPreferenceScreen().removePreference(findPreference(PREF_SHORTCUT_4));
            case 4:
                getPreferenceScreen().removePreference(findPreference(PREF_SHORTCUT_5));
                break;
        }
    }

    @SuppressWarnings("deprecation")
    private void generateWindowSizes() {
        getPreferenceScreen().removeAll();

        addPreferencesFromResource(R.xml.tb_pref_context_menu_window_size_list);
        findPreference(PREF_WINDOW_SIZE_STANDARD).setOnPreferenceClickListener(this);
        findPreference(PREF_WINDOW_SIZE_LARGE).setOnPreferenceClickListener(this);
        findPreference(PREF_WINDOW_SIZE_FULLSCREEN).setOnPreferenceClickListener(this);
        findPreference(PREF_WINDOW_SIZE_HALF_LEFT).setOnPreferenceClickListener(this);
        findPreference(PREF_WINDOW_SIZE_HALF_RIGHT).setOnPreferenceClickListener(this);
        findPreference(PREF_WINDOW_SIZE_PHONE_SIZE).setOnPreferenceClickListener(this);

        String windowSizePref = SavedWindowSizes.getInstance(this).getWindowSize(this, entry.getPackageName());
        CharSequence title = findPreference("window_size_" + windowSizePref).getTitle();
        findPreference("window_size_" + windowSizePref).setTitle('\u2713' + " " + title);
    }

    @SuppressWarnings("deprecation")
    private void generateWallpaperOptions() {
        getPreferenceScreen().removeAll();

        addPreferencesFromResource(R.xml.tb_pref_context_menu_change_wallpaper_secondary);
        findPreference(PREF_CHANGE_WALLPAPER_GLOBAL).setOnPreferenceClickListener(this);
        findPreference(PREF_CHANGE_WALLPAPER_DESKTOP).setOnPreferenceClickListener(this);

        File file = new File(getFilesDir() + "/tb_images", "desktop_wallpaper");
        if(!file.exists()) return;

        addPreferencesFromResource(R.xml.tb_pref_remove_desktop_wallpaper);
        findPreference(PREF_REMOVE_DESKTOP_WALLPAPER).setOnPreferenceClickListener(this);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.N_MR1)
    @Override
    public boolean onPreferenceClick(Preference p) {
        UserManager userManager = (UserManager) getSystemService(USER_SERVICE);
        LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        boolean appIsValid = isStartButton || isOverflowMenu || desktopIcon != null ||
                (entry != null && !launcherApps.getActivityList(entry.getPackageName(),
                        userManager.getUserForSerialNumber(entry.getUserId(this))).isEmpty());
        secondaryMenu = false;

        if(appIsValid) switch(p.getKey()) {
            case PREF_APP_INFO:
                U.launchApp(this, () ->
                        launcherApps.startAppDetailsActivity(
                                ComponentName.unflattenFromString(entry.getComponentName()),
                                userManager.getUserForSerialNumber(entry.getUserId(this)),
                                null,
                                U.getActivityOptionsBundle(this, ApplicationType.APP_PORTRAIT, getListView().getChildAt(p.getOrder()))));

                prepareToClose();
                break;
            case PREF_UNINSTALL:
                if(U.hasFreeformSupport(this) && isInMultiWindowMode()) {
                    Intent intent2 = new Intent(this, DummyActivity.class);
                    intent2.putExtra("uninstall", entry.getPackageName());
                    intent2.putExtra("user_id", entry.getUserId(this));

                    try {
                        startActivity(intent2);
                    } catch (IllegalArgumentException ignored) {}
                } else {
                    Intent intent2 = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + entry.getPackageName()));
                    intent2.putExtra(Intent.EXTRA_USER, userManager.getUserForSerialNumber(entry.getUserId(this)));

                    try {
                        startActivity(intent2);
                    } catch (ActivityNotFoundException | IllegalArgumentException ignored) {}
                }

                prepareToClose();
                break;
            case PREF_OPEN_TASKBAR_SETTINGS:
                U.launchApp(this, () -> {
                    Intent intent2 = new Intent(this, MainActivity.class);
                    intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    
                    LauncherHelper helper = LauncherHelper.getInstance();
                    if(helper.isOnHomeScreen(this) || helper.isOnSecondaryHomeScreen(this))
                        U.applyOpenInNewWindow(this, intent2);

                    try {
                        startActivity(intent2,
                                U.getActivityOptionsBundle(this, ApplicationType.APP_PORTRAIT, getListView().getChildAt(p.getOrder())));
                    } catch (IllegalArgumentException ignored) {}
                });

                prepareToClose();
                break;
            case PREF_QUIT_TASKBAR:
                Intent quitIntent = new Intent(ACTION_QUIT);
                quitIntent.setPackage(getPackageName());
                sendBroadcast(quitIntent);
                prepareToClose();
                break;
            case PREF_PIN_APP:
                PinnedBlockedApps pba = PinnedBlockedApps.getInstance(this);
                if(pba.isPinned(entry.getComponentName()))
                    pba.removePinnedApp(this, entry.getComponentName());
                else {
                    Intent intent = new Intent();
                    intent.setComponent(ComponentName.unflattenFromString(entry.getComponentName()));

                    LauncherActivityInfo appInfo = launcherApps.resolveActivity(intent, userManager.getUserForSerialNumber(entry.getUserId(this)));
                    if(appInfo != null) {
                        AppEntry newEntry = new AppEntry(
                                entry.getPackageName(),
                                entry.getComponentName(),
                                entry.getLabel(),
                                IconCache.getInstance(this).getIcon(this, appInfo),
                                true);

                        newEntry.setUserId(entry.getUserId(this));
                        pba.addPinnedApp(this, newEntry);
                    }
                }
                break;
            case PREF_BLOCK_APP:
                PinnedBlockedApps pba2 = PinnedBlockedApps.getInstance(this);
                if(pba2.isBlocked(entry.getComponentName()))
                    pba2.removeBlockedApp(this, entry.getComponentName());
                else
                    pba2.addBlockedApp(this, entry);
                break;
            case PREF_SHOW_WINDOW_SIZES:
                generateWindowSizes();

                if(U.hasBrokenSetLaunchBoundsApi())
                    U.showToastLong(this, R.string.tb_window_sizes_not_available);

                getListView().setOnItemLongClickListener((parent, view, position, id) -> {
                    String[] windowSizes = getResources().getStringArray(R.array.tb_pref_window_size_list_values);

                    SavedWindowSizes.getInstance(this).setWindowSize(this, entry.getPackageName(), windowSizes[position]);

                    generateWindowSizes();
                    return true;
                });

                secondaryMenu = true;
                break;
            case PREF_WINDOW_SIZE_STANDARD:
            case PREF_WINDOW_SIZE_LARGE:
            case PREF_WINDOW_SIZE_FULLSCREEN:
            case PREF_WINDOW_SIZE_HALF_LEFT:
            case PREF_WINDOW_SIZE_HALF_RIGHT:
            case PREF_WINDOW_SIZE_PHONE_SIZE:
                String windowSize = p.getKey().replace("window_size_", "");

                SharedPreferences pref2 = U.getSharedPreferences(this);
                if(pref2.getBoolean(PREF_SAVE_WINDOW_SIZES, true)) {
                    SavedWindowSizes.getInstance(this).setWindowSize(this, entry.getPackageName(), windowSize);
                }

                U.launchApp(
                        U.getDisplayContext(this),
                        entry,
                        windowSize,
                        false,
                        true,
                        getListView().getChildAt(p.getOrder()));

                if(U.hasBrokenSetLaunchBoundsApi())
                    U.cancelToast();

                prepareToClose();
                break;
            case PREF_APP_SHORTCUTS:
                getPreferenceScreen().removeAll();
                generateShortcuts();

                secondaryMenu = true;
                break;
            case PREF_SHORTCUT_1:
            case PREF_SHORTCUT_2:
            case PREF_SHORTCUT_3:
            case PREF_SHORTCUT_4:
            case PREF_SHORTCUT_5:
                U.startShortcut(
                        U.getDisplayContext(this),
                        entry,
                        shortcuts.get(Integer.parseInt(p.getKey().replace("shortcut_", "")) - 1),
                        getListView().getChildAt(p.getOrder()));

                prepareToClose();
                break;
            case PREF_START_MENU_APPS:
                Intent intent = U.getThemedIntent(this, SelectAppActivity.class);

                if(U.hasFreeformSupport(this)
                        && U.isFreeformModeEnabled(this)
                        && isInMultiWindowMode()) {
                    intent.putExtra("no_shadow", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);

                    U.startActivityMaximized(U.getDisplayContext(this), intent);
                } else {
                    try {
                        startActivity(intent);
                    } catch (IllegalArgumentException ignored) {}
                }

                prepareToClose();
                break;
            case PREF_VOLUME:
                AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
                audio.adjustSuggestedStreamVolume(AudioManager.ADJUST_SAME, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI);

                if(LauncherHelper.getInstance().isOnSecondaryHomeScreen(this)) {
                    U.showToast(this, R.string.tb_opening_volume_control);
                    U.sendBroadcast(this, ACTION_UNDIM_SCREEN);
                }

                prepareToClose();
                break;
            case PREF_FILE_MANAGER:
                U.launchApp(this, () -> {
                    Intent fileManagerIntent;

                    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1)
                        fileManagerIntent = new Intent(Intent.ACTION_VIEW);
                    else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        fileManagerIntent = new Intent("android.provider.action.BROWSE");
                    else {
                        fileManagerIntent = new Intent("android.provider.action.BROWSE_DOCUMENT_ROOT");
                        fileManagerIntent.setComponent(ComponentName.unflattenFromString("com.android.documentsui/.DocumentsActivity"));
                    }

                    fileManagerIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    fileManagerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    fileManagerIntent.setData(Uri.parse("content://com.android.externalstorage.documents/root/primary"));

                    try {
                        startActivity(fileManagerIntent,
                                U.getActivityOptionsBundle(this, ApplicationType.APP_PORTRAIT, getListView().getChildAt(p.getOrder())));
                    } catch (ActivityNotFoundException e) {
                        U.showToast(this, R.string.tb_lock_device_not_supported);
                    } catch (IllegalArgumentException ignored) {}
                });

                prepareToClose();
                break;
            case PREF_SYSTEM_SETTINGS:
                U.launchApp(this, () -> {
                    Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
                    settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    try {
                        startActivity(settingsIntent,
                                U.getActivityOptionsBundle(this, ApplicationType.APP_PORTRAIT, getListView().getChildAt(p.getOrder())));
                    } catch (ActivityNotFoundException e) {
                        U.showToast(this, R.string.tb_lock_device_not_supported);
                    } catch (IllegalArgumentException ignored) {}
                });

                prepareToClose();
                break;
            case PREF_LOCK_DEVICE:
                U.lockDevice(this);
                prepareToClose();
                break;
            case PREF_POWER_MENU:
                U.sendAccessibilityAction(this, AccessibilityService.GLOBAL_ACTION_POWER_DIALOG, () -> {
                    if(LauncherHelper.getInstance().isOnSecondaryHomeScreen(this)) {
                        U.showToast(this, R.string.tb_opening_power_menu);
                        U.sendBroadcast(this, ACTION_UNDIM_SCREEN);
                    }
                });

                prepareToClose();
                break;
            case PREF_ADD_ICON_TO_DESKTOP:
                Intent intent2 = U.getThemedIntent(this, DesktopIconSelectAppActivity.class);
                intent2.putExtra("desktop_icon", desktopIcon);

                if(U.hasFreeformSupport(this)
                        && U.isFreeformModeEnabled(this)
                        && isInMultiWindowMode()) {
                    intent2.putExtra("no_shadow", true);
                    intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);

                    U.startActivityMaximized(U.getDisplayContext(this), intent2);
                } else {
                    try {
                        startActivity(intent2);
                    } catch (IllegalArgumentException ignored) {}
                }

                prepareToClose();
                break;
            case PREF_ARRANGE_ICONS:
                U.sendBroadcast(this, ACTION_ENTER_ICON_ARRANGE_MODE);
                break;
            case PREF_SORT_BY_NAME:
                U.sendBroadcast(this, ACTION_SORT_DESKTOP_ICONS);
                break;
            case PREF_CHANGE_WALLPAPER:
                if(LauncherHelper.getInstance().isOnSecondaryHomeScreen(this)) {
                    generateWallpaperOptions();
                    secondaryMenu = true;
                } else if(U.isChromeOs(this)) {
                    U.sendBroadcast(this, ACTION_WALLPAPER_CHANGE_REQUESTED);
                } else {
                    changeWallpaper();
                    prepareToClose();
                }
                break;
            case PREF_REMOVE_DESKTOP_ICON:
                try {
                    SharedPreferences pref5 = U.getSharedPreferences(this);
                    JSONArray jsonIcons = new JSONArray(pref5.getString(PREF_DESKTOP_ICONS, "[]"));
                    int iconToRemove = -1;

                    for(int i = 0; i < jsonIcons.length(); i++) {
                        DesktopIconInfo info = DesktopIconInfo.fromJson(jsonIcons.getJSONObject(i));
                        if(info != null && info.column == desktopIcon.column && info.row == desktopIcon.row) {
                            iconToRemove = i;
                            break;
                        }
                    }

                    if(iconToRemove > -1) {
                        jsonIcons.remove(iconToRemove);

                        pref5.edit().putString(PREF_DESKTOP_ICONS, jsonIcons.toString()).apply();
                        U.sendBroadcast(this, ACTION_REFRESH_DESKTOP_ICONS);
                    }
                } catch (JSONException ignored) {}
                break;
            case PREF_CHANGE_WALLPAPER_GLOBAL:
                changeWallpaper();
                prepareToClose();
                break;
            case PREF_CHANGE_WALLPAPER_DESKTOP:
                U.sendBroadcast(this, ACTION_WALLPAPER_CHANGE_REQUESTED);
                break;
            case PREF_REMOVE_DESKTOP_WALLPAPER:
                U.sendBroadcast(this, ACTION_REMOVE_DESKTOP_WALLPAPER);
                break;
        }

        if(!secondaryMenu) finish();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(!isFinishing()) finish();
    }

    @Override
    public void finish() {
        U.sendBroadcast(this, ACTION_CONTEXT_MENU_DISAPPEARING);
        MenuHelper.getInstance().setContextMenuOpen(false);

        if(!dashboardOrStartMenuAppearing) {
            if(showStartMenu) {
                U.sendBroadcast(this, ACTION_TOGGLE_START_MENU);
            } else {
                U.sendBroadcast(this, ACTION_RESET_START_MENU);

                if(shouldHideTaskbar && U.shouldCollapse(this, true)) {
                    U.sendBroadcast(this, ACTION_HIDE_TASKBAR);
                }
            }
        }

        SharedPreferences pref = U.getSharedPreferences(this);

        super.finish();
        if(showStartMenu || pref.getBoolean(PREF_DISABLE_ANIMATIONS, false))
            overridePendingTransition(0, 0);
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private int getLauncherShortcuts() {
        LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        if(launcherApps.hasShortcutHostPermission()) {
            UserManager userManager = (UserManager) getSystemService(USER_SERVICE);

            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
            query.setActivity(ComponentName.unflattenFromString(entry.getComponentName()));
            query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                    | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                    | LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);

            shortcuts = launcherApps.getShortcuts(query, userManager.getUserForSerialNumber(entry.getUserId(this)));
            if(shortcuts != null)
                return shortcuts.size();
        }

        return 0;
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private CharSequence getShortcutTitle(ShortcutInfo shortcut) {
        CharSequence longLabel = shortcut.getLongLabel();
        if(longLabel != null && longLabel.length() > 0 && longLabel.length() <= 20)
            return longLabel;
        else
            return shortcut.getShortLabel();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if(secondaryMenu) {
            secondaryMenu = false;

            getPreferenceScreen().removeAll();
            generateMenu();

            getListView().setOnItemLongClickListener(null);

            if(U.hasBrokenSetLaunchBoundsApi())
                U.cancelToast();
        } else {
            if(contextMenuFix && !showStartMenu)
                U.startFreeformHack(this);

            super.onBackPressed();
            if(FreeformHackHelper.getInstance().isInFreeformWorkspace())
                overridePendingTransition(0, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        U.unregisterReceiver(this, dashboardOrStartMenuAppearingReceiver);
        U.unregisterReceiver(this, finishReceiver);
    }

    private void changeWallpaper() {
        if(LauncherHelper.getInstance().isOnHomeScreen(this))
            U.sendBroadcast(this, ACTION_TEMP_HIDE_TASKBAR);

        Intent intent = Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER), getString(R.string.tb_set_wallpaper));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        U.startActivityMaximized(U.getDisplayContext(this), intent);
    }
    
    private void prepareToClose() {
        showStartMenu = false;
        shouldHideTaskbar = true;
        contextMenuFix = false;
    }
}
