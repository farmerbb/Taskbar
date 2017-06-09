/* Copyright 2017 Braden Farmer
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

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.util.U;

public class NavigationBarButtonsActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener {

    @SuppressWarnings("deprecation")
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setTitle(R.string.navigation_bar_buttons);

        addPreferencesFromResource(R.xml.pref_navigation_bar_buttons);

        findPreference("button_back").setOnPreferenceClickListener(this);
        findPreference("button_home").setOnPreferenceClickListener(this);
        findPreference("button_recents").setOnPreferenceClickListener(this);

        if(U.hasSupportLibrary(this))
            findPreference("auto_hide_navbar").setOnPreferenceClickListener(this);
        else
            getPreferenceScreen().removePreference(findPreference("auto_hide_navbar_category"));
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch(preference.getKey()) {
            case "auto_hide_navbar":
                if(U.isServiceRunning(this, TaskbarService.class))
                    U.showHideNavigationBar(this, !((CheckBoxPreference) preference).isChecked());

                break;
            default:
                U.restartTaskbar(this);
                break;
        }
        return true;
    }
}