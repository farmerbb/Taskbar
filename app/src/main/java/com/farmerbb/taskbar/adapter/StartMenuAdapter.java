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

package com.farmerbb.taskbar.adapter;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.ColorUtils;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.ContextMenuActivity;
import com.farmerbb.taskbar.activity.dark.ContextMenuActivityDark;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.TopApps;
import com.farmerbb.taskbar.util.U;

import java.util.List;

public class StartMenuAdapter extends ArrayAdapter<AppEntry> {

    private boolean isGrid = false;

    public StartMenuAdapter(Context context, int layout, List<AppEntry> list) {
        super(context, layout, list);
        isGrid = layout == R.layout.row_alt;
    }

    @Override
    public @NonNull View getView(int position, View convertView, final @NonNull ViewGroup parent) {
        // Check if an existing view is being reused, otherwise inflate the view
        if(convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(isGrid ? R.layout.row_alt : R.layout.row, parent, false);

        final AppEntry entry = getItem(position);
        assert entry != null;

        final SharedPreferences pref = U.getSharedPreferences(getContext());

        TextView textView = (TextView) convertView.findViewById(R.id.name);
        textView.setText(entry.getLabel());

        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(entry.getComponentName()));
        ActivityInfo activityInfo = intent.resolveActivityInfo(getContext().getPackageManager(), 0);

        if(activityInfo != null)
            textView.setTypeface(null, isTopApp(activityInfo) ? Typeface.BOLD : Typeface.NORMAL);

        switch(pref.getString("theme", "light")) {
            case "light":
                textView.setTextColor(ContextCompat.getColor(getContext(), R.color.text_color));
                break;
            case "dark":
                textView.setTextColor(ContextCompat.getColor(getContext(), R.color.text_color_dark));
                break;
        }

        ImageView imageView = (ImageView) convertView.findViewById(R.id.icon);
        imageView.setImageDrawable(entry.getIcon(getContext()));

        LinearLayout layout = (LinearLayout) convertView.findViewById(R.id.entry);
        layout.setOnClickListener(view -> {
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
            U.launchApp(getContext(), entry.getPackageName(), entry.getComponentName(), entry.getUserId(getContext()), null, false, false);
        });

        layout.setOnLongClickListener(view -> {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            openContextMenu(entry, location);
            return true;
        });

        layout.setOnGenericMotionListener((view, motionEvent) -> {
            int action = motionEvent.getAction();

            if(action == MotionEvent.ACTION_BUTTON_PRESS
                    && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                openContextMenu(entry, location);
            }

            if(action == MotionEvent.ACTION_SCROLL && pref.getBoolean("visual_feedback", true))
                view.setBackgroundColor(0);

            return false;
        });

        if(pref.getBoolean("visual_feedback", true)) {
            layout.setOnHoverListener((v, event) -> {
                if(event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                    int backgroundTint = pref.getBoolean("transparent_start_menu", false)
                            ? U.getAccentColor(getContext())
                            : U.getBackgroundTint(getContext());

                    //noinspection ResourceAsColor
                    backgroundTint = ColorUtils.setAlphaComponent(backgroundTint, Color.alpha(backgroundTint) / 2);
                    v.setBackgroundColor(backgroundTint);
                }

                if(event.getAction() == MotionEvent.ACTION_HOVER_EXIT)
                    v.setBackgroundColor(0);

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    v.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_DEFAULT));

                return false;
            });

            layout.setOnTouchListener((v, event) -> {
                v.setAlpha(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE ? 0.5f : 1);
                return false;
            });
        }

        return convertView;
    }

    private boolean isTopApp(ActivityInfo activityInfo) {
        TopApps topApps = TopApps.getInstance(getContext());
        return topApps.isTopApp(activityInfo.packageName + "/" + activityInfo.name) || topApps.isTopApp(activityInfo.name);
    }

    @SuppressWarnings("deprecation")
    private void openContextMenu(final AppEntry entry, final int[] location) {
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));

        new Handler().postDelayed(() -> {
            SharedPreferences pref = U.getSharedPreferences(getContext());
            Intent intent = null;

            switch(pref.getString("theme", "light")) {
                case "light":
                    intent = new Intent(getContext(), ContextMenuActivity.class);
                    break;
                case "dark":
                    intent = new Intent(getContext(), ContextMenuActivityDark.class);
                    break;
            }

            if(intent != null) {
                intent.putExtra("package_name", entry.getPackageName());
                intent.putExtra("app_name", entry.getLabel());
                intent.putExtra("component_name", entry.getComponentName());
                intent.putExtra("user_id", entry.getUserId(getContext()));
                intent.putExtra("launched_from_start_menu", true);
                intent.putExtra("x", location[0]);
                intent.putExtra("y", location[1]);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && FreeformHackHelper.getInstance().isInFreeformWorkspace()) {
                DisplayManager dm = (DisplayManager) getContext().getSystemService(Context.DISPLAY_SERVICE);
                Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);

                getContext().startActivity(intent, ActivityOptions.makeBasic().setLaunchBounds(new Rect(0, 0, display.getWidth(), display.getHeight())).toBundle());
            } else
                getContext().startActivity(intent);
        }, shouldDelay() ? 100 : 0);
    }

    private boolean shouldDelay() {
        SharedPreferences pref = U.getSharedPreferences(getContext());
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && pref.getBoolean("freeform_hack", false)
                && !FreeformHackHelper.getInstance().isFreeformHackActive();
    }
}
