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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.Blacklist;
import com.farmerbb.taskbar.util.BlacklistEntry;
import com.farmerbb.taskbar.util.TopApps;
import com.farmerbb.taskbar.util.U;

import java.util.ArrayList;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Blacklist blacklist;
    private final TopApps topApps;

    private Context context;
    private List<BlacklistEntry> entries = new ArrayList<>();
    private int type;

    public AppListAdapter(Context context, List<BlacklistEntry> list, int type) {
        this.context = context;
        entries.addAll(list);
        this.type = type;

        blacklist = Blacklist.getInstance(context);
        topApps = TopApps.getInstance(context);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.tb_row_blacklist, viewGroup, false);
        return new RecyclerView.ViewHolder(view) {};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        switch(type) {
            case U.HIDDEN:
                setupHidden(i, viewHolder.itemView);
                break;
            case U.TOP_APPS:
                setupTopApps(i, viewHolder.itemView);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    private void setupHidden(int position, View convertView) {
        final BlacklistEntry entry = entries.get(position);
        assert entry != null;

        final String componentName = entry.getPackageName();
        final String componentNameAlt = componentName.contains(":") ? componentName.split(":")[0] : componentName;
        final String componentNameAlt2 = componentNameAlt.contains("/") ? componentNameAlt.split("/")[1] : componentNameAlt;

        TextView textView = convertView.findViewById(R.id.name);
        textView.setText(entry.getLabel());

        final CheckBox checkBox = convertView.findViewById(R.id.checkbox);
        checkBox.setChecked(blacklist.isBlocked(componentName)
                || blacklist.isBlocked(componentNameAlt)
                || blacklist.isBlocked(componentNameAlt2));

        LinearLayout layout = convertView.findViewById(R.id.entry);
        layout.setOnClickListener(view -> {
            if(topApps.isTopApp(componentName)
                    || topApps.isTopApp(componentNameAlt)
                    || topApps.isTopApp(componentNameAlt2)) {
                U.showToast(context,
                        context.getString(R.string.tb_already_top_app, entry.getLabel()),
                        Toast.LENGTH_LONG);
            } else if(blacklist.isBlocked(componentName)) {
                blacklist.removeBlockedApp(context, componentName);
                checkBox.setChecked(false);
            } else if(blacklist.isBlocked(componentNameAlt)) {
                blacklist.removeBlockedApp(context, componentNameAlt);
                checkBox.setChecked(false);
            } else if(blacklist.isBlocked(componentNameAlt2)) {
                blacklist.removeBlockedApp(context, componentNameAlt2);
                checkBox.setChecked(false);
            } else {
                blacklist.addBlockedApp(context, entry);
                checkBox.setChecked(true);
            }
        });
    }

    private void setupTopApps(int position, View convertView) {
        final BlacklistEntry entry = entries.get(position);
        assert entry != null;

        final String componentName = entry.getPackageName();
        final String componentNameAlt = componentName.contains(":") ? componentName.split(":")[0] : componentName;
        final String componentNameAlt2 = componentNameAlt.contains("/") ? componentNameAlt.split("/")[1] : componentNameAlt;

        TextView textView = convertView.findViewById(R.id.name);
        textView.setText(entry.getLabel());

        final CheckBox checkBox = convertView.findViewById(R.id.checkbox);
        checkBox.setChecked(topApps.isTopApp(componentName)
                || topApps.isTopApp(componentNameAlt)
                || topApps.isTopApp(componentNameAlt2));

        LinearLayout layout = convertView.findViewById(R.id.entry);
        layout.setOnClickListener(view -> {
            if(blacklist.isBlocked(componentName)
                    || blacklist.isBlocked(componentNameAlt)
                    || blacklist.isBlocked(componentNameAlt2)) {
                U.showToast(context,
                        context.getString(R.string.tb_already_blacklisted, entry.getLabel()),
                        Toast.LENGTH_LONG);
            } else if(topApps.isTopApp(componentName)) {
                topApps.removeTopApp(context, componentName);
                checkBox.setChecked(false);
            } else if(topApps.isTopApp(componentNameAlt)) {
                topApps.removeTopApp(context, componentNameAlt);
                checkBox.setChecked(false);
            } else if(topApps.isTopApp(componentNameAlt2)) {
                topApps.removeTopApp(context, componentNameAlt2);
                checkBox.setChecked(false);
            } else {
                topApps.addTopApp(context, entry);
                checkBox.setChecked(true);
            }
        });
    }
}