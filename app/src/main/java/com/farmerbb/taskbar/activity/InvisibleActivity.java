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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class InvisibleActivity extends Activity {

    private final BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Detect outside touches, and finish the activity when one is detected
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);

        U.registerReceiver(this, finishReceiver, ACTION_START_MENU_DISAPPEARING);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_OUTSIDE) onBackPressed();
        return super.onTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        U.sendBroadcast(this, ACTION_HIDE_START_MENU);
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN) {
            event.getKeyCode();

            return true;
        }
        return super.dispatchKeyShortcutEvent(event);
    }

    @Override
    protected void onStop() {
        super.onStop();
        onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        U.unregisterReceiver(this, finishReceiver);
    }
}
