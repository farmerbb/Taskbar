package com.farmerbb.taskbar.adapter;

import android.content.Context;
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

    private int type = -1;

    public AppListAdapter(Context context, int layout, List<BlacklistEntry> list, int type) {
        super(context, layout, list);

        this.type = type;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        // Check if an existing view is being reused, otherwise inflate the view
        if(convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_blacklist, parent, false);

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

        TextView textView = (TextView) convertView.findViewById(R.id.name);
        textView.setText(entry.getLabel());

        final CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);
        checkBox.setChecked(blacklist.isBlocked(entry.getPackageName()));

        LinearLayout layout = (LinearLayout) convertView.findViewById(R.id.entry);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(topApps.isTopApp(entry.getPackageName())) {
                    U.showToast(getContext(),
                            getContext().getString(R.string.already_top_app, entry.getLabel()),
                            Toast.LENGTH_SHORT);
                } else if(blacklist.isBlocked(entry.getPackageName())) {
                    blacklist.removeBlockedApp(getContext(), entry.getPackageName());
                    checkBox.setChecked(false);
                } else {
                    blacklist.addBlockedApp(getContext(), entry);
                    checkBox.setChecked(true);
                }
            }
        });
    }

    private void setupTopApps(int position, View convertView) {
        final BlacklistEntry entry = getItem(position);

        TextView textView = (TextView) convertView.findViewById(R.id.name);
        textView.setText(entry.getLabel());

        final CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);
        checkBox.setChecked(topApps.isTopApp(entry.getPackageName()));

        LinearLayout layout = (LinearLayout) convertView.findViewById(R.id.entry);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(blacklist.isBlocked(entry.getPackageName())) {
                    U.showToast(getContext(),
                            getContext().getString(R.string.already_blacklisted, entry.getLabel()),
                            Toast.LENGTH_SHORT);
                } else if(topApps.isTopApp(entry.getPackageName())) {
                    topApps.removeTopApp(getContext(), entry.getPackageName());
                    checkBox.setChecked(false);
                } else {
                    topApps.addTopApp(getContext(), entry);
                    checkBox.setChecked(true);
                }
            }
        });
    }
}