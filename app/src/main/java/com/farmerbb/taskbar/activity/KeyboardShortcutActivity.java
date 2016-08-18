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
import android.app.ActivityManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.util.U;

public class KeyboardShortcutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Perform different actions depending on how this activity was launched
        switch(getIntent().getAction()) {
            case Intent.ACTION_MAIN:
                SharedPreferences pref = U.getSharedPreferences(this);
                if(pref.getBoolean("taskbar_active", false))
                    sendBroadcast(new Intent("com.farmerbb.taskbar.QUIT"));
                else
                    sendBroadcast(new Intent("com.farmerbb.taskbar.START"));
                break;
            case Intent.ACTION_ASSIST:
                if(getIntent().hasExtra(Intent.EXTRA_ASSIST_INPUT_HINT_KEYBOARD) && isServiceRunning()) {
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.TOGGLE_START_MENU"));
                } else {
                    Intent intent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
                    if(intent.resolveActivity(getPackageManager()) != null)
                        startActivity(intent);
                }
                break;
        }

        finish();
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(StartMenuService.class.getName().equals(service.service.getClassName()))
                return true;
        }

        return false;
    }
}
