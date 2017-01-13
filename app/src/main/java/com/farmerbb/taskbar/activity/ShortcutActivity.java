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
import android.os.Handler;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.U;

public class ShortcutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent().hasExtra("is_launching_shortcut")) {
            SharedPreferences pref = U.getSharedPreferences(this);
            if(pref.getBoolean("freeform_hack", false) && U.hasFreeformSupport(this)) {
                sendBroadcast(new Intent("com.farmerbb.taskbar.START"));

                new Handler().postDelayed(() -> U.startFreeformHack(ShortcutActivity.this, true, false), 100);
            } else
                U.showToastLong(this, R.string.no_freeform_support);
        } else
            setResult(RESULT_OK, U.getShortcutIntent(this));

        finish();
    }
}