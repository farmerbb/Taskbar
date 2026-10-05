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

package com.farmerbb.taskbar.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.U;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_OK;
import static com.farmerbb.taskbar.util.Constants.*;

/**
 * Hosts home screen style widgets on the Taskbar desktop.
 *
 * Widgets are placed freely on top of the wallpaper.  Long-press a widget to move,
 * resize or remove it.  Positions and sizes are saved in the shared preferences.
 */
public class DesktopWidgetManager {
    public static final int REQUEST_PICK_WIDGET = 4601;
    public static final int REQUEST_CONFIGURE_WIDGET = 4602;

    private static final int HOST_ID = 4242;
    private static final int MIN_SIZE_DP = 56;
    private static final int HANDLE_SIZE_DP = 36;

    private static final class Entry {
        int id;
        int x;
        int y;
        int width;
        int height;
        AppWidgetHostView hostView;
        WidgetFrame frame;
        View border;
        View[] handles;
        ImageView done;
    }

    private final Activity activity;
    private final AppWidgetManager appWidgetManager;
    private final AppWidgetHost appWidgetHost;
    private final FrameLayout container;
    private final float density;
    private final Map<Integer, Entry> entries = new LinkedHashMap<>();

    private int pendingId = -1;
    private int editingId = -1;

    public DesktopWidgetManager(Activity activity, FrameLayout parent) {
        this.activity = activity;
        this.density = activity.getResources().getDisplayMetrics().density;
        this.appWidgetManager = AppWidgetManager.getInstance(activity);
        this.appWidgetHost = new AppWidgetHost(activity, HOST_ID);

        container = new FrameLayout(activity);
        parent.addView(container, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        container.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if(r - l != or - ol || b - t != ob - ot)
                for(Entry entry : entries.values())
                    applyBounds(entry, false);
        });

