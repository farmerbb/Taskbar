package com.farmerbb.taskbar.util

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.activity.MainActivity
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppEntryTest {
    private lateinit var context: Context
    private lateinit var appEntry: AppEntry
    private lateinit var componentName: ComponentName
    private lateinit var icon: Drawable

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        componentName = ComponentName(context, MainActivity::class.java)
        icon = context.resources.getDrawable(R.drawable.tb_apps)
        appEntry = AppEntry(
                context.packageName,
                componentName.flattenToString(),
                context.packageName,
                icon,
                true
        )
    }

    @Test
    fun testGetComponentName() {
        Assert.assertEquals(componentName.flattenToString(), appEntry.componentName)
    }

    @Test
    fun testGetPackageName() {
        Assert.assertEquals(context.packageName, appEntry.packageName)
    }

    @Test
    fun testGetLabel() {
        Assert.assertEquals(context.packageName, appEntry.label)
    }

    @Test
    fun testGetUserId() {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val currentUser = userManager.getSerialNumberForUser(Process.myUserHandle())
        Assert.assertEquals(currentUser, appEntry.getUserId(context))
        appEntry.setUserId(currentUser + 1)
        Assert.assertEquals(currentUser + 1, appEntry.getUserId(context))
        appEntry.setUserId(currentUser)
    }

    @Test
    fun testGetIcon() {
        Assert.assertEquals(icon, appEntry.getIcon(context))
    }

    @Test
    fun testSetLastTimeUsed() {
        Assert.assertEquals(0, appEntry.lastTimeUsed)
        appEntry.lastTimeUsed = 100
        Assert.assertEquals(100, appEntry.lastTimeUsed)
        appEntry.lastTimeUsed = 0
    }

    @Test
    fun testSetTotalTimeInForeground() {
        Assert.assertEquals(0, appEntry.totalTimeInForeground)
        appEntry.totalTimeInForeground = 100
        Assert.assertEquals(100, appEntry.totalTimeInForeground)
        appEntry.totalTimeInForeground = 0
    }
}
