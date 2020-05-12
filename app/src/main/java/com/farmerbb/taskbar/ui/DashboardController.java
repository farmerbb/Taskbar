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

package com.farmerbb.taskbar.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import androidx.core.graphics.ColorUtils;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.DashboardActivity;
import com.farmerbb.taskbar.util.TaskbarPosition;
import com.farmerbb.taskbar.util.DashboardHelper;
import com.farmerbb.taskbar.widget.DashboardCell;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.U;

import java.util.List;

import static com.farmerbb.taskbar.util.Constants.*;

public class DashboardController extends UIController {

    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHost mAppWidgetHost;

    private LinearLayout layout;

    private SparseArray<DashboardCell> cells = new SparseArray<>();
    private SparseArray<AppWidgetHostView> widgets = new SparseArray<>();

    private final int APPWIDGET_HOST_ID = 123;

    private int columns;
    private int rows;
    private int maxSize;
    private int previouslySelectedCell = -1;

    private View.OnClickListener ocl = view -> toggleDashboard();

    private View.OnClickListener cellOcl = view -> cellClick(view, true);

    private View.OnHoverListener cellOhl = (v, event) -> {
        cellClick(v, false);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            v.setPointerIcon(PointerIcon.getSystemIcon(context, PointerIcon.TYPE_DEFAULT));

        return false;
    };

    private View.OnLongClickListener olcl = v -> {
        cellLongClick(v);
        return true;
    };

    private View.OnGenericMotionListener ogml = (view, motionEvent) -> {
        if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY)
            cellLongClick(view);

