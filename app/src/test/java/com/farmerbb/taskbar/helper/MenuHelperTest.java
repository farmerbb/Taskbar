package com.farmerbb.taskbar.helper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class MenuHelperTest {
    private MenuHelper menuHelper = MenuHelper.getInstance();

    @Test
    public void testGetInstance() {
        assertNotNull(menuHelper);
        for (int i = 1; i <= 20; i++) {
            assertEquals(menuHelper, MenuHelper.getInstance());
        }
    }

    @Test
    public void testSetStartMenuOpen() {
        assertFalse(menuHelper.isStartMenuOpen());
        menuHelper.setStartMenuOpen(true);
        assertTrue(menuHelper.isStartMenuOpen());
        menuHelper.setStartMenuOpen(false);
    }

    @Test
    public void testSetContextMenuOpen() {
        assertFalse(menuHelper.isContextMenuOpen());
        menuHelper.setContextMenuOpen(true);
        assertTrue(menuHelper.isContextMenuOpen());
        menuHelper.setContextMenuOpen(false);
    }
}
