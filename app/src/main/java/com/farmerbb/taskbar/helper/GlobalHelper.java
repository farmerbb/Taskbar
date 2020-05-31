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

package com.farmerbb.taskbar.helper;

import android.os.Build;

public class GlobalHelper {

    private int onMainActivity = 0;
    private boolean reflectionAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.P;

    private static GlobalHelper theInstance;

    private GlobalHelper() {}

    public static GlobalHelper getInstance() {
        if(theInstance == null) theInstance = new GlobalHelper();

        return theInstance;
    }

    public boolean isOnMainActivity() {
        return onMainActivity > 0;
    }

    public void setOnMainActivity(boolean value) {
        int factor = value ? 1 : -1;
        onMainActivity = Math.max(0, onMainActivity + factor);
    }

    public boolean isReflectionAllowed() {
        return reflectionAllowed;
    }

    public void setReflectionAllowed(boolean value) {
        reflectionAllowed = value;
    }
}