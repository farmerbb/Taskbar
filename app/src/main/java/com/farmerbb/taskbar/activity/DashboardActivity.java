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
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.helper.DashboardHelper;
import com.farmerbb.taskbar.util.DisplayInfo;
import com.farmerbb.taskbar.helper.LauncherHelper;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class DashboardActivity extends Activity {

    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHost mAppWidgetHost;

    private final int REQUEST_PICK_APPWIDGET = 456;
    private final int REQUEST_CREATE_APPWIDGET = 789;

    private boolean shouldFinish = true;
    private boolean shouldCollapse = true;
    private boolean contextMenuFix = false;
    private int cellId = -1;

    private final BroadcastReceiver addWidgetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mAppWidgetManager = AppWidgetManager.getInstance(context);
            mAppWidgetHost = new AppWidgetHost(context, intent.getIntExtra(EXTRA_APPWIDGET_ID, -1));
            cellId = intent.getIntExtra(EXTRA_CELL_ID, -1);

            int appWidgetId = mAppWidgetHost.allocateAppWidgetId();
            Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
            pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

            try {
                startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
                U.sendBroadcast(DashboardActivity.this, ACTION_TEMP_HIDE_TASKBAR);
            } catch (ActivityNotFoundException e) {
                U.showToast(DashboardActivity.this, R.string.tb_lock_device_not_supported);
                finish();
            }

            shouldFinish = false;
        }
    };

    private final BroadcastReceiver removeWidgetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            cellId = intent.getIntExtra(EXTRA_CELL_ID, -1);

            AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
            builder.setTitle(R.string.tb_remove_widget)
                    .setMessage(R.string.tb_are_you_sure)
                    .setNegativeButton(R.string.tb_action_cancel, (dialog, which) -> {
                        U.sendBroadcast(DashboardActivity.this, ACTION_REMOVE_WIDGET_COMPLETED);

                        shouldFinish = true;
                    })
                    .setPositiveButton(R.string.tb_action_ok, (dialog, which) -> {
                        Intent intent1 = new Intent(ACTION_REMOVE_WIDGET_COMPLETED);
                        intent1.putExtra(EXTRA_CELL_ID, cellId);
                        U.sendBroadcast(DashboardActivity.this, intent1);

                        shouldFinish = true;
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.setCancelable(false);

            shouldFinish = false;
        }
    };

    private final BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            shouldCollapse = false;

            if(contextMenuFix)
                U.startFreeformHack(DashboardActivity.this);

            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        contextMenuFix = getIntent().hasExtra(EXTRA_CONTEXT_MENU_FIX);

        // Detect outside touches, and finish the activity when one is detected
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);

        DisplayInfo display = U.getDisplayInfo(this);

        setContentView(R.layout.tb_incognito);

        LinearLayout layout = findViewById(R.id.incognitoLayout);
        layout.setLayoutParams(new FrameLayout.LayoutParams(display.width, display.height));

        U.registerReceiver(this, addWidgetReceiver, ACTION_ADD_WIDGET_REQUESTED);
        U.registerReceiver(this, removeWidgetReceiver, ACTION_REMOVE_WIDGET_REQUESTED);
        U.registerReceiver(this, finishReceiver, ACTION_DASHBOARD_DISAPPEARING);

        if(!DashboardHelper.getInstance().isDashboardOpen()) finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_OUTSIDE) onBackPressed();
        return super.onTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        if(contextMenuFix) {
            U.startFreeformHack(this);
        }

        U.sendBroadcast(this, ACTION_HIDE_DASHBOARD);
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
    public void onTopResumedActivityChanged(boolean isTopResumedActivity) {
        if(!isTopResumedActivity)
            performOnPauseLogic();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
            performOnPauseLogic();
    }

    private void performOnPauseLogic() {
        if(shouldFinish) {
            if(shouldCollapse) {
                if(U.shouldCollapse(this, true)) {
                    U.sendBroadcast(this, ACTION_HIDE_TASKBAR);
                } else {
                    U.sendBroadcast(this, ACTION_HIDE_START_MENU);
                }
            }

            contextMenuFix = false;
            onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        U.unregisterReceiver(this, addWidgetReceiver);
        U.unregisterReceiver(this, removeWidgetReceiver);
        U.unregisterReceiver(this, finishReceiver);
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

            U.sendBroadcast(this, ACTION_ADD_WIDGET_COMPLETED);
            U.sendBroadcast(this, ACTION_TEMP_SHOW_TASKBAR);

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

            try {
                startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
            } catch (Exception e) {
                U.showToast(this, R.string.tb_error_creating_widget);
                return;
            }

            SharedPreferences pref = U.getSharedPreferences(this);
            if(LauncherHelper.getInstance().isOnHomeScreen(this)
                    && (!pref.getBoolean(PREF_TASKBAR_ACTIVE, false)
                    || pref.getBoolean(PREF_IS_HIDDEN, false)))
                pref.edit().putBoolean(PREF_DONT_STOP_DASHBOARD, true).apply();

            shouldFinish = false;
        } else {
            createWidget(data);
        }
    }

    private void createWidget(Intent data) {
        Intent intent = new Intent(ACTION_ADD_WIDGET_COMPLETED);
        intent.putExtra(
                EXTRA_APPWIDGET_ID,
                data.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        );
        intent.putExtra(EXTRA_CELL_ID, cellId);

        U.sendBroadcast(this, intent);
        U.sendBroadcast(this, ACTION_TEMP_SHOW_TASKBAR);

        shouldFinish = true;
    }
}
