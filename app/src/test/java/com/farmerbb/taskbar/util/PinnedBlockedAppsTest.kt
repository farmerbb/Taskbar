package com.farmerbb.taskbar.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.apache.commons.lang3.SerializationUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PinnedBlockedAppsTest {
    private lateinit var context: Context
    private lateinit var packageName: String
    private lateinit var pinnedBlockedApps: PinnedBlockedApps
    private lateinit var appEntry: AppEntry

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        pinnedBlockedApps = PinnedBlockedApps.getInstance(context)
        packageName = context.packageName
        appEntry = AppEntry(packageName, packageName, packageName, null, false)
    }

    @After
    fun tearDown() {
        pinnedBlockedApps.blockedApps.clear()
        pinnedBlockedApps.pinnedApps.clear()
    }

    @Test
    fun testGetInstance() {
        Assert.assertNotNull(pinnedBlockedApps)
        for (i in 1..20) {
            Assert.assertEquals(pinnedBlockedApps, PinnedBlockedApps.getInstance(context))
        }
    }

    @Test
    fun testAddPinnedApp() {
        var pinnedApps = pinnedBlockedApps.pinnedApps
        Assert.assertEquals(0, pinnedApps.size.toLong())
        pinnedBlockedApps.addPinnedApp(context, appEntry)
        pinnedApps = pinnedBlockedApps.pinnedApps
        Assert.assertEquals(1, pinnedApps.size.toLong())
        Assert.assertEquals(appEntry, pinnedApps[0])
    }

    @Test
    fun testRemovePinnedApp() {
        pinnedBlockedApps.addPinnedApp(context, appEntry)
        pinnedBlockedApps.removePinnedApp(context, packageName)
        Assert.assertEquals(0, pinnedBlockedApps.pinnedApps.size.toLong())
    }

    @Test
    fun testAddBlockedApp() {
        var blockedApps = pinnedBlockedApps.blockedApps
        Assert.assertEquals(0, blockedApps.size.toLong())
        pinnedBlockedApps.addBlockedApp(context, appEntry)
        blockedApps = pinnedBlockedApps.blockedApps
        Assert.assertEquals(1, blockedApps.size.toLong())
        Assert.assertEquals(appEntry, blockedApps[0])
    }

    @Test
    fun testRemoveBlockedApp() {
        pinnedBlockedApps.addBlockedApp(context, appEntry)
        pinnedBlockedApps.removeBlockedApp(context, packageName)
        Assert.assertEquals(0, pinnedBlockedApps.blockedApps.size.toLong())
    }

    @Test
    fun testIsPinned() {
        Assert.assertFalse(pinnedBlockedApps.isPinned(packageName))
        pinnedBlockedApps.addPinnedApp(context, appEntry)
        Assert.assertTrue(pinnedBlockedApps.isPinned(packageName))
    }

    @Test
    fun testIsBlocked() {
        Assert.assertFalse(pinnedBlockedApps.isBlocked(packageName))
        pinnedBlockedApps.addBlockedApp(context, appEntry)
        Assert.assertTrue(pinnedBlockedApps.isBlocked(packageName))
    }

    @Test
    fun testClear() {
        pinnedBlockedApps.addPinnedApp(context, appEntry)
        pinnedBlockedApps.addBlockedApp(context, appEntry)
        pinnedBlockedApps.clear(context)
        Assert.assertEquals(0, pinnedBlockedApps.pinnedApps.size.toLong())
        Assert.assertEquals(0, pinnedBlockedApps.blockedApps.size.toLong())
    }

    @Test
    fun testSerializable() {
        pinnedBlockedApps.addPinnedApp(context, appEntry)
        pinnedBlockedApps.addBlockedApp(context, appEntry)
        val newPinnedBlockedApps =
                SerializationUtils.deserialize<PinnedBlockedApps>(
                        SerializationUtils.serialize(pinnedBlockedApps))
        Assert.assertTrue(newPinnedBlockedApps.isPinned(packageName))
        Assert.assertTrue(newPinnedBlockedApps.isBlocked(packageName))
    }
}
