package com.farmerbb.taskbar.util;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class BlacklistTest {
    private Context context;
    private Blacklist blacklist;
    private BlacklistEntry entry;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        blacklist = Blacklist.getInstance(context);
        entry = new BlacklistEntry(context.getPackageName(), context.getPackageName());
    }

    @After
    public void tearDown() {
        blacklist.clear(context);
    }

    @Test
    public void testGetInstance() {
        assertNotNull(blacklist);
        for (int i = 1; i <= 20; i++) {
            assertEquals(blacklist, Blacklist.getInstance(context));
        }
    }

    @Test
    public void testAddBlockedApp() {
        assertEquals(0, blacklist.getBlockedApps().size());
        blacklist.addBlockedApp(context, entry);
        assertEquals(1, blacklist.getBlockedApps().size());
        assertTrue(blacklist.isBlocked(context.getPackageName()));
    }

    @Test
    public void testRemoveBlockedApp() {
        blacklist.addBlockedApp(context, entry);
        blacklist.removeBlockedApp(context, context.getPackageName());
        assertEquals(0, blacklist.getBlockedApps().size());
    }

    @Test
    public void testClear() {
        blacklist.addBlockedApp(context, entry);
        blacklist.clear(context);
        assertEquals(0, blacklist.getBlockedApps().size());
    }

    @Test
    public void testSerializable() {
        blacklist.addBlockedApp(context, entry);
        Blacklist newBlacklist =
                SerializationUtils.deserialize(SerializationUtils.serialize(blacklist));
        assertTrue(newBlacklist.isBlocked(context.getPackageName()));
    }
}
