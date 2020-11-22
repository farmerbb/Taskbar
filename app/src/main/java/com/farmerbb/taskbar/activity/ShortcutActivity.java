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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.ShortcutUtils;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class ShortcutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent().hasExtra(EXTRA_IS_LAUNCHING_SHORTCUT)) {
            if(U.hasFreeformSupport(this)) {
                U.restartNotificationService(this);

                SharedPreferences pref = U.getSharedPreferences(this);
                if(!U.isFreeformModeEnabled(this)) {
                    pref.edit().putBoolean(PREF_FREEFORM_HACK, true).apply();

                    U.sendBroadcast(this, ACTION_UPDATE_FREEFORM_CHECKBOX);
                }

                Intent intent = new Intent(ACTION_START);
                intent.setPackage(getPackageName());
                sendBroadcast(intent);

                U.newHandler().postDelayed(() -> U.startFreeformHack(this, true), 100);
            } else
                U.showToastLong(this, R.string.tb_no_freeform_support);
        } else
            setResult(RESULT_OK, ShortcutUtils.getShortcutIntent(this));

        finish();
    }
}