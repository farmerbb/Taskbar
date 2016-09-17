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

            AppEntry[] pinnedAppsArray = (AppEntry[]) intent.getSerializableExtra("pinned_apps");
            if(pinnedAppsArray != null)
                for(AppEntry entry : pinnedAppsArray) {
                    pba.addPinnedApp(context, entry);
                }

            AppEntry[] blockedAppsArray = (AppEntry[]) intent.getSerializableExtra("blocked_apps");
            if(blockedAppsArray != null)
                for(AppEntry entry : blockedAppsArray) {
                    pba.addBlockedApp(context, entry);
                }

            // Get blacklist
            Blacklist blacklist = Blacklist.getInstance(context);
            blacklist.clear(context);

            BlacklistEntry[] blacklistedAppsArray = (BlacklistEntry[]) intent.getSerializableExtra("blacklist");
            if(blacklistedAppsArray != null)
                for(BlacklistEntry entry : blacklistedAppsArray) {
                    blacklist.addBlockedApp(context, entry);
                }

            // Get saved window sizes
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                SavedWindowSizes savedWindowSizes = SavedWindowSizes.getInstance(context);
                savedWindowSizes.clear(context);

                SavedWindowSizesEntry[] savedWindowSizesArray = (SavedWindowSizesEntry[]) intent.getSerializableExtra("saved_window_sizes");
                if(savedWindowSizesArray != null)
                    for(SavedWindowSizesEntry entry : savedWindowSizesArray) {
                        savedWindowSizes.setWindowSize(context, entry.getComponentName(), entry.getWindowSize());
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

            System.out.println("done");
        }
    }
}
