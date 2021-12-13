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

import android.Manifest;
import android.widget.Button;
import android.widget.TextView;

import com.farmerbb.taskbar.R;

public class EnableAdditionalSettingsActivity extends EnableAdditionalSettingsActivityBase {

    @Override
    protected void proceedWithOnCreate() {
        setContentView(R.layout.tb_enable_additional_settings);
        setTitle(R.string.tb_enable_additional_settings);

        TextView adbShellCommand = findViewById(R.id.adb_shell_command);
        adbShellCommand.setText(getString(R.string.tb_adb_shell_command, getPackageName(), Manifest.permission.WRITE_SECURE_SETTINGS));

        Button button = findViewById(R.id.close_button);
        button.setOnClickListener(v -> finish());
    }
}