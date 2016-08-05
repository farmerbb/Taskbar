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

package com.farmerbb.taskbar.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

public class AppEntry implements Serializable {
    private String packageName;
    private String componentName;
    private String label;
    private transient Bitmap icon;
    private byte[] iconByteArray;

    public AppEntry(String packageName, String componentName, String label, Bitmap icon, boolean shouldCompress) {
        this.packageName = packageName;
        this.componentName = componentName;
        this.label = label;
        this.icon = icon;

        if(shouldCompress) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            icon.compress(Bitmap.CompressFormat.PNG, 0, stream);
            iconByteArray = stream.toByteArray();
        }
    }

    public String getPackageName() {
        return packageName;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getLabel() {
        return label;
    }

    public Bitmap getIcon() {
        if(icon == null) icon = BitmapFactory.decodeByteArray(iconByteArray, 0, iconByteArray.length);
        return icon;
    }
}