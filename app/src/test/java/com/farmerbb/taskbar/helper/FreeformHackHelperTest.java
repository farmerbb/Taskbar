package com.farmerbb.taskbar.helper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class FreeformHackHelperTest {
    private FreeformHackHelper freeformHackHelper = FreeformHackHelper.getInstance();

    @Test
    public void testGetInstance() {
        assertNotNull(freeformHackHelper);
        for (int i = 1; i <= 20; i++) {
            assertEquals(freeformHackHelper, FreeformHackHelper.getInstance());
        }
    }

    @Test
    public void testSetFreeformHackActive() {
        assertFalse(freeformHackHelper.isFreeformHackActive());
        freeformHackHelper.setFreeformHackActive(true);
        assertTrue(freeformHackHelper.isFreeformHackActive());
        freeformHackHelper.setFreeformHackActive(false);
    }

    @Test
    public void testSetInFreeformWorkspace() {
        assertFalse(freeformHackHelper.isInFreeformWorkspace());
        freeformHackHelper.setInFreeformWorkspace(true);
        assertTrue(freeformHackHelper.isInFreeformWorkspace());
        freeformHackHelper.setInFreeformWorkspace(false);
    }

    @Test
    public void testSetTouchAbsorberActive() {
        assertFalse(freeformHackHelper.isTouchAbsorberActive());
        freeformHackHelper.setTouchAbsorberActive(true);
        assertTrue(freeformHackHelper.isTouchAbsorberActive());
        freeformHackHelper.setTouchAbsorberActive(false);
    }
}
