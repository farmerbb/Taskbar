package com.farmerbb.taskbar.util

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.activity.MainActivity
import org.apache.commons.lang3.SerializationUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class DesktopIconInfoTest {
    private lateinit var context: Context
    private lateinit var appEntry: AppEntry
    private lateinit var desktopIconInfo: DesktopIconInfo
    private val defaultColumn = 3
    private val defaultRow = 3
    private lateinit var packageName: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        packageName = context.packageName
        val icon = context.resources.getDrawable(R.drawable.tb_apps)
        val componentName = ComponentName(context, MainActivity::class.java)
        appEntry = AppEntry(
                packageName,
                componentName.flattenToString(),
                packageName,
                icon,
                false
        )
        desktopIconInfo = DesktopIconInfo(defaultColumn, defaultRow, appEntry)
    }

    @Test
    fun testFromJson() {
        val newDesktopIconInfo = DesktopIconInfo.fromJson(desktopIconInfo.toJson(context))
        testNewDesktopIconInfo(newDesktopIconInfo)
    }

    @Test
    fun testSerializable() {
        val newDesktopIconInfo = SerializationUtils.deserialize<DesktopIconInfo>(
                SerializationUtils.serialize(desktopIconInfo))
        testNewDesktopIconInfo(newDesktopIconInfo)
    }

    private fun testNewDesktopIconInfo(newDesktopIconInfo: DesktopIconInfo) {
        Assert.assertNotNull(newDesktopIconInfo)
        Assert.assertEquals(desktopIconInfo.column.toLong(), newDesktopIconInfo.column.toLong())
        Assert.assertEquals(desktopIconInfo.row.toLong(), newDesktopIconInfo.row.toLong())
        Assert.assertEquals(
                desktopIconInfo.entry.componentName,
                newDesktopIconInfo.entry.componentName
        )
        Assert.assertEquals(
                desktopIconInfo.entry.packageName,
                newDesktopIconInfo.entry.packageName
        )
        Assert.assertEquals(
                desktopIconInfo.entry.label,
                newDesktopIconInfo.entry.label
        )
        Assert.assertNotNull(ReflectionHelpers.getField(desktopIconInfo.entry, "icon"))
        Assert.assertNull(ReflectionHelpers.getField(newDesktopIconInfo.entry, "icon"))
    }
}
