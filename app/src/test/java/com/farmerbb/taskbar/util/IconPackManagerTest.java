package com.farmerbb.taskbar.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

@RunWith(RobolectricTestRunner.class)
public class IconPackManagerTest {
    private IconPackManager iconPackManager = IconPackManager.getInstance();
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testGetInstance() {
        assertNotNull(iconPackManager);
        for (int i = 1; i <= 20; i++) {
            assertEquals(iconPackManager, IconPackManager.getInstance());
        }
    }

    @Test
    public void testGetAvailableIconPacks() throws PackageManager.NameNotFoundException {
        List<IconPack> iconPacks = iconPackManager.getAvailableIconPacks(context);
        assertEquals(0, iconPacks.size());

        String testPackageName = "com.test.package";
        PackageManager packageManager = context.getPackageManager();
        ShadowPackageManager shadowPackageManager = Shadows.shadowOf(packageManager);
        int testIconPackSize = 20;
        for (int i = 1; i <= testIconPackSize; i++) {
            ComponentName componentName =
                    new ComponentName(testPackageName + i, testPackageName + i);
            IntentFilter intentFilter = new IntentFilter("org.adw.launcher.THEMES");
            shadowPackageManager.addActivityIfNotPresent(componentName);
            shadowPackageManager.addIntentFilterForActivity(componentName, intentFilter);
            iconPacks = iconPackManager.getAvailableIconPacks(context);
        }
        assertEquals(testIconPackSize, iconPacks.size());
        Set<String> fetchedIconPackPackages = new HashSet<>();
        iconPacks.forEach(iconPack -> fetchedIconPackPackages.add(iconPack.getPackageName()));
        assertEquals(testIconPackSize, fetchedIconPackPackages.size());
        Set<String> expectedIconPackPackages = new HashSet<>();
        for (int i = 1; i <= testIconPackSize; i++) {
            expectedIconPackPackages.add(testPackageName + i);
            shadowPackageManager.removeActivity(
                    new ComponentName(testPackageName + i, testPackageName + i)
            );
        }
        assertEquals(expectedIconPackPackages, fetchedIconPackPackages);
    }

    @Test
    public void testGetIconPack() {
        IconPack iconPack = iconPackManager.getIconPack(context.getPackageName());
        assertNotNull(iconPack);
        assertEquals(context.getPackageName(), iconPack.getPackageName());
        assertSame(iconPack, iconPackManager.getIconPack(context.getPackageName()));
        iconPackManager.nullify();
        IconPack newIconPack = iconPackManager.getIconPack(context.getPackageName());
        assertEquals(context.getPackageName(), newIconPack.getPackageName());
        assertNotSame(iconPack, newIconPack);
    }
}
