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

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.provider.Settings;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.SelectAppActivity;
import com.farmerbb.taskbar.util.Blacklist;
import com.farmerbb.taskbar.util.TopApps;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class GeneralFragment extends SettingsFragment {

    @Override
    protected void loadPrefs() {
        // Add preferences
        addPreferencesFromResource(R.xml.tb_pref_general);

        // Set OnClickListeners for certain preferences
        findPreference(PREF_BLACKLIST).setOnPreferenceClickListener(this);

        if(U.isLibrary(getActivity()) || U.isAndroidTV(getActivity())) {
            getPreferenceScreen().removePreference(findPreference(PREF_NOTIFICATION_SETTINGS));

            if(!U.isAndroidTV(getActivity()))
                getPreferenceScreen().removePreference(findPreference(PREF_START_ON_BOOT));
        } else
            findPreference(PREF_NOTIFICATION_SETTINGS).setOnPreferenceClickListener(this);

        if(U.canEnableFreeform(getActivity())
                && !U.isChromeOs(getActivity())
                && !U.isOverridingFreeformHack(getActivity(), false))
            findPreference(PREF_HIDE_TASKBAR).setSummary(R.string.tb_hide_taskbar_disclaimer);

        bindPreferenceSummaryToValue(findPreference(PREF_START_MENU_LAYOUT));
        bindPreferenceSummaryToValue(findPreference(PREF_SCROLLBAR));
        bindPreferenceSummaryToValue(findPreference(PREF_POSITION));
        bindPreferenceSummaryToValue(findPreference(PREF_ANCHOR));
        bindPreferenceSummaryToValue(findPreference(PREF_ALT_BUTTON_CONFIG));
        bindPreferenceSummaryToValue(findPreference(PREF_SHOW_SEARCH_BAR));
        bindPreferenceSummaryToValue(findPreference(PREF_HIDE_WHEN_KEYBOARD_SHOWN));

        if(U.isChromeOs(getActivity()) && U.getCurrentApiVersion() < 30.0f)
            bindPreferenceSummaryToValue(findPreference(PREF_CHROME_OS_CONTEXT_MENU_FIX));
        else
            getPreferenceScreen().removePreference(findPreference(PREF_CHROME_OS_CONTEXT_MENU_FIX));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.tb_pref_header_general);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        int size = Blacklist.getInstance(getActivity()).getBlockedApps().size();
        String summary = size == 1 ? getString(R.string.tb_app_hidden) : getString(R.string.tb_apps_hidden, size);

        size = TopApps.getInstance(getActivity()).getTopApps().size();
        summary = summary + "\n" + (size == 1 ? getString(R.string.tb_top_app) : getString(R.string.tb_top_apps, size));

        Preference blacklistPref = findPreference(PREF_BLACKLIST);
        if(blacklistPref != null) {
            blacklistPref.setSummary(summary);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public boolean onPreferenceClick(final Preference p) {
        switch(p.getKey()) {
            case PREF_BLACKLIST:
                Intent intent = U.getThemedIntent(getActivity(), SelectAppActivity.class);
                startActivity(intent);
                break;
            case PREF_NOTIFICATION_SETTINGS:
                Intent intent2 = new Intent();
                intent2.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);

                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1)
                    intent2.putExtra(Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
                else {
                    intent2.putExtra("app_package", getActivity().getPackageName());
                    intent2.putExtra("app_uid", getActivity().getApplicationInfo().uid);
                }

                try {
                    startActivity(intent2);
                    restartNotificationService = true;
                } catch (ActivityNotFoundException ignored) {}
                break;
        }

        return super.onPreferenceClick(p);
    }
}