        return false;
    };

    private DashboardCell.OnInterceptedLongPressListener listener = this::cellLongClick;

    private BroadcastReceiver toggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            toggleDashboard();
        }
    };

    private BroadcastReceiver addWidgetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            fadeIn();

            if(intent.hasExtra("appWidgetId") && intent.hasExtra("cellId")) {
                int appWidgetId = intent.getExtras().getInt("appWidgetId", -1);
                int cellId = intent.getExtras().getInt("cellId", -1);

                addWidget(appWidgetId, cellId, true);
            }
        }
    };

    private BroadcastReceiver removeWidgetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            fadeIn();

            if(intent.hasExtra("cellId")) {
                int cellId = intent.getExtras().getInt("cellId", -1);

                removeWidget(cellId, false);
            }
        }
    };

    private BroadcastReceiver hideReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideDashboard();
        }
    };

    public DashboardController(Context context) {
        super(context);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreateHost(UIHost host) {
        SharedPreferences pref = U.getSharedPreferences(context);
        if(pref.getBoolean(PREF_DASHBOARD, context.getResources().getBoolean(R.bool.tb_def_dashboard)))
            init(context, host, () -> drawDashboard(host));
        else
            host.terminate();
    }

    @SuppressLint("RtlHardcoded")
    private void drawDashboard(UIHost host) {
        final ViewParams params = new ViewParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                -1,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        );

        // Initialize views
        layout = new LinearLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.setVisibility(View.GONE);
        layout.setAlpha(0);

        SharedPreferences pref = U.getSharedPreferences(context);
        int width = pref.getInt(PREF_DASHBOARD_WIDTH, context.getResources().getInteger(R.integer.tb_dashboard_width));
        int height = pref.getInt(PREF_DASHBOARD_HEIGHT, context.getResources().getInteger(R.integer.tb_dashboard_height));

        boolean isPortrait = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        boolean isLandscape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        if(isPortrait) {
            columns = height;
            rows = width;
        }

        if(isLandscape) {
            columns = width;
            rows = height;
        }

        maxSize = columns * rows;

        int backgroundTint = U.getBackgroundTint(context);
        int accentColor = U.getAccentColor(context);
        int accentColorAlt = accentColor;
        accentColorAlt = ColorUtils.setAlphaComponent(accentColorAlt, Color.alpha(accentColorAlt) / 3);

        int cellCount = 0;

        for(int i = 0; i < columns; i++) {
            LinearLayout layout2 = new LinearLayout(context);
            layout2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
            layout2.setOrientation(LinearLayout.VERTICAL);

            for(int j = 0; j < rows; j++) {
                DashboardCell cellLayout = (DashboardCell) View.inflate(context, R.layout.tb_dashboard, null);
                cellLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                cellLayout.setBackgroundColor(backgroundTint);
                cellLayout.setOnClickListener(cellOcl);
                cellLayout.setOnHoverListener(cellOhl);

                TextView empty = cellLayout.findViewById(R.id.empty);
                empty.setBackgroundColor(accentColorAlt);
                empty.setTextColor(accentColor);

                Bundle bundle = new Bundle();
                bundle.putInt("cellId", cellCount);

                cellLayout.setTag(bundle);
                cells.put(cellCount, cellLayout);
                cellCount++;

                layout2.addView(cellLayout);
            }

            layout.addView(layout2);
        }

        mAppWidgetManager = AppWidgetManager.getInstance(context);
        mAppWidgetHost = new AppWidgetHost(context, APPWIDGET_HOST_ID);
        mAppWidgetHost.startListening();

        for(int i = 0; i < maxSize; i++) {
            int appWidgetId = pref.getInt("dashboard_widget_" + i, -1);
            if(appWidgetId != -1)
                addWidget(appWidgetId, i, false);
            else if(pref.getBoolean("dashboard_widget_" + i + "_placeholder", false))
                addPlaceholder(i);
        }

        mAppWidgetHost.stopListening();

        U.registerReceiver(context, toggleReceiver, ACTION_TOGGLE_DASHBOARD);
        U.registerReceiver(context, addWidgetReceiver, ACTION_ADD_WIDGET_COMPLETED);
        U.registerReceiver(context, removeWidgetReceiver, ACTION_REMOVE_WIDGET_COMPLETED);
        U.registerReceiver(context, hideReceiver, ACTION_HIDE_DASHBOARD);

        host.addView(layout, params);

        new Handler().postDelayed(() -> {
            int paddingSize = context.getResources().getDimensionPixelSize(R.dimen.tb_icon_size);

            switch(TaskbarPosition.getTaskbarPosition(context)) {
                case POSITION_TOP_VERTICAL_LEFT:
                case POSITION_BOTTOM_VERTICAL_LEFT:
                    layout.setPadding(paddingSize, 0, 0, 0);
                    break;
                case POSITION_TOP_LEFT:
                case POSITION_TOP_RIGHT:
                    layout.setPadding(0, paddingSize, 0, 0);
                    break;
                case POSITION_TOP_VERTICAL_RIGHT:
                case POSITION_BOTTOM_VERTICAL_RIGHT:
                    layout.setPadding(0, 0, paddingSize, 0);
                    break;
                case POSITION_BOTTOM_LEFT:
                case POSITION_BOTTOM_RIGHT:
                    layout.setPadding(0, 0, 0, paddingSize);
                    break;
            }
        }, 100);
    }

    private void toggleDashboard() {
        if(layout.getVisibility() == View.GONE)
            showDashboard();
        else
            hideDashboard();
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void showDashboard() {
        if(layout.getVisibility() == View.GONE) {
            layout.setOnClickListener(ocl);
            fadeIn();

            U.sendBroadcast(context, ACTION_DASHBOARD_APPEARING);
            U.sendBroadcast(context, ACTION_HIDE_START_MENU);

            boolean inFreeformMode = FreeformHackHelper.getInstance().isInFreeformWorkspace();

            Intent intent = U.getThemedIntent(context, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

            if(inFreeformMode) {
                if(U.hasBrokenSetLaunchBoundsApi())
                    intent.putExtra("context_menu_fix", true);

                U.startActivityMaximized(context, intent);
            } else
                context.startActivity(intent);

            for(int i = 0; i < maxSize; i++) {
                final DashboardCell cellLayout = cells.get(i);
                final AppWidgetHostView hostView = widgets.get(i);

                if(hostView != null) {
                    try {
                        context.getPackageManager().getApplicationInfo(hostView.getAppWidgetInfo().provider.getPackageName(), 0);
                        hostView.post(() -> {
                            ViewGroup.LayoutParams params = hostView.getLayoutParams();
                            params.width = cellLayout.getWidth();
                            params.height = cellLayout.getHeight();
                            hostView.setLayoutParams(params);
                            hostView.updateAppWidgetSize(null, cellLayout.getWidth(), cellLayout.getHeight(), cellLayout.getWidth(), cellLayout.getHeight());
                        });
                    } catch (PackageManager.NameNotFoundException e) {
                        removeWidget(i, false);
                    } catch (NullPointerException e) {
                        removeWidget(i, true);
                    }
                }
            }

            final SharedPreferences pref = U.getSharedPreferences(context);
            if(!pref.getBoolean(PREF_DASHBOARD_TUTORIAL_SHOWN, false)) {
                U.showToastLong(context, R.string.tb_dashboard_tutorial);
                pref.edit().putBoolean(PREF_DASHBOARD_TUTORIAL_SHOWN, true).apply();
            }
        }
    }

    private void hideDashboard() {
        if(layout.getVisibility() == View.VISIBLE) {
            layout.setOnClickListener(null);
            fadeOut(true);

            for(int i = 0; i < maxSize; i++) {
                FrameLayout frameLayout = cells.get(i);
                frameLayout.findViewById(R.id.empty).setVisibility(View.GONE);
            }

            previouslySelectedCell = -1;
        }
    }

    private void fadeIn() {
        mAppWidgetHost.startListening();

        DashboardHelper.getInstance().setDashboardOpen(true);

        layout.setVisibility(View.VISIBLE);
        layout.animate()
                .alpha(1)
                .setDuration(context.getResources().getInteger(android.R.integer.config_shortAnimTime))
                .setListener(null);
    }

    private void fadeOut(final boolean sendIntent) {
        mAppWidgetHost.stopListening();

        DashboardHelper.getInstance().setDashboardOpen(false);

        layout.animate()
                .alpha(0)
                .setDuration(context.getResources().getInteger(android.R.integer.config_shortAnimTime))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        layout.setVisibility(View.GONE);
                        if(sendIntent) {
                            U.sendBroadcast(context, ACTION_DASHBOARD_DISAPPEARING);
                        }
                    }
                });
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRecreateHost(UIHost host) {
        if(layout != null) {
            try {
                host.removeView(layout);
            } catch (IllegalArgumentException e) { /* Gracefully fail */ }

            SharedPreferences pref = U.getSharedPreferences(context);
            if(U.canDrawOverlays(context))
                drawDashboard(host);
            else {
                pref.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();

                host.terminate();
            }
        }
    }

    @Override
    public void onDestroyHost(UIHost host) {
        if(layout != null)
            try {
                host.removeView(layout);
            } catch (IllegalArgumentException e) { /* Gracefully fail */ }

        U.unregisterReceiver(context, toggleReceiver);
        U.unregisterReceiver(context, addWidgetReceiver);
        U.unregisterReceiver(context, removeWidgetReceiver);
        U.unregisterReceiver(context, hideReceiver);

        U.sendBroadcast(context, ACTION_DASHBOARD_DISAPPEARING);

        SharedPreferences pref = U.getSharedPreferences(context);
        pref.edit().remove(PREF_DONT_STOP_DASHBOARD).apply();
    }

    private void cellClick(View view, boolean isActualClick) {
        Bundle bundle = (Bundle) view.getTag();
        int cellId = bundle.getInt("cellId");
        int appWidgetId = bundle.getInt("appWidgetId", -1);

        int currentlySelectedCell = appWidgetId == -1 ? cellId : -1;

        SharedPreferences pref = U.getSharedPreferences(context);
        boolean shouldShowPlaceholder = pref.getBoolean("dashboard_widget_" + cellId + "_placeholder", false);
        if(isActualClick && ((appWidgetId == -1 && currentlySelectedCell == previouslySelectedCell) || shouldShowPlaceholder)) {
            fadeOut(false);

            FrameLayout frameLayout = cells.get(currentlySelectedCell);
            frameLayout.findViewById(R.id.empty).setVisibility(View.GONE);

            Intent intent = new Intent(ACTION_ADD_WIDGET_REQUESTED);
            intent.putExtra("appWidgetId", APPWIDGET_HOST_ID);
            intent.putExtra("cellId", cellId);
            U.sendBroadcast(context, intent);

            if(shouldShowPlaceholder) {
                String providerName = pref.getString("dashboard_widget_" + cellId + "_provider", "null");
                if(!providerName.equals("null")) {
                    ComponentName componentName = ComponentName.unflattenFromString(providerName);

                    List<AppWidgetProviderInfo> providerInfoList = mAppWidgetManager.getInstalledProvidersForProfile(Process.myUserHandle());
                    for(AppWidgetProviderInfo info : providerInfoList) {
                        if(info.provider.equals(componentName)) {
                            U.showToast(context, context.getString(R.string.tb_widget_restore_toast, info.loadLabel(context.getPackageManager())), Toast.LENGTH_SHORT);
                            break;
                        }
                    }
                }
            }

            previouslySelectedCell = -1;
        } else {
            for(int i = 0; i < maxSize; i++) {
                FrameLayout frameLayout = cells.get(i);
                frameLayout.findViewById(R.id.empty).setVisibility(i == currentlySelectedCell && !shouldShowPlaceholder ? View.VISIBLE : View.GONE);
            }

            previouslySelectedCell = currentlySelectedCell;
        }
    }

    private void cellLongClick(View view) {
        fadeOut(false);

        Bundle bundle = (Bundle) view.getTag();
        int cellId = bundle.getInt("cellId");

        Intent intent = new Intent(ACTION_REMOVE_WIDGET_REQUESTED);
        intent.putExtra("cellId", cellId);
        U.sendBroadcast(context, intent);
    }

    private void addWidget(int appWidgetId, int cellId, boolean shouldSave) {
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

        final DashboardCell cellLayout = cells.get(cellId);
        final AppWidgetHostView hostView = mAppWidgetHost.createView(context, appWidgetId, appWidgetInfo);
        hostView.setAppWidget(appWidgetId, appWidgetInfo);

        Bundle bundle = new Bundle();
        bundle.putInt("cellId", cellId);
        hostView.setTag(bundle);

        cellLayout.findViewById(R.id.empty).setVisibility(View.GONE);
        cellLayout.findViewById(R.id.placeholder).setVisibility(View.GONE);
        cellLayout.setOnLongClickListener(olcl);
        cellLayout.setOnGenericMotionListener(ogml);
        cellLayout.setOnInterceptedLongPressListener(listener);

        LinearLayout linearLayout = cellLayout.findViewById(R.id.dashboard);
        linearLayout.addView(hostView);

        Bundle bundle2 = (Bundle) cellLayout.getTag();
        bundle2.putInt("appWidgetId", appWidgetId);
        cellLayout.setTag(bundle2);

        widgets.put(cellId, hostView);

        if(shouldSave) {
            SharedPreferences pref = U.getSharedPreferences(context);
            SharedPreferences.Editor editor = pref.edit();
            editor.putInt("dashboard_widget_" + cellId, appWidgetId);
            editor.putString("dashboard_widget_" + cellId + "_provider", appWidgetInfo.provider.flattenToString());
            editor.remove("dashboard_widget_" + cellId + "_placeholder");
            editor.apply();
        }

        new Handler().post(() -> {
            ViewGroup.LayoutParams params = hostView.getLayoutParams();
            params.width = cellLayout.getWidth();
            params.height = cellLayout.getHeight();
            hostView.setLayoutParams(params);
            hostView.updateAppWidgetSize(null, cellLayout.getWidth(), cellLayout.getHeight(), cellLayout.getWidth(), cellLayout.getHeight());
        });
    }

    private void removeWidget(int cellId, boolean tempRemove) {
        widgets.remove(cellId);

        DashboardCell cellLayout = cells.get(cellId);
        Bundle bundle = (Bundle) cellLayout.getTag();

        mAppWidgetHost.deleteAppWidgetId(bundle.getInt("appWidgetId"));
        bundle.remove("appWidgetId");

        LinearLayout linearLayout = cellLayout.findViewById(R.id.dashboard);
        linearLayout.removeAllViews();

        cellLayout.setTag(bundle);
        cellLayout.setOnClickListener(cellOcl);
        cellLayout.setOnHoverListener(cellOhl);
        cellLayout.setOnLongClickListener(null);
        cellLayout.setOnGenericMotionListener(null);
        cellLayout.setOnInterceptedLongPressListener(null);

        SharedPreferences pref = U.getSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove("dashboard_widget_" + cellId);

        if(tempRemove) {
            editor.putBoolean("dashboard_widget_" + cellId + "_placeholder", true);
            addPlaceholder(cellId);
        } else
            editor.remove("dashboard_widget_" + cellId + "_provider");

        editor.apply();
    }

    private void addPlaceholder(int cellId) {
        FrameLayout placeholder = cells.get(cellId).findViewById(R.id.placeholder);
        SharedPreferences pref = U.getSharedPreferences(context);
        String providerName = pref.getString("dashboard_widget_" + cellId + "_provider", "null");

        if(!providerName.equals("null")) {
            ImageView imageView = placeholder.findViewById(R.id.placeholder_image);
            ComponentName componentName = ComponentName.unflattenFromString(providerName);

            List<AppWidgetProviderInfo> providerInfoList = mAppWidgetManager.getInstalledProvidersForProfile(Process.myUserHandle());
            for(AppWidgetProviderInfo info : providerInfoList) {
                if(info.provider.equals(componentName)) {
                    Drawable drawable = info.loadPreviewImage(context, -1);
                    if(drawable == null) drawable = info.loadIcon(context, -1);

                    ColorMatrix matrix = new ColorMatrix();
                    matrix.setSaturation(0);

                    imageView.setImageDrawable(drawable);
                    imageView.setColorFilter(new ColorMatrixColorFilter(matrix));
                    break;
                }
            }
        }

        placeholder.setVisibility(View.VISIBLE);
    }
}
