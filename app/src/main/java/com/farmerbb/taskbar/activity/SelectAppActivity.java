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

package com.farmerbb.taskbar.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.Blacklist;
import com.farmerbb.taskbar.util.BlacklistEntry;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SelectAppActivity extends AppCompatActivity {

    private AppListGenerator appListGenerator;
    private ProgressBar progressBar;
    private ListView appList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.select_app);
        setFinishOnTouchOutside(false);
        setTitle(R.string.blacklist_dialog_title);

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        appList = (ListView) findViewById(R.id.list);

        appListGenerator = new AppListGenerator();
        appListGenerator.execute();
    }

    @Override
    public void finish() {
        if(appListGenerator != null && appListGenerator.getStatus() == AsyncTask.Status.RUNNING)
            appListGenerator.cancel(true);

        super.finish();
    }

    private class AppListAdapter extends ArrayAdapter<BlacklistEntry> {
        AppListAdapter(Context context, int layout, List<BlacklistEntry> list) {
            super(context, layout, list);
        }

        @Override
        public View getView(int position, View convertView, final ViewGroup parent) {
            // Check if an existing view is being reused, otherwise inflate the view
            if(convertView == null)
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_blacklist, parent, false);

            final BlacklistEntry entry = getItem(position);
            final Blacklist blacklist = Blacklist.getInstance(getContext());

            TextView textView = (TextView) convertView.findViewById(R.id.name);
            textView.setText(entry.getLabel());

            final CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);
            checkBox.setChecked(blacklist.isBlocked(entry.getPackageName()));

            LinearLayout layout = (LinearLayout) convertView.findViewById(R.id.entry);
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(blacklist.isBlocked(entry.getPackageName())) {
                        blacklist.removeBlockedApp(getContext(), entry.getPackageName());
                        checkBox.setChecked(false);
                    } else {
                        blacklist.addBlockedApp(getContext(), entry);
                        checkBox.setChecked(true);
                    }
                }
            });

            return convertView;
        }
    }

    private final class AppListGenerator extends AsyncTask<Void, Void, AppListAdapter> {
        @Override
        protected AppListAdapter doInBackground(Void... params) {
            final PackageManager pm = getPackageManager();

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);

            // Remove any uninstalled apps from the blacklist
            Blacklist blacklist = Blacklist.getInstance(SelectAppActivity.this);
            List<String> blacklistedApps = new ArrayList<>();
            List<String> installedApps = new ArrayList<>();

            for(BlacklistEntry entry : blacklist.getBlockedApps()) {
               blacklistedApps.add(entry.getPackageName());
            }

            for(ResolveInfo appInfo : list) {
                installedApps.add(appInfo.activityInfo.name);
            }

            for(String packageName : blacklistedApps) {
                if(!installedApps.contains(packageName))
                    blacklist.removeBlockedApp(SelectAppActivity.this, packageName);
            }

            Collections.sort(list, new Comparator<ResolveInfo>() {
                @Override
                public int compare(ResolveInfo ai1, ResolveInfo ai2) {
                    String label1;
                    String label2;

                    try {
                        label1 = ai1.activityInfo.loadLabel(pm).toString();
                        label2 = ai2.activityInfo.loadLabel(pm).toString();
                    } catch (OutOfMemoryError e) {
                        System.gc();

                        label1 = ai1.activityInfo.packageName;
                        label2 = ai2.activityInfo.packageName;
                    }

                    return Collator.getInstance().compare(label1, label2);
                }
            });

            final List<BlacklistEntry> entries = new ArrayList<>();
            for(ResolveInfo appInfo : list) {
                String label;

                try {
                    label = appInfo.loadLabel(pm).toString();
                } catch (OutOfMemoryError e) {
                    System.gc();

                    label = appInfo.activityInfo.packageName;
                }

                entries.add(new BlacklistEntry(
                        appInfo.activityInfo.name,
                        label));
            }

            return new AppListAdapter(SelectAppActivity.this, R.layout.row, entries);
        }

        @Override
        protected void onPostExecute(AppListAdapter adapter) {
            progressBar.setVisibility(View.GONE);
            appList.setAdapter(adapter);
            setFinishOnTouchOutside(true);
        }
    }
}