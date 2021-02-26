package com.farmerbb.taskbar.helper

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MenuHelperTest {
    private val menuHelper = MenuHelper.getInstance()

    @Test
    fun testGetInstance() {
        Assert.assertNotNull(menuHelper)
        for (i in 1..20) {
            Assert.assertEquals(menuHelper, MenuHelper.getInstance())
        }
    }

    @Test
    fun testSetStartMenuOpen() {
        Assert.assertFalse(menuHelper.isStartMenuOpen)
        menuHelper.isStartMenuOpen = true
        Assert.assertTrue(menuHelper.isStartMenuOpen)
        menuHelper.isStartMenuOpen = false
    }

    @Test
    fun testSetContextMenuOpen() {
        Assert.assertFalse(menuHelper.isContextMenuOpen)
        menuHelper.isContextMenuOpen = true
        Assert.assertTrue(menuHelper.isContextMenuOpen)
        menuHelper.isContextMenuOpen = false
    }
}
