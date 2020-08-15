package com.farmerbb.taskbar.shadow;

import android.appwidget.AppWidgetProviderInfo;
import android.content.pm.PackageManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(AppWidgetProviderInfo.class)
public class TaskbarShadowAppWidgetProviderInfo {
    public String label = "";

    @Implementation
    protected final String loadLabel(PackageManager packageManager) {
        return label;
    }
}
