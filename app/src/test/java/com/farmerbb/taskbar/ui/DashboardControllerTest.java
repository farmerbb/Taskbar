package com.farmerbb.taskbar.ui;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.Constants;
import com.farmerbb.taskbar.R;
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

import static com.farmerbb.taskbar.Constants.TEST_NAME;
import static com.farmerbb.taskbar.Constants.TEST_PACKAGE;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_RIGHT;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_VERTICAL_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_VERTICAL_RIGHT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_RIGHT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_VERTICAL_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_VERTICAL_RIGHT;
import static com.farmerbb.taskbar.util.Constants.PREF_DASHBOARD_WIDGET_PLACEHOLDER_SUFFIX;
import static com.farmerbb.taskbar.util.Constants.PREF_DASHBOARD_WIDGET_PREFIX;
import static com.farmerbb.taskbar.util.Constants.PREF_DASHBOARD_WIDGET_PROVIDER_SUFFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*",
        "android.*", "androidx.*", "com.farmerbb.taskbar.shadow.*"
})
@PrepareForTest(value = {U.class, TaskbarPosition.class})
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
    public void testSaveWidgetInfo() {
        AppWidgetProviderInfo info = new AppWidgetProviderInfo();
        info.provider = new ComponentName(TEST_PACKAGE, TEST_NAME);
        int cellId = 1000;
        int appWidgetId = 100;
        prefs.edit().putString(PREF_DASHBOARD_WIDGET_PREFIX + cellId + PREF_DASHBOARD_WIDGET_PLACEHOLDER_SUFFIX, "");
        uiController.saveWidgetInfo(context, info, cellId, appWidgetId);
        assertEquals(
                appWidgetId,
                prefs.getInt(PREF_DASHBOARD_WIDGET_PREFIX + cellId, -1)
        );
        assertEquals(
                info.provider.flattenToString(),
                prefs.getString(
                        PREF_DASHBOARD_WIDGET_PREFIX
                                + cellId
                                + PREF_DASHBOARD_WIDGET_PROVIDER_SUFFIX,
                        ""
                )
        );
        assertFalse(
                prefs.contains(
                        PREF_DASHBOARD_WIDGET_PREFIX
                                + cellId
                                + PREF_DASHBOARD_WIDGET_PLACEHOLDER_SUFFIX
                )
        );
    }

    private void verifyViewPadding(View view, int left, int top, int right, int bottom) {
        assertEquals(left, view.getPaddingLeft());
        assertEquals(top, view.getPaddingTop());
        assertEquals(right, view.getPaddingRight());
        assertEquals(bottom, view.getPaddingBottom());
    }
}
