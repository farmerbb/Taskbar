/* Copyright 2019 Braden Farmer
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

package com.farmerbb.taskbar.ui;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;

import com.farmerbb.taskbar.util.U;

public abstract class UIHostService extends Service implements UIHost {

    private UIController controller;
    private WindowManager windowManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        controller = newController();
        controller.onCreateHost(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        controller.onRecreateHost(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        controller.onDestroyHost(this);
    }

    @Override
    public void addView(View view, ViewParams params) {
        final WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams(
                params.width,
                params.height,
                U.getOverlayType(),
                params.flags,
                PixelFormat.TRANSLUCENT
        );

        if(params.gravity > -1)
            wmParams.gravity = params.gravity;

        windowManager.addView(view, wmParams);
    }

    @Override
    public void removeView(View view) {
        windowManager.removeView(view);
    }

    @Override
    public void terminate() {
        stopSelf();
    }

    public abstract UIController newController();
}