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

package com.farmerbb.taskbar.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class DimScreenActivity extends AppCompatActivity {

    private boolean isResumed = false;
    private boolean isTopResumed = false;

    private final DisplayManager.DisplayListener listener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
            checkIfShouldFinish();
        }

        @Override
        public void onDisplayChanged(int displayId) {
            checkIfShouldFinish();
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            checkIfShouldFinish();
        }
    };

    private boolean checkIfShouldFinish() {
        boolean shouldFinish = !U.isDesktopModeActive(this);
        if(shouldFinish) finish();

        return shouldFinish;
    }

    private final BroadcastReceiver unDimScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dimScreen(false);
        }
    };

    private final BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tb_activity_hsl_config);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        TextView textView = findViewById(R.id.textView);
        textView.setText(R.string.tb_desktop_mode_is_active);

        DisplayManager manager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        manager.registerDisplayListener(listener, null);

        U.registerReceiver(this, unDimScreenReceiver, ACTION_UNDIM_SCREEN);
        U.registerReceiver(this, finishReceiver,
                ACTION_FINISH_DIM_SCREEN_ACTIVITY, ACTION_KILL_HOME_ACTIVITY);

        if(getSupportActionBar() == null) return;

        // Make action bar invisible
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0));

        setTitle(null);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(isResumed && isTopResumed)
            dimScreen(hasFocus);
    }

    @Override
    protected void onDestroy() {
        DisplayManager manager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        manager.unregisterDisplayListener(listener);
        
        U.unregisterReceiver(this, unDimScreenReceiver);
        U.unregisterReceiver(this, finishReceiver);

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate action bar menu
        getMenuInflater().inflate(R.menu.tb_hsl_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        if(item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        return true;
    }

    @Override
    public void onBackPressed() {}

    @Override
    public void onTopResumedActivityChanged(boolean isTopResumedActivity) {
        isTopResumed = isTopResumedActivity;
        dimScreen(true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        isResumed = false;
        dimScreen(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        isResumed = true;
        dimScreen(true);
    }

    private void dimScreen(boolean shouldDim) {
        if(checkIfShouldFinish()) return;

        // From android.os.PowerManager
        int BRIGHTNESS_CONSTRAINT_TYPE_MINIMUM = 0;
        Float screenBrightness = null;

        if(shouldDim) {
            U.allowReflection();
            try {
                screenBrightness = (Float) Class.forName("android.os.PowerManager")
                        .getMethod("getBrightnessConstraint", int.class)
                        .invoke(getSystemService(POWER_SERVICE), BRIGHTNESS_CONSTRAINT_TYPE_MINIMUM);
            } catch (Exception ignored) {}
        }

        if(screenBrightness == null)
            screenBrightness = -1f;

        WindowManager.LayoutParams wmParams = getWindow().getAttributes();
        wmParams.screenBrightness = screenBrightness;
        getWindow().setAttributes(wmParams);

        if(shouldDim) {
            View view = getWindow().getDecorView();
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }
}
