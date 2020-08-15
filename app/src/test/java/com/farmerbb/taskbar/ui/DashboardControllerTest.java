package com.farmerbb.taskbar.ui;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;
import android.view.View;
import android.widget.LinearLayout;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.Constants;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.helper.LauncherHelper;
import com.farmerbb.taskbar.mockito.BooleanAnswer;
import com.farmerbb.taskbar.shadow.TaskbarShadowAppWidgetManager;
import com.farmerbb.taskbar.shadow.TaskbarShadowAppWidgetProviderInfo;
import com.farmerbb.taskbar.util.TaskbarPosition;
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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowToast;

import static com.farmerbb.taskbar.Constants.DEFAULT_TEST_CELL_ID;
import static com.farmerbb.taskbar.Constants.TEST_LABEL;
import static com.farmerbb.taskbar.Constants.TEST_NAME;
import static com.farmerbb.taskbar.Constants.TEST_PACKAGE;
import static com.farmerbb.taskbar.Constants.UNSUPPORTED;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_RIGHT;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_VERTICAL_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_VERTICAL_RIGHT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_RIGHT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_VERTICAL_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_VERTICAL_RIGHT;
import static com.farmerbb.taskbar.util.Constants.PREF_DASHBOARD_TUTORIAL_SHOWN;
import static com.farmerbb.taskbar.util.Constants.PREF_DASHBOARD_WIDGET_PLACEHOLDER_SUFFIX;
import static com.farmerbb.taskbar.util.Constants.PREF_DASHBOARD_WIDGET_PREFIX;
import static com.farmerbb.taskbar.util.Constants.PREF_DASHBOARD_WIDGET_PROVIDER_SUFFIX;
import static com.farmerbb.taskbar.util.Constants.PREF_DEFAULT_NULL;
import static com.farmerbb.taskbar.util.Constants.PREF_DONT_STOP_DASHBOARD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*",
        "android.*", "androidx.*", "com.farmerbb.taskbar.shadow.*"
})
@PrepareForTest(value = {U.class, TaskbarPosition.class,
        DashboardController.class, LauncherHelper.class})
