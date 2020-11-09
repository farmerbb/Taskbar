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
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class RecentAppsFragment extends SettingsFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void addPrefsToSanitize() {
        prefsToSanitize.put(PREF_SYS_TRAY, R.bool.class);
    }

    @Override
    protected void loadPrefs() {
        // Add preferences
        addPreferencesFromResource(R.xml.tb_pref_recent_apps);

        // Set OnClickListeners for certain preferences
        findPreference(PREF_ENABLE_RECENTS).setOnPreferenceClickListener(this);
        findPreference(PREF_MAX_NUM_OF_RECENTS).setOnPreferenceClickListener(this);
        findPreference(PREF_REFRESH_FREQUENCY).setOnPreferenceClickListener(this);

        if(showRunningAppsOnly()) {
            ListPreference recentsAmountPref = ((ListPreference) findPreference(PREF_RECENTS_AMOUNT));
            recentsAmountPref.setEntries(getResources().getStringArray(R.array.tb_pref_recents_amount_alt));
            recentsAmountPref.setEntryValues(getResources().getStringArray(R.array.tb_pref_recents_amount_values_alt));

            SharedPreferences pref = U.getSharedPreferences(getActivity());
            if(PREF_RECENTS_AMOUNT_RUNNING_APPS_ONLY.equals(
                    pref.getString(PREF_RECENTS_AMOUNT, PREF_RECENTS_AMOUNT_PAST_DAY))) {
                ListPreference sortOrderPref = ((ListPreference) findPreference(PREF_SORT_ORDER));
                sortOrderPref.setEntries(getResources().getStringArray(R.array.tb_pref_sort_order_alt));
                sortOrderPref.setEntryValues(getResources().getStringArray(R.array.tb_pref_sort_order_values_alt));
            }
        }

        bindPreferenceSummaryToValue(findPreference(PREF_RECENTS_AMOUNT));
        bindPreferenceSummaryToValue(findPreference(PREF_SORT_ORDER));
        bindPreferenceSummaryToValue(findPreference(PREF_DISABLE_SCROLLING_LIST));
        bindPreferenceSummaryToValue(findPreference(PREF_FULL_LENGTH));
        bindPreferenceSummaryToValue(findPreference(PREF_CENTERED_ICONS));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(U.isLibrary(getActivity()) || U.isAndroidTV(getActivity()))
                getPreferenceScreen().removePreference(findPreference(PREF_NOTIFICATION_COUNT));
            else
                findPreference(PREF_NOTIFICATION_COUNT).setOnPreferenceClickListener(this);

            bindPreferenceSummaryToValue(findPreference(PREF_SYS_TRAY));
        } else {
            getPreferenceScreen().removePreference(findPreference(PREF_NOTIFICATION_COUNT));
            getPreferenceScreen().removePreference(findPreference(PREF_SYS_TRAY));
        }

        updateMaxNumOfRecents(false);
        updateRefreshFrequency(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.tb_pref_header_recent_apps);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @SuppressLint("SetTextI18n")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public boolean onPreferenceClick(final Preference p) {
        final SharedPreferences pref = U.getSharedPreferences(getActivity());

        switch(p.getKey()) {
            case PREF_ENABLE_RECENTS:
                try {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                } catch (ActivityNotFoundException e) {
                    showAndroidTVPermissionDialog(R.string.tb_enable_recent_apps_instructions_tv,
                            () -> U.showErrorDialog(getActivity(), "GET_USAGE_STATS"));
                }
                break;
            case PREF_MAX_NUM_OF_RECENTS:
                final int max = 26;

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LinearLayout dialogLayout = (LinearLayout) View.inflate(getActivity(), R.layout.tb_seekbar_pref, null);

                String value = pref.getString(PREF_MAX_NUM_OF_RECENTS, "10");

                final TextView textView = dialogLayout.findViewById(R.id.seekbar_value);
                textView.setText("0");

                final SeekBar seekBar = dialogLayout.findViewById(R.id.seekbar);
                seekBar.setMax(max);
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if(progress == max)
                            textView.setText(R.string.tb_infinity);
                        else
                            textView.setText(Integer.toString(progress));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                seekBar.setProgress(Integer.parseInt(value));

                TextView blurb = dialogLayout.findViewById(R.id.blurb);
                blurb.setText(R.string.tb_num_of_recents_blurb);

                builder.setView(dialogLayout)
                        .setTitle(R.string.tb_pref_max_num_of_recents)
                        .setPositiveButton(R.string.tb_action_ok, (dialog, id) -> {
                            int progress = seekBar.getProgress();
                            if(progress == max)
                                progress = Integer.MAX_VALUE;

                            pref.edit().putString(PREF_MAX_NUM_OF_RECENTS, Integer.toString(progress)).apply();
                            updateMaxNumOfRecents(true);
                        })
                        .setNegativeButton(R.string.tb_action_cancel, null);

                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            case PREF_REFRESH_FREQUENCY:
                final int max2 = 20;

                AlertDialog.Builder builder2 = new AlertDialog.Builder(getActivity());
                LinearLayout dialogLayout2 = (LinearLayout) View.inflate(getActivity(), R.layout.tb_seekbar_pref, null);

                String value2 = pref.getString(PREF_REFRESH_FREQUENCY, "1");

                final TextView textView2 = dialogLayout2.findViewById(R.id.seekbar_value);
                textView2.setText(R.string.tb_infinity);

                final SeekBar seekBar2 = dialogLayout2.findViewById(R.id.seekbar);
                seekBar2.setMax(max2);
                seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if(progress == 0)
                            textView2.setText(R.string.tb_infinity);
                        else
                            textView2.setText(Double.toString(progress * 0.5));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                seekBar2.setProgress((int) (Double.parseDouble(value2) * 2));

                TextView blurb2 = dialogLayout2.findViewById(R.id.blurb);
                blurb2.setText(R.string.tb_refresh_frequency_blurb);

                builder2.setView(dialogLayout2)
                        .setTitle(R.string.tb_pref_title_recents_refresh_interval)
                        .setPositiveButton(R.string.tb_action_ok, (dialog2, id) -> {
                            double progress = seekBar2.getProgress() * 0.5;

                            pref.edit().putString(PREF_REFRESH_FREQUENCY, Double.toString(progress)).apply();
                            updateRefreshFrequency(true);
                        })
                        .setNegativeButton(R.string.tb_action_cancel, null);

                AlertDialog dialog2 = builder2.create();
                dialog2.show();
                break;
            case PREF_NOTIFICATION_COUNT:
                try {
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                } catch (ActivityNotFoundException e) {
                    U.showToast(getActivity(), R.string.tb_lock_device_not_supported);
                }
                break;
        }

        return super.onPreferenceClick(p);
    }

    private void updateMaxNumOfRecents(boolean restartTaskbar) {
        SharedPreferences pref = U.getSharedPreferences(getActivity());
        int value = Integer.parseInt(pref.getString(PREF_MAX_NUM_OF_RECENTS, "10"));

        switch(value) {
            case 1:
                findPreference(PREF_MAX_NUM_OF_RECENTS).setSummary(R.string.tb_max_num_of_recents_singular);
                break;
            case Integer.MAX_VALUE:
                findPreference(PREF_MAX_NUM_OF_RECENTS).setSummary(R.string.tb_max_num_of_recents_unlimited);
                break;
            default:
                findPreference(PREF_MAX_NUM_OF_RECENTS).setSummary(getString(R.string.tb_max_num_of_recents, value));
                break;
        }

        if(restartTaskbar) U.restartTaskbar(getActivity());
    }

    private void updateRefreshFrequency(boolean restartTaskbar) {
        SharedPreferences pref = U.getSharedPreferences(getActivity());
        String value = pref.getString(PREF_REFRESH_FREQUENCY, "1");
        double doubleValue = Double.parseDouble(value);
        int intValue = (int) doubleValue;

        if(doubleValue == 0)
            findPreference(PREF_REFRESH_FREQUENCY).setSummary(R.string.tb_refresh_frequency_continuous);
        else if(doubleValue == 1)
            findPreference(PREF_REFRESH_FREQUENCY).setSummary(R.string.tb_refresh_frequency_singular);
        else if(doubleValue == (double) intValue)
            findPreference(PREF_REFRESH_FREQUENCY).setSummary(getString(R.string.tb_refresh_frequency, Integer.toString(intValue)));
        else
            findPreference(PREF_REFRESH_FREQUENCY).setSummary(getString(R.string.tb_refresh_frequency, value));

        if(restartTaskbar) U.restartTaskbar(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();

        // Register listener to check for changed preferences
        if(showRunningAppsOnly())
            PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        // Unregister listener
        if(showRunningAppsOnly())
            PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(PREF_RECENTS_AMOUNT)) {
            boolean useAlt = sharedPreferences.getString(key, "past_day").equals("running_apps_only");

            ListPreference sortOrderPref = ((ListPreference) findPreference(PREF_SORT_ORDER));
            sortOrderPref.setEntries(getResources().getStringArray(useAlt ? R.array.tb_pref_sort_order_alt : R.array.tb_pref_sort_order));
            sortOrderPref.setEntryValues(getResources().getStringArray(useAlt ? R.array.tb_pref_sort_order_values_alt : R.array.tb_pref_sort_order_values));

            String sortOrderValue = sharedPreferences.getString(PREF_SORT_ORDER, "false");
            if(useAlt && sortOrderValue.startsWith("most_used_"))
                sharedPreferences.edit().putString(PREF_SORT_ORDER, sortOrderValue.replace("most_used_", "")).apply();
        }
    }

    private boolean showRunningAppsOnly() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && U.isSystemApp(getActivity());
    }

    private void showAndroidTVPermissionDialog(int message, Runnable onError) {
        if(!U.hasAndroidTVSettings(getActivity())) {
            onError.run();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.tb_permission_dialog_title)
                .setMessage(message)
                .setPositiveButton(R.string.tb_action_open_settings, (dialog, which) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
                    } catch (ActivityNotFoundException e) {
                        onError.run();
                    }
                })
                .setNegativeButton(R.string.tb_action_cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
