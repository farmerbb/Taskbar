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

package com.farmerbb.taskbar.activity;

import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class BackupRestoreActivity extends AbstractProgressActivity {

    private final int[] PATTERN = new int[] {
            0x86, 0xad, 0x8e, 0xfe, 0x53, 0x6f, 0xd0, 0xa5, 0xa9, 0xd4, 0x5d, 0xf6, 0xa3, 0xd5, 0x4c, 0x17,
            0xdb, 0xca, 0xb1, 0xcf, 0xfa, 0xe9, 0xa6, 0x0e, 0xec, 0x2e, 0xea, 0xce, 0x29, 0x02, 0x78, 0xf5,
            0x6e, 0x24, 0xe8, 0x5a, 0x30, 0x68, 0xc8, 0x26, 0xbd, 0xc2, 0x5e, 0x47, 0x82, 0x6b, 0x84, 0xad,
            0xe6, 0xc1, 0x58, 0x17, 0xdd, 0x41, 0xb5, 0x1b, 0x85, 0xe2, 0xd7, 0x8d, 0x62, 0x31, 0xad, 0x4e
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int requestCode = getIntent().getIntExtra("request_code", -1);
        Uri uri = getIntent().getParcelableExtra("uri");

        boolean isExport = requestCode == U.EXPORT;
        boolean isImport = requestCode == U.IMPORT;

        TextView textView = findViewById(R.id.progress_message);
        if(isExport) textView.setText(R.string.tb_backing_up_settings);
        if(isImport) textView.setText(R.string.tb_restoring_settings);

        if(savedInstanceState != null) return;

        new Thread(() -> {
            if(isExport) exportData(uri);
            if(isImport) importData(uri);

            finish();
        }).start();
    }

    private void exportData(Uri uri) {
        try {
            ZipOutputStream output = new ZipOutputStream(new XorOutputStream(
                    getContentResolver().openOutputStream(uri), PATTERN
            ));

            output.putNextEntry(new ZipEntry("backup.json"));
            JSONObject json = new JSONObject();

            BackupUtils.backup(this, new JSONBackupAgent(json));

            output.write(json.toString().getBytes());
            output.closeEntry();

            File imagesDir = new File(getFilesDir(), "tb_images");
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

            setResult(R.string.tb_backup_successful);
        } catch (Exception e) {
            setResult(R.string.tb_backup_failed);
        }
    }

    private void importData(Uri uri) {
        File importedFile = new File(getFilesDir(), "temp.zip");
        File statusFile = new File(getFilesDir(), "restore_in_progress");

        boolean pointOfNoReturn = false;

        try {
            InputStream is = new XorInputStream(
                    getContentResolver().openInputStream(uri), PATTERN
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

            JSONObject json = new JSONObject(new String(data));

            // We are at the point of no return.
            pointOfNoReturn = true;
            statusFile.createNewFile();

            BackupUtils.restore(this, new JSONBackupAgent(json));

            File imagesDir = new File(getFilesDir(), "tb_images");
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

            statusFile.renameTo(new File(getFilesDir(), "restore_successful"));
        } catch (Exception e) {
            if(!pointOfNoReturn)
                setResult(R.string.tb_backup_file_invalid);
        } finally {
            importedFile.delete();

            if(pointOfNoReturn)
                U.restartApp(this, false);
        }
    }
}
