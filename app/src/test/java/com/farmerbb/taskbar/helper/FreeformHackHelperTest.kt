package com.farmerbb.taskbar.helper

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FreeformHackHelperTest {
    private val freeformHackHelper = FreeformHackHelper.getInstance()

    @Test
    fun testGetInstance() {
        Assert.assertNotNull(freeformHackHelper)
        for (i in 1..20) {
            Assert.assertEquals(freeformHackHelper, FreeformHackHelper.getInstance())
        }
    }

    @Test
    fun testSetFreeformHackActive() {
        Assert.assertFalse(freeformHackHelper.isFreeformHackActive)
        freeformHackHelper.isFreeformHackActive = true
        Assert.assertTrue(freeformHackHelper.isFreeformHackActive)
        freeformHackHelper.isFreeformHackActive = false
    }

    @Test
    fun testSetInFreeformWorkspace() {
        Assert.assertFalse(freeformHackHelper.isInFreeformWorkspace)
        freeformHackHelper.isInFreeformWorkspace = true
        Assert.assertTrue(freeformHackHelper.isInFreeformWorkspace)
        freeformHackHelper.isInFreeformWorkspace = false
    }

    @Test
    fun testSetTouchAbsorberActive() {
        Assert.assertFalse(freeformHackHelper.isTouchAbsorberActive)
        freeformHackHelper.isTouchAbsorberActive = true
        Assert.assertTrue(freeformHackHelper.isTouchAbsorberActive)
        freeformHackHelper.isTouchAbsorberActive = false
    }
}
