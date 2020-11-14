/* Copyright 2017 Braden Farmer
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

import com.farmerbb.taskbar.R;

class ToastFrameworkImpl implements ToastInterface {
    private final Toast toast;

    @SuppressLint("ShowToast")
    ToastFrameworkImpl(Context context, String message, int length) {
        int offset = context.getResources().getDimensionPixelSize(R.dimen.tb_toast_y_offset);
        if(U.getCurrentApiVersion() > 29.0 && U.isDesktopModeActive(context)) {
            offset = offset + U.getNavbarHeight(context);
        }

        toast = Toast.makeText(context, message, length);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_VERTICAL, 0, offset);
    }

    @Override
    public void show() {
        toast.show();
    }

    @Override
    public void cancel() {
        toast.cancel();
    }
}
