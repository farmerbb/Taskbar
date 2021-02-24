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
class BlacklistTest {
    private lateinit var context: Context
    private lateinit var blacklist: Blacklist
    private lateinit var entry: BlacklistEntry

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        blacklist = Blacklist.getInstance(context)
        entry = BlacklistEntry(context.packageName, context.packageName)
    }

    @After
    fun tearDown() {
        blacklist.clear(context)
    }

    @Test
    fun testGetInstance() {
        Assert.assertNotNull(blacklist)
        for (i in 1..20) {
            Assert.assertEquals(blacklist, Blacklist.getInstance(context))
        }
    }

    @Test
    fun testAddBlockedApp() {
        Assert.assertEquals(0, blacklist.blockedApps.size.toLong())
        blacklist.addBlockedApp(context, entry)
        Assert.assertEquals(1, blacklist.blockedApps.size.toLong())
        Assert.assertTrue(blacklist.isBlocked(context.packageName))
    }

    @Test
    fun testRemoveBlockedApp() {
        blacklist.addBlockedApp(context, entry)
        blacklist.removeBlockedApp(context, context.packageName)
        Assert.assertEquals(0, blacklist.blockedApps.size.toLong())
    }

    @Test
    fun testClear() {
        blacklist.addBlockedApp(context, entry)
        blacklist.clear(context)
        Assert.assertEquals(0, blacklist.blockedApps.size.toLong())
    }

    @Test
    fun testSerializable() {
        blacklist.addBlockedApp(context, entry)
        val newBlacklist = SerializationUtils.deserialize<Blacklist>(
                SerializationUtils.serialize(blacklist))
        Assert.assertTrue(newBlacklist.isBlocked(context.packageName))
    }
}
