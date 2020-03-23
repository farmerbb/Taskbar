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

package com.farmerbb.taskbar.content;

public class TaskbarIntent {

    private TaskbarIntent() {
        // Empty constructor
    }

    public static final String ACTION_HIDE_TASKBAR = "com.farmerbb.taskbar.HIDE_TASKBAR";
    public static final String ACTION_SHOW_TASKBAR = "com.farmerbb.taskbar.SHOW_TASKBAR";
    public static final String ACTION_TEMP_HIDE_TASKBAR = "com.farmerbb.taskbar.TEMP_HIDE_TASKBAR";
    public static final String ACTION_TEMP_SHOW_TASKBAR = "com.farmerbb.taskbar.TEMP_SHOW_TASKBAR";
    public static final String ACTION_FORCE_TASKBAR_RESTART =
            "com.farmerbb.taskbar.FORCE_TASKBAR_RESTART";
    public static final String ACTION_SHOW_HIDE_TASKBAR = "com.farmerbb.taskbar.SHOW_HIDE_TASKBAR";
    public static final String ACTION_HIDE_START_MENU = "com.farmerbb.taskbar.HIDE_START_MENU";
    public static final String ACTION_TOGGLE_START_MENU = "com.farmerbb.taskbar.TOGGLE_START_MENU";
    public static final String ACTION_START_MENU_DISAPPEARING =
            "com.farmerbb.taskbar.START_MENU_DISAPPEARING";
    public static final String ACTION_START_MENU_APPEARING =
            "com.farmerbb.taskbar.START_MENU_APPEARING";
    public static final String ACTION_RESET_START_MENU = "com.farmerbb.taskbar.RESET_START_MENU";
    public static final String ACTION_HIDE_START_MENU_NO_RESET =
            "com.farmerbb.taskbar.HIDE_START_MENU_NO_RESET";
    public static final String ACTION_SHOW_START_MENU_SPACE =
            "com.farmerbb.taskbar.SHOW_START_MENU_SPACE";
    public static final String ACTION_HIDE_START_MENU_SPACE =
            "com.farmerbb.taskbar.HIDE_START_MENU_SPACE";
    public static final String ACTION_UPDATE_FREEFORM_CHECKBOX =
            "com.farmerbb.taskbar.UPDATE_FREEFORM_CHECKBOX";
    public static final String ACTION_FINISH_FREEFORM_ACTIVITY =
            "com.farmerbb.taskbar.FINISH_FREEFORM_ACTIVITY";
    public static final String ACTION_FREEFORM_PREF_CHANGED =
            "com.farmerbb.taskbar.FREEFORM_PREF_CHANGED";
    public static final String ACTION_TOGGLE_FREEFORM_MODE =
            "com.farmerbb.taskbar.TOGGLE_FREEFORM_MODE";
    public static final String ACTION_CONTEXT_MENU_APPEARING =
            "com.farmerbb.taskbar.CONTEXT_MENU_APPEARING";
    public static final String ACTION_CONTEXT_MENU_DISAPPEARING =
            "com.farmerbb.taskbar.CONTEXT_MENU_DISAPPEARING";
    public static final String ACTION_HIDE_CONTEXT_MENU =
            "com.farmerbb.taskbar.HIDE_CONTEXT_MENU";
    public static final String ACTION_ADD_WIDGET_COMPLETED =
            "com.farmerbb.taskbar.ADD_WIDGET_COMPLETED";
    public static final String ACTION_REMOVE_WIDGET_COMPLETED =
            "com.farmerbb.taskbar.REMOVE_WIDGET_COMPLETED";
    public static final String ACTION_ADD_WIDGET_REQUESTED =
            "com.farmerbb.taskbar.ADD_WIDGET_REQUESTED";
    public static final String ACTION_REMOVE_WIDGET_REQUESTED =
            "com.farmerbb.taskbar.REMOVE_WIDGET_REQUESTED";
    public static final String ACTION_REFRESH_DESKTOP_ICONS =
            "com.farmerbb.taskbar.REFRESH_DESKTOP_ICONS";
    public static final String ACTION_SORT_DESKTOP_ICONS =
            "com.farmerbb.taskbar.SORT_DESKTOP_ICONS";
    public static final String ACTION_ENTER_ICON_ARRANGE_MODE =
            "com.farmerbb.taskbar.ENTER_ICON_ARRANGE_MODE";
    public static final String ACTION_TOGGLE_DASHBOARD =
            "com.farmerbb.taskbar.TOGGLE_DASHBOARD";
}
