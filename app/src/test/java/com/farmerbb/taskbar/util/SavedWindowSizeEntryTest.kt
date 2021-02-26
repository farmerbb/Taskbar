package com.farmerbb.taskbar.util

import org.apache.commons.lang3.SerializationUtils
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SavedWindowSizeEntryTest {
    private val entry = SavedWindowSizesEntry(COMPONENT_NAME, WINDOW_SIZE)

    @Test
    fun testGetComponentName() {
        Assert.assertEquals(COMPONENT_NAME, entry.componentName)
    }

    @Test
    fun testGetWindowSize() {
        Assert.assertEquals(WINDOW_SIZE, entry.windowSize)
    }

    @Test
    fun testSerializable() {
        val newEntry = SerializationUtils.deserialize<SavedWindowSizesEntry>(
                SerializationUtils.serialize(entry))
        Assert.assertEquals(entry.componentName, newEntry.componentName)
        Assert.assertEquals(entry.windowSize, newEntry.windowSize)
    }

    companion object {
        private const val COMPONENT_NAME = "test-component-name"
        private const val WINDOW_SIZE = "test-window-size"
    }
}
