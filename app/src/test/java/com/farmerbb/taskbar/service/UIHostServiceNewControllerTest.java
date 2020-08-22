package com.farmerbb.taskbar.service;

import com.farmerbb.taskbar.ui.DashboardController;
import com.farmerbb.taskbar.ui.StartMenuController;
import com.farmerbb.taskbar.ui.TaskbarController;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class UIHostServiceNewControllerTest {
    @Test
    public void testDashboardService() {
        ServiceController<DashboardService> dashboardService =
                Robolectric.buildService(DashboardService.class);
        assertTrue(dashboardService.get().newController() instanceof DashboardController);
    }

    @Test
    public void testStartMenuService() {
        ServiceController<StartMenuService> startMenuService =
                Robolectric.buildService(StartMenuService.class);
        assertTrue(startMenuService.get().newController() instanceof StartMenuController);
    }

    @Test
    public void testTaskbarService() {
        ServiceController<TaskbarService> taskbarService =
                Robolectric.buildService(TaskbarService.class);
        assertTrue(taskbarService.get().newController() instanceof TaskbarController);
    }
}
