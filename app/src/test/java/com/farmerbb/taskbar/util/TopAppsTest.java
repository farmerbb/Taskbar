package com.farmerbb.taskbar.util;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class TopAppsTest {
    private Context context;
    private TopApps topApps;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        topApps = TopApps.getInstance(context);
    }

    @Test
    public void testGetInstance() {
        assertNotNull(topApps);
        for (int i = 1; i <= 20; i++) {
            assertEquals(topApps, TopApps.getInstance(context));
        }
    }

    @Test
    public void testIsTopApp() {
        assertFalse(topApps.isTopApp(context.getPackageName()));
        addCurrentPackageToTopApps();
        assertTrue(topApps.isTopApp(context.getPackageName()));
        topApps.removeTopApp(context, context.getPackageName());
    }

    @Test
    public void testRemoveTopApp() {
        addCurrentPackageToTopApps();
        assertTrue(topApps.isTopApp(context.getPackageName()));
        topApps.removeTopApp(context, context.getPackageName());
        assertFalse(topApps.isTopApp(context.getPackageName()));
    }

    private void addCurrentPackageToTopApps() {
        String packageName = context.getPackageName();
        BlacklistEntry entry = new BlacklistEntry(packageName, packageName);
        topApps.addTopApp(context, entry);
    }
}