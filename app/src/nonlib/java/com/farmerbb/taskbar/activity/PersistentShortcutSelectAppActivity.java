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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.service.quicksettings.TileService;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class PersistentShortcutSelectAppActivity extends AbstractSelectAppActivity {

    private AppEntry selectedEntry;
    private float threshold;

    private boolean processing = false;
    private float thresholdInProcess;

    @Override
    public void selectApp(AppEntry entry) {
        selectedEntry = entry;

        boolean windowSizeOptions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && U.hasFreeformSupport(this);
        boolean iconOptions = getIntent().getIntExtra(PREF_QS_TILE, 0) > 0;

        if(!windowSizeOptions && !iconOptions) {
            createShortcut(null);
            return;
        }

        LinearLayout layout = (LinearLayout) View.inflate(this, R.layout.tb_shortcut_options, null);
        final Spinner spinner = layout.findViewById(R.id.spinner);
        final CheckBox checkBox = layout.findViewById(R.id.checkBox);

        String[] windowSizes = getResources().getStringArray(R.array.tb_pref_window_size_list_values);

        if(windowSizeOptions) {
            layout.findViewById(R.id.window_size_options).setVisibility(View.VISIBLE);

            SharedPreferences pref = U.getSharedPreferences(this);
            boolean isFreeformEnabled = U.isFreeformModeEnabled(this);

            checkBox.setChecked(isFreeformEnabled);
            spinner.setEnabled(isFreeformEnabled);

            String defaultWindowSize = pref.getString(PREF_WINDOW_SIZE, "standard");
            for(int i = 0; i < windowSizes.length; i++) {
                if(windowSizes[i].equals(defaultWindowSize)) {
                    spinner.setSelection(i);
                    break;
                }
            }

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> spinner.setEnabled(isChecked));
        }

        if(iconOptions) {
            layout.findViewById(R.id.icon_options).setVisibility(View.VISIBLE);

            SeekBar seekBar = layout.findViewById(R.id.seekbar);
            ImageView imageView = layout.findViewById(R.id.icon_preview);

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    Context context = PersistentShortcutSelectAppActivity.this;
                    Drawable icon = selectedEntry.getIcon(context);
                    threshold = (float) Math.log10(progress + 1) / 2;

                    if(processing) return;

                    Handler handler = U.newHandler();
                    new Thread(() -> {
                        processing = true;

                        while(threshold != thresholdInProcess) {
                            thresholdInProcess = threshold;

                            Drawable monoIcon = U.convertToMonochrome(context, icon, thresholdInProcess);
                            Drawable resizedIcon = U.resizeDrawable(context, monoIcon, R.dimen.tb_qs_icon_preview_size);

                            handler.post(() -> imageView.setImageDrawable(resizedIcon));
                        }

                        processing = false;
                    }).start();
                }

                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            thresholdInProcess = -1;
            seekBar.setProgress(50);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(selectedEntry.getLabel())
                .setView(layout)
                .setPositiveButton(R.string.tb_action_ok, (dialog, which) -> {
                    if(windowSizeOptions)
                        createShortcut(checkBox.isChecked() ? windowSizes[spinner.getSelectedItemPosition()] : null);
                    else
                        createShortcut(null);
                })
                .setNegativeButton(R.string.tb_action_cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void createShortcut(String windowSize) {
        int num = getIntent().getIntExtra(PREF_QS_TILE, 0);
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
            shortcutIntent.putExtra("user_id", selectedEntry.getUserId(this));

            if(windowSize != null) shortcutIntent.putExtra("window_size", windowSize);

            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(packageContext, applicationInfo.icon));
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, selectedEntry.getLabel());

            setResult(RESULT_OK, intent);
        } catch (PackageManager.NameNotFoundException ignored) {}
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void createQuickSettingTileShortcut(String windowSize, int num) {
        String prefix = "qs_tile_" + num + "_";

        SharedPreferences pref = U.getSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();

        editor.putString(prefix + "package_name", selectedEntry.getPackageName());
        editor.putString(prefix + "component_name", selectedEntry.getComponentName());
        editor.putString(prefix + "label", selectedEntry.getLabel());
        editor.putString(prefix + "window_size", windowSize);
        editor.putLong(prefix + "user_id", selectedEntry.getUserId(this));
        editor.putFloat(prefix + "icon_threshold", threshold);
        editor.putBoolean(prefix + PREF_ADDED_SUFFIX, true);
        editor.apply();

        try {
            Class<?> clazz = Class.forName("com.farmerbb.taskbar.service.FavoriteApp" + num);
            TileService.requestListeningState(this, new ComponentName(this, clazz));
        } catch (ClassNotFoundException ignored) {}
    }
}