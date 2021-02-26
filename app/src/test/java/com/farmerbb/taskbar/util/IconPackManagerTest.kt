package com.farmerbb.taskbar.util

import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import java.util.function.Consumer
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class IconPackManagerTest {
    private val iconPackManager = IconPackManager.getInstance()
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testGetInstance() {
        Assert.assertNotNull(iconPackManager)
        for (i in 1..20) {
            Assert.assertEquals(iconPackManager, IconPackManager.getInstance())
        }
    }

    @Test
    @Throws(PackageManager.NameNotFoundException::class)
    fun testGetAvailableIconPacks() {
        var iconPacks = iconPackManager.getAvailableIconPacks(context)
        Assert.assertEquals(0, iconPacks.size.toLong())
        val testPackageName = "com.test.package"
        val packageManager = context.packageManager
        val shadowPackageManager = Shadows.shadowOf(packageManager)
        val testIconPackSize = 20
        for (i in 1..testIconPackSize) {
            val componentName = ComponentName(testPackageName + i, testPackageName + i)
            val intentFilter = IntentFilter("org.adw.launcher.THEMES")
            shadowPackageManager.addActivityIfNotPresent(componentName)
            shadowPackageManager.addIntentFilterForActivity(componentName, intentFilter)
            iconPacks = iconPackManager.getAvailableIconPacks(context)
        }
        Assert.assertEquals(testIconPackSize.toLong(), iconPacks.size.toLong())
        val fetchedIconPackPackages: MutableSet<String> = HashSet()
        iconPacks.forEach(Consumer { iconPack: IconPack ->
            fetchedIconPackPackages.add(iconPack.packageName) })
        Assert.assertEquals(testIconPackSize.toLong(), fetchedIconPackPackages.size.toLong())
        val expectedIconPackPackages: MutableSet<String> = HashSet()
        for (i in 1..testIconPackSize) {
            expectedIconPackPackages.add(testPackageName + i)
            shadowPackageManager.removeActivity(
                    ComponentName(testPackageName + i, testPackageName + i)
            )
        }
        Assert.assertEquals(expectedIconPackPackages, fetchedIconPackPackages)
    }

    @Test
    fun testGetIconPack() {
        val iconPack = iconPackManager.getIconPack(context.packageName)
        Assert.assertNotNull(iconPack)
        Assert.assertEquals(context.packageName, iconPack.packageName)
        Assert.assertSame(iconPack, iconPackManager.getIconPack(context.packageName))
        iconPackManager.nullify()
        val newIconPack = iconPackManager.getIconPack(context.packageName)
        Assert.assertEquals(context.packageName, newIconPack.packageName)
        Assert.assertNotSame(iconPack, newIconPack)
    }
}
