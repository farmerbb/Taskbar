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

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;

import com.farmerbb.taskbar.R;
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

    @TargetApi(Build.VERSION_CODES.P)
    private void checkIfShouldDisable() {
        if(!U.isDesktopModeActive(this)) {
            switchToPreviousInputMethod();
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

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        boolean isEditingText = attribute.inputType != InputType.TYPE_NULL;
        boolean hasHardwareKeyboard = getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;

        if(isEditingText && !hasHardwareKeyboard) {
            U.showToast(this, R.string.tb_desktop_mode_ime_fix_toast_alt);
            checkIfShouldDisable();
        }
    }
}