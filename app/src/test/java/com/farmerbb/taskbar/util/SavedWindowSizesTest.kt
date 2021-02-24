package com.farmerbb.taskbar.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.apache.commons.lang3.SerializationUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SavedWindowSizesTest {
    private lateinit var context: Context
    private lateinit var savedWindowSizes: SavedWindowSizes

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        savedWindowSizes = SavedWindowSizes.getInstance(context)
    }

    @Test
    fun testGetInstance() {
        Assert.assertNotNull(savedWindowSizes)
        for (i in 1..20) {
            Assert.assertEquals(savedWindowSizes, SavedWindowSizes.getInstance(context))
        }
    }

    @Test
    fun testGetWindowSize() {
        val prefs = U.getSharedPreferences(context)
        val defaultWindowSize = prefs.getString(Constants.PREF_WINDOW_SIZE, "standard")
        Assert.assertEquals(
                defaultWindowSize,
                savedWindowSizes.getWindowSize(context, context.packageName)
        )
        val newWindowSize = "$defaultWindowSize-new"
        prefs.edit().putString(Constants.PREF_WINDOW_SIZE, newWindowSize).apply()
        Assert.assertEquals(
                newWindowSize,
                savedWindowSizes.getWindowSize(context, context.packageName)
        )
        prefs.edit().remove(Constants.PREF_WINDOW_SIZE).apply()
        savedWindowSizes.setWindowSize(context, context.packageName, CUSTOM_WINDOW_SIZE)
        Assert.assertEquals(
                CUSTOM_WINDOW_SIZE,
                savedWindowSizes.getWindowSize(context, context.packageName)
        )
        savedWindowSizes.clear(context)
    }

    @Test
    fun testClear() {
        savedWindowSizes.setWindowSize(context, context.packageName, CUSTOM_WINDOW_SIZE)
        savedWindowSizes.clear(context)
        org.junit.Assert.assertNotEquals(
                CUSTOM_WINDOW_SIZE,
                savedWindowSizes.getWindowSize(context, context.packageName)
        )
    }

    @Test
    fun testSerializable() {
        savedWindowSizes.setWindowSize(context, context.packageName, CUSTOM_WINDOW_SIZE)
        val newSavedWindowSizes =
                SerializationUtils.deserialize<SavedWindowSizes>(
                        SerializationUtils.serialize(savedWindowSizes))
        Assert.assertEquals(
                CUSTOM_WINDOW_SIZE,
                newSavedWindowSizes.getWindowSize(context, context.packageName)
        )
        savedWindowSizes.clear(context)
    }

    companion object {
        private const val CUSTOM_WINDOW_SIZE = "custom-window-size"
    }
}
