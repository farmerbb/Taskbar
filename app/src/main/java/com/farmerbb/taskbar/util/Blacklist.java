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

public class Blacklist implements Serializable {
    static final long serialVersionUID = 9033765395690141037L;

    private List<BlacklistEntry> blockedApps = new ArrayList<>();

    private static Blacklist theInstance;

    private Blacklist() {}

    public List<BlacklistEntry> getBlockedApps() {
        return blockedApps;
    }

    public void addBlockedApp(Context context, BlacklistEntry entry) {
        blockedApps.add(entry);
        save(context);
    }

    public void removeBlockedApp(Context context, String packageName) {
        int number = -1;

        for(int i = 0; i < blockedApps.size(); i++) {
            if(blockedApps.get(i).getPackageName().equals(packageName)) {
                number = i;
                break;
            }
        }

        if(number != -1) blockedApps.remove(number);

        save(context);
    }

    public boolean isBlocked(String packageName) {
        for(int i = 0; i < blockedApps.size(); i++) {
            if(blockedApps.get(i).getPackageName().equals(packageName))
                return true;
        }

        return false;
    }

    private boolean save(Context context) {
        try {
            FileOutputStream fileOutputStream = context.openFileOutput("Blacklist", Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

            objectOutputStream.writeObject(this);

            objectOutputStream.close();
            fileOutputStream.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void clear(Context context) {
        blockedApps.clear();
        save(context);
    }

    public static Blacklist getInstance(Context context) {
        if(theInstance == null)
            try {
                FileInputStream fileInputStream = context.openFileInput("Blacklist");
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

                theInstance = (Blacklist) objectInputStream.readObject();

                objectInputStream.close();
                fileInputStream.close();
            } catch (IOException | ClassNotFoundException e) {
                theInstance = new Blacklist();
            }

        return theInstance;
    }
}