/* Copyright 2019 Braden Farmer
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.AbstractSelectAppActivity;
import com.farmerbb.taskbar.util.AppEntry;

import java.util.List;

public class DesktopIconAppListAdapter extends ArrayAdapter<AppEntry> {
    public DesktopIconAppListAdapter(Context context, int layout, List<AppEntry> list) {
        super(context, layout, list);
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        // Check if an existing view is being reused, otherwise inflate the view
        if(convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.tb_desktop_icon_row, parent, false);

        final AppEntry entry = getItem(position);
        assert entry != null;

        TextView textView = convertView.findViewById(R.id.name);
        textView.setText(entry.getLabel());

        ImageView imageView = convertView.findViewById(R.id.icon);
        imageView.setImageDrawable(entry.getIcon(getContext()));

        LinearLayout layout = convertView.findViewById(R.id.entry);
        layout.setOnClickListener(view -> {
            AbstractSelectAppActivity activity = (AbstractSelectAppActivity) getContext();
            activity.selectApp(entry);
        });

        return convertView;
    }
}