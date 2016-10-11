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

public class PinnedBlockedApps implements Serializable {
    static final long serialVersionUID = 6649239269100390617L;

    private List<AppEntry> pinnedApps = new ArrayList<>();
    private List<AppEntry> blockedApps = new ArrayList<>();

    private static PinnedBlockedApps theInstance;

    private PinnedBlockedApps() {}

    public List<AppEntry> getPinnedApps() {
        return pinnedApps;
    }

    public List<AppEntry> getBlockedApps() {
        return blockedApps;
    }

    public void addPinnedApp(Context context, AppEntry entry) {
        pinnedApps.add(entry);
        save(context);
    }

    public void addBlockedApp(Context context, AppEntry entry) {
        blockedApps.add(entry);
        save(context);
    }

    public void removePinnedApp(Context context, String componentName) {
        int number = -1;

        for(int i = 0; i < pinnedApps.size(); i++) {
            if(pinnedApps.get(i).getComponentName().equals(componentName)) {
                number = i;
                break;
            }
        }

        if(number != -1) pinnedApps.remove(number);

        save(context);
    }

    public void removeBlockedApp(Context context, String componentName) {
        int number = -1;

        for(int i = 0; i < blockedApps.size(); i++) {
            if(blockedApps.get(i).getComponentName().equals(componentName)) {
                number = i;
                break;
            }
        }

        if(number != -1) blockedApps.remove(number);

        save(context);
    }

    public boolean isPinned(String componentName) {
        for(int i = 0; i < pinnedApps.size(); i++) {
            if(pinnedApps.get(i).getComponentName().equals(componentName))
                return true;
        }

        return false;
    }

    public boolean isBlocked(String componentName) {
        for(int i = 0; i < blockedApps.size(); i++) {
            if(blockedApps.get(i).getComponentName().equals(componentName))
                return true;
        }

        return false;
    }

    public void clear(Context context) {
        pinnedApps.clear();
        blockedApps.clear();
        save(context);
    }

    private boolean save(Context context) {
        try {
            FileOutputStream fileOutputStream = context.openFileOutput("PinnedBlockedApps", Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

            objectOutputStream.writeObject(this);

            objectOutputStream.close();
            fileOutputStream.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static PinnedBlockedApps getInstance(Context context) {
        if(theInstance == null)
            try {
                FileInputStream fileInputStream = context.openFileInput("PinnedBlockedApps");
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

                theInstance = (PinnedBlockedApps) objectInputStream.readObject();

                objectInputStream.close();
                fileInputStream.close();
            } catch (IOException | ClassNotFoundException e) {
                theInstance = new PinnedBlockedApps();
            }

        return theInstance;
    }
}
