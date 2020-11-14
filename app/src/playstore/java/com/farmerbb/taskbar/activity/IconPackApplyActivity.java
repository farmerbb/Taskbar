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

package com.farmerbb.taskbar.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class IconPackApplyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(U.isExternalAccessDisabled(this))
            finish();
        else if(getIntent().hasExtra(Intent.EXTRA_PACKAGE_NAME)) {
            SharedPreferences pref = U.getSharedPreferences(this);
            if(U.isDarkTheme(this))
                setTheme(R.style.Taskbar_Dialog_Dark);

            @SuppressLint("InlinedApi")
            final String iconPackPackage = getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME);
            PackageManager pm = getPackageManager();

            boolean iconPackValid = true;
            try {
                pm.getPackageInfo(iconPackPackage, 0);
            } catch (PackageManager.NameNotFoundException e) {
                iconPackValid = false;
            }

            if(iconPackValid) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.tb_apply_icon_pack)
                        .setNegativeButton(R.string.tb_action_cancel, (dialog, which) -> finish())
                        .setPositiveButton(R.string.tb_action_ok, (dialog, which) -> {
                            pref.edit().putString(PREF_ICON_PACK, iconPackPackage).apply();

                            U.refreshPinnedIcons(this);
                            U.restartTaskbar(this);

                            finish();
                        });

                try {
                    builder.setMessage(getString(R.string.tb_apply_icon_pack_description,
                            pm.getApplicationLabel(pm.getApplicationInfo(iconPackPackage, 0))));
                } catch (PackageManager.NameNotFoundException ignored) {}

                AlertDialog dialog = builder.create();
                dialog.show();
                dialog.setCancelable(false);
            } else {
                U.showToast(this, R.string.tb_invalid_package_name);
                finish();
            }
        } else {
            U.showToast(this, R.string.tb_must_specify_extra);
            finish();
        }
    }
}
