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
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.XmlRes;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.ClearDataActivity;
import com.farmerbb.taskbar.activity.MainActivity;
import com.farmerbb.taskbar.helper.FreeformHackHelper;
import com.farmerbb.taskbar.helper.LauncherHelper;
import com.farmerbb.taskbar.util.U;

import java.util.HashMap;
import java.util.Map;

import static com.farmerbb.taskbar.util.Constants.*;

public abstract class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    boolean finishedLoadingPrefs;
    boolean showReminderToast = false;
    boolean restartNotificationService = false;

    String isModified = "_is_modified";

    protected Map<String, Class<?>> prefsToSanitize = new HashMap<>();

    protected void addPrefsToSanitize() {}

    protected abstract void loadPrefs();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onCreate(savedInstanceState);

        if(U.isLibrary(getActivity()))
            getPreferenceManager().setSharedPreferencesName(BuildConfig.APPLICATION_ID + "_preferences");

        // Set values
        setRetainInstance(true);
        setHasOptionsMenu(true);

        addPrefsToSanitize();

        SharedPreferences pref = U.getSharedPreferences(getActivity());
        for(String key : prefsToSanitize.keySet()) {
            if(pref.contains(key + "_default")) continue;

            Class<?> rClass = prefsToSanitize.get(key);
            if(rClass == R.bool.class)
                pref.edit().putBoolean(key + "_default", U.getBooleanPrefWithDefault(getActivity(), key)).apply();
            else if(rClass == R.integer.class)
                pref.edit().putInt(key + "_default", U.getIntPrefWithDefault(getActivity(), key)).apply();
        }

        loadPrefs();

        for(String key : prefsToSanitize.keySet()) {
            if(!pref.getBoolean(key + "_is_modified", false))
                pref.edit().remove(key).apply();
        }

        finishedLoadingPrefs = true;
    }

    @Override
    public void onStart() {
        super.onStart();

        ((MainActivity) getActivity()).updateHelpButton(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Remove dividers
        View rootView = getView();
        if(rootView != null) {
            ListView list = rootView.findViewById(android.R.id.list);
            if(list != null) list.setDivider(null);
        }
    }

    private final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if(preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
            } else if(!(preference instanceof CheckBoxPreference)) {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }

            if(finishedLoadingPrefs) {
                boolean shouldRestart = true;

                switch(preference.getKey()) {
                    case PREF_THEME:
                        if(U.isLibrary(getActivity())) break;

                        // Restart MainActivity
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("theme_change", true);
                        startActivity(intent);
                        getActivity().overridePendingTransition(0, 0);
                        break;
                    case PREF_CHROME_OS_CONTEXT_MENU_FIX:
                        FreeformHackHelper helper = FreeformHackHelper.getInstance();
                        helper.setFreeformHackActive(false);
                        helper.setInFreeformWorkspace(false);

                        U.sendBroadcast(getActivity(), ACTION_FINISH_FREEFORM_ACTIVITY);

                        SharedPreferences pref = U.getSharedPreferences(getActivity());
                        if(pref.getBoolean(PREF_TASKBAR_ACTIVE, false) && !pref.getBoolean(PREF_IS_HIDDEN, false))
                            U.newHandler().post(() -> U.startFreeformHack(getActivity()));
                        break;
                    case PREF_START_BUTTON_IMAGE:
                        if(stringValue.equals(PREF_START_BUTTON_IMAGE_CUSTOM)) {
                            U.showImageChooser(getActivity());
                        }
                        break;
                    case PREF_DISPLAY_DENSITY:
                        boolean isOnHomeScreen = LauncherHelper.getInstance().isOnSecondaryHomeScreen(getActivity());
                        int displayID = U.getExternalDisplayID(getActivity());

                        try {
                            U.setDensity(displayID, stringValue);

                            SharedPreferences pref2 = U.getSharedPreferences(getActivity());
                            if(pref2.getBoolean(PREF_AUTO_HIDE_NAVBAR_DESKTOP_MODE, false) && isOnHomeScreen)
                                U.showHideNavigationBar(U.getDisplayContext(getActivity()), displayID, false, 250);
                        } catch (Exception e) {
                            U.showToast(getActivity(), R.string.tb_unable_to_apply_density_change);
                        }

                        shouldRestart = false;
                        break;
                    case PREF_HIDE_ICON_LABELS:
                        U.sendBroadcast(getActivity(), ACTION_REFRESH_DESKTOP_ICONS);
                        break;
                    case PREF_DASHBOARD:
                    case PREF_SYS_TRAY:
                        SharedPreferences pref2 = U.getSharedPreferences(getActivity());
                        pref2.edit().putBoolean(preference.getKey() + isModified, true).apply();
                        break;
                }

                if(shouldRestart) U.restartTaskbar(getActivity());
            }

            return true;
        }
    };

    void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        if(!(preference instanceof CheckBoxPreference))
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    U.getSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            // Override default Android "up" behavior to instead mimic the back button
            getActivity().onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        if(restartNotificationService) {
            restartNotificationService = false;

            U.restartNotificationService(getActivity());
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public boolean onPreferenceClick(final Preference p) {
        if(p.getKey().equals(PREF_CLEAR_PINNED_APPS)) {
            Intent clearIntent = U.getThemedIntent(getActivity(), ClearDataActivity.class);
            startActivity(clearIntent);
        }

        return true;
    }

    protected void navigateTo(SettingsFragment fragment) {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment, fragment.getClass().getSimpleName())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    @Override
    public void addPreferencesFromResource(@XmlRes int preferencesResId) {
        if(!(this instanceof AboutFragment)) {
            U.allowReflection();
            try {
                Context context = U.wrapContext(U.getDisplayContext(getActivity()));
                Class.forName("android.preference.PreferenceManager")
                        .getMethod("inflateFromResource", Context.class, int.class, PreferenceScreen.class)
                        .invoke(getPreferenceManager(), context, preferencesResId, getPreferenceScreen());
            } catch (Exception ignored) {}
        }

        super.addPreferencesFromResource(preferencesResId);
    }
}