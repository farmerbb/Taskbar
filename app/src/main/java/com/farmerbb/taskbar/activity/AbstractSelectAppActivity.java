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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.adapter.DesktopIconAppListAdapter;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.U;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.farmerbb.taskbar.util.Constants.*;

public abstract class AbstractSelectAppActivity extends AppCompatActivity {

    private DesktopIconAppListGenerator appListGenerator;
    private ProgressBar progressBar;
    private ListView appList;

    private boolean isCollapsed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean noShadow = getIntent().hasExtra("no_shadow");

        setContentView(R.layout.tb_desktop_icon_select_app);
        setFinishOnTouchOutside(false);
        setTitle(getString(R.string.tb_select_an_app));

        if(noShadow) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.dimAmount = 0;
            getWindow().setAttributes(params);

            if(U.isChromeOs(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
                getWindow().setElevation(0);
        }

        SharedPreferences pref = U.getSharedPreferences(this);
        isCollapsed = !pref.getBoolean(PREF_COLLAPSED, false);

        if(!isCollapsed) {
            U.sendBroadcast(this, ACTION_HIDE_TASKBAR);
        }

        progressBar = findViewById(R.id.progress_bar);
        appList = findViewById(R.id.list);

        appListGenerator = new DesktopIconAppListGenerator();
        appListGenerator.execute();
    }

    @Override
    public void finish() {
        if(appListGenerator != null && appListGenerator.getStatus() == AsyncTask.Status.RUNNING) {
            appListGenerator.cancel(true);
        }

        if(!isCollapsed) {
            U.sendBroadcast(this, ACTION_SHOW_TASKBAR);
        }
        super.finish();
    }

    public abstract void selectApp(AppEntry entry);

    private final class DesktopIconAppListGenerator extends AsyncTask<Void, Void, DesktopIconAppListAdapter> {
        @Override
        protected DesktopIconAppListAdapter doInBackground(Void... params) {
            UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
            LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);

            final List<UserHandle> userHandles = userManager.getUserProfiles();
            final List<LauncherActivityInfo> info = new ArrayList<>();

            for(UserHandle handle : userHandles) {
                info.addAll(launcherApps.getActivityList(null, handle));
            }

            Collections.sort(info, (ai1, ai2) -> {
                String label1;
                String label2;

                try {
                    label1 = ai1.getLabel().toString();
                    label2 = ai2.getLabel().toString();
                } catch (OutOfMemoryError e) {
                    System.gc();

                    label1 = ai1.getApplicationInfo().packageName;
                    label2 = ai2.getApplicationInfo().packageName;
                }

                return Collator.getInstance().compare(label1, label2);
            });

            final List<AppEntry> entries = new ArrayList<>();
            for(LauncherActivityInfo appInfo : info) {
                AppEntry entry = new AppEntry(
                        appInfo.getApplicationInfo().packageName,
                        new ComponentName(
                                appInfo.getApplicationInfo().packageName,
                                appInfo.getName()).flattenToString(),
                        appInfo.getLabel().toString(),
                        IconCache.getInstance(AbstractSelectAppActivity.this)
                                .getIcon(AbstractSelectAppActivity.this, appInfo),
                        false);

                entry.setUserId(userManager.getSerialNumberForUser(appInfo.getUser()));
                entries.add(entry);
            }

            return new DesktopIconAppListAdapter(AbstractSelectAppActivity.this, R.layout.tb_desktop_icon_row, entries);
        }

        @Override
        protected void onPostExecute(DesktopIconAppListAdapter adapter) {
            progressBar.setVisibility(View.GONE);
            appList.setAdapter(adapter);
            setFinishOnTouchOutside(true);
        }
    }
}