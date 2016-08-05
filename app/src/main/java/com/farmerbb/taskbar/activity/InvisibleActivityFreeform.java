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
import android.support.v4.content.LocalBroadcastManager;

public class InvisibleActivityFreeform extends Activity {
    @Override
    protected void onResume() {
        super.onResume();

        // Show the taskbar every time this activity is resumed (no other freeform windows are active)
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("com.farmerbb.taskbar.SHOW_TASKBAR"));
    }

    // We don't want this activity to finish under normal circumstances
    @Override
    public void finish() {}
}
