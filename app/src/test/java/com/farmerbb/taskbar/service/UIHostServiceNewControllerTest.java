package com.farmerbb.taskbar.service;

import com.farmerbb.taskbar.ui.DashboardController;
import com.farmerbb.taskbar.ui.StartMenuController;
import com.farmerbb.taskbar.ui.TaskbarController;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class UIHostServiceNewControllerTest {
    @Test
    public void testDashboardService() {
        DashboardService dashboardService = Robolectric.setupService(DashboardService.class);
        assertTrue(dashboardService.newController() instanceof DashboardController);
    }

    @Test
    public void testStartMenuService() {
        StartMenuService startMenuService = Robolectric.setupService(StartMenuService.class);
        assertTrue(startMenuService.newController() instanceof StartMenuController);
    }

    @Test
    public void testTaskbarService() {
        TaskbarService taskbarService = Robolectric.setupService(TaskbarService.class);
        assertTrue(taskbarService.newController() instanceof TaskbarController);
    }
}
