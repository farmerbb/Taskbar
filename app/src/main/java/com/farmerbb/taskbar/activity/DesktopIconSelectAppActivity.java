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

package com.farmerbb.taskbar.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.dark.DesktopIconSelectAppActivityDark;
import com.farmerbb.taskbar.adapter.DesktopIconAppListAdapter;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.DesktopIconInfo;
import com.farmerbb.taskbar.util.U;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DesktopIconSelectAppActivity extends AppCompatActivity {

    private DesktopIconAppListGenerator appListGenerator;
    private ProgressBar progressBar;
    private ListView appList;

    private DesktopIconInfo desktopIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        desktopIcon = (DesktopIconInfo) getIntent().getSerializableExtra("desktop_icon");
        boolean noShadow = getIntent().hasExtra("no_shadow");

        if(savedInstanceState == null) {
            setContentView(R.layout.desktop_icon_select_app);
            setFinishOnTouchOutside(false);
            setTitle(getString(R.string.select_an_app));

            if(noShadow) {
                WindowManager.LayoutParams params = getWindow().getAttributes();
                params.dimAmount = 0;
                getWindow().setAttributes(params);

                if(U.isChromeOs(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
                    getWindow().setElevation(0);
            }

            progressBar = findViewById(R.id.progress_bar);
            appList = findViewById(R.id.list);

            appListGenerator = new DesktopIconAppListGenerator();
            appListGenerator.execute();
        } else {
            finish();

            if(!noShadow)
                new Handler().post(() -> {
                    Intent intent = null;
                    SharedPreferences pref = U.getSharedPreferences(this);

                    switch(pref.getString("theme", "light")) {
                        case "light":
                            intent = new Intent(this, DesktopIconSelectAppActivity.class);
                            break;
                        case "dark":
                            intent = new Intent(this, DesktopIconSelectAppActivityDark.class);
                            break;
                    }

                    if(intent != null)
                        intent.putExtra("desktop_icon", desktopIcon);

                    startActivity(intent);
                });
        }
    }

    @Override
    public void finish() {
        if(appListGenerator != null && appListGenerator.getStatus() == AsyncTask.Status.RUNNING)
            appListGenerator.cancel(true);

        super.finish();
    }

    public void selectApp(AppEntry entry) {
        desktopIcon.entry = entry;

        try {
            SharedPreferences pref = U.getSharedPreferences(this);
            JSONArray icons = new JSONArray(pref.getString("desktop_icons", "[]"));
            icons.put(desktopIcon.toJson(this));

            pref.edit().putString("desktop_icons", icons.toString()).apply();
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.REFRESH_DESKTOP_ICONS"));
        } catch (JSONException e) { /* Gracefully fail */ }

        finish();
    }

    private final class DesktopIconAppListGenerator extends AsyncTask<Void, Void, DesktopIconAppListAdapter> {
        @Override
        protected DesktopIconAppListAdapter doInBackground(Void... params) {
            final PackageManager pm = getPackageManager();

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> info = pm.queryIntentActivities(intent, 0);

            Collections.sort(info, (ai1, ai2) -> ai1.activityInfo.loadLabel(pm).toString().compareTo(ai2.activityInfo.loadLabel(pm).toString()));

            final List<AppEntry> entries = new ArrayList<>();
            for(ResolveInfo appInfo : info) {
                entries.add(new AppEntry(
                        appInfo.activityInfo.applicationInfo.packageName,
                        new ComponentName(
                                appInfo.activityInfo.applicationInfo.packageName,
                                appInfo.activityInfo.name).flattenToString(),
                        appInfo.loadLabel(pm).toString(),
                        appInfo.loadIcon(pm),
                        false));
            }

            return new DesktopIconAppListAdapter(DesktopIconSelectAppActivity.this, R.layout.desktop_icon_row, entries);
        }

        @Override
        protected void onPostExecute(DesktopIconAppListAdapter adapter) {
            progressBar.setVisibility(View.GONE);
            appList.setAdapter(adapter);
        }
    }
}