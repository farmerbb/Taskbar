package com.farmerbb.taskbar.helper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class LauncherHelperTest {
    private LauncherHelper launcherHelper = LauncherHelper.getInstance();

    @Test
    public void testGetInstance() {
        assertNotNull(launcherHelper);
        for (int i = 1; i <= 20; i++) {
            assertEquals(launcherHelper, LauncherHelper.getInstance());
        }
    }

    @Test
    public void testIsHomeScreen() {
        assertFalse(launcherHelper.isOnHomeScreen());
        launcherHelper.setOnPrimaryHomeScreen(true);
        assertTrue(launcherHelper.isOnHomeScreen());
        launcherHelper.setOnPrimaryHomeScreen(false);
        launcherHelper.setOnSecondaryHomeScreen(true, 1);
        assertTrue(launcherHelper.isOnHomeScreen());
        launcherHelper.setOnSecondaryHomeScreen(false, 1);
        assertEquals(-1, launcherHelper.getSecondaryDisplayId());
    }

    @Test
    public void testIsOnSecondaryHomeScreen() {
        assertFalse(launcherHelper.isOnSecondaryHomeScreen());
        launcherHelper.setOnPrimaryHomeScreen(true);
        assertFalse(launcherHelper.isOnSecondaryHomeScreen());
        launcherHelper.setOnSecondaryHomeScreen(true, 1);
        assertTrue(launcherHelper.isOnSecondaryHomeScreen());
        launcherHelper.setOnPrimaryHomeScreen(false);
        assertTrue(launcherHelper.isOnSecondaryHomeScreen());
        launcherHelper.setOnSecondaryHomeScreen(false, 1);
    }
}
