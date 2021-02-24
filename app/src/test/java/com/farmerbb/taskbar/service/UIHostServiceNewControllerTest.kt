package com.farmerbb.taskbar.service

import com.farmerbb.taskbar.ui.DashboardController
import com.farmerbb.taskbar.ui.StartMenuController
import com.farmerbb.taskbar.ui.TaskbarController
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UIHostServiceNewControllerTest {
    @Test
    fun testDashboardService() {
        val dashboardService = Robolectric.setupService(DashboardService::class.java)
        Assert.assertTrue(dashboardService.newController() is DashboardController)
    }

    @Test
    fun testStartMenuService() {
        val startMenuService = Robolectric.setupService(StartMenuService::class.java)
        Assert.assertTrue(startMenuService.newController() is StartMenuController)
    }

    @Test
    fun testTaskbarService() {
        val taskbarService = Robolectric.setupService(TaskbarService::class.java)
        Assert.assertTrue(taskbarService.newController() is TaskbarController)
    }
}
