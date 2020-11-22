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
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.helper.FreeformHackHelper;
import com.farmerbb.taskbar.util.TopApps;
import com.farmerbb.taskbar.util.U;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.farmerbb.taskbar.util.Constants.*;

public class StartMenuAdapter extends ArrayAdapter<AppEntry> implements SectionIndexer {

    private final boolean isGrid;

    private final List<Character> sections = new ArrayList<>();
    private final SparseIntArray gpfsCache = new SparseIntArray();
    private final SparseIntArray gsfpCache = new SparseIntArray();
    private final Map<AppEntry, Boolean> topAppsCache = new HashMap<>();

    private final List<Character> lowercase = Arrays.asList(
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
    );

    private final List<Character> uppercase = Arrays.asList(
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
    );

    public StartMenuAdapter(Context context, int layout, List<AppEntry> list) {
        super(context, layout, list);
        isGrid = layout == R.layout.tb_row_alt;

        updateList(list, true);
    }

    @Override
    public @NonNull View getView(int position, View convertView, final @NonNull ViewGroup parent) {
        // Check if an existing view is being reused, otherwise inflate the view
        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(isGrid ? R.layout.tb_row_alt : R.layout.tb_row, parent, false);
            convertView.setBackgroundColor(0);
        }

        final AppEntry entry = getItem(position);
        assert entry != null;

        final SharedPreferences pref = U.getSharedPreferences(getContext());

        TextView textView = convertView.findViewById(R.id.name);
        textView.setText(pref.getBoolean(PREF_HIDE_ICON_LABELS, false) ? "" : entry.getLabel());
        textView.setTypeface(null, isTopApp(entry) ? Typeface.BOLD : Typeface.NORMAL);
        textView.setTextColor(ContextCompat.getColor(getContext(),
                U.isDarkTheme(getContext()) ? R.color.tb_text_color_dark : R.color.tb_text_color));

        ImageView imageView = convertView.findViewById(R.id.icon);
        imageView.setImageDrawable(entry.getIcon(getContext()));

        LinearLayout layout = convertView.findViewById(R.id.entry);
        layout.setOnClickListener(view -> {
            U.sendBroadcast(getContext(), ACTION_HIDE_START_MENU);
            U.launchApp(getContext(), entry, null, false, false, view);
        });

        layout.setOnLongClickListener(view -> {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            openContextMenu(entry, location);
            return true;
        });

        boolean visualFeedbackEnabled = pref.getBoolean(PREF_VISUAL_FEEDBACK, true);

        layout.setOnGenericMotionListener((view, motionEvent) -> {
            int action = motionEvent.getAction();

            if(action == MotionEvent.ACTION_BUTTON_PRESS
                    && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                openContextMenu(entry, location);
            }

            if(action == MotionEvent.ACTION_SCROLL && visualFeedbackEnabled)
                view.setBackgroundColor(0);

            return false;
        });

        if(visualFeedbackEnabled) {
            layout.setOnHoverListener((v, event) -> {
                if(event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                    int backgroundTint = pref.getBoolean(PREF_TRANSPARENT_START_MENU, false)
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
        }

        if(pref.getBoolean(PREF_VISUAL_FEEDBACK, true)) {
            layout.setOnTouchListener((v, event) -> {
                v.setAlpha(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE ? 0.5f : 1);
                return false;
            });
        }

        return convertView;
    }

    private boolean isTopApp(AppEntry entry) {
        if(topAppsCache.containsKey(entry))
            return topAppsCache.get(entry);

        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(entry.getComponentName()));
        ActivityInfo activityInfo = intent.resolveActivityInfo(getContext().getPackageManager(), 0);

        if(activityInfo != null) {
            TopApps topApps = TopApps.getInstance(getContext());
            boolean isTopApp = topApps.isTopApp(activityInfo.packageName + "/" + activityInfo.name + ":" + entry.getUserId(getContext()))
                    || topApps.isTopApp(activityInfo.packageName + "/" + activityInfo.name)
                    || topApps.isTopApp(activityInfo.name);

            topAppsCache.put(entry, isTopApp);
            return isTopApp;
        }

        topAppsCache.put(entry, false);
        return false;
    }

    private void openContextMenu(final AppEntry entry, final int[] location) {
        U.sendBroadcast(getContext(), ACTION_HIDE_START_MENU_NO_RESET);

        Bundle args = new Bundle();
        args.putSerializable("app_entry", entry);
        args.putBoolean("launched_from_start_menu", true);
        args.putInt("x", location[0]);
        args.putInt("y", location[1]);

        U.newHandler().postDelayed(() -> U.startContextMenuActivity(getContext(), args), shouldDelay() ? 100 : 0);
    }

    private boolean shouldDelay() {
        return U.hasFreeformSupport(getContext())
                && U.isFreeformModeEnabled(getContext())
                && !FreeformHackHelper.getInstance().isFreeformHackActive();
    }

    public void updateList(List<AppEntry> list) {
        updateList(list, false);
    }

    private void updateList(List<AppEntry> list, boolean firstUpdate) {
        if(!firstUpdate) {
            clear();

            sections.clear();
            gsfpCache.clear();
            gpfsCache.clear();
            topAppsCache.clear();

            addAll(list);
        }

        SharedPreferences pref = U.getSharedPreferences(getContext());
        if(pref.getBoolean(PREF_SCROLLBAR, false)) {
            for(AppEntry entry : list) {
                char firstLetter = getSectionForAppEntry(entry);
                if(!sections.contains(firstLetter))
                    sections.add(firstLetter);
            }
        }
    }

    private char getSectionForAppEntry(AppEntry entry) {
        if(isTopApp(entry))
            return '\u2605';

        if(entry.getLabel().equals(""))
            return ' ';

        char origChar = entry.getLabel().charAt(0);
        if(uppercase.contains(origChar))
            return origChar;

        if(lowercase.contains(origChar))
            return uppercase.get(lowercase.indexOf(origChar));

        return '#';
    }

    @Override
    public int getPositionForSection(int section) {
        int cachedPos = gpfsCache.get(section, -1);
        if(cachedPos != -1)
            return cachedPos;

        for(int i = 0; i < getCount(); i++) {
            if(sections.get(section) == getSectionForAppEntry(getItem(i))) {
                gpfsCache.put(section, i);
                return i;
            }
        }

        gpfsCache.put(section, 0);
        return 0;
    }

    @Override
    public int getSectionForPosition(int position) {
        int cachedSection = gsfpCache.get(position, -1);
        if(cachedSection != -1)
            return cachedSection;

        for(int i = 0; i < sections.size(); i++) {
            if(sections.get(i) == getSectionForAppEntry(getItem(position))) {
                gsfpCache.put(position, i);
                return i;
            }
        }

        gsfpCache.put(position, 0);
        return 0;
    }

    @Override
    public Object[] getSections() {
        return sections.toArray();
    }
}
