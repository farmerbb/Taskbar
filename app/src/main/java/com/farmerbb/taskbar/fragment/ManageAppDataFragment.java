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

package com.farmerbb.taskbar.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.BackupRestoreActivity;
import com.farmerbb.taskbar.util.U;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.farmerbb.taskbar.util.Constants.*;

public class ManageAppDataFragment extends SettingsFragment {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-kkmmss", Locale.US);

    @Override
    protected void loadPrefs() {
        // Add preferences
        addPreferencesFromResource(R.xml.tb_pref_manage_app_data);

        // Set OnClickListeners for certain preferences
        findPreference(PREF_BACKUP_SETTINGS).setOnPreferenceClickListener(this);
        findPreference(PREF_RESTORE_SETTINGS).setOnPreferenceClickListener(this);
        findPreference(PREF_CLEAR_PINNED_APPS).setOnPreferenceClickListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.tb_manage_app_data);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public boolean onPreferenceClick(final Preference p) {
        switch(p.getKey()) {
            case PREF_BACKUP_SETTINGS:
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_TITLE, "Taskbar-" + dateFormat.format(new Date()) + ".bak");

                try {
                    startActivityForResult(intent, U.EXPORT);
                } catch (ActivityNotFoundException e) {
                    U.showToastLong(getActivity(), R.string.tb_backup_restore_not_available);
                }
                break;
            case PREF_RESTORE_SETTINGS:
                Intent intent2 = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent2.addCategory(Intent.CATEGORY_OPENABLE);
                intent2.setType("*/*");

                try {
                    startActivityForResult(intent2, U.IMPORT);
                } catch (ActivityNotFoundException e) {
                    U.showToastLong(getActivity(), R.string.tb_backup_restore_not_available);
                }
                break;
        }

        return super.onPreferenceClick(p);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        int performBackupRestore = 789;
        if(requestCode == performBackupRestore) {
            U.showToastLong(getActivity(), resultCode);
            return;
        }

        if(resultCode != Activity.RESULT_OK || resultData == null) return;

        Intent intent = new Intent(getActivity(), BackupRestoreActivity.class);
        intent.putExtra("request_code", requestCode);
        intent.putExtra("uri", resultData.getData());

        startActivityForResult(intent, performBackupRestore);
    }
}