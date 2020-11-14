/* Copyright 2019 Braden Farmer
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

package com.farmerbb.taskbar.util;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class DesktopIconInfo implements Serializable {
    public int column;
    public int row;
    public AppEntry entry;

    static final long serialVersionUID = 1L;

    public DesktopIconInfo(int column, int row, AppEntry entry) {
        this.column = column;
        this.row = row;
        this.entry = entry;
    }

    public JSONObject toJson(Context context) {
        JSONObject json = new JSONObject();

        try {
            json.put("column", column);
            json.put("row", row);
            json.put("package_name", entry.getPackageName());
            json.put("app_name", entry.getLabel());
            json.put("component_name", entry.getComponentName());
            json.put("user_id", entry.getUserId(context));
        } catch (JSONException ignored) {}

        return json;
    }

    public static DesktopIconInfo fromJson(JSONObject json) {
        try {
            int column = json.getInt("column");
            int row = json.getInt("row");
            String packageName = json.getString("package_name");
            String label = json.getString("app_name");
            String componentName = json.getString("component_name");
            long userId = json.getLong("user_id");

            AppEntry entry = new AppEntry(packageName, componentName, label, null, false);
            entry.setUserId(userId);

            return new DesktopIconInfo(column, row, entry);
        } catch (JSONException e) {
            return null;
        }
    }
}
