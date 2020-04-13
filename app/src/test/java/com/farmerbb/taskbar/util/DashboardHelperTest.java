package com.farmerbb.taskbar.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class DashboardHelperTest {
    private DashboardHelper helper = DashboardHelper.getInstance();

    @Test
    public void testGetInstance() {
        assertNotNull(helper);
        for (int i = 1; i <= 20; i++) {
            assertEquals(helper, DashboardHelper.getInstance());
        }
    }

    @Test
    public void testSetDashboardOpen() {
        assertFalse(helper.isDashboardOpen());
        helper.setDashboardOpen(true);
        assertTrue(helper.isDashboardOpen());
        helper.setDashboardOpen(false);
    }
}
