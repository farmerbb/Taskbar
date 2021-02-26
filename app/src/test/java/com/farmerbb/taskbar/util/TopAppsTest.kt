package com.farmerbb.taskbar.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TopAppsTest {
    private lateinit var context: Context
    private lateinit var topApps: TopApps

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        topApps = TopApps.getInstance(context)
    }

    @Test
    fun testGetInstance() {
        Assert.assertNotNull(topApps)
        for (i in 1..20) {
            Assert.assertEquals(topApps, TopApps.getInstance(context))
        }
    }

    @Test
    fun testIsTopApp() {
        Assert.assertFalse(topApps.isTopApp(context.packageName))
        addCurrentPackageToTopApps()
        Assert.assertTrue(topApps.isTopApp(context.packageName))
        topApps.removeTopApp(context, context.packageName)
    }

    @Test
    fun testRemoveTopApp() {
        addCurrentPackageToTopApps()
        Assert.assertTrue(topApps.isTopApp(context.packageName))
        topApps.removeTopApp(context, context.packageName)
        Assert.assertFalse(topApps.isTopApp(context.packageName))
    }

    private fun addCurrentPackageToTopApps() {
        val packageName = context.packageName
        val entry = BlacklistEntry(packageName, packageName)
        topApps.addTopApp(context, entry)
    }
}
