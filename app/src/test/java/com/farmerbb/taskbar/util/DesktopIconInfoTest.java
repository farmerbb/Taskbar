package com.farmerbb.taskbar.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherApps;
import android.graphics.drawable.Drawable;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.MainActivity;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class DesktopIconInfoTest {
    private Context context;
    private AppEntry appEntry;
    private DesktopIconInfo desktopIconInfo;
    private int defaultColumn = 3;
    private int defaultRow = 3;
    private String packageName;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        packageName = context.getPackageName();
        Drawable icon = context.getResources().getDrawable(R.drawable.tb_apps);
        ComponentName componentName = new ComponentName(context, MainActivity.class);
        appEntry =
                new AppEntry(
                        packageName,
                        componentName.flattenToString(),
                        packageName,
                        icon,
                        false
                );
        desktopIconInfo = new DesktopIconInfo(defaultColumn, defaultRow, appEntry);
    }

    @Test
    public void testFromJson() {
        DesktopIconInfo newDesktopIconInfo =
                DesktopIconInfo.fromJson(desktopIconInfo.toJson(context));
        testNewDesktopIconInfo(newDesktopIconInfo);
    }

    @Test
    public void testSerializable() {
        DesktopIconInfo newDesktopIconInfo =
                SerializationUtils.deserialize(SerializationUtils.serialize(desktopIconInfo));
        testNewDesktopIconInfo(newDesktopIconInfo);
    }

    private void testNewDesktopIconInfo(DesktopIconInfo newDesktopIconInfo) {
        assertNotNull(newDesktopIconInfo);
        assertEquals(desktopIconInfo.column, newDesktopIconInfo.column);
        assertEquals(desktopIconInfo.row, newDesktopIconInfo.row);
        assertEquals(
                desktopIconInfo.entry.getComponentName(),
                newDesktopIconInfo.entry.getComponentName()
        );
        assertEquals(
                desktopIconInfo.entry.getPackageName(),
                newDesktopIconInfo.entry.getPackageName()
        );
        assertEquals(
                desktopIconInfo.entry.getLabel(),
                newDesktopIconInfo.entry.getLabel()
        );
        assertNotNull(ReflectionHelpers.getField(desktopIconInfo.entry, "icon"));
        assertNull(ReflectionHelpers.getField(newDesktopIconInfo.entry, "icon"));
    }
}