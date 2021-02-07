package com.farmerbb.taskbar.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.helper.LauncherHelper
import com.farmerbb.taskbar.mockito.BooleanAnswer
import com.farmerbb.taskbar.util.Constants
import com.farmerbb.taskbar.util.U
import org.junit.*
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.rule.PowerMockRule
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.android.controller.ServiceController

@RunWith(RobolectricTestRunner::class)
@PowerMockIgnore("org.mockito.*", "org.robolectric.*", "android.*", "androidx.*")
@PrepareForTest(U::class)
class UIControllerTest {
    @get:Rule
    var rule = PowerMockRule()
    private var controller: ServiceController<TestUIHostService>? = null
    private var hostService: TestUIHostService? = null
    private var uiController: TestUIController? = null
    private var context: Context? = null

    @Before
    fun setUp() {
        controller = Robolectric.buildService(TestUIHostService::class.java)
        hostService = controller!!.create().get()
        uiController = hostService!!.controller
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        setTaskbarActive(false)
    }

    @Test
    fun testInitWithoutProceed() {
        LauncherHelper.getInstance().setOnSecondaryHomeScreen(true, 1)
        val runnable = TestRunnable()
        val context = ApplicationProvider.getApplicationContext<Context>()
        uiController!!.init(context, hostService, runnable)
        val shadowService = Shadows.shadowOf(hostService)
        Assert.assertTrue(shadowService.isStoppedBySelf)
    }

    @Test
    fun testInitWithProceedAndTaskbarActive() {
        setTaskbarActive(true)
        testInitWithProceed()
    }

    @Test
    fun testInitWithProceedAndOnHomeScreen() {
        testInitWithoutProceed()
    }

    private fun testInitWithProceed() {
        LauncherHelper.getInstance().setOnPrimaryHomeScreen(true)
        PowerMockito.spy(U::class.java)
        val canDrawOverlaysAnswer = BooleanAnswer()
        PowerMockito.`when`(U.canDrawOverlays(context)).thenAnswer(canDrawOverlaysAnswer)
        canDrawOverlaysAnswer.answer = true
        val runnable = TestRunnable()
        val context = ApplicationProvider.getApplicationContext<Context>()
        uiController!!.init(context, hostService, runnable)
        Assert.assertTrue(runnable.hasRun)
        runnable.hasRun = false
        canDrawOverlaysAnswer.answer = false
        uiController!!.init(context, hostService, runnable)
        val shadowService = Shadows.shadowOf(hostService)
        Assert.assertTrue(shadowService.isStoppedBySelf)
        Assert.assertFalse(
                U.getSharedPreferences(context).getBoolean(Constants.PREF_TASKBAR_ACTIVE, true)
        )
    }

    private fun setTaskbarActive(active: Boolean) {
        U.getSharedPreferences(context)
                .edit()
                .putBoolean(Constants.PREF_TASKBAR_ACTIVE, active)
                .apply()
    }

    private class TestRunnable : Runnable {
        var hasRun = false
        override fun run() {
            hasRun = true
        }
    }
}