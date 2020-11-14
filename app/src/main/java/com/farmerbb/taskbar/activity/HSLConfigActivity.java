/* Copyright 2020 Braden Farmer
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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.U;

import java.util.ArrayList;
import java.util.List;

import static com.farmerbb.taskbar.util.Constants.*;

public class HSLConfigActivity extends AppCompatActivity {

    private boolean returnToSettings;

    private static final class LauncherListAdapter extends ArrayAdapter<String> {
        private LauncherListAdapter(Context context, ArrayList<String> notes) {
            super(context, R.layout.tb_hsl_row_layout, notes);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            String launcher = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if(convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.tb_hsl_row_layout, parent, false);
            }
            // Lookup view for data population
            TextView launcherTitle = convertView.findViewById(R.id.launcherTitle);
            // Populate the data into the template view using the data object
            launcherTitle.setText(launcher);

            // Return the completed view to render on screen
            return convertView;
        }
    }

    ArrayList<String> packageIds;
    ArrayList<String> packageNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isDarkTheme = U.isDarkTheme(this);
        setTheme(isDarkTheme ? R.style.Taskbar_Dark : R.style.Taskbar);
        setContentView(R.layout.tb_activity_hsl_config);

        returnToSettings = getIntent().getBooleanExtra("return_to_settings", false);

        SharedPreferences pref = U.getSharedPreferences(this);
        pref.edit().putBoolean(PREF_DESKTOP_MODE, U.isDesktopModeSupported(this)).apply();

        if(getSupportActionBar() == null) return;

        if(returnToSettings) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(returnToSettings);
            findViewById(R.id.space).setVisibility(View.GONE);
        } else {
            // Make action bar invisible
            getSupportActionBar().setElevation(0);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0));
            getWindow().setStatusBarColor(ContextCompat.getColor(this, isDarkTheme
                    ? R.color.tb_main_activity_background_dark
                    : R.color.tb_main_activity_background));

            if(!isDarkTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View view = getWindow().getDecorView();
                view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }

            setTitle(null);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        packageIds = new ArrayList<>();
        packageNames = new ArrayList<>();

        // Get list of currently installed launchers
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        PackageManager manager = getPackageManager();
        final List<ResolveInfo> listOfLaunchers = manager.queryIntentActivities(homeIntent, 0);

        for(ResolveInfo launcher : listOfLaunchers) {
            String string = launcher.activityInfo.packageName;

            // Don't include the Settings dummy launcher, or Taskbar itself, on the list
            if(!string.equals("com.android.settings")
                    && !string.equals("com.android.tv.settings")
                    && !string.equals("com.android.shell")
                    && !string.startsWith("com.farmerbb.taskbar")) {
                if(string.equals("com.google.android.googlequicksearchbox")) {
                    // Only add the Google App onto the list if Google Now Launcher is installed
                    try {
                        packageNames.add(manager.getApplicationInfo("com.google.android.launcher", 0).loadLabel(manager).toString());
                        packageIds.add(string);
                    } catch (PackageManager.NameNotFoundException ignored) {}
                } else {
                    packageNames.add(launcher.activityInfo.applicationInfo.loadLabel(manager).toString());
                    packageIds.add(string);
                }
            }
        }

        ListView listView = findViewById(R.id.listView);
        TextView textView = findViewById(R.id.textView);

        // Display the list of launchers
        if(packageNames.size() > 0) {
            textView.setText(R.string.tb_hsl_main_activity_text);

            // Create the custom adapter to bind the array to the ListView
            final LauncherListAdapter adapter = new LauncherListAdapter(this, packageNames);

            // Display the ListView
            listView.setAdapter(adapter);
            listView.setClickable(true);
            listView.setOnItemClickListener((arg0, arg1, position, arg3) -> {
                SharedPreferences pref = U.getSharedPreferences(HSLConfigActivity.this);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString(PREF_HSL_ID, packageIds.get(position));
                editor.putString(PREF_HSL_NAME, packageNames.get(position));
                editor.apply();

                for(ResolveInfo launcher : listOfLaunchers) {
                    if(packageIds.get(position).equals(launcher.activityInfo.packageName)) {
                        if(!returnToSettings) {
                            try {
                                startActivity(homeIntent);
                            } catch (ActivityNotFoundException ignored) {}
                        }

                        finish();
                    }
                }
            });
        } else
            textView.setText(R.string.tb_hsl_no_launchers_found);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate action bar menu
        if(!returnToSettings) getMenuInflater().inflate(R.menu.tb_hsl_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        if(item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if(item.getItemId() == android.R.id.home)
            onBackPressed();

        return true;
    }

    @Override
    public void onBackPressed() {
        if(returnToSettings) super.onBackPressed();
    }
}
