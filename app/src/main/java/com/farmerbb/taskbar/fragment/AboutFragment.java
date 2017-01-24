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

import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.U;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class AboutFragment extends SettingsFragment implements Preference.OnPreferenceClickListener {

    private int noThanksCount = 0;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onActivityCreated(savedInstanceState);

        if(findPreference("dummy") == null) {
            // Add preferences
            addPreferencesFromResource(R.xml.pref_base);

            boolean playStoreInstalled = true;
            try {
                getActivity().getPackageManager().getPackageInfo("com.android.vending", 0);
            } catch (PackageManager.NameNotFoundException e) {
                playStoreInstalled = false;
            }

            SharedPreferences pref = U.getSharedPreferences(getActivity());
            if(BuildConfig.APPLICATION_ID.equals(BuildConfig.BASE_APPLICATION_ID)
                    && playStoreInstalled
                    && !pref.getBoolean("hide_donate", false)) {
                addPreferencesFromResource(R.xml.pref_about_donate);
                findPreference("donate").setOnPreferenceClickListener(this);
            } else
                addPreferencesFromResource(R.xml.pref_about);

            // Set OnClickListeners for certain preferences
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                findPreference("pref_screen_freeform").setOnPreferenceClickListener(this);

            findPreference("pref_screen_general").setOnPreferenceClickListener(this);
            findPreference("pref_screen_appearance").setOnPreferenceClickListener(this);
            findPreference("pref_screen_recent_apps").setOnPreferenceClickListener(this);
            findPreference("pref_screen_advanced").setOnPreferenceClickListener(this);
            findPreference("about").setOnPreferenceClickListener(this);
            findPreference("about").setSummary(getString(R.string.pref_about_description, new String(Character.toChars(0x1F601))));
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.app_name);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(false);

        finishedLoadingPrefs = true;
    }

    @Override
    public boolean onPreferenceClick(final Preference p) {
        final SharedPreferences pref = U.getSharedPreferences(getActivity());

        switch(p.getKey()) {
            case "about":
                U.checkForUpdates(getActivity());
                break;
            case "donate":
                NumberFormat format = NumberFormat.getCurrencyInstance();
                format.setCurrency(Currency.getInstance(Locale.US));

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.pref_title_donate)
                        .setMessage(getString(R.string.dialog_donate_message, format.format(1.99)))
                        .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.BASE_APPLICATION_ID + ".paid"));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            try {
                                startActivity(intent);
                            } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
                        })
                        .setNegativeButton(R.string.action_no_thanks, (dialog, which) -> {
                            noThanksCount++;

                            if(noThanksCount == 3) {
                                pref.edit().putBoolean("hide_donate", true).apply();
                                findPreference("donate").setEnabled(false);
                            }
                        });

                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            case "pref_screen_general":
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new GeneralFragment(), "GeneralFragment")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
                break;
            case "pref_screen_appearance":
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new AppearanceFragment(), "AppearanceFragment")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
                break;
            case "pref_screen_recent_apps":
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new RecentAppsFragment(), "RecentAppsFragment")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
                break;
            case "pref_screen_freeform":
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new FreeformModeFragment(), "FreeformModeFragment")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
                break;
            case "pref_screen_advanced":
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new AdvancedFragment(), "AdvancedFragment")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
                break;
        }

        return true;
    }
}