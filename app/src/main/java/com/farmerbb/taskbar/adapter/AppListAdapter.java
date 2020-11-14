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
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.Blacklist;
import com.farmerbb.taskbar.util.BlacklistEntry;
import com.farmerbb.taskbar.util.TopApps;
import com.farmerbb.taskbar.util.U;

import java.util.List;

public class AppListAdapter extends ArrayAdapter<BlacklistEntry> {
    private final Blacklist blacklist = Blacklist.getInstance(getContext());
    private final TopApps topApps = TopApps.getInstance(getContext());

    private final int type;

    public AppListAdapter(Context context, int layout, List<BlacklistEntry> list, int type) {
        super(context, layout, list);

        this.type = type;
    }

    @Override
    public @NonNull View getView(int position, View convertView, final @NonNull ViewGroup parent) {
        // Check if an existing view is being reused, otherwise inflate the view
        if(convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.tb_row_blacklist, parent, false);

        switch(type) {
            case U.HIDDEN:
                setupHidden(position, convertView);
                break;
            case U.TOP_APPS:
                setupTopApps(position, convertView);
                break;
        }

        return convertView;
    }

    private void setupHidden(int position, View convertView) {
        final BlacklistEntry entry = getItem(position);
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
                U.showToast(getContext(),
                        getContext().getString(R.string.tb_already_top_app, entry.getLabel()),
                        Toast.LENGTH_LONG);
            } else if(blacklist.isBlocked(componentName)) {
                blacklist.removeBlockedApp(getContext(), componentName);
                checkBox.setChecked(false);
            } else if(blacklist.isBlocked(componentNameAlt)) {
                blacklist.removeBlockedApp(getContext(), componentNameAlt);
                checkBox.setChecked(false);
            } else if(blacklist.isBlocked(componentNameAlt2)) {
                blacklist.removeBlockedApp(getContext(), componentNameAlt2);
                checkBox.setChecked(false);
            } else {
                blacklist.addBlockedApp(getContext(), entry);
                checkBox.setChecked(true);
            }
        });
    }

    private void setupTopApps(int position, View convertView) {
        final BlacklistEntry entry = getItem(position);
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
                U.showToast(getContext(),
                        getContext().getString(R.string.tb_already_blacklisted, entry.getLabel()),
                        Toast.LENGTH_LONG);
            } else if(topApps.isTopApp(componentName)) {
                topApps.removeTopApp(getContext(), componentName);
                checkBox.setChecked(false);
            } else if(topApps.isTopApp(componentNameAlt)) {
                topApps.removeTopApp(getContext(), componentNameAlt);
                checkBox.setChecked(false);
            } else if(topApps.isTopApp(componentNameAlt2)) {
                topApps.removeTopApp(getContext(), componentNameAlt2);
                checkBox.setChecked(false);
            } else {
                topApps.addTopApp(getContext(), entry);
                checkBox.setChecked(true);
            }
        });
    }
}