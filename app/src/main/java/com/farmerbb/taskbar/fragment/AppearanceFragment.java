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

package com.farmerbb.taskbar.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.IconPackActivity;
import com.farmerbb.taskbar.activity.dark.IconPackActivityDark;
import com.farmerbb.taskbar.util.U;

public class AppearanceFragment extends SettingsFragment implements Preference.OnPreferenceClickListener {
    private int alpha, red, green, blue;

    private enum ColorPickerType { BACKGROUND_TINT, ACCENT_COLOR }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onActivityCreated(savedInstanceState);

        if(findPreference("dummy") == null) {
            // Add preferences
            addPreferencesFromResource(R.xml.pref_appearance);

            // Set OnClickListeners for certain preferences
            findPreference("icon_pack_list").setOnPreferenceClickListener(this);
            findPreference("reset_colors").setOnPreferenceClickListener(this);
            findPreference("background_tint_pref").setOnPreferenceClickListener(this);
            findPreference("accent_color_pref").setOnPreferenceClickListener(this);

            bindPreferenceSummaryToValue(findPreference("theme"));
            bindPreferenceSummaryToValue(findPreference("invisible_button"));
            bindPreferenceSummaryToValue(findPreference("app_drawer_icon"));
            bindPreferenceSummaryToValue(findPreference("icon_pack_use_mask"));
            bindPreferenceSummaryToValue(findPreference("visual_feedback"));
            bindPreferenceSummaryToValue(findPreference("shortcut_icon"));
            bindPreferenceSummaryToValue(findPreference("transparent_start_menu"));

            findPreference("background_tint_pref").setSummary("#" + String.format("%08x", U.getBackgroundTint(getActivity())).toUpperCase());
            findPreference("accent_color_pref").setSummary("#" + String.format("%08x", U.getAccentColor(getActivity())).toUpperCase());
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.pref_header_appearance);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        finishedLoadingPrefs = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        Preference iconPackListPref = findPreference("icon_pack_list");
        if(iconPackListPref != null) {
            SharedPreferences pref = U.getSharedPreferences(getActivity());
            String iconPackPackage = pref.getString("icon_pack", getActivity().getPackageName());
            PackageManager pm = getActivity().getPackageManager();

            boolean iconPackValid = true;
            try {
                pm.getPackageInfo(iconPackPackage, 0);
            } catch (PackageManager.NameNotFoundException e) {
                iconPackValid = false;
            }

            if(!iconPackValid || iconPackPackage.equals(getActivity().getPackageName())) {
                iconPackListPref.setSummary(getString(R.string.icon_pack_none));
            } else {
                try {
                    iconPackListPref.setSummary(pm.getApplicationLabel(pm.getApplicationInfo(iconPackPackage, 0)));
                } catch (PackageManager.NameNotFoundException e) { /* Gracefully fail */ }
            }
        }
    }

    @Override
    public boolean onPreferenceClick(final Preference p) {
        final SharedPreferences pref = U.getSharedPreferences(getActivity());

        switch(p.getKey()) {
            case "icon_pack_list":
                Intent intent = null;

                switch(pref.getString("theme", "light")) {
                    case "light":
                        intent = new Intent(getActivity(), IconPackActivity.class);
                        break;
                    case "dark":
                        intent = new Intent(getActivity(), IconPackActivityDark.class);
                        break;
                }

                startActivityForResult(intent, 123);
                break;
            case "reset_colors":
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.reset_colors)
                        .setMessage(R.string.are_you_sure)
                        .setNegativeButton(R.string.action_cancel, null)
                        .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                            finishedLoadingPrefs = false;

                            pref.edit().remove("background_tint").remove("accent_color").apply();

                            findPreference("background_tint_pref").setSummary("#" + String.format("%08x", U.getBackgroundTint(getActivity())).toUpperCase());
                            findPreference("accent_color_pref").setSummary("#" + String.format("%08x", U.getAccentColor(getActivity())).toUpperCase());

                            finishedLoadingPrefs = true;
                            U.restartTaskbar(getActivity());
                        });

                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            case "background_tint_pref":
                showColorPicker(ColorPickerType.BACKGROUND_TINT);
                break;
            case "accent_color_pref":
                showColorPicker(ColorPickerType.ACCENT_COLOR);
                break;
        }

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 123 && resultCode == Activity.RESULT_OK) {
            U.refreshPinnedIcons(getActivity());
            U.restartTaskbar(getActivity());
        }
    }

    @SuppressLint("SetTextI18n")
    private void showColorPicker(ColorPickerType type) {
        int color = -1;
        int dialogTitle = -1;

        switch(type) {
            case BACKGROUND_TINT:
                color = U.getBackgroundTint(getActivity());
                dialogTitle = R.string.pref_title_background_tint;
                break;
            case ACCENT_COLOR:
                color = U.getAccentColor(getActivity());
                dialogTitle = R.string.pref_title_accent_color;
                break;
        }

        alpha = Color.alpha(color);
        red = Color.red(color);
        green = Color.green(color);
        blue = Color.blue(color);

        ScrollView dialogLayout = (ScrollView) View.inflate(getActivity(), R.layout.color_picker_pref, null);

        View colorPreview = dialogLayout.findViewById(R.id.color_preview);
        colorPreview.setBackgroundColor(Color.argb(alpha, red, green, blue));

        TextView hexPreview = (TextView) dialogLayout.findViewById(R.id.hex_preview);
        hexPreview.setText("#" + String.format("%08x", Color.argb(alpha, red, green, blue)).toUpperCase());

        final TextView alphaValue = (TextView) dialogLayout.findViewById(R.id.alpha_value);
        alphaValue.setText("0");

        final SeekBar alphaSeekBar = (SeekBar) dialogLayout.findViewById(R.id.alpha_seekbar);
        alphaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                alpha = progress;

                alphaValue.setText(Integer.toString(alpha));
                colorPreview.setBackgroundColor(Color.argb(alpha, red, green, blue));
                hexPreview.setText("#" + String.format("%08x", Color.argb(alpha, red, green, blue)).toUpperCase());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        alphaSeekBar.setProgress(Color.alpha(color));

        final TextView redValue = (TextView) dialogLayout.findViewById(R.id.red_value);
        redValue.setText("0");

        final SeekBar redSeekBar = (SeekBar) dialogLayout.findViewById(R.id.red_seekbar);
        redSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                red = progress;

                redValue.setText(Integer.toString(red));
                colorPreview.setBackgroundColor(Color.argb(alpha, red, green, blue));
                hexPreview.setText("#" + String.format("%08x", Color.argb(alpha, red, green, blue)).toUpperCase());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        redSeekBar.setProgress(Color.red(color));

        final TextView greenValue = (TextView) dialogLayout.findViewById(R.id.green_value);
        greenValue.setText("0");

        final SeekBar greenSeekBar = (SeekBar) dialogLayout.findViewById(R.id.green_seekbar);
        greenSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                green = progress;

                greenValue.setText(Integer.toString(green));
                colorPreview.setBackgroundColor(Color.argb(alpha, red, green, blue));
                hexPreview.setText("#" + String.format("%08x", Color.argb(alpha, red, green, blue)).toUpperCase());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        greenSeekBar.setProgress(Color.green(color));

        final TextView blueValue = (TextView) dialogLayout.findViewById(R.id.blue_value);
        blueValue.setText("0");

        final SeekBar blueSeekBar = (SeekBar) dialogLayout.findViewById(R.id.blue_seekbar);
        blueSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                blue = progress;

                blueValue.setText(Integer.toString(blue));
                colorPreview.setBackgroundColor(Color.argb(alpha, red, green, blue));
                hexPreview.setText("#" + String.format("%08x", Color.argb(alpha, red, green, blue)).toUpperCase());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        blueSeekBar.setProgress(Color.blue(color));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogLayout)
                .setTitle(dialogTitle)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                    String preferenceId = null;
                    switch(type) {
                        case BACKGROUND_TINT:
                            preferenceId = "background_tint";
                            break;
                        case ACCENT_COLOR:
                            preferenceId = "accent_color";
                            break;
                    }

                    SharedPreferences pref = U.getSharedPreferences(getActivity());
                    pref.edit().putInt(preferenceId, Color.argb(alpha, red, green, blue)).apply();

                    findPreference(preferenceId + "_pref").setSummary("#" + String.format("%08x", Color.argb(alpha, red, green, blue)).toUpperCase());

                    U.restartTaskbar(getActivity());
                })
                .setNegativeButton(R.string.action_cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
