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

package com.farmerbb.taskbar.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.Process;
import android.os.UserManager;
import android.support.v4.content.LocalBroadcastManager;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.Blacklist;
import com.farmerbb.taskbar.util.BlacklistEntry;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.PinnedBlockedApps;
import com.farmerbb.taskbar.util.SavedWindowSizes;
import com.farmerbb.taskbar.util.TopApps;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ReceiveSettingsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Ignore this broadcast if this is the free version
        if(!BuildConfig.APPLICATION_ID.equals(BuildConfig.BASE_APPLICATION_ID)) {
            // Get pinned and blocked apps
            PinnedBlockedApps pba = PinnedBlockedApps.getInstance(context);
            pba.clear(context);

            String[] pinnedAppsPackageNames = intent.getStringArrayExtra("pinned_apps_package_names");
            String[] pinnedAppsComponentNames = intent.getStringArrayExtra("pinned_apps_component_names");
            String[] pinnedAppsLabels = intent.getStringArrayExtra("pinned_apps_labels");
            long[] pinnedAppsUserIds = intent.getLongArrayExtra("pinned_apps_user_ids");

            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

            if(pinnedAppsPackageNames != null && pinnedAppsComponentNames != null && pinnedAppsLabels != null)
                for(int i = 0; i < pinnedAppsPackageNames.length; i++) {
                    Intent throwaway = new Intent();
                    throwaway.setComponent(ComponentName.unflattenFromString(pinnedAppsComponentNames[i]));

                    long userId;
                    if(pinnedAppsUserIds != null)
                        userId = pinnedAppsUserIds[i];
                    else
                        userId = userManager.getSerialNumberForUser(Process.myUserHandle());

                    AppEntry newEntry = new AppEntry(
                            pinnedAppsPackageNames[i],
                            pinnedAppsComponentNames[i],
                            pinnedAppsLabels[i],
                            IconCache.getInstance(context).getIcon(
                                    context,
                                    context.getPackageManager(),
                                    launcherApps.resolveActivity(throwaway, userManager.getUserForSerialNumber(userId))),
                            true
                    );

                    newEntry.setUserId(userId);
                    pba.addPinnedApp(context, newEntry);
                }

            String[] blockedAppsPackageNames = intent.getStringArrayExtra("blocked_apps_package_names");
            String[] blockedAppsComponentNames = intent.getStringArrayExtra("blocked_apps_component_names");
            String[] blockedAppsLabels = intent.getStringArrayExtra("blocked_apps_labels");

            if(blockedAppsPackageNames != null && blockedAppsComponentNames != null && blockedAppsLabels != null)
                for(int i = 0; i < blockedAppsPackageNames.length; i++) {
                    pba.addBlockedApp(context, new AppEntry(
                            blockedAppsPackageNames[i],
                            blockedAppsComponentNames[i],
                            blockedAppsLabels[i],
                            null,
                            false
                    ));
                }

            // Get blacklist
            Blacklist blacklist = Blacklist.getInstance(context);
            blacklist.clear(context);

            String[] blacklistPackageNames = intent.getStringArrayExtra("blacklist_package_names");
            String[] blacklistLabels = intent.getStringArrayExtra("blacklist_labels");

            if(blacklistPackageNames != null && blacklistLabels != null)
                for(int i = 0; i < blacklistPackageNames.length; i++) {
                    blacklist.addBlockedApp(context, new BlacklistEntry(
                            blacklistPackageNames[i],
                            blacklistLabels[i]
                    ));
                }

            // Get top apps
            TopApps topApps = TopApps.getInstance(context);
            topApps.clear(context);

            String[] topAppsPackageNames = intent.getStringArrayExtra("top_apps_package_names");
            String[] topAppsLabels = intent.getStringArrayExtra("top_apps_labels");

            if(topAppsPackageNames != null && topAppsLabels != null)
                for(int i = 0; i < topAppsPackageNames.length; i++) {
                    topApps.addTopApp(context, new BlacklistEntry(
                            topAppsPackageNames[i],
                            topAppsLabels[i]
                    ));
                }

            // Get saved window sizes
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                SavedWindowSizes savedWindowSizes = SavedWindowSizes.getInstance(context);
                savedWindowSizes.clear(context);

                String[] savedWindowSizesComponentNames = intent.getStringArrayExtra("saved_window_sizes_component_names");
                String[] savedWindowSizesWindowSizes = intent.getStringArrayExtra("saved_window_sizes_window_sizes");

                if(savedWindowSizesComponentNames != null && savedWindowSizesWindowSizes != null)
                    for(int i = 0; i < savedWindowSizesComponentNames.length; i++) {
                        savedWindowSizes.setWindowSize(context,
                                savedWindowSizesComponentNames[i],
                                savedWindowSizesWindowSizes[i]
                        );
                    }
            }

            // Get shared preferences
            String contents = intent.getStringExtra("preferences");
            if(contents.length() > 0)
                try {
                    File file = new File(context.getFilesDir().getParent() + "/shared_prefs/" + BuildConfig.APPLICATION_ID + "_preferences.xml");
                    FileOutputStream output = new FileOutputStream(file);
                    output.write(contents.getBytes());
                    output.close();
                } catch (IOException e) { /* Gracefully fail */ }

            try {
                File file = new File(context.getFilesDir() + File.separator + "imported_successfully");
                if(file.createNewFile())
                    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.farmerbb.taskbar.IMPORT_FINISHED"));
            } catch (IOException e) { /* Gracefully fail */ }
        }
    }
}
