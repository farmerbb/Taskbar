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
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.DesktopIconSelectAppActivity;
import com.farmerbb.taskbar.util.AppEntry;

import java.util.ArrayList;
import java.util.List;

public class DesktopIconAppListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<AppEntry> entries = new ArrayList<>();

    public DesktopIconAppListAdapter(Context context, List<AppEntry> list) {
        this.context = context;
        entries.addAll(list);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.tb_desktop_icon_row, viewGroup, false);
        return new RecyclerView.ViewHolder(view) {};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        final AppEntry entry = entries.get(i);
        assert entry != null;

        TextView textView = viewHolder.itemView.findViewById(R.id.name);
        textView.setText(entry.getLabel());

        ImageView imageView = viewHolder.itemView.findViewById(R.id.icon);
        imageView.setImageDrawable(entry.getIcon(context));

        LinearLayout layout = viewHolder.itemView.findViewById(R.id.entry);
        layout.setOnClickListener(view -> {
            DesktopIconSelectAppActivity activity = (DesktopIconSelectAppActivity) context;
            activity.selectApp(entry);
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }
}