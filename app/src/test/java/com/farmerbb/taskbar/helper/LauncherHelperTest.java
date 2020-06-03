package com.farmerbb.taskbar.helper;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.mockito.IntAnswer;
import com.farmerbb.taskbar.util.U;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
@PrepareForTest(U.class)
public class LauncherHelperTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private LauncherHelper launcherHelper;
    private Context context;
    private IntAnswer getExternalDisplayIdAnswer;

    @Before
    public void setUp() {
        launcherHelper = LauncherHelper.getInstance();
        context = ApplicationProvider.getApplicationContext();
        assertNotNull(context);

        PowerMockito.spy(U.class);
        getExternalDisplayIdAnswer = new IntAnswer();
        when(U.getExternalDisplayID(context)).thenAnswer(getExternalDisplayIdAnswer);
    }

    @Test
    public void testGetInstance() {
        assertNotNull(launcherHelper);
        for (int i = 1; i <= 20; i++) {
            assertEquals(launcherHelper, LauncherHelper.getInstance());
        }
    }

    @Test
    public void testIsHomeScreen() {
        assertFalse(launcherHelper.isOnHomeScreen(context));
        launcherHelper.setOnPrimaryHomeScreen(true);
        assertTrue(launcherHelper.isOnHomeScreen(context));
        launcherHelper.setOnPrimaryHomeScreen(false);
        launcherHelper.setOnSecondaryHomeScreen(true, 1);
        assertFalse(launcherHelper.isOnHomeScreen(context));
        getExternalDisplayIdAnswer.answer = 1;
        assertTrue(launcherHelper.isOnHomeScreen(context));
        launcherHelper.setOnSecondaryHomeScreen(false, 1);
        assertEquals(-1, launcherHelper.getSecondaryDisplayId());
    }

    @Test
    public void testIsOnSecondaryHomeScreen() {
        assertFalse(launcherHelper.isOnSecondaryHomeScreen(context));
        launcherHelper.setOnPrimaryHomeScreen(true);
        assertFalse(launcherHelper.isOnSecondaryHomeScreen(context));
        launcherHelper.setOnSecondaryHomeScreen(true, 1);
        assertFalse(launcherHelper.isOnSecondaryHomeScreen(context));
        getExternalDisplayIdAnswer.answer = 1;
        assertTrue(launcherHelper.isOnSecondaryHomeScreen(context));
        launcherHelper.setOnPrimaryHomeScreen(false);
        assertTrue(launcherHelper.isOnSecondaryHomeScreen(context));
        launcherHelper.setOnSecondaryHomeScreen(false, 1);
    }
}
