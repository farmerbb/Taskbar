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
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.receiver.KeyboardChangeReceiver;
import com.farmerbb.taskbar.util.U;

import java.util.Random;

public class DisableKeyboardService extends InputMethodService {

    Integer notificationId;

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
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        boolean isEditingText = attribute.inputType != InputType.TYPE_NULL;
        boolean hasHardwareKeyboard = getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;

        if(notificationId == null && isEditingText && !hasHardwareKeyboard) {
            Intent keyboardChangeIntent = new Intent(this, KeyboardChangeReceiver.class);
            PendingIntent keyboardChangePendingIntent = PendingIntent.getBroadcast(this, 0, keyboardChangeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder notification = new NotificationCompat.Builder(this)
                    .setContentIntent(keyboardChangePendingIntent)
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setContentTitle(getString(R.string.tb_disabling_soft_keyboard))
                    .setContentText(getString(R.string.tb_tap_to_change_keyboards))
                    .setOngoing(true)
                    .setShowWhen(false);

            notificationId = new Random().nextInt();

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(notificationId, notification.build());

            boolean autoShowInputMethodPicker = false;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                switch(devicePolicyManager.getStorageEncryptionStatus()) {
                    case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE:
                    case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER:
                        break;
                    default:
                        autoShowInputMethodPicker = true;
                        break;
                }
            }

            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if(keyguardManager.inKeyguardRestrictedInputMode() && autoShowInputMethodPicker) {
                InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                manager.showInputMethodPicker();
            }
        } else if(notificationId != null && !isEditingText) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(notificationId);

            notificationId = null;
        }
    }

    @Override
    public void onDestroy() {
        if(notificationId != null) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(notificationId);

            notificationId = null;
        }

        DisplayManager manager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        manager.unregisterDisplayListener(listener);

        super.onDestroy();
    }
}