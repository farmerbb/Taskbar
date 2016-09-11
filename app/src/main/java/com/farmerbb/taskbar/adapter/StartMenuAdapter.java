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
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.ContextMenuActivity;
import com.farmerbb.taskbar.activity.ContextMenuActivityDark;
import com.farmerbb.taskbar.activity.InvisibleActivityFreeform;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.U;

import java.util.List;

import static android.content.Context.DISPLAY_SERVICE;

public class StartMenuAdapter extends ArrayAdapter<AppEntry> {

    private boolean isGrid = false;

    public StartMenuAdapter(Context context, int layout, List<AppEntry> list) {
        super(context, layout, list);
        isGrid = layout == R.layout.row_alt;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        // Check if an existing view is being reused, otherwise inflate the view
        if(convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(isGrid ? R.layout.row_alt : R.layout.row, parent, false);

        final AppEntry entry = getItem(position);
        final SharedPreferences pref = U.getSharedPreferences(getContext());

        TextView textView = (TextView) convertView.findViewById(R.id.name);
        textView.setText(entry.getLabel());

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
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean shouldDelay = false;

                SharedPreferences pref = U.getSharedPreferences(getContext());
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                        && pref.getBoolean("freeform_hack", false)
                        && !FreeformHackHelper.getInstance().isFreeformHackActive()) {
                    shouldDelay = true;

                    Intent freeformHackIntent = new Intent(getContext(), InvisibleActivityFreeform.class);
                    freeformHackIntent.putExtra("check_multiwindow", true);
                    freeformHackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(freeformHackIntent);
                }

                if(shouldDelay) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            launchApp(entry.getComponentName());
                        }
                    }, 100);
                } else launchApp(entry.getComponentName());
            }
        });

        layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                openContextMenu(entry, location);
                return true;
            }
        });

        layout.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                        && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    int[] location = new int[2];
                    view.getLocationOnScreen(location);
                    openContextMenu(entry, location);
                }

                return false;
            }
        });

        return convertView;
    }

    @SuppressWarnings("deprecation")
    private void openContextMenu(AppEntry entry, int[] location) {
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));

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
            intent.putExtra("launched_from_start_menu", true);
            intent.putExtra("x", location[0]);
            intent.putExtra("y", location[1]);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && pref.getBoolean("freeform_hack", false)) {
            DisplayManager dm = (DisplayManager) getContext().getSystemService(DISPLAY_SERVICE);
            Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);

            getContext().startActivity(intent, ActivityOptions.makeBasic().setLaunchBounds(new Rect(0, 0, display.getWidth(), display.getHeight())).toBundle());
        } else
            getContext().startActivity(intent);
    }

    private void launchApp(String componentName) {
        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(componentName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        SharedPreferences pref = U.getSharedPreferences(getContext());
        switch(pref.getString("window_size", "standard")) {
            case "standard":
                U.launchStandard(getContext(), intent);
                break;
            case "fullscreen":
                U.launchFullscreen(getContext(), intent, true);
                break;
            case "phone_size":
                U.launchPhoneSize(getContext(), intent);
                break;
        }

        if(pref.getBoolean("hide_taskbar", true) && !pref.getBoolean("in_freeform_workspace", false))
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_TASKBAR"));
        else
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
    }
}
