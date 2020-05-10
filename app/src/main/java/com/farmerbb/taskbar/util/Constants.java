/* Copyright 2020 Braden Farmer
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

public class Constants {

    private Constants() {}

    // Intent actions

    public static final String ACTION_ACCESSIBILITY_ACTION = "com.farmerbb.taskbar.ACCESSIBILITY_ACTION";
    public static final String ACTION_ADD_WIDGET_COMPLETED = "com.farmerbb.taskbar.ADD_WIDGET_COMPLETED";
    public static final String ACTION_ADD_WIDGET_REQUESTED = "com.farmerbb.taskbar.ADD_WIDGET_REQUESTED";
    public static final String ACTION_CONTEXT_MENU_APPEARING = "com.farmerbb.taskbar.CONTEXT_MENU_APPEARING";
    public static final String ACTION_CONTEXT_MENU_DISAPPEARING = "com.farmerbb.taskbar.CONTEXT_MENU_DISAPPEARING";
    public static final String ACTION_DASHBOARD_APPEARING = "com.farmerbb.taskbar.DASHBOARD_APPEARING";
    public static final String ACTION_DASHBOARD_DISAPPEARING = "com.farmerbb.taskbar.DASHBOARD_DISAPPEARING";
    public static final String ACTION_ENTER_ICON_ARRANGE_MODE = "com.farmerbb.taskbar.ENTER_ICON_ARRANGE_MODE";
    public static final String ACTION_FINISH_FREEFORM_ACTIVITY = "com.farmerbb.taskbar.FINISH_FREEFORM_ACTIVITY";
    public static final String ACTION_FORCE_TASKBAR_RESTART = "com.farmerbb.taskbar.FORCE_TASKBAR_RESTART";
    public static final String ACTION_FREEFORM_PREF_CHANGED = "com.farmerbb.taskbar.FREEFORM_PREF_CHANGED";
    public static final String ACTION_HIDE_CONTEXT_MENU = "com.farmerbb.taskbar.HIDE_CONTEXT_MENU";
    public static final String ACTION_HIDE_DASHBOARD = "com.farmerbb.taskbar.HIDE_DASHBOARD";
    public static final String ACTION_HIDE_START_MENU = "com.farmerbb.taskbar.HIDE_START_MENU";
    public static final String ACTION_HIDE_START_MENU_NO_RESET = "com.farmerbb.taskbar.HIDE_START_MENU_NO_RESET";
    public static final String ACTION_HIDE_START_MENU_SPACE = "com.farmerbb.taskbar.HIDE_START_MENU_SPACE";
    public static final String ACTION_HIDE_TASKBAR = "com.farmerbb.taskbar.HIDE_TASKBAR";
    public static final String ACTION_IMPORT_FINISHED = "com.farmerbb.taskbar.IMPORT_FINISHED";
    public static final String ACTION_KILL_HOME_ACTIVITY = "com.farmerbb.taskbar.KILL_HOME_ACTIVITY";
    public static final String ACTION_LAUNCHER_PREF_CHANGED = "com.farmerbb.taskbar.LAUNCHER_PREF_CHANGED";
    public static final String ACTION_QUIT = "com.farmerbb.taskbar.QUIT";
    public static final String ACTION_RECEIVE_SETTINGS = "com.farmerbb.taskbar.RECEIVE_SETTINGS";
    public static final String ACTION_REFRESH_DESKTOP_ICONS = "com.farmerbb.taskbar.REFRESH_DESKTOP_ICONS";
    public static final String ACTION_REMOVE_WIDGET_COMPLETED = "com.farmerbb.taskbar.REMOVE_WIDGET_COMPLETED";
    public static final String ACTION_REMOVE_WIDGET_REQUESTED = "com.farmerbb.taskbar.REMOVE_WIDGET_REQUESTED";
    public static final String ACTION_RESET_START_MENU = "com.farmerbb.taskbar.RESET_START_MENU";
    public static final String ACTION_RESTART = "com.farmerbb.taskbar.RESTART";
    public static final String ACTION_SEND_SETTINGS = "com.farmerbb.taskbar.SEND_SETTINGS";
    public static final String ACTION_SHOW_HIDE_TASKBAR = "com.farmerbb.taskbar.SHOW_HIDE_TASKBAR";
    public static final String ACTION_SHOW_START_MENU_SPACE = "com.farmerbb.taskbar.SHOW_START_MENU_SPACE";
    public static final String ACTION_SHOW_TASKBAR = "com.farmerbb.taskbar.SHOW_TASKBAR";
    public static final String ACTION_SORT_DESKTOP_ICONS = "com.farmerbb.taskbar.SORT_DESKTOP_ICONS";
    public static final String ACTION_START = "com.farmerbb.taskbar.START";
    public static final String ACTION_START_MENU_APPEARING = "com.farmerbb.taskbar.START_MENU_APPEARING";
    public static final String ACTION_START_MENU_DISAPPEARING = "com.farmerbb.taskbar.START_MENU_DISAPPEARING";
    public static final String ACTION_TEMP_HIDE_TASKBAR = "com.farmerbb.taskbar.TEMP_HIDE_TASKBAR";
    public static final String ACTION_TEMP_SHOW_TASKBAR = "com.farmerbb.taskbar.TEMP_SHOW_TASKBAR";
    public static final String ACTION_TOGGLE_DASHBOARD = "com.farmerbb.taskbar.TOGGLE_DASHBOARD";
    public static final String ACTION_TOGGLE_FREEFORM_MODE = "com.farmerbb.taskbar.TOGGLE_FREEFORM_MODE";
    public static final String ACTION_TOGGLE_START_MENU = "com.farmerbb.taskbar.TOGGLE_START_MENU";
    public static final String ACTION_TOUCH_ABSORBER_STATE_CHANGED = "com.farmerbb.taskbar.TOUCH_ABSORBER_STATE_CHANGED";
    public static final String ACTION_UPDATE_FREEFORM_CHECKBOX = "com.farmerbb.taskbar.UPDATE_FREEFORM_CHECKBOX";
    public static final String ACTION_UPDATE_HOME_SCREEN_MARGINS = "com.farmerbb.taskbar.UPDATE_HOME_SCREEN_MARGINS";
    public static final String ACTION_UPDATE_SWITCH = "com.farmerbb.taskbar.UPDATE_SWITCH";

    // SharedPreference keys

    public static final String PREF_ABOUT = "about";
    public static final String PREF_ACCENT_COLOR = "accent_color";
    public static final String PREF_ACCENT_COLOR_PREF = "accent_color_pref";
    public static final String PREF_ADD_SHORTCUT = "add_shortcut";
    public static final String PREF_ALT_BUTTON_CONFIG = "alt_button_config";
    public static final String PREF_ANCHOR = "anchor";
    public static final String PREF_ANDROID_X86_PREFS = "android_x86_prefs";
    public static final String PREF_APP_DRAWER_ICON = "app_drawer_icon";
    public static final String PREF_AUTO_HIDE_NAVBAR = "auto_hide_navbar";
    public static final String PREF_AUTO_HIDE_NAVBAR_DESKTOP_MODE = "auto_hide_navbar_desktop_mode";
    public static final String PREF_BACKGROUND_TINT = "background_tint";
    public static final String PREF_BACKGROUND_TINT_PREF = "background_tint_pref";
    public static final String PREF_BACKUP_SETTINGS = "backup_settings";
    public static final String PREF_BLACKLIST = "blacklist";
    public static final String PREF_BLISS_OS_PREFS = "bliss_os_prefs";
    public static final String PREF_BUTTON_BACK = "button_back";
    public static final String PREF_BUTTON_HOME = "button_home";
    public static final String PREF_BUTTON_RECENTS = "button_recents";
    public static final String PREF_CENTERED_ICONS = "centered_icons";
    public static final String PREF_CHROME_OS_CONTEXT_MENU_FIX = "chrome_os_context_menu_fix";
    public static final String PREF_CLEAR_PINNED_APPS = "clear_pinned_apps";
    public static final String PREF_DASHBOARD = "dashboard";
    public static final String PREF_DASHBOARD_GRID_SIZE = "dashboard_grid_size";
    public static final String PREF_DESKTOP_MODE = "desktop_mode";
    public static final String PREF_DISABLE_ANIMATIONS = "disable_animations";
    public static final String PREF_DISABLE_SCROLLING_LIST = "disable_scrolling_list";
    public static final String PREF_DISPLAY_DENSITY = "display_density";
    public static final String PREF_DONATE = "donate";
    public static final String PREF_DONT_STOP_DASHBOARD = "dont_stop_dashboard";
    public static final String PREF_ENABLE_RECENTS = "enable_recents";
    public static final String PREF_FORCE_NEW_WINDOW = "force_new_window";
    public static final String PREF_FREEFORM_HACK = "freeform_hack";
    public static final String PREF_FREEFORM_HACK_OVERRIDE = "freeform_hack_override";
    public static final String PREF_FULL_LENGTH = "full_length";
    public static final String PREF_HIDE_FOREGROUND = "hide_foreground";
    public static final String PREF_HIDE_ICON_LABELS = "hide_icon_labels";
    public static final String PREF_HIDE_TASKBAR = "hide_taskbar";
    public static final String PREF_HIDE_WHEN_KEYBOARD_SHOWN = "hide_when_keyboard_shown";
    public static final String PREF_ICON_PACK_LIST = "icon_pack_list";
    public static final String PREF_ICON_PACK_USE_MASK = "icon_pack_use_mask";
    public static final String PREF_INVISIBLE_BUTTON = "invisible_button";
    public static final String PREF_IS_HIDDEN = "is_hidden";
    public static final String PREF_IS_RESTARTING = "is_restarting";
    public static final String PREF_KEYBOARD_SHORTCUT = "keyboard_shortcut";
    public static final String PREF_LAUNCH_GAMES_FULLSCREEN = "launch_games_fullscreen";
    public static final String PREF_LAUNCHER = "launcher";
    public static final String PREF_MANAGE_APP_DATA = "manage_app_data";
    public static final String PREF_MAX_NUM_OF_RECENTS = "max_num_of_recents";
    public static final String PREF_NAVIGATION_BAR_BUTTONS = "navigation_bar_buttons";
    public static final String PREF_NOTIFICATION_SETTINGS = "notification_settings";
    public static final String PREF_POSITION = "position";
    public static final String PREF_PREF_SCREEN_ADVANCED = "pref_screen_advanced";
    public static final String PREF_PREF_SCREEN_APPEARANCE = "pref_screen_appearance";
    public static final String PREF_PREF_SCREEN_DESKTOP_MODE = "pref_screen_desktop_mode";
    public static final String PREF_PREF_SCREEN_FREEFORM = "pref_screen_freeform";
    public static final String PREF_PREF_SCREEN_GENERAL = "pref_screen_general";
    public static final String PREF_PREF_SCREEN_RECENT_APPS = "pref_screen_recent_apps";
    public static final String PREF_PRIMARY_LAUNCHER = "primary_launcher";
    public static final String PREF_RECENTS_AMOUNT = "recents_amount";
    public static final String PREF_REFRESH_FREQUENCY = "refresh_frequency";
    public static final String PREF_RESET_COLORS = "reset_colors";
    public static final String PREF_RESTORE_SETTINGS = "restore_settings";
    public static final String PREF_SAVE_WINDOW_SIZES = "save_window_sizes";
    public static final String PREF_SCROLLBAR = "scrollbar";
    public static final String PREF_SECONDSCREEN = "secondscreen";
    public static final String PREF_SET_LAUNCHER_DEFAULT = "set_launcher_default";
    public static final String PREF_SHORTCUT_ICON = "shortcut_icon";
    public static final String PREF_SHOW_BACKGROUND = "show_background";
    public static final String PREF_SHOW_FREEFORM_DISABLED_MESSAGE = "show_freeform_disabled_message";
    public static final String PREF_SHOW_SEARCH_BAR = "show_search_bar";
    public static final String PREF_SKIP_AUTO_HIDE_NAVBAR = "skip_auto_hide_navbar";
    public static final String PREF_SKIP_DISABLE_HOME_RECEIVER = "skip_disable_home_receiver";
    public static final String PREF_SORT_ORDER = "sort_order";
    public static final String PREF_START_BUTTON_IMAGE = "start_button_image";
    public static final String PREF_START_MENU_LAYOUT = "start_menu_layout";
    public static final String PREF_START_ON_BOOT = "start_on_boot";
    public static final String PREF_SYS_TRAY = "sys_tray";
    public static final String PREF_TASKBAR_ACTIVE = "taskbar_active";
    public static final String PREF_TASKER_ENABLED = "tasker_enabled";
    public static final String PREF_THEME = "theme";
    public static final String PREF_TRANSPARENT_START_MENU = "transparent_start_menu";
    public static final String PREF_VISUAL_FEEDBACK = "visual_feedback";
    public static final String PREF_WINDOW_SIZE = "window_size";

    // TaskbarPosition values

    public static final String POSITION_BOTTOM_LEFT = "bottom_left";
    public static final String POSITION_BOTTOM_RIGHT = "bottom_right";
    public static final String POSITION_BOTTOM_VERTICAL_LEFT = "bottom_vertical_left";
    public static final String POSITION_BOTTOM_VERTICAL_RIGHT = "bottom_vertical_right";
    public static final String POSITION_TOP_LEFT = "top_left";
    public static final String POSITION_TOP_RIGHT = "top_right";
    public static final String POSITION_TOP_VERTICAL_LEFT = "top_vertical_left";
    public static final String POSITION_TOP_VERTICAL_RIGHT = "top_vertical_right";
}
