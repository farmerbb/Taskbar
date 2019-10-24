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

package com.farmerbb.taskbar.util;

public class LauncherHelper {

    private boolean onPrimaryHomeScreen = false;
    private boolean onSecondaryHomeScreen = false;

    private static LauncherHelper theInstance;

    private LauncherHelper() {}

    public static LauncherHelper getInstance() {
        if(theInstance == null) theInstance = new LauncherHelper();

        return theInstance;
    }

    public boolean isOnHomeScreen() {
        return isOnHomeScreen(true, true);
    }

    public boolean isOnHomeScreen(boolean checkPrimary, boolean checkSecondary) {
        if(checkPrimary && checkSecondary)
            return onPrimaryHomeScreen || onSecondaryHomeScreen;

        if(!checkPrimary && checkSecondary)
            return onSecondaryHomeScreen;

        if(checkPrimary)
            return onPrimaryHomeScreen;

        return false;
    }

    public void setOnPrimaryHomeScreen(boolean value) {
        onPrimaryHomeScreen = value;
    }

    public void setOnSecondaryHomeScreen(boolean value) {
        onSecondaryHomeScreen = value;
    }
}