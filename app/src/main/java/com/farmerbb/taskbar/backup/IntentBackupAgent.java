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

package com.farmerbb.taskbar.backup;

import android.content.Intent;

public class IntentBackupAgent implements BackupAgent {

    private final Intent intent;

    public IntentBackupAgent(Intent intent) {
        this.intent = intent;
    }

    @Override
    public void putString(String key, String value) {
        intent.putExtra(key, value);
    }

    @Override
    public void putStringArray(String key, String[] value) {
        intent.putExtra(key, value);
    }

    @Override
    public void putLongArray(String key, long[] value) {
        intent.putExtra(key, value);
    }

    @Override
    public String getString(String key) {
        return intent.getStringExtra(key);
    }

    @Override
    public String[] getStringArray(String key) {
        return intent.getStringArrayExtra(key);
    }

    @Override
    public long[] getLongArray(String key) {
        return intent.getLongArrayExtra(key);
    }
}