        restoreWidgets();
    }

    /** Keeps widgets clear of the taskbar. */
    public void setMargins(int left, int top, int right, int bottom) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        params.setMargins(left, top, right, bottom);
        container.setLayoutParams(params);
    }

    public void startListening() {
        try {
            appWidgetHost.startListening();
        } catch(RuntimeException ignored) {}
    }

    public void stopListening() {
        try {
            appWidgetHost.stopListening();
        } catch(RuntimeException ignored) {}
    }

    /* Adding widgets */

    public void startAddWidget() {
        int id = appWidgetHost.allocateAppWidgetId();
        pendingId = id;

        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);

        try {
            U.sendBroadcast(activity, ACTION_TEMP_HIDE_TASKBAR);
            activity.startActivityForResult(pickIntent, REQUEST_PICK_WIDGET);
        } catch(ActivityNotFoundException e) {
            discardPending();
            U.showToast(activity, R.string.tb_error_creating_widget);
        }
    }

    /** @return true if the result belonged to this manager */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode != REQUEST_PICK_WIDGET && requestCode != REQUEST_CONFIGURE_WIDGET)
            return false;

        int id = data != null
                ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingId)
                : pendingId;
        if(id == -1) id = pendingId;

        if(resultCode != RESULT_OK) {
            if(id != -1) appWidgetHost.deleteAppWidgetId(id);
            pendingId = -1;
            return true;
        }

        if(requestCode == REQUEST_PICK_WIDGET) {
            AppWidgetProviderInfo info = id != -1 ? appWidgetManager.getAppWidgetInfo(id) : null;
            if(info == null) {
                discardPending();
                U.showToast(activity, R.string.tb_error_creating_widget);
            } else if(info.configure != null) {
                try {
                    appWidgetHost.startAppWidgetConfigureActivityForResult(
                            activity, id, 0, REQUEST_CONFIGURE_WIDGET, null);
                } catch(RuntimeException e) {
                    discardPending();
                    U.showToast(activity, R.string.tb_error_creating_widget);
                }
            } else
                finishAddingWidget(id);
        } else
            finishAddingWidget(id);

        return true;
    }

    private void discardPending() {
        if(pendingId != -1) appWidgetHost.deleteAppWidgetId(pendingId);
        pendingId = -1;
    }

    private void finishAddingWidget(int id) {
        pendingId = -1;

        AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(id);
        if(info == null) {
            appWidgetHost.deleteAppWidgetId(id);
            U.showToast(activity, R.string.tb_error_creating_widget);
            return;
        }

        Entry entry = new Entry();
        entry.id = id;
        entry.width = Math.max(info.minWidth, dp(160));
        entry.height = Math.max(info.minHeight, dp(80));

        int offset = dp(24) * (entries.size() % 6);
        entry.x = dp(48) + offset;
        entry.y = dp(96) + offset;

        addEntry(entry);
        saveWidgets();

        // Show the move/resize handles right away so they're easy to discover
        if(entry.frame != null) enterEditMode(entry);
    }

    /* Widget views */

    private void addEntry(Entry entry) {
        AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(entry.id);
        if(info == null) return;

        entry.hostView = appWidgetHost.createView(activity, entry.id, info);
        entry.frame = new WidgetFrame(activity, entry);
        entry.frame.addView(entry.hostView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        container.addView(entry.frame, new FrameLayout.LayoutParams(entry.width, entry.height));
        entries.put(entry.id, entry);

        applyBounds(entry, false);
    }

    private void applyBounds(Entry entry, boolean save) {
        int maxW = container.getWidth();
        int maxH = container.getHeight();

        entry.width = Math.max(dp(MIN_SIZE_DP), maxW > 0 ? Math.min(entry.width, maxW) : entry.width);
        entry.height = Math.max(dp(MIN_SIZE_DP), maxH > 0 ? Math.min(entry.height, maxH) : entry.height);
        entry.x = Math.max(0, maxW > 0 ? Math.min(entry.x, maxW - entry.width) : entry.x);
        entry.y = Math.max(0, maxH > 0 ? Math.min(entry.y, maxH - entry.height) : entry.y);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(entry.width, entry.height);
        params.gravity = Gravity.TOP | Gravity.START;
        params.leftMargin = entry.x;
        params.topMargin = entry.y;
        entry.frame.setLayoutParams(params);

        int widthDp = Math.round(entry.width / density);
        int heightDp = Math.round(entry.height / density);
        entry.hostView.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp);

        if(save) saveWidgets();
    }

    private void removeEntry(Entry entry) {
        if(editingId == entry.id) editingId = -1;

        container.removeView(entry.frame);
        entries.remove(entry.id);
        appWidgetHost.deleteAppWidgetId(entry.id);
        saveWidgets();
    }

    /* Menu and edit mode */

    private void showMenu(Entry entry) {
        CharSequence[] items = {
                activity.getString(R.string.tb_move_resize_widget),
                activity.getString(R.string.tb_remove_widget)
        };

        AlertDialog menu = new AlertDialog.Builder(activity)
                .setItems(items, (dialog, which) -> {
                    if(which == 0)
                        enterEditMode(entry);
                    else
                        removeEntry(entry);
                })
                .create();

        menu.show();

        // Keep the desktop visible behind the menu instead of darkening it
        if(menu.getWindow() != null)
            menu.getWindow().setDimAmount(0.15f);
    }

    private void enterEditMode(Entry entry) {
        exitEditMode();
        editingId = entry.id;

        GradientDrawable border = new GradientDrawable();
        border.setColor(Color.argb(40, 255, 255, 255));
        border.setStroke(dp(2), Color.WHITE);
        entry.border = new View(activity);
        entry.border.setBackground(border);
        entry.frame.addView(entry.border, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        int size = dp(HANDLE_SIZE_DP);

        // One handle on each corner, so the widget can be resized from anywhere
        int[] gravities = {
                Gravity.TOP | Gravity.START,
                Gravity.TOP | Gravity.END,
                Gravity.BOTTOM | Gravity.START,
                Gravity.BOTTOM | Gravity.END
        };

        entry.handles = new View[4];
        for(int i = 0; i < 4; i++) {
            GradientDrawable handleBackground = new GradientDrawable();
            handleBackground.setShape(GradientDrawable.OVAL);
            handleBackground.setColor(Color.WHITE);
            handleBackground.setStroke(dp(3), Color.DKGRAY);

            boolean isLeft = (gravities[i] & Gravity.START) == Gravity.START;
            boolean isTop = (gravities[i] & Gravity.TOP) == Gravity.TOP;

            View handle = new View(activity);
            handle.setBackground(handleBackground);
            handle.setOnTouchListener(new ResizeTouchListener(entry, isLeft, isTop));

            FrameLayout.LayoutParams handleParams = new FrameLayout.LayoutParams(size, size);
            handleParams.gravity = gravities[i];
            handleParams.setMargins(dp(2), dp(2), dp(2), dp(2));
            entry.frame.addView(handle, handleParams);
            entry.handles[i] = handle;
        }

        entry.done = new ImageView(activity);
        entry.done.setImageResource(R.drawable.tb_done);
        entry.done.setBackgroundColor(Color.argb(160, 0, 0, 0));
        entry.done.setPadding(dp(6), dp(6), dp(6), dp(6));
        entry.done.setOnClickListener(v -> exitEditMode());

        FrameLayout.LayoutParams doneParams = new FrameLayout.LayoutParams(size, size);
        doneParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        entry.frame.addView(entry.done, doneParams);
    }

    /** @return true if an edit session was active and has now been closed */
    public boolean exitEditMode() {
        Entry entry = entries.get(editingId);
        editingId = -1;

        if(entry == null)
            return false;

        entry.frame.removeView(entry.border);
        for(View handle : entry.handles)
            entry.frame.removeView(handle);
        entry.frame.removeView(entry.done);
        entry.border = null;
        entry.handles = null;
        entry.done = null;

        saveWidgets();
        return true;
    }

    /** Intercepts long-presses (and drags, in edit mode) before the widget itself sees them. */
    private final class WidgetFrame extends FrameLayout {
        private final Entry entry;
        private final GestureDetector detector;
        private float downRawX;
        private float downRawY;
        private int startX;
        private int startY;

        WidgetFrame(Context context, Entry entry) {
            super(context);
            this.entry = entry;

            detector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public void onLongPress(MotionEvent e) {
                    showMenu(entry);
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if(editingId == entry.id) {
                // Let the handle and done button receive their own touches
                if(isInside(entry.done, ev)) return false;

                if(entry.handles != null)
                    for(View handle : entry.handles)
                        if(isInside(handle, ev)) return false;

                return true;
            }

            detector.onTouchEvent(ev);
            return false;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if(editingId != entry.id)
                return super.onTouchEvent(ev);

            switch(ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = ev.getRawX();
                    downRawY = ev.getRawY();
                    startX = entry.x;
                    startY = entry.y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    entry.x = startX + Math.round(ev.getRawX() - downRawX);
                    entry.y = startY + Math.round(ev.getRawY() - downRawY);
                    applyBounds(entry, false);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    saveWidgets();
                    break;
            }

            return true;
        }

        private boolean isInside(View view, MotionEvent ev) {
            return view != null
                    && ev.getX() >= view.getLeft() && ev.getX() <= view.getRight()
                    && ev.getY() >= view.getTop() && ev.getY() <= view.getBottom();
        }
    }

    private final class ResizeTouchListener implements View.OnTouchListener {
        private final Entry entry;
        private final boolean fromLeft;
        private final boolean fromTop;
        private float downRawX;
        private float downRawY;
        private int startX;
        private int startY;
        private int startWidth;
        private int startHeight;

        ResizeTouchListener(Entry entry, boolean fromLeft, boolean fromTop) {
            this.entry = entry;
            this.fromLeft = fromLeft;
            this.fromTop = fromTop;
        }

        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            switch(ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = ev.getRawX();
                    downRawY = ev.getRawY();
                    startX = entry.x;
                    startY = entry.y;
                    startWidth = entry.width;
                    startHeight = entry.height;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = Math.round(ev.getRawX() - downRawX);
                    int dy = Math.round(ev.getRawY() - downRawY);
                    int minSize = dp(MIN_SIZE_DP);

                    int newWidth = Math.max(minSize, fromLeft ? startWidth - dx : startWidth + dx);
                    int newHeight = Math.max(minSize, fromTop ? startHeight - dy : startHeight + dy);
                    int newX = fromLeft ? startX + startWidth - newWidth : startX;
                    int newY = fromTop ? startY + startHeight - newHeight : startY;

                    // Keep the widget on screen
                    if(newX < 0) {
                        newWidth += newX;
                        newX = 0;
                    }

                    if(newY < 0) {
                        newHeight += newY;
                        newY = 0;
                    }

                    int maxW = container.getWidth();
                    int maxH = container.getHeight();
                    if(maxW > 0 && newX + newWidth > maxW) newWidth = maxW - newX;
                    if(maxH > 0 && newY + newHeight > maxH) newHeight = maxH - newY;

                    entry.x = newX;
                    entry.y = newY;
                    entry.width = newWidth;
                    entry.height = newHeight;
                    applyBounds(entry, false);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    saveWidgets();
                    return true;
            }

            return false;
        }
    }

    /* Persistence */

    private void saveWidgets() {
        StringBuilder builder = new StringBuilder();
        for(Entry entry : entries.values()) {
            if(builder.length() > 0) builder.append(';');
            builder.append(entry.id).append(',')
                    .append(entry.x).append(',')
                    .append(entry.y).append(',')
                    .append(entry.width).append(',')
                    .append(entry.height);
        }

        U.getSharedPreferences(activity).edit()
                .putString(PREF_DESKTOP_WIDGETS, builder.toString())
                .apply();
    }

    private void restoreWidgets() {
        String saved = U.getSharedPreferences(activity).getString(PREF_DESKTOP_WIDGETS, "");
        if(saved == null || saved.isEmpty()) return;

        List<Entry> restored = new ArrayList<>();
        for(String item : saved.split(";")) {
            String[] parts = item.split(",");
            if(parts.length != 5) continue;

            try {
                Entry entry = new Entry();
                entry.id = Integer.parseInt(parts[0]);
                entry.x = Integer.parseInt(parts[1]);
                entry.y = Integer.parseInt(parts[2]);
                entry.width = Integer.parseInt(parts[3]);
                entry.height = Integer.parseInt(parts[4]);
                restored.add(entry);
            } catch(NumberFormatException ignored) {}
        }

        for(Entry entry : restored) {
            // Widgets whose provider has been uninstalled are dropped
            if(appWidgetManager.getAppWidgetInfo(entry.id) == null)
                appWidgetHost.deleteAppWidgetId(entry.id);
            else
                addEntry(entry);
        }

        saveWidgets();
    }

    private int dp(int value) {
        return Math.round(value * density);
    }
}
