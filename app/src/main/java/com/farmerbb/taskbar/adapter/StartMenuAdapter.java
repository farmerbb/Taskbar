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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.TopApps;
import com.farmerbb.taskbar.util.U;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StartMenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface ItemClickListener {
        void onClick(View view, AppEntry entry);
    }

    private Context context;
    private ItemClickListener listener;
    private boolean isGrid;

    private List<AppEntry> entries = new ArrayList<>();
    private final Map<AppEntry, Boolean> topAppsCache = new HashMap<>();

    public StartMenuAdapter(Context context, ItemClickListener listener, boolean isGrid, List<AppEntry> list) {
        this.context = context;
        this.listener = listener;
        this.isGrid = isGrid;
        entries.addAll(list);

        updateList(list, true);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View view = LayoutInflater.from(context).inflate(
                isGrid ? R.layout.tb_row_alt : R.layout.tb_row, parent, false);

        return new RecyclerView.ViewHolder(view) {};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        final AppEntry entry = entries.get(i);
        assert entry != null;

        viewHolder.itemView.setOnClickListener(v -> listener.onClick(viewHolder.itemView, entry));

        final SharedPreferences pref = U.getSharedPreferences(context);

        TextView textView = viewHolder.itemView.findViewById(R.id.name);
        textView.setText(entry.getLabel());
        textView.setTypeface(null, isTopApp(entry) ? Typeface.BOLD : Typeface.NORMAL);

        switch(pref.getString("theme", "light")) {
            case "light":
                textView.setTextColor(ContextCompat.getColor(context, R.color.tb_text_color));
                break;
            case "dark":
                textView.setTextColor(ContextCompat.getColor(context, R.color.tb_text_color_dark));
                break;
        }

        ImageView imageView = viewHolder.itemView.findViewById(R.id.icon);
        imageView.setImageDrawable(entry.getIcon(context));

        LinearLayout layout = viewHolder.itemView.findViewById(R.id.entry);
        layout.setOnClickListener(view -> {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
            U.launchApp(context, entry, null, false, false, view);
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
                            ? U.getAccentColor(context)
                            : U.getBackgroundTint(context);

                    //noinspection ResourceAsColor
                    backgroundTint = ColorUtils.setAlphaComponent(backgroundTint, Color.alpha(backgroundTint) / 2);
                    v.setBackgroundColor(backgroundTint);
                }

                if(event.getAction() == MotionEvent.ACTION_HOVER_EXIT)
                    v.setBackgroundColor(0);

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    v.setPointerIcon(PointerIcon.getSystemIcon(context, PointerIcon.TYPE_DEFAULT));

                return false;
            });

            layout.setOnTouchListener((v, event) -> {
                v.setAlpha(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE ? 0.5f : 1);
                return false;
            });
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    private boolean isTopApp(AppEntry entry) {
        if(topAppsCache.containsKey(entry))
            return topAppsCache.get(entry);

        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(entry.getComponentName()));
        ActivityInfo activityInfo = intent.resolveActivityInfo(context.getPackageManager(), 0);

        if(activityInfo != null) {
            TopApps topApps = TopApps.getInstance(context);
            boolean isTopApp = topApps.isTopApp(activityInfo.packageName + "/" + activityInfo.name + ":" + entry.getUserId(context))
                    || topApps.isTopApp(activityInfo.packageName + "/" + activityInfo.name)
                    || topApps.isTopApp(activityInfo.name);

            topAppsCache.put(entry, isTopApp);
            return isTopApp;
        }

        topAppsCache.put(entry, false);
        return false;
    }

    private void openContextMenu(final AppEntry entry, final int[] location) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU_NO_RESET"));

        Bundle args = new Bundle();
        args.putSerializable("app_entry", entry);
        args.putBoolean("launched_from_start_menu", true);
        args.putInt("x", location[0]);
        args.putInt("y", location[1]);

        new Handler().postDelayed(() -> U.startContextMenuActivity(context, args), shouldDelay() ? 100 : 0);
    }

    private boolean shouldDelay() {
        SharedPreferences pref = U.getSharedPreferences(context);
        return U.hasFreeformSupport(context)
                && pref.getBoolean("freeform_hack", false)
                && !FreeformHackHelper.getInstance().isFreeformHackActive();
    }

    public void updateList(List<AppEntry> list) {
        updateList(list, false);
    }

    private void updateList(List<AppEntry> list, boolean firstUpdate) {
        if(!firstUpdate) {
            entries.clear();
            topAppsCache.clear();

            entries.addAll(list);
            notifyDataSetChanged();
        }
    }
}
