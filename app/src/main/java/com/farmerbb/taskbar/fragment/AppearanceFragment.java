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
import android.preference.ListPreference;
import android.preference.Preference;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.IconPackActivity;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class AppearanceFragment extends SettingsFragment {
    private int alpha, red, green, blue;

    private enum ColorPickerType { BACKGROUND_TINT, ACCENT_COLOR }

    @Override
    protected void loadPrefs() {
        SharedPreferences pref = U.getSharedPreferences(getActivity());
        if(pref.getString(PREF_START_BUTTON_IMAGE, "null").equals("null"))
            pref.edit().putString(PREF_START_BUTTON_IMAGE, U.getDefaultStartButtonImage(getActivity())).apply();

        // Add preferences
        addPreferencesFromResource(R.xml.tb_pref_appearance);

        // Set OnClickListeners for certain preferences
        findPreference(PREF_ICON_PACK_LIST).setOnPreferenceClickListener(this);
        findPreference(PREF_RESET_COLORS).setOnPreferenceClickListener(this);
        findPreference(PREF_BACKGROUND_TINT_PREF).setOnPreferenceClickListener(this);
        findPreference(PREF_ACCENT_COLOR_PREF).setOnPreferenceClickListener(this);

        if(U.isAndroidGeneric(getActivity())) {
            String[] array = getResources().getStringArray(R.array.tb_pref_start_button_image_list);
            String flavor = U.getSystemProperty("ro.ag.flavor");

            if(flavor == null || flavor.isEmpty())
                flavor = getString(R.string.tb_rom);

            array[1] = getString(R.string.tb_pref_title_app_drawer_icon_ag, flavor);
            ((ListPreference) findPreference(PREF_START_BUTTON_IMAGE)).setEntries(array);
        }

        bindPreferenceSummaryToValue(findPreference(PREF_THEME));
        bindPreferenceSummaryToValue(findPreference(PREF_INVISIBLE_BUTTON));
        bindPreferenceSummaryToValue(findPreference(PREF_START_BUTTON_IMAGE));
        bindPreferenceSummaryToValue(findPreference(PREF_ICON_PACK_USE_MASK));
        bindPreferenceSummaryToValue(findPreference(PREF_VISUAL_FEEDBACK));
        bindPreferenceSummaryToValue(findPreference(PREF_SHORTCUT_ICON));
        bindPreferenceSummaryToValue(findPreference(PREF_TRANSPARENT_START_MENU));
        bindPreferenceSummaryToValue(findPreference(PREF_HIDE_ICON_LABELS));

        findPreference(PREF_BACKGROUND_TINT_PREF).setSummary("#" + String.format("%08x", U.getBackgroundTint(getActivity())).toUpperCase());
        findPreference(PREF_ACCENT_COLOR_PREF).setSummary("#" + String.format("%08x", U.getAccentColor(getActivity())).toUpperCase());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.tb_pref_header_appearance);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        Preference iconPackListPref = findPreference(PREF_ICON_PACK_LIST);
        if(iconPackListPref != null) {
            SharedPreferences pref = U.getSharedPreferences(getActivity());
            String iconPackPackage = pref.getString(PREF_ICON_PACK, getActivity().getPackageName());
            PackageManager pm = getActivity().getPackageManager();

            boolean iconPackValid = true;
            try {
                pm.getPackageInfo(iconPackPackage, 0);
            } catch (PackageManager.NameNotFoundException e) {
                iconPackValid = false;
            }

            if(!iconPackValid || iconPackPackage.equals(getActivity().getPackageName())) {
                iconPackListPref.setSummary(getString(R.string.tb_icon_pack_none));
            } else {
                try {
                    iconPackListPref.setSummary(pm.getApplicationLabel(pm.getApplicationInfo(iconPackPackage, 0)));
                } catch (PackageManager.NameNotFoundException ignored) {}
            }
        }
    }

    @Override
    public boolean onPreferenceClick(final Preference p) {
        final SharedPreferences pref = U.getSharedPreferences(getActivity());

        switch(p.getKey()) {
            case PREF_ICON_PACK_LIST:
                Intent intent = U.getThemedIntent(getActivity(), IconPackActivity.class);
                startActivityForResult(intent, 123);
                break;
            case PREF_RESET_COLORS:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.tb_reset_colors)
                        .setMessage(R.string.tb_are_you_sure)
                        .setNegativeButton(R.string.tb_action_cancel, null)
                        .setPositiveButton(R.string.tb_action_ok, (dialog, which) -> {
                            finishedLoadingPrefs = false;

                            pref.edit().remove(PREF_BACKGROUND_TINT).remove(PREF_ACCENT_COLOR).apply();

                            findPreference(PREF_BACKGROUND_TINT_PREF).setSummary("#" + String.format("%08x", U.getBackgroundTint(getActivity())).toUpperCase());
                            findPreference(PREF_ACCENT_COLOR_PREF).setSummary("#" + String.format("%08x", U.getAccentColor(getActivity())).toUpperCase());

                            finishedLoadingPrefs = true;
                            U.restartTaskbar(getActivity());
                        });

                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            case PREF_BACKGROUND_TINT_PREF:
                showColorPicker(ColorPickerType.BACKGROUND_TINT);
                break;
            case PREF_ACCENT_COLOR_PREF:
                showColorPicker(ColorPickerType.ACCENT_COLOR);
                break;
        }

        return super.onPreferenceClick(p);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK)
            return;

        if(requestCode == 123) {
            U.refreshPinnedIcons(getActivity());
            U.restartTaskbar(getActivity());
        }

        if(requestCode == U.IMAGE_REQUEST_CODE) {
            if(data.getData() == null)
                return;

            if(U.importImage(getActivity(), data.getData(), "custom_image"))
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
                dialogTitle = R.string.tb_pref_title_background_tint;
                break;
            case ACCENT_COLOR:
                color = U.getAccentColor(getActivity());
                dialogTitle = R.string.tb_pref_title_accent_color;
                break;
        }

        alpha = Color.alpha(color);
        red = Color.red(color);
        green = Color.green(color);
        blue = Color.blue(color);

        ScrollView dialogLayout = (ScrollView) View.inflate(getActivity(), R.layout.tb_color_picker_pref, null);

        View colorPreview = dialogLayout.findViewById(R.id.color_preview);
        colorPreview.setBackgroundColor(Color.argb(alpha, red, green, blue));

        TextView hexPreview = dialogLayout.findViewById(R.id.hex_preview);
        hexPreview.setText("#" + String.format("%08x", Color.argb(alpha, red, green, blue)).toUpperCase());

        final TextView alphaValue = dialogLayout.findViewById(R.id.alpha_value);
        alphaValue.setText("0");

        final SeekBar alphaSeekBar = dialogLayout.findViewById(R.id.alpha_seekbar);
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

        final TextView redValue = dialogLayout.findViewById(R.id.red_value);
        redValue.setText("0");

        final SeekBar redSeekBar = dialogLayout.findViewById(R.id.red_seekbar);
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

        final TextView greenValue = dialogLayout.findViewById(R.id.green_value);
        greenValue.setText("0");

        final SeekBar greenSeekBar = dialogLayout.findViewById(R.id.green_seekbar);
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

        final TextView blueValue = dialogLayout.findViewById(R.id.blue_value);
        blueValue.setText("0");

        final SeekBar blueSeekBar = dialogLayout.findViewById(R.id.blue_seekbar);
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
                .setPositiveButton(R.string.tb_action_ok, (dialog, which) -> {
                    String preferenceId = null;
                    switch(type) {
                        case BACKGROUND_TINT:
                            preferenceId = PREF_BACKGROUND_TINT;
                            break;
                        case ACCENT_COLOR:
                            preferenceId = PREF_ACCENT_COLOR;
                            break;
                    }

                    SharedPreferences pref = U.getSharedPreferences(getActivity());
                    pref.edit().putInt(preferenceId, Color.argb(alpha, red, green, blue)).apply();

                    findPreference(preferenceId + "_pref").setSummary("#" + String.format("%08x", Color.argb(alpha, red, green, blue)).toUpperCase());

                    U.restartTaskbar(getActivity());
                })
                .setNegativeButton(R.string.tb_action_cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
