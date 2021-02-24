package com.farmerbb.taskbar.helper

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DashboardHelperTest {
    private val helper = DashboardHelper.getInstance()

    @Test
    fun testGetInstance() {
        Assert.assertNotNull(helper)
        for (i in 1..20) {
            Assert.assertEquals(helper, DashboardHelper.getInstance())
        }
    }

    @Test
    fun testSetDashboardOpen() {
        Assert.assertFalse(helper.isDashboardOpen)
        helper.isDashboardOpen = true
        Assert.assertTrue(helper.isDashboardOpen)
        helper.isDashboardOpen = false
    }
}
