/* Based on code by Leonardo Fischer
 * See https://github.com/lgfischer/WidgetHostExample
 *
 * Copyright 2016 Braden Farmer
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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.DashboardActivity;
import com.farmerbb.taskbar.activity.DashboardActivityDark;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.LauncherAppWidgetHost;
import com.farmerbb.taskbar.util.LauncherAppWidgetHostView;
import com.farmerbb.taskbar.util.LauncherHelper;
import com.farmerbb.taskbar.util.U;

public class DashboardService extends Service {

    private AppWidgetManager mAppWidgetManager;
    private LauncherAppWidgetHost mAppWidgetHost;

    private WindowManager windowManager;
    private LinearLayout layout;

    private SparseArray<FrameLayout> cells = new SparseArray<>();

    private final int APPWIDGET_HOST_ID = 123;

    private int columns;
    private int rows;
    private int maxSize;
    private int previouslySelectedCell = -1;

    private boolean tutorialShown = false;

    private View.OnClickListener ocl = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            toggleDashboard();
        }
    };

    private View.OnClickListener cellOcl = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Bundle bundle = (Bundle) view.getTag();
            int cellId = bundle.getInt("cellId");
            int appWidgetId = bundle.getInt("appWidgetId", -1);

            int currentlySelectedCell = appWidgetId == -1 ? cellId : -1;

            if(appWidgetId == -1 && currentlySelectedCell == previouslySelectedCell) {
                layout.setVisibility(View.GONE);

                FrameLayout frameLayout = cells.get(currentlySelectedCell);
                frameLayout.findViewById(R.id.empty).setVisibility(View.GONE);

                Intent intent = new Intent("com.farmerbb.taskbar.ADD_WIDGET_REQUESTED");
                intent.putExtra("appWidgetId", APPWIDGET_HOST_ID);
                intent.putExtra("cellId", cellId);
                LocalBroadcastManager.getInstance(DashboardService.this).sendBroadcast(intent);
            } else {
                for(int i = 0; i < maxSize; i++) {
                    FrameLayout frameLayout = cells.get(i);
                    frameLayout.findViewById(R.id.empty).setVisibility(i == currentlySelectedCell ? View.VISIBLE : View.GONE);
                }

                previouslySelectedCell = currentlySelectedCell;
            }
        }
    };

    private View.OnLongClickListener olcl = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            layout.setVisibility(View.GONE);

            Bundle bundle = (Bundle) v.getTag();
            int cellId = bundle.getInt("cellId");

            Intent intent = new Intent("com.farmerbb.taskbar.REMOVE_WIDGET_REQUESTED");
            intent.putExtra("cellId", cellId);
            LocalBroadcastManager.getInstance(DashboardService.this).sendBroadcast(intent);

            return true;
        }
    };

    private BroadcastReceiver toggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            toggleDashboard();
        }
    };

    private BroadcastReceiver addWidgetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            layout.setVisibility(View.VISIBLE);

            if(intent.hasExtra("appWidgetId") && intent.hasExtra("cellId")) {
                int appWidgetId = intent.getExtras().getInt("appWidgetId", -1);
                int cellId = intent.getExtras().getInt("cellId", -1);

                AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

                LauncherAppWidgetHostView hostView = (LauncherAppWidgetHostView) mAppWidgetHost.createView(DashboardService.this, appWidgetId, appWidgetInfo);
                hostView.setAppWidget(appWidgetId, appWidgetInfo);
                hostView.setOnLongClickListener(olcl);

                Bundle bundle = new Bundle();
                bundle.putInt("cellId", cellId);
                hostView.setTag(bundle);

                FrameLayout cellLayout = cells.get(cellId);
                cellLayout.findViewById(R.id.empty).setVisibility(View.GONE);
                cellLayout.setOnLongClickListener(olcl);

                LinearLayout linearLayout = (LinearLayout) cellLayout.findViewById(R.id.dashboard);
                linearLayout.addView(hostView);

                Bundle bundle2 = (Bundle) cellLayout.getTag();
                bundle2.putInt("appWidgetId", appWidgetId);
                cellLayout.setTag(bundle2);
            }
        }
    };

    private BroadcastReceiver removeWidgetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            layout.setVisibility(View.VISIBLE);

            if(intent.hasExtra("cellId")) {
                int cellId = intent.getExtras().getInt("cellId", -1);

                FrameLayout cellLayout = cells.get(cellId);
                Bundle bundle = (Bundle) cellLayout.getTag();

                mAppWidgetHost.deleteAppWidgetId(bundle.getInt("appWidgetId"));
                bundle.remove("appWidgetId");

                LinearLayout linearLayout = (LinearLayout) cellLayout.findViewById(R.id.dashboard);
                linearLayout.removeAllViews();

                cellLayout.setTag(bundle);
                cellLayout.setOnClickListener(cellOcl);
                cellLayout.setOnLongClickListener(null);
            }
        }
    };

    private BroadcastReceiver hideReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideDashboard();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean("dashboard", false)) {
            if(pref.getBoolean("taskbar_active", false) || LauncherHelper.getInstance().isOnHomeScreen()) {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))
                    drawDashboard();
                else {
                    pref.edit().putBoolean("taskbar_active", false).apply();

                    stopSelf();
                }
            } else stopSelf();
        } else stopSelf();
    }

    @SuppressLint("RtlHardcoded")
    private void drawDashboard() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                PixelFormat.TRANSLUCENT);

        // Initialize views
        layout = new LinearLayout(this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.setVisibility(View.GONE);

        int paddingSize = getResources().getDimensionPixelSize(R.dimen.icon_size);

        switch(U.getTaskbarPosition(this)) {
            case "top_vertical_left":
            case "bottom_vertical_left":
                layout.setPadding(paddingSize, 0, 0, 0);
                break;
            case "top_left":
            case "top_right":
                layout.setPadding(0, paddingSize, 0, 0);
                break;
            case "top_vertical_right":
            case "bottom_vertical_right":
                layout.setPadding(0, 0, paddingSize, 0);
                break;
            case "bottom_left":
            case "bottom_right":
                layout.setPadding(0, 0, 0, paddingSize);
                break;
        }

        SharedPreferences pref = U.getSharedPreferences(this);
        int width = pref.getInt("dashboard_width", getResources().getInteger(R.integer.dashboard_width));
        int height = pref.getInt("dashboard_height", getResources().getInteger(R.integer.dashboard_height));

        boolean isPortrait = getApplicationContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        boolean isLandscape = getApplicationContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        if(isPortrait) {
            columns = height;
            rows = width;
        }

        if(isLandscape) {
            columns = width;
            rows = height;
        }

        maxSize = columns * rows;

        int cellCount = 0;

        for(int i = 0; i < columns; i++) {
            LinearLayout layout2 = new LinearLayout(this);
            layout2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
            layout2.setOrientation(LinearLayout.VERTICAL);

            for(int j = 0; j < rows; j++) {
                FrameLayout cellLayout = (FrameLayout) View.inflate(this, R.layout.dashboard, null);
                cellLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                cellLayout.setOnClickListener(cellOcl);

                Bundle bundle = new Bundle();
                bundle.putInt("cellId", cellCount);

                cellLayout.setTag(bundle);
                cells.put(cellCount, cellLayout);
                cellCount++;

                layout2.addView(cellLayout);
            }

            layout.addView(layout2);
        }

        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mAppWidgetHost = new LauncherAppWidgetHost(this, APPWIDGET_HOST_ID);
        mAppWidgetHost.stopListening();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(toggleReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(addWidgetReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(removeWidgetReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hideReceiver);

        LocalBroadcastManager.getInstance(this).registerReceiver(toggleReceiver, new IntentFilter("com.farmerbb.taskbar.TOGGLE_DASHBOARD"));
        LocalBroadcastManager.getInstance(this).registerReceiver(addWidgetReceiver, new IntentFilter("com.farmerbb.taskbar.ADD_WIDGET_COMPLETED"));
        LocalBroadcastManager.getInstance(this).registerReceiver(removeWidgetReceiver, new IntentFilter("com.farmerbb.taskbar.REMOVE_WIDGET_COMPLETED"));
        LocalBroadcastManager.getInstance(this).registerReceiver(hideReceiver, new IntentFilter("com.farmerbb.taskbar.HIDE_DASHBOARD"));

        windowManager.addView(layout, params);
    }

    private void toggleDashboard() {
        if(layout.getVisibility() == View.GONE)
            showDashboard();
        else
            hideDashboard();
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.N)
    private void showDashboard() {
        layout.setOnClickListener(ocl);
        layout.setVisibility(View.VISIBLE);

        mAppWidgetHost.startListening();

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.DASHBOARD_APPEARING"));

        boolean inFreeformMode = FreeformHackHelper.getInstance().isInFreeformWorkspace();

        final SharedPreferences pref = U.getSharedPreferences(this);
        Intent intent = null;

        switch(pref.getString("theme", "light")) {
            case "light":
                intent = new Intent(this, DashboardActivity.class);
                break;
            case "dark":
                intent = new Intent(this, DashboardActivityDark.class);
                break;
        }

        if(intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        }

        if(inFreeformMode) {
            U.launchAppFullscreen(this, intent);
        } else
            startActivity(intent);

        if(!tutorialShown) {
            U.showToast(this, R.string.dashboard_tutorial); // TODO if num of saved widgets == 0
            tutorialShown = true;
        }
    }

    private void hideDashboard() {
        layout.setOnClickListener(null);
        layout.setVisibility(View.INVISIBLE);

        mAppWidgetHost.stopListening();

        for(int i = 0; i < maxSize; i++) {
            FrameLayout frameLayout = cells.get(i);
            frameLayout.findViewById(R.id.empty).setVisibility(View.GONE);
        }

        previouslySelectedCell = -1;

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.DASHBOARD_DISAPPEARING"));

        layout.postDelayed(new Runnable() {
            @Override
            public void run() {
                layout.setVisibility(View.GONE);
            }
        }, 250);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(layout != null) {
            try {
                windowManager.removeView(layout);
            } catch (IllegalArgumentException e) { /* Gracefully fail */ }

            SharedPreferences pref = U.getSharedPreferences(this);
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))
                drawDashboard();
            else {
                pref.edit().putBoolean("taskbar_active", false).apply();

                stopSelf();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(layout != null)
            try {
                windowManager.removeView(layout);
            } catch (IllegalArgumentException e) { /* Gracefully fail */ }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(toggleReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(addWidgetReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(removeWidgetReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hideReceiver);
    }
}
