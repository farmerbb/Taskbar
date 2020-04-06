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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.MainActivity;
import com.farmerbb.taskbar.util.U;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Locale;
import java.util.TimeZone;

public class AboutFragment extends SettingsFragment {

    private int noThanksCount = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onCreate(savedInstanceState);

        // Add preferences
        addPreferencesFromResource(R.xml.tb_pref_base);
        addPreferencesFromResource(R.xml.tb_pref_about);

        boolean isLibrary = U.isLibrary(getActivity());
        if(!isLibrary) {
            SharedPreferences pref = U.getSharedPreferences(getActivity());
            if(getActivity().getPackageName().equals(BuildConfig.BASE_APPLICATION_ID)
                    && U.isPlayStoreInstalled(getActivity())
                    && U.isPlayStoreRelease(getActivity())
                    && !U.isSystemApp(getActivity())
                    && !pref.getBoolean("hide_donate", false)) {
                findPreference("donate").setOnPreferenceClickListener(this);
            } else
                getPreferenceScreen().removePreference(findPreference("donate_category"));
        }

        // Set OnClickListeners for certain preferences
        if(U.canEnableFreeform())
            findPreference("pref_screen_freeform").setOnPreferenceClickListener(this);
        else
            getPreferenceScreen().removePreference(findPreference("pref_screen_freeform"));

        if(U.isDesktopModeSupported(getActivity())) {
            findPreference("pref_screen_desktop_mode").setOnPreferenceClickListener(this);
            findPreference("pref_screen_desktop_mode").setIcon(getDesktopModeDrawable());
        } else
            getPreferenceScreen().removePreference(findPreference("pref_screen_desktop_mode"));

        findPreference("pref_screen_general").setOnPreferenceClickListener(this);
        findPreference("pref_screen_appearance").setOnPreferenceClickListener(this);
        findPreference("pref_screen_recent_apps").setOnPreferenceClickListener(this);
        findPreference("pref_screen_advanced").setOnPreferenceClickListener(this);

        if(!isLibrary) {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Denver"));
            calendar.setTimeInMillis(BuildConfig.TIMESTAMP);

            int year = calendar.get(Calendar.YEAR);
            if(BuildConfig.DEBUG || getActivity().getPackageName().equals(BuildConfig.ANDROIDX86_APPLICATION_ID))
                findPreference("about").setSummary(getString(R.string.tb_pref_about_description_alt, year));
            else {
                String emoji = new String(Character.toChars(0x1F601));

                findPreference("about").setSummary(getString(R.string.tb_pref_about_description, year, emoji));
                findPreference("about").setOnPreferenceClickListener(this);
            }
        }

        finishedLoadingPrefs = true;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(((MainActivity) getActivity()).getAboutFragmentTitle());
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(((MainActivity) getActivity()).getAboutFragmentBackArrow());
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
                builder.setTitle(R.string.tb_pref_title_donate)
                        .setMessage(getString(R.string.tb_dialog_donate_message, format.format(1.99)))
                        .setPositiveButton(R.string.tb_action_ok, (dialog, which) -> {
                            Intent intent2 = new Intent(Intent.ACTION_VIEW);
                            intent2.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.PAID_APPLICATION_ID));
                            intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            try {
                                startActivity(intent2);
                            } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
                        })
                        .setNegativeButton(noThanksCount == 2 ? R.string.tb_action_dont_show_again : R.string.tb_action_no_thanks, (dialog, which) -> {
                            noThanksCount++;

                            if(noThanksCount == 3) {
                                pref.edit().putBoolean("hide_donate", true).apply();
                                getPreferenceScreen().removePreference(findPreference("donate_category"));
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
            case "pref_screen_desktop_mode":
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new DesktopModeFragment(), "DesktopModeFragment")
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

        return super.onPreferenceClick(p);
    }

    private Drawable getDesktopModeDrawable() {
        Context context = getActivity().getApplicationContext();
        Drawable loadedIcon = ContextCompat.getDrawable(context, R.drawable.tb_desktop_mode);
        int width = Math.max(1, loadedIcon.getIntrinsicWidth());
        int height = Math.max(1, loadedIcon.getIntrinsicHeight());

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        loadedIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        loadedIcon.draw(canvas);

        int iconSize = context.getResources().getDimensionPixelSize(R.dimen.tb_settings_icon_size);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, iconSize, iconSize, true);

        return new BitmapDrawable(getResources(), resizedBitmap);
    }
}