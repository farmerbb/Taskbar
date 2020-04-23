package com.farmerbb.taskbar.ui;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.mockito.BooleanAnswer;
import com.farmerbb.taskbar.util.LauncherHelper;
import com.farmerbb.taskbar.util.U;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowService;

import static com.farmerbb.taskbar.util.Constants.PREF_TASKBAR_ACTIVE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
@PrepareForTest(U.class)
public class UIControllerTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private ServiceController<TestUIHostService> controller;
    private TestUIHostService hostService;
    private TestUIController uiController;
    private Context context;

    @Before
    public void setUp() {
        controller = Robolectric.buildService(TestUIHostService.class);
        hostService = controller.create().get();
        uiController = hostService.controller;
        context = ApplicationProvider.getApplicationContext();
    }

    @After
    public void tearDown() {
        setTaskbarActive(false);
    }

    @Test
    public void testInitWithoutProceed() {
        LauncherHelper.getInstance().setOnSecondaryHomeScreen(true, 1);
        TestRunnable runnable = new TestRunnable();
        Context context = ApplicationProvider.getApplicationContext();
        uiController.init(context, hostService, runnable);
        ShadowService shadowService = Shadows.shadowOf(hostService);
        assertTrue(shadowService.isStoppedBySelf());
    }

    @Test
    public void testInitWithProceedAndTaskbarActive() {
        setTaskbarActive(true);
        testInitWithProceed();
    }

    @Test
    public void testInitWithProceedAndOnHomeScreen() {
        testInitWithoutProceed();
    }

    private void testInitWithProceed() {
        LauncherHelper.getInstance().setOnPrimaryHomeScreen(true);
        PowerMockito.spy(U.class);
        BooleanAnswer canDrawOverlaysAnswer = new BooleanAnswer();
        when(U.canDrawOverlays(context)).thenAnswer(canDrawOverlaysAnswer);
        canDrawOverlaysAnswer.answer = true;
        TestRunnable runnable = new TestRunnable();
        Context context = ApplicationProvider.getApplicationContext();
        uiController.init(context, hostService, runnable);
        assertTrue(runnable.hasRun);
        runnable.hasRun = false;
        canDrawOverlaysAnswer.answer = false;
        uiController.init(context, hostService, runnable);
        ShadowService shadowService = Shadows.shadowOf(hostService);
        assertTrue(shadowService.isStoppedBySelf());
        assertFalse(
                U.getSharedPreferences(context).getBoolean(PREF_TASKBAR_ACTIVE, true)
        );
    }

    private void setTaskbarActive(boolean active) {
        U.getSharedPreferences(context)
                .edit()
                .putBoolean(PREF_TASKBAR_ACTIVE, active)
                .apply();
    }

    private static final class TestRunnable implements Runnable {
        boolean hasRun = false;

        @Override
        public void run() {
            hasRun = true;
        }
    }
}
