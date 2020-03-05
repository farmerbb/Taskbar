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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.backup.BackupUtils;
import com.farmerbb.taskbar.backup.JSONBackupAgent;
import com.farmerbb.taskbar.backup.XorInputStream;
import com.farmerbb.taskbar.backup.XorOutputStream;
import com.farmerbb.taskbar.util.U;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ManageAppDataFragment extends SettingsFragment {

    private int EXPORT = 123;
    private int IMPORT = 456;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-kkmmss", Locale.US);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        finishedLoadingPrefs = false;

        super.onCreate(savedInstanceState);

        // Add preferences
        addPreferencesFromResource(R.xml.tb_pref_manage_app_data);

        // Set OnClickListeners for certain preferences
        findPreference("backup_settings").setOnPreferenceClickListener(this);
        findPreference("restore_settings").setOnPreferenceClickListener(this);
        findPreference("clear_pinned_apps").setOnPreferenceClickListener(this);

        finishedLoadingPrefs = true;
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
            case "backup_settings":
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/octet-stream");
                intent.putExtra(Intent.EXTRA_TITLE, "Taskbar-" + dateFormat.format(new Date()) + ".bak");

                try {
                    startActivityForResult(intent, EXPORT);
                } catch (ActivityNotFoundException e) {
                    U.showToastLong(getActivity(), R.string.tb_backup_restore_not_available);
                }
                break;
            case "restore_settings":
                Intent intent2 = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent2.addCategory(Intent.CATEGORY_OPENABLE);
                intent2.setType("application/octet-stream");

                try {
                    startActivityForResult(intent2, IMPORT);
                } catch (ActivityNotFoundException e) {
                    U.showToastLong(getActivity(), R.string.tb_backup_restore_not_available);
                }
                break;
        }

        return super.onPreferenceClick(p);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if(resultCode != Activity.RESULT_OK || resultData == null)
            return;

        if(requestCode == EXPORT)
            exportData(resultData.getData());

        if(requestCode == IMPORT)
            importData(resultData.getData());
    }

    private void exportData(Uri uri) {
        try {
            ZipOutputStream output = new ZipOutputStream(new XorOutputStream(
                    getActivity().getContentResolver().openOutputStream(uri)
            ));

            output.putNextEntry(new ZipEntry("backup.json"));
            JSONObject json = new JSONObject();

            BackupUtils.backup(getActivity(), new JSONBackupAgent(json));

            output.write(json.toString().getBytes());
            output.closeEntry();

            File imagesDir = new File(getActivity().getFilesDir(), "tb_images");
            imagesDir.mkdirs();

            File customImage = new File(imagesDir, "custom_image");
            if(customImage.exists()) {
                output.putNextEntry(new ZipEntry("tb_images/custom_image"));

                BufferedInputStream input = new BufferedInputStream(new FileInputStream(customImage));
                byte[] data = new byte[input.available()];

                if(data.length > 0) {
                    input.read(data);
                    input.close();
                }

                output.write(data);
                output.closeEntry();
            }

            output.close();

            U.showToast(getActivity(), R.string.tb_backup_successful);
        } catch (Exception e) {
            U.showToastLong(getActivity(), R.string.tb_backup_failed);
        }
    }

    private void importData(Uri uri) {
        File importedFile = new File(getActivity().getFilesDir(), "temp.zip");
        File statusFile = new File(getActivity().getFilesDir(), "restore_in_progress");

        try {
            statusFile.createNewFile();

            InputStream is = new XorInputStream(
                    getActivity().getContentResolver().openInputStream(uri)
            );

            byte[] zipData = new byte[is.available()];

            if(zipData.length > 0) {
                OutputStream os = new FileOutputStream(importedFile);
                is.read(zipData);
                os.write(zipData);
                is.close();
                os.close();
            }

            ZipFile zipFile = new ZipFile(importedFile);
            ZipEntry backupJsonEntry = zipFile.getEntry("backup.json");
            ZipEntry customImageEntry = zipFile.getEntry("tb_images/custom_image");

            if(backupJsonEntry == null) {
                // Backup file is invalid; fail immediately
                throw new Exception();
            }

            byte[] data = new byte[(int) backupJsonEntry.getSize()];
            InputStream input = zipFile.getInputStream(backupJsonEntry);
            input.read(data);
            input.close();

            BackupUtils.restore(getActivity(), new JSONBackupAgent(new JSONObject(new String(data))));

            File imagesDir = new File(getActivity().getFilesDir(), "tb_images");
            imagesDir.mkdirs();

            File customImage = new File(imagesDir, "custom_image");
            if(customImage.exists()) customImage.delete();

            if(customImageEntry != null) {
                data = new byte[(int) customImageEntry.getSize()];
                input = zipFile.getInputStream(customImageEntry);
                input.read(data);
                input.close();

                BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(customImage));
                if(data.length > 0) {
                    output.write(data);
                    output.close();
                }
            }

            statusFile.renameTo(new File(getActivity().getFilesDir(), "restore_successful"));
        } catch (Exception e) {
            // no-op
        } finally {
            importedFile.delete();
            U.restartApp(getActivity(), false);
        }
    }
}