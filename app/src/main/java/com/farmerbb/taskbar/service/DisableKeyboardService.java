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

package com.farmerbb.taskbar.service;

import android.hardware.display.DisplayManager;
import android.inputmethodservice.InputMethodService;

import com.farmerbb.taskbar.util.U;

public class DisableKeyboardService extends InputMethodService {

    @Override
    public boolean onShowInputRequested(int flags, boolean configChange) {
        return false;
    }

    private final DisplayManager.DisplayListener listener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
            checkIfShouldDisable();
        }

        @Override
        public void onDisplayChanged(int displayId) {
            checkIfShouldDisable();
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            checkIfShouldDisable();
        }
    };

    private void checkIfShouldDisable() {
        if(!U.isDesktopModeActive(this)) {
            U.setComponentEnabled(this, getClass(), false);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        DisplayManager manager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        manager.registerDisplayListener(listener, null);
    }

    @Override
    public void onDestroy() {
        DisplayManager manager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        manager.unregisterDisplayListener(listener);

        super.onDestroy();
    }
}