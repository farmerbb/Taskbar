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

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.TaskbarIntent;
import com.farmerbb.taskbar.util.U;

public class PersistentShortcutSelectAppActivity extends AbstractSelectAppActivity {

    private AppEntry selectedEntry;

    @Override
    public void selectApp(AppEntry entry) {
        selectedEntry = entry;

        if(U.hasFreeformSupport(this))
            showWindowSizeDialog();
        else
            createShortcut(null);
    }

    private void showWindowSizeDialog() {
        LinearLayout layout = (LinearLayout) View.inflate(this, R.layout.tb_window_size, null);
        final Spinner spinner = layout.findViewById(R.id.spinner);
        final CheckBox checkBox = layout.findViewById(R.id.checkBox);

        SharedPreferences pref = U.getSharedPreferences(this);
        boolean isFreeformEnabled = pref.getBoolean("freeform_hack", false);
        boolean hasBrokenSetLaunchBoundsApi = U.hasBrokenSetLaunchBoundsApi();

        checkBox.setChecked(isFreeformEnabled);
        spinner.setEnabled(isFreeformEnabled && !hasBrokenSetLaunchBoundsApi);

        String defaultWindowSize = pref.getString("window_size", getString(R.string.tb_def_window_size));
        String[] windowSizes = getResources().getStringArray(R.array.tb_pref_window_size_list_values);
        for(int i = 0; i < windowSizes.length; i++) {
            if(windowSizes[i].equals(defaultWindowSize)) {
                spinner.setSelection(i);
                break;
            }
        }

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(!hasBrokenSetLaunchBoundsApi)
                spinner.setEnabled(isChecked);
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(selectedEntry.getLabel())
                .setView(layout)
                .setPositiveButton(R.string.tb_action_ok, (dialog, which) ->
                        createShortcut(checkBox.isChecked() ? windowSizes[spinner.getSelectedItemPosition()] : null))
                .setNegativeButton(R.string.tb_action_cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void createShortcut(String windowSize) {
        int num = getIntent().getIntExtra("qs_tile", 0);
        if(num > 0)
            createQuickSettingTileShortcut(windowSize, num);
        else
            createHomeScreenShortcut(windowSize);

        finish();
    }

    private void createHomeScreenShortcut(String windowSize) {
        try {
            Context packageContext = createPackageContext(selectedEntry.getPackageName(), Context.CONTEXT_IGNORE_SECURITY);
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(selectedEntry.getPackageName(), PackageManager.GET_META_DATA);

            Intent shortcutIntent = new Intent(this, PersistentShortcutLaunchActivity.class);
            shortcutIntent.setAction(Intent.ACTION_MAIN);
            shortcutIntent.putExtra("package_name", selectedEntry.getPackageName());
            shortcutIntent.putExtra("component_name", selectedEntry.getComponentName());

            if(windowSize != null) shortcutIntent.putExtra("window_size", windowSize);

            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(packageContext, applicationInfo.icon));
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, selectedEntry.getLabel());

            setResult(RESULT_OK, intent);
        } catch (PackageManager.NameNotFoundException e) { /* Gracefully fail */ }
    }

    private void createQuickSettingTileShortcut(String windowSize, int num) {
        String prefix = "qs_tile_" + num + "_";

        SharedPreferences pref = U.getSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();

        editor.putString(prefix + "package_name", selectedEntry.getPackageName());
        editor.putString(prefix + "component_name", selectedEntry.getComponentName());
        editor.putString(prefix + "label", selectedEntry.getLabel());
        editor.putString(prefix + "window_size", windowSize);
        editor.putBoolean(prefix + "added", true);
        editor.apply();

        U.sendBroadcast(this, TaskbarIntent.ACTION_UPDATE_FAVORITE_APP_TILE);
    }
}