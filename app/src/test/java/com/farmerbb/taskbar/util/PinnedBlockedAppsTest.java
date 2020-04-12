package com.farmerbb.taskbar.util;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PinnedBlockedAppsTest {
    private Context context;
    private String packageName;
    private PinnedBlockedApps pinnedBlockedApps;
    private AppEntry appEntry;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        pinnedBlockedApps = PinnedBlockedApps.getInstance(context);
        packageName = context.getPackageName();
        appEntry = new AppEntry(packageName, packageName, packageName, null, false);
    }

    @After
    public void tearDown() {
        pinnedBlockedApps.getBlockedApps().clear();
        pinnedBlockedApps.getPinnedApps().clear();
    }

    @Test
    public void testGetInstance() {
        assertNotNull(pinnedBlockedApps);
        for (int i = 1; i <= 20; i++) {
            assertEquals(pinnedBlockedApps, PinnedBlockedApps.getInstance(context));
        }
    }

    @Test
    public void testAddPinnedApp() {
        List<AppEntry> pinnedApps = pinnedBlockedApps.getPinnedApps();
        assertEquals(0, pinnedApps.size());
        pinnedBlockedApps.addPinnedApp(context, appEntry);
        pinnedApps = pinnedBlockedApps.getPinnedApps();
        assertEquals(1, pinnedApps.size());
        assertEquals(appEntry, pinnedApps.get(0));
    }

    @Test
    public void testRemovePinnedApp() {
        pinnedBlockedApps.addPinnedApp(context, appEntry);
        pinnedBlockedApps.removePinnedApp(context, packageName);
        assertEquals(0, pinnedBlockedApps.getPinnedApps().size());
    }

    @Test
    public void testAddBlockedApp() {
        List<AppEntry> blockedApps = pinnedBlockedApps.getBlockedApps();
        assertEquals(0, blockedApps.size());
        pinnedBlockedApps.addBlockedApp(context, appEntry);
        blockedApps = pinnedBlockedApps.getBlockedApps();
        assertEquals(1, blockedApps.size());
        assertEquals(appEntry, blockedApps.get(0));
    }

    @Test
    public void testRemoveBlockedApp() {
        pinnedBlockedApps.addBlockedApp(context, appEntry);
        pinnedBlockedApps.removeBlockedApp(context, packageName);
        assertEquals(0, pinnedBlockedApps.getBlockedApps().size());
    }

    @Test
    public void testIsPinned() {
        assertFalse(pinnedBlockedApps.isPinned(packageName));
        pinnedBlockedApps.addPinnedApp(context, appEntry);
        assertTrue(pinnedBlockedApps.isPinned(packageName));
    }

    @Test
    public void testIsBlocked() {
        assertFalse(pinnedBlockedApps.isBlocked(packageName));
        pinnedBlockedApps.addBlockedApp(context, appEntry);
        assertTrue(pinnedBlockedApps.isBlocked(packageName));
    }

    @Test
    public void testClear() {
        pinnedBlockedApps.addPinnedApp(context, appEntry);
        pinnedBlockedApps.addBlockedApp(context, appEntry);
        pinnedBlockedApps.clear(context);
        assertEquals(0, pinnedBlockedApps.getPinnedApps().size());
        assertEquals(0, pinnedBlockedApps.getBlockedApps().size());
    }

    @Test
    public void testSerializable() {
        pinnedBlockedApps.addPinnedApp(context, appEntry);
        pinnedBlockedApps.addBlockedApp(context, appEntry);
        PinnedBlockedApps newPinnedBlockedApps =
                SerializationUtils.deserialize(SerializationUtils.serialize(pinnedBlockedApps));
        assertTrue(newPinnedBlockedApps.isPinned(packageName));
        assertTrue(newPinnedBlockedApps.isBlocked(packageName));
    }
}
