package com.farmerbb.taskbar.ui

import android.content.res.Configuration
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.android.controller.ServiceController

@RunWith(RobolectricTestRunner::class)
class UIHostServiceTest {
    private lateinit var controller: ServiceController<TestUIHostService>
    private lateinit var hostService: TestUIHostService
    private lateinit var uiController: TestUIController

    @Before
    fun setUp() {
        controller = Robolectric.buildService(TestUIHostService::class.java)
        hostService = controller.create().get()
        uiController = hostService.controller
    }

    @After
    fun tearDown() {
        uiController.onCreateHost = null
        uiController.onRecreateHost = null
        uiController.onDestroyHost = null
    }

    @Test
    fun testOnCreate() {
        Assert.assertEquals(uiController.onCreateHost, hostService)
    }

    @Test
    fun testOnConfigurationChanged() {
        Assert.assertNull(uiController.onRecreateHost)
        hostService.onConfigurationChanged(Configuration())
        Assert.assertEquals(hostService, uiController.onRecreateHost)
    }

    @Test
    fun testOnDestroy() {
        Assert.assertNull(uiController.onDestroyHost)
        controller.destroy()
        Assert.assertEquals(hostService, uiController.onDestroyHost)
    }

    @Test
    fun testTerminate() {
        val shadowService = Shadows.shadowOf(hostService)
        Assert.assertFalse(shadowService.isStoppedBySelf)
        hostService.terminate()
        Assert.assertTrue(shadowService.isStoppedBySelf)
    }
}