public class DashboardControllerTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private DashboardController uiController;
    private Context context;
    private SharedPreferences prefs;
    private final UIHost host = new MockUIHost();

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        uiController = new DashboardController(context);
        prefs = U.getSharedPreferences(context);

        uiController.onCreateHost(host);
    }

    @After
    public void tearDown() {
        uiController.onDestroyHost(host);
        prefs.edit().remove(PREF_DASHBOARD_TUTORIAL_SHOWN).apply();
    }

    @Test
    public void testUpdatePaddingSize() {
        int paddingDefault = Integer.MAX_VALUE;
        LinearLayout layout = new LinearLayout(context);
        layout.setPadding(paddingDefault, paddingDefault, paddingDefault, paddingDefault);
        uiController.updatePaddingSize(context, layout, Constants.UNSUPPORTED);
        verifyViewPadding(layout, paddingDefault, paddingDefault, paddingDefault, paddingDefault);

        int paddingSize = context.getResources().getDimensionPixelSize(R.dimen.tb_icon_size);

        uiController.updatePaddingSize(context, layout, POSITION_TOP_VERTICAL_LEFT);
        verifyViewPadding(layout, paddingSize, 0, 0, 0);

        uiController.updatePaddingSize(context, layout, POSITION_BOTTOM_VERTICAL_LEFT);
        verifyViewPadding(layout, paddingSize, 0, 0, 0);

        uiController.updatePaddingSize(context, layout, POSITION_TOP_LEFT);
        verifyViewPadding(layout, 0, paddingSize, 0, 0);

        uiController.updatePaddingSize(context, layout, POSITION_TOP_RIGHT);
        verifyViewPadding(layout, 0, paddingSize, 0, 0);

        uiController.updatePaddingSize(context, layout, POSITION_TOP_VERTICAL_RIGHT);
        verifyViewPadding(layout, 0, 0, paddingSize, 0);

        uiController.updatePaddingSize(context, layout, POSITION_BOTTOM_VERTICAL_RIGHT);
        verifyViewPadding(layout, 0, 0, paddingSize, 0);

        uiController.updatePaddingSize(context, layout, POSITION_BOTTOM_LEFT);
        verifyViewPadding(layout, 0, 0, 0, paddingSize);

        uiController.updatePaddingSize(context, layout, POSITION_BOTTOM_RIGHT);
        verifyViewPadding(layout, 0, 0, 0, paddingSize);
    }

    @Test
    public void testShouldSendDisappearingBroadcast() {
        LauncherHelper helper = PowerMockito.mock(LauncherHelper.class);
        BooleanAnswer isOnSecondaryHomeScreenAnswer = new BooleanAnswer();
        when(helper.isOnSecondaryHomeScreen(context))
                .thenAnswer(isOnSecondaryHomeScreenAnswer);
        PowerMockito.mockStatic(LauncherHelper.class);
        when(LauncherHelper.getInstance()).thenReturn(helper);

        isOnSecondaryHomeScreenAnswer.answer = true;
        prefs.edit().putBoolean(PREF_DONT_STOP_DASHBOARD, true).apply();
        assertFalse(uiController.shouldSendDisappearingBroadcast(context, prefs));

        isOnSecondaryHomeScreenAnswer.answer = true;
        prefs.edit().putBoolean(PREF_DONT_STOP_DASHBOARD, false).apply();
        assertTrue(uiController.shouldSendDisappearingBroadcast(context, prefs));

        isOnSecondaryHomeScreenAnswer.answer = false;
        prefs.edit().putBoolean(PREF_DONT_STOP_DASHBOARD, true).apply();
        assertTrue(uiController.shouldSendDisappearingBroadcast(context, prefs));

        isOnSecondaryHomeScreenAnswer.answer = false;
        prefs.edit().putBoolean(PREF_DONT_STOP_DASHBOARD, false).apply();
        assertTrue(uiController.shouldSendDisappearingBroadcast(context, prefs));
    }

    @Test
    public void testSaveWidgetInfo() {
        AppWidgetProviderInfo info = new AppWidgetProviderInfo();
        info.provider = new ComponentName(TEST_PACKAGE, TEST_NAME);
        int cellId = DEFAULT_TEST_CELL_ID;
        int appWidgetId = 100;
        prefs.edit().putString(uiController.generateProviderPlaceholderPrefKey(cellId), "").apply();
        uiController.saveWidgetInfo(context, info, cellId, appWidgetId);
        assertEquals(
                appWidgetId,
                prefs.getInt(PREF_DASHBOARD_WIDGET_PREFIX + cellId, -1)
        );
        assertEquals(
                info.provider.flattenToString(),
                prefs.getString(uiController.generateProviderPrefKey(cellId), "")
        );
        assertFalse(prefs.contains(uiController.generateProviderPlaceholderPrefKey(cellId)));
    }

    @Test
    public void testShowDashboardTutorialToast() {
        prefs.edit().putBoolean(PREF_DASHBOARD_TUTORIAL_SHOWN, true).apply();
        uiController.showDashboardTutorialToast(context);
        assertNull(ShadowToast.getTextOfLatestToast());

        prefs.edit().putBoolean(PREF_DASHBOARD_TUTORIAL_SHOWN, false).apply();
        uiController.showDashboardTutorialToast(context);
        assertTrue(prefs.getBoolean(PREF_DASHBOARD_TUTORIAL_SHOWN, false));
        String toastText = ShadowToast.getTextOfLatestToast();
        assertEquals(context.getString(R.string.tb_dashboard_tutorial, toastText), toastText);
    }

    @Test
    public void testGenerateProviderPrefKey() {
        assertEquals(
                PREF_DASHBOARD_WIDGET_PREFIX
                        + DEFAULT_TEST_CELL_ID
                        + PREF_DASHBOARD_WIDGET_PROVIDER_SUFFIX,
                uiController.generateProviderPrefKey(DEFAULT_TEST_CELL_ID)
        );
    }

    @Test
    public void testGenerateProviderPlaceholderPrefKey() {
        assertEquals(
                PREF_DASHBOARD_WIDGET_PREFIX
                        + DEFAULT_TEST_CELL_ID
                        + PREF_DASHBOARD_WIDGET_PLACEHOLDER_SUFFIX,
                uiController.generateProviderPlaceholderPrefKey(DEFAULT_TEST_CELL_ID)
        );
    }

    @Test
    @Config(shadows = {TaskbarShadowAppWidgetManager.class,
            TaskbarShadowAppWidgetProviderInfo.class})
    public void testShowPlaceholderToast() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int cellId = DEFAULT_TEST_CELL_ID;
        String providerPrefKey = uiController.generateProviderPrefKey(cellId);

        TaskbarShadowAppWidgetManager shadowAppWidgetManager =
                (TaskbarShadowAppWidgetManager) shadowOf(appWidgetManager);
        AppWidgetProviderInfo info = new AppWidgetProviderInfo();
        info.provider = new ComponentName(TEST_PACKAGE, TEST_NAME);
        TaskbarShadowAppWidgetProviderInfo shadowAppWidgetProviderInfo =
                (TaskbarShadowAppWidgetProviderInfo) Shadow.extract(info);
        shadowAppWidgetProviderInfo.label = TEST_LABEL;
        shadowAppWidgetManager.addInstalledProvidersForProfile(Process.myUserHandle(), info);

        prefs.edit().putString(providerPrefKey, null).apply();
        uiController.showPlaceholderToast(context, appWidgetManager, cellId, prefs);
        assertNull(ShadowToast.getLatestToast());

        prefs.edit().putString(providerPrefKey, PREF_DEFAULT_NULL).apply();
        uiController.showPlaceholderToast(context, appWidgetManager, cellId, prefs);
        assertNull(ShadowToast.getLatestToast());

        prefs
                .edit()
                .putString(providerPrefKey, info.provider.flattenToString() + UNSUPPORTED)
                .apply();
        uiController.showPlaceholderToast(context, appWidgetManager, cellId, prefs);
        assertNull(ShadowToast.getLatestToast());

        prefs.edit().putString(providerPrefKey, info.provider.flattenToString()).apply();
        uiController.showPlaceholderToast(context, appWidgetManager, cellId, prefs);
        String lastToast = ShadowToast.getTextOfLatestToast();
        assertNotNull(lastToast);
        String expectedText = context.getString(R.string.tb_widget_restore_toast, TEST_LABEL);
        assertEquals(expectedText, lastToast);
    }

    private void verifyViewPadding(View view, int left, int top, int right, int bottom) {
        assertEquals(left, view.getPaddingLeft());
        assertEquals(top, view.getPaddingTop());
        assertEquals(right, view.getPaddingRight());
        assertEquals(bottom, view.getPaddingBottom());
    }
}
