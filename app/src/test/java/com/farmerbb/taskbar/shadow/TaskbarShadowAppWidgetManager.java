package com.farmerbb.taskbar.shadow;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.os.UserHandle;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowAppWidgetManager;

import java.util.ArrayList;
import java.util.List;

@Implements(AppWidgetManager.class)
public class TaskbarShadowAppWidgetManager extends ShadowAppWidgetManager {
    private Multimap<UserHandle, AppWidgetProviderInfo> installedProvidersForProfile =
            HashMultimap.create();

    public void addInstalledProvidersForProfile(UserHandle userHandle,
                                                AppWidgetProviderInfo appWidgetProviderInfo) {
        installedProvidersForProfile.put(userHandle, appWidgetProviderInfo);
    }

    @Implementation
    protected List<AppWidgetProviderInfo> getInstalledProvidersForProfile(UserHandle profile) {
        return new ArrayList<>(installedProvidersForProfile.get(profile));
    }
}
