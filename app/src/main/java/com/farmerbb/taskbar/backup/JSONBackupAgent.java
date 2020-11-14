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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONBackupAgent implements BackupAgent {

    private final JSONObject json;

    public JSONBackupAgent(JSONObject json) {
        this.json = json;
    }

    @Override
    public void putString(String key, String value) {
        try {
            json.put(key, value);
        } catch (JSONException ignored) {}
    }

    @Override
    public void putStringArray(String key, String[] value) {
        try {
            JSONArray array = new JSONArray();

            for(String v : value) {
                array.put(v);
            }

            json.put(key, array);
        } catch (JSONException ignored) {}
    }

    @Override
    public void putLongArray(String key, long[] value) {
        try {
            JSONArray array = new JSONArray();

            for(long v : value) {
                array.put(v);
            }

            json.put(key, array);
        } catch (JSONException ignored) {}
    }

    @Override
    public String getString(String key) {
        try {
            return json.getString(key);
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public String[] getStringArray(String key) {
        try {
            JSONArray array = json.getJSONArray(key);
            String[] returnValue = new String[array.length()];

            for(int i = 0; i < array.length(); i++) {
                returnValue[i] = array.getString(i);
            }

            return returnValue;
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public long[] getLongArray(String key) {
        try {
            JSONArray array = json.getJSONArray(key);
            long[] returnValue = new long[array.length()];

            for(int i = 0; i < array.length(); i++) {
                returnValue[i] = array.getLong(i);
            }

            return returnValue;
        } catch (JSONException e) {
            return null;
        }
    }
}