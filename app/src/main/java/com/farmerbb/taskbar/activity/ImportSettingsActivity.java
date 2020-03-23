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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.content.TaskbarIntent;
import com.farmerbb.taskbar.util.U;

public class ImportSettingsActivity extends AbstractProgressActivity {

    boolean broadcastSent = false;

    private BroadcastReceiver settingsReceivedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            U.restartApp(ImportSettingsActivity.this, true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(
                        settingsReceivedReceiver,
                        new IntentFilter(TaskbarIntent.ACTION_IMPORT_FINISHED)
                );

        if (!broadcastSent) {
            Intent intent = new Intent(TaskbarIntent.ACTION_RECEIVE_SETTINGS);
            intent.setPackage(BuildConfig.BASE_APPLICATION_ID);
            sendBroadcast(intent);

            broadcastSent = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(settingsReceivedReceiver);
    }
}
