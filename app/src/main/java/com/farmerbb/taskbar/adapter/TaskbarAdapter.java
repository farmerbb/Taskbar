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
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.List;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.ContextMenuActivity;
import com.farmerbb.taskbar.activity.ContextMenuActivityDark;
import com.farmerbb.taskbar.activity.InvisibleActivityFreeform;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.SavedWindowSizes;
import com.farmerbb.taskbar.util.U;

import static android.content.Context.DISPLAY_SERVICE;

public class TaskbarAdapter extends ArrayAdapter<AppEntry> {

    private int numOfPinnedApps = 0;

    public TaskbarAdapter(Context context, int layout, List<AppEntry> list, int numOfPinnedApps) {
        super(context, layout, list);
        this.numOfPinnedApps = numOfPinnedApps;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        // Check if an existing view is being reused, otherwise inflate the view
        if(convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.icon, parent, false);

        final AppEntry entry = getItem(position);
        final SharedPreferences pref = U.getSharedPreferences(getContext());

        ImageView imageView = (ImageView) convertView.findViewById(R.id.icon);
        ImageView imageView2 = (ImageView) convertView.findViewById(R.id.shortcut_icon);
        imageView.setImageDrawable(entry.getIcon(getContext()));

        String taskbarPosition = U.getTaskbarPosition(getContext());
        if(pref.getBoolean("shortcut_icon", true)) {
            boolean shouldShowShortcutIcon;
            if(taskbarPosition.contains("vertical"))
                shouldShowShortcutIcon = position >= getCount() - numOfPinnedApps;
            else
                shouldShowShortcutIcon = position < numOfPinnedApps;

            if(shouldShowShortcutIcon) imageView2.setVisibility(View.VISIBLE);
        }

        if(taskbarPosition.equals("bottom_right") || taskbarPosition.equals("top_right")) {
            imageView.setRotationY(180);
            imageView2.setRotationY(180);
        }

        FrameLayout layout = (FrameLayout) convertView.findViewById(R.id.entry);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                U.launchApp(getContext(), entry.getPackageName(), entry.getComponentName(), true);
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
}
