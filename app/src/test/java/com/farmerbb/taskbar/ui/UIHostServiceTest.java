package com.farmerbb.taskbar.ui;

import android.content.res.Configuration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class UIHostServiceTest {
    private ServiceController<TestUIHostService> controller;
    private TestUIHostService hostService;
    private TestUIController uiController;

    @Before
    public void setUp() {
        controller = Robolectric.buildService(TestUIHostService.class);
        hostService = controller.create().get();
        uiController = hostService.controller;
    }

    @After
    public void tearDown() {
        uiController.onCreateHost = null;
        uiController.onRecreateHost = null;
        uiController.onDestroyHost = null;
    }

    @Test
    public void testOnCreate() {
        assertEquals(uiController.onCreateHost, hostService);
    }

    @Test
    public void testOnConfigurationChanged() {
        assertNull(uiController.onRecreateHost);
        hostService.onConfigurationChanged(new Configuration());
        assertEquals(hostService, uiController.onRecreateHost);
    }

    @Test
    public void testOnDestroy() {
        assertNull(uiController.onDestroyHost);
        controller.destroy();
        assertEquals(hostService, uiController.onDestroyHost);
    }

    @Test
    public void testTerminate() {
        ShadowService shadowService = Shadows.shadowOf(hostService);
        assertFalse(shadowService.isStoppedBySelf());
        hostService.terminate();
        assertTrue(shadowService.isStoppedBySelf());
    }

}
