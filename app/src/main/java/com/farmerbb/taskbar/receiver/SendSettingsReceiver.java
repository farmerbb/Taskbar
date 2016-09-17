package com.farmerbb.taskbar.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.Blacklist;
import com.farmerbb.taskbar.util.BlacklistEntry;
import com.farmerbb.taskbar.util.PinnedBlockedApps;
import com.farmerbb.taskbar.util.SavedWindowSizes;
import com.farmerbb.taskbar.util.SavedWindowSizesEntry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class SendSettingsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Ignore this broadcast if this is the paid version
        if(BuildConfig.APPLICATION_ID.equals(BuildConfig.BASE_APPLICATION_ID)) {
            Intent sendSettingsIntent = new Intent("com.farmerbb.taskbar.SEND_SETTINGS");

            // Get pinned and blocked apps
            PinnedBlockedApps pba = PinnedBlockedApps.getInstance(context);
            List<AppEntry> pinnedAppsList = pba.getPinnedApps();
            AppEntry[] pinnedAppsArray = new AppEntry[pinnedAppsList.size()];

            for(int i = 0; i < pinnedAppsArray.length; i++) {
                pinnedAppsArray[i] = pinnedAppsList.get(i);
            }

            sendSettingsIntent.putExtra("pinned_apps", pinnedAppsArray);

            List<AppEntry> blockedAppsList = pba.getBlockedApps();
            AppEntry[] blockedAppsArray = new AppEntry[blockedAppsList.size()];

            for(int i = 0; i < blockedAppsArray.length; i++) {
                blockedAppsArray[i] = blockedAppsList.get(i);
            }

            sendSettingsIntent.putExtra("blocked_apps", blockedAppsArray);

            // Get blacklist
            Blacklist blacklist = Blacklist.getInstance(context);
            List<BlacklistEntry> blacklistList = blacklist.getBlockedApps();
            BlacklistEntry[] blacklistArray = new BlacklistEntry[blacklistList.size()];

            for(int i = 0; i < blacklistArray.length; i++) {
                blacklistArray[i] = blacklistList.get(i);
            }

            sendSettingsIntent.putExtra("blacklist", blacklistArray);

            // Get saved window sizes
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                SavedWindowSizes savedWindowSizes = SavedWindowSizes.getInstance(context);
                List<SavedWindowSizesEntry> savedWindowSizesList = savedWindowSizes.getSavedWindowSizes();
                SavedWindowSizesEntry[] savedWindowSizesArray = new SavedWindowSizesEntry[savedWindowSizesList.size()];

                for(int i = 0; i < savedWindowSizesArray.length; i++) {
                    savedWindowSizesArray[i] = savedWindowSizesList.get(i);
                }

                sendSettingsIntent.putExtra("saved_window_sizes", savedWindowSizesArray);
            }

            // Get shared preferences
            StringBuilder preferences = new StringBuilder("");

            try {
                File file = new File(context.getFilesDir().getParent() + "/shared_prefs/" + BuildConfig.APPLICATION_ID + "_preferences.xml");
                FileInputStream input = new FileInputStream(file);
                InputStreamReader reader = new InputStreamReader(input);
                BufferedReader buffer = new BufferedReader(reader);

                String line = buffer.readLine();
                while(line != null) {
                    preferences.append(line);
                    line = buffer.readLine();
                    if(line != null)
                        preferences.append("\n");
                }

                reader.close();
            } catch (IOException e) { /* Gracefully fail */ }

            sendSettingsIntent.putExtra("preferences", preferences.toString());

            // Finally, send the broadcast
            context.sendBroadcast(sendSettingsIntent);
        }
    }
}
