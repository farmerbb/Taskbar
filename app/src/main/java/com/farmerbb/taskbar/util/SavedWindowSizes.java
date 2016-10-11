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

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SavedWindowSizes implements Serializable {
    static final long serialVersionUID = 7111185146180868281L;

    private List<SavedWindowSizesEntry> savedApps = new ArrayList<>();

    private static SavedWindowSizes theInstance;

    private SavedWindowSizes() {}

    public void setWindowSize(Context context, String packageName, String windowSize) {
        int number = -1;

        for(int i = 0; i < savedApps.size(); i++) {
            if(savedApps.get(i).getComponentName().equals(packageName)) {
                number = i;
                break;
            }
        }

        if(number != -1) savedApps.remove(number);

        savedApps.add(new SavedWindowSizesEntry(packageName, windowSize));
        save(context);
    }

    public String getWindowSize(Context context, String packageName) {
        for(SavedWindowSizesEntry entry : savedApps) {
            if(entry.getComponentName().equals(packageName))
                return entry.getWindowSize();
        }

        return U.getSharedPreferences(context).getString("window_size", "standard");
    }

    public void clear(Context context) {
        savedApps.clear();
        save(context);
    }

    private boolean save(Context context) {
        try {
            FileOutputStream fileOutputStream = context.openFileOutput("SavedWindowSizes", Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

            objectOutputStream.writeObject(this);

            objectOutputStream.close();
            fileOutputStream.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static SavedWindowSizes getInstance(Context context) {
        if(theInstance == null)
            try {
                FileInputStream fileInputStream = context.openFileInput("SavedWindowSizes");
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

                theInstance = (SavedWindowSizes) objectInputStream.readObject();

                objectInputStream.close();
                fileInputStream.close();
            } catch (IOException | ClassNotFoundException e) {
                theInstance = new SavedWindowSizes();
            }

        return theInstance;
    }

    public List<SavedWindowSizesEntry> getSavedWindowSizes() {
        return savedApps;
    }
}