package com.farmerbb.taskbar.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.util.TaskbarPosition;
import com.farmerbb.taskbar.util.U;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import static com.farmerbb.taskbar.util.Constants.PREF_SHOW_SEARCH_BAR;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*",
        "android.*", "androidx.*", "com.farmerbb.taskbar.shadow.*"})
@PrepareForTest(value = {U.class, TaskbarPosition.class})
public class StartMenuControllerTest {
    private static final String UNSUPPORTED = "unsupported";

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private StartMenuController uiController;
    private Context context;
    private SharedPreferences prefs;
    private UIHost host = new MockUIHost();

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        uiController = new StartMenuController(context);
        prefs = U.getSharedPreferences(context);

        uiController.onCreateHost(host);
    }

    @After
    public void tearDown() {
        prefs.edit().remove(PREF_SHOW_SEARCH_BAR).apply();
        uiController.onDestroyHost(host);
    }

    @Test
    public void testShouldShowSearchBox() {
        prefs.edit().remove(PREF_SHOW_SEARCH_BAR).apply();
        assertTrue(uiController.shouldShowSearchBox(prefs, false));

        prefs.edit().putString(PREF_SHOW_SEARCH_BAR, "always").apply();
        assertTrue(uiController.shouldShowSearchBox(prefs, false));

        prefs.edit().putString(PREF_SHOW_SEARCH_BAR, "keyboard").apply();
        assertFalse(uiController.shouldShowSearchBox(prefs, false));
        assertTrue(uiController.shouldShowSearchBox(prefs, true));

        prefs.edit().putString(PREF_SHOW_SEARCH_BAR, "never").apply();
        assertFalse(uiController.shouldShowSearchBox(prefs, true));

        prefs.edit().putString(PREF_SHOW_SEARCH_BAR, UNSUPPORTED).apply();
        assertFalse(uiController.shouldShowSearchBox(prefs, true));
    }
}
