/* Copyright 2018 Braden Farmer
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
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.DisplayInfo;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.U;

public class TouchAbsorberActivity extends Activity {

    private static long lastStartTime = 0;

    private BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @SuppressLint("HardwareIds")
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.incognito);

        DisplayInfo display = U.getDisplayInfo(this);
        LinearLayout layout = findViewById(R.id.incognitoLayout);
        layout.setLayoutParams(new FrameLayout.LayoutParams(display.width, display.height));

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(finishReceiver, new IntentFilter("com.farmerbb.taskbar.FINISH_FREEFORM_ACTIVITY"));

        FreeformHackHelper.getInstance().setTouchAbsorberActive(true);
        lbm.sendBroadcast(new Intent("com.farmerbb.taskbar.TOUCH_ABSORBER_STATE_CHANGED"));

        lastStartTime = System.currentTimeMillis();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(finishReceiver);

        FreeformHackHelper.getInstance().setTouchAbsorberActive(false);
        lbm.sendBroadcast(new Intent("com.farmerbb.taskbar.TOUCH_ABSORBER_STATE_CHANGED"));

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if((!U.isAccessibilityServiceEnabled(this) && !U.hasWriteSecureSettingsPermission(this))
                || lastStartTime > System.currentTimeMillis() - 250)
            return;

        super.onBackPressed();

        new Handler().postDelayed(() ->
                U.sendAccessibilityAction(this, AccessibilityService.GLOBAL_ACTION_BACK, () ->
                        new Handler().postDelayed(() -> U.startTouchAbsorberActivity(this), 100)
                ), 100);
    }
}
