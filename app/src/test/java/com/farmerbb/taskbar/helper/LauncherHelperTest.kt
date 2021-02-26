package com.farmerbb.taskbar.helper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.mockito.IntAnswer
import com.farmerbb.taskbar.util.U
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.rule.PowerMockRule
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@PowerMockIgnore("org.mockito.*", "org.robolectric.*", "android.*", "androidx.*")
@PrepareForTest(U::class)
class LauncherHelperTest {
    @get:Rule
    val rule = PowerMockRule()
    private lateinit var launcherHelper: LauncherHelper
    private lateinit var context: Context
    private lateinit var getExternalDisplayIdAnswer: IntAnswer

    @Before
    fun setUp() {
        launcherHelper = LauncherHelper.getInstance()
        context = ApplicationProvider.getApplicationContext()
        Assert.assertNotNull(context)
        PowerMockito.spy(U::class.java)
        getExternalDisplayIdAnswer = IntAnswer()
        PowerMockito.`when`(U.getExternalDisplayID(context)).thenAnswer(getExternalDisplayIdAnswer)
    }

    @Test
    fun testGetInstance() {
        Assert.assertNotNull(launcherHelper)
        for (i in 1..20) {
            Assert.assertEquals(launcherHelper, LauncherHelper.getInstance())
        }
    }

    @Test
    fun testIsHomeScreen() {
        Assert.assertFalse(launcherHelper.isOnHomeScreen(context))
        launcherHelper.setOnPrimaryHomeScreen(true)
        Assert.assertTrue(launcherHelper.isOnHomeScreen(context))
        launcherHelper.setOnPrimaryHomeScreen(false)
        launcherHelper.setOnSecondaryHomeScreen(true, 1)
        Assert.assertFalse(launcherHelper.isOnHomeScreen(context))
        getExternalDisplayIdAnswer.answer = 1
        Assert.assertTrue(launcherHelper.isOnHomeScreen(context))
        launcherHelper.setOnSecondaryHomeScreen(false, 1)
        Assert.assertEquals(-1, launcherHelper.secondaryDisplayId.toLong())
    }

    @Test
    fun testIsOnSecondaryHomeScreen() {
        Assert.assertFalse(launcherHelper.isOnSecondaryHomeScreen(context))
        launcherHelper.setOnPrimaryHomeScreen(true)
        Assert.assertFalse(launcherHelper.isOnSecondaryHomeScreen(context))
        launcherHelper.setOnSecondaryHomeScreen(true, 1)
        Assert.assertFalse(launcherHelper.isOnSecondaryHomeScreen(context))
        getExternalDisplayIdAnswer.answer = 1
        Assert.assertTrue(launcherHelper.isOnSecondaryHomeScreen(context))
        launcherHelper.setOnPrimaryHomeScreen(false)
        Assert.assertTrue(launcherHelper.isOnSecondaryHomeScreen(context))
        launcherHelper.setOnSecondaryHomeScreen(false, 1)
    }
}
