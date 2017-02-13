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

package com.farmerbb.taskbar.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.DashboardHelper;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.U;

public class DashboardActivity extends Activity {

    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHost mAppWidgetHost;

    private final int REQUEST_PICK_APPWIDGET = 456;
    private final int REQUEST_CREATE_APPWIDGET = 789;

    private boolean shouldFinish = true;
    private boolean shouldCollapse = true;
    private int cellId = -1;

    private BroadcastReceiver addWidgetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mAppWidgetManager = AppWidgetManager.getInstance(context);
            mAppWidgetHost = new AppWidgetHost(context, intent.getIntExtra("appWidgetId", -1));
            cellId = intent.getIntExtra("cellId", -1);

            int appWidgetId = mAppWidgetHost.allocateAppWidgetId();
            Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
            pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

            try {
                startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
            } catch (ActivityNotFoundException e) {
                U.showToast(DashboardActivity.this, R.string.lock_device_not_supported);
                finish();
            }

            shouldFinish = false;
        }
    };

    private BroadcastReceiver removeWidgetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            cellId = intent.getIntExtra("cellId", -1);

            AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
            builder.setTitle(R.string.remove_widget)
                    .setMessage(R.string.are_you_sure)
                    .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                        LocalBroadcastManager.getInstance(DashboardActivity.this).sendBroadcast(new Intent("com.farmerbb.taskbar.REMOVE_WIDGET_COMPLETED"));

                        shouldFinish = true;
                    })
                    .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                        Intent intent1 = new Intent("com.farmerbb.taskbar.REMOVE_WIDGET_COMPLETED");
                        intent1.putExtra("cellId", cellId);
                        LocalBroadcastManager.getInstance(DashboardActivity.this).sendBroadcast(intent1);

                        shouldFinish = true;
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.setCancelable(false);

            shouldFinish = false;
        }
    };

    private BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            shouldCollapse = false;
            finish();
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Detect outside touches, and finish the activity when one is detected
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);

        DisplayManager dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);

        setContentView(R.layout.incognito);

        LinearLayout layout = (LinearLayout) findViewById(R.id.incognitoLayout);
        layout.setLayoutParams(new FrameLayout.LayoutParams(display.getWidth(), display.getHeight()));

        LocalBroadcastManager.getInstance(this).registerReceiver(addWidgetReceiver, new IntentFilter("com.farmerbb.taskbar.ADD_WIDGET_REQUESTED"));
        LocalBroadcastManager.getInstance(this).registerReceiver(removeWidgetReceiver, new IntentFilter("com.farmerbb.taskbar.REMOVE_WIDGET_REQUESTED"));
        LocalBroadcastManager.getInstance(this).registerReceiver(finishReceiver, new IntentFilter("com.farmerbb.taskbar.DASHBOARD_DISAPPEARING"));

        if(!DashboardHelper.getInstance().isDashboardOpen()) finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_OUTSIDE) onBackPressed();
        return super.onTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_DASHBOARD"));
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
    protected void onPause() {
        super.onPause();

        if(shouldFinish) {
            if(shouldCollapse) {
                SharedPreferences pref = U.getSharedPreferences(this);
                if(pref.getBoolean("hide_taskbar", true) && !FreeformHackHelper.getInstance().isInFreeformWorkspace())
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_TASKBAR"));
                else
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
            }

            onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(addWidgetReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(removeWidgetReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(finishReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            if(requestCode == REQUEST_PICK_APPWIDGET) {
                configureWidget(data);
            } else if(requestCode == REQUEST_CREATE_APPWIDGET) {
                createWidget(data);
            }
        } else if(resultCode == RESULT_CANCELED) {
            if(data != null) {
                int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                if(appWidgetId != -1) {
                    mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                }
            }

            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.ADD_WIDGET_COMPLETED"));

            shouldFinish = true;
        }
    }

    private void configureWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if(appWidgetInfo.configure != null) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);

            shouldFinish = false;
        } else {
            createWidget(data);
        }
    }

    private void createWidget(Intent data) {
        Intent intent = new Intent("com.farmerbb.taskbar.ADD_WIDGET_COMPLETED");
        intent.putExtra("appWidgetId", data.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1));
        intent.putExtra("cellId", cellId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        shouldFinish = true;
    }
}
