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
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.util.FreeformHackHelper;
import com.farmerbb.taskbar.util.U;

import java.util.Set;

public class KeyboardShortcutActivity extends Activity {

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Perform different actions depending on how this activity was launched
        switch(getIntent().getAction()) {
            case Intent.ACTION_MAIN:
                Intent selector = getIntent().getSelector();
                Set<String> categories = selector != null ? selector.getCategories() : getIntent().getCategories();

                if(categories.contains(Intent.CATEGORY_APP_MAPS)) {
                    SharedPreferences pref = U.getSharedPreferences(this);
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                            && pref.getBoolean("freeform_hack", false)
                            && isInMultiWindowMode()
                            && !FreeformHackHelper.getInstance().isFreeformHackActive()) {
                        U.startFreeformHack(this, false, false);
                    }

                    if(pref.getBoolean("taskbar_active", false))
                        sendBroadcast(new Intent("com.farmerbb.taskbar.QUIT"));
                    else
                        sendBroadcast(new Intent("com.farmerbb.taskbar.START"));
                } else if(categories.contains(Intent.CATEGORY_APP_CALENDAR))
                    U.lockDevice(this);

                break;
            case Intent.ACTION_ASSIST:
                if(getIntent().hasExtra(Intent.EXTRA_ASSIST_INPUT_HINT_KEYBOARD) && U.isServiceRunning(this, StartMenuService.class)) {
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.TOGGLE_START_MENU"));
                } else {
                    Intent intent = new Intent("com.google.android.googlequicksearchbox.TEXT_ASSIST");
                    if(intent.resolveActivity(getPackageManager()) == null)
                        intent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);

                    if(intent.resolveActivity(getPackageManager()) != null) {
                        SharedPreferences pref = U.getSharedPreferences(this);
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                                && pref.getBoolean("freeform_hack", false)
                                && isInMultiWindowMode()) {
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                            U.launchAppMaximized(getApplicationContext(), intent);
                        } else {
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }
                    }
                }
                break;
        }

        finish();
    }
}
