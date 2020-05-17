package com.farmerbb.taskbar.ui;

import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.mockito.BooleanAnswer;
import com.farmerbb.taskbar.util.DisplayInfo;
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
import org.robolectric.util.ReflectionHelpers;

import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_RIGHT;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_VERTICAL_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_VERTICAL_RIGHT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_RIGHT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_VERTICAL_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_VERTICAL_RIGHT;
import static com.farmerbb.taskbar.util.Constants.PREF_BUTTON_BACK;
import static com.farmerbb.taskbar.util.Constants.PREF_BUTTON_HOME;
import static com.farmerbb.taskbar.util.Constants.PREF_BUTTON_RECENTS;
import static com.farmerbb.taskbar.util.Constants.PREF_DASHBOARD;
import static com.farmerbb.taskbar.util.Constants.PREF_RECENTS_AMOUNT;
import static com.farmerbb.taskbar.util.Constants.PREF_RECENTS_AMOUNT_APP_START;
import static com.farmerbb.taskbar.util.Constants.PREF_RECENTS_AMOUNT_RUNNING_APPS_ONLY;
import static com.farmerbb.taskbar.util.Constants.PREF_RECENTS_AMOUNT_SHOW_ALL;
import static com.farmerbb.taskbar.util.Constants.PREF_START_BUTTON_IMAGE;
import static com.farmerbb.taskbar.util.Constants.PREF_TIME_OF_SERVICE_START;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
@PrepareForTest(value = {U.class, TaskbarPosition.class})
public class TaskbarControllerTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private TaskbarController uiController;
    private Context context;
    SharedPreferences prefs;

    private UIHost host = new MockUIHost();

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        uiController = new TaskbarController(context);
        prefs = U.getSharedPreferences(context);

        uiController.onCreateHost(host);
    }

    @After
    public void tearDown() {
        prefs.edit().remove(PREF_START_BUTTON_IMAGE).apply();

        uiController.onDestroyHost(host);
    }

    @Test
    public void testInitialization() {
        assertNotNull(uiController);
    }

    @Test
    public void testDrawStartButtonPadding() {
        ImageView startButton = new ImageView(context);
        prefs = U.getSharedPreferences(context);
        prefs.edit().putString(PREF_START_BUTTON_IMAGE, "default").apply();
        uiController.drawStartButton(context, startButton, prefs, Color.RED);
        int padding =
                context.getResources().getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding);
        checkStartButtonPadding(padding, startButton);

        PowerMockito.spy(U.class);
        // Use bliss os logic to avoid using LauncherApps, that robolectric doesn't support
        when(U.isBlissOs(context)).thenReturn(true);
        prefs.edit().putString(PREF_START_BUTTON_IMAGE, "app_logo").apply();
        uiController.drawStartButton(context, startButton, prefs, Color.RED);
        padding =
                context.getResources().getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding_alt);
        checkStartButtonPadding(padding, startButton);

        prefs.edit().putString(PREF_START_BUTTON_IMAGE, "custom").apply();
        uiController.drawStartButton(context, startButton, prefs, Color.RED);
        padding = context.getResources().getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding);
        checkStartButtonPadding(padding, startButton);

        prefs.edit().putString(PREF_START_BUTTON_IMAGE, "non-support").apply();
        uiController.drawStartButton(context, startButton, prefs, Color.RED);
        checkStartButtonPadding(0, startButton);
    }

    @Test
    public void testGetTaskbarGravity() {
        assertEquals(
                Gravity.BOTTOM | Gravity.LEFT,
                uiController.getTaskbarGravity(POSITION_BOTTOM_LEFT)
        );
        assertEquals(
                Gravity.BOTTOM | Gravity.LEFT,
                uiController.getTaskbarGravity(POSITION_BOTTOM_VERTICAL_LEFT)
        );
        assertEquals(
                Gravity.BOTTOM | Gravity.RIGHT,
                uiController.getTaskbarGravity(POSITION_BOTTOM_RIGHT)
        );
        assertEquals(
                Gravity.BOTTOM | Gravity.RIGHT,
                uiController.getTaskbarGravity(POSITION_BOTTOM_VERTICAL_RIGHT)
        );
        assertEquals(
                Gravity.TOP | Gravity.LEFT,
                uiController.getTaskbarGravity(POSITION_TOP_LEFT)
        );
        assertEquals(
                Gravity.TOP | Gravity.LEFT,
                uiController.getTaskbarGravity(POSITION_TOP_VERTICAL_LEFT)
        );
        assertEquals(
                Gravity.TOP | Gravity.RIGHT,
                uiController.getTaskbarGravity(POSITION_TOP_RIGHT)
        );
        assertEquals(
                Gravity.TOP | Gravity.RIGHT,
                uiController.getTaskbarGravity(POSITION_TOP_VERTICAL_RIGHT)
        );
        assertEquals(
                Gravity.BOTTOM | Gravity.LEFT,
                uiController.getTaskbarGravity("unsupported")
        );
    }

    @Test
    public void testGetTaskbarLayoutId() {
        assertEquals(
                R.layout.tb_taskbar_left,
                uiController.getTaskbarLayoutId(POSITION_BOTTOM_LEFT)
        );
        assertEquals(
                R.layout.tb_taskbar_vertical,
                uiController.getTaskbarLayoutId(POSITION_BOTTOM_VERTICAL_LEFT)
        );
        assertEquals(
                R.layout.tb_taskbar_right,
                uiController.getTaskbarLayoutId(POSITION_BOTTOM_RIGHT)
        );
        assertEquals(
                R.layout.tb_taskbar_vertical,
                uiController.getTaskbarLayoutId(POSITION_BOTTOM_VERTICAL_RIGHT)
        );
        assertEquals(
                R.layout.tb_taskbar_left,
                uiController.getTaskbarLayoutId(POSITION_TOP_LEFT)
        );
        assertEquals(
                R.layout.tb_taskbar_top_vertical,
                uiController.getTaskbarLayoutId(POSITION_TOP_VERTICAL_LEFT)
        );
        assertEquals(
                R.layout.tb_taskbar_right,
                uiController.getTaskbarLayoutId(POSITION_TOP_RIGHT)
        );
        assertEquals(
                R.layout.tb_taskbar_top_vertical,
                uiController.getTaskbarLayoutId(POSITION_TOP_VERTICAL_RIGHT)
        );
        assertEquals(
                R.layout.tb_taskbar_left,
                uiController.getTaskbarLayoutId("unsupported")
        );
    }

    @Test
    public void testDrawDashboardButtonWithDefaultConfig() {
        prefs.edit().remove(PREF_DASHBOARD).apply();
        checkDashboardEnabled(false);
    }

    @Test
    @Config(qualifiers = "sw540dp")
    public void testDrawDashboardButtonWithDefaultConfigForSw540dp() {
        prefs.edit().remove(PREF_DASHBOARD).apply();
        checkDashboardEnabled(false);
    }

    @Test
    @Config(qualifiers = "sw720dp")
    public void testDrawDashboardButtonWithDefaultConfigForSw720dp() {
        prefs.edit().remove(PREF_DASHBOARD).apply();
        checkDashboardEnabled(true);
    }

    @Test
    public void testDrawDashboardButtonForDashboardButton() {
        int accentColor = Color.RED;
        int layoutId = uiController.getTaskbarLayoutId(POSITION_BOTTOM_LEFT);
        LinearLayout layout = (LinearLayout) LayoutInflater.from(context).inflate(layoutId, null);
        FrameLayout dashboardButton = layout.findViewById(R.id.dashboard_button);

        prefs.edit().putBoolean(PREF_DASHBOARD + "_is_modified", true).apply();
        prefs.edit().putBoolean(PREF_DASHBOARD, false).apply();
        boolean dashboardEnabled =
                uiController.drawDashboardButton(context, layout, dashboardButton, accentColor);
        assertFalse(dashboardEnabled);
        assertEquals(View.GONE, dashboardButton.getVisibility());

        prefs.edit().putBoolean(PREF_DASHBOARD, true).apply();
        dashboardEnabled =
                uiController.drawDashboardButton(context, layout, dashboardButton, accentColor);
        assertTrue(dashboardEnabled);
        assertTrue(dashboardButton.hasOnClickListeners());
        assertEquals(View.VISIBLE, dashboardButton.getVisibility());
        Drawable drawable = layout.findViewById(R.id.square1).getBackground();
        checkDrawableBackgroundColor(drawable, accentColor);
        drawable = layout.findViewById(R.id.square2).getBackground();
        checkDrawableBackgroundColor(drawable, accentColor);
        drawable = layout.findViewById(R.id.square3).getBackground();
        checkDrawableBackgroundColor(drawable, accentColor);
        drawable = layout.findViewById(R.id.square4).getBackground();
        checkDrawableBackgroundColor(drawable, accentColor);
        drawable = layout.findViewById(R.id.square5).getBackground();
        checkDrawableBackgroundColor(drawable, accentColor);
        drawable = layout.findViewById(R.id.square6).getBackground();
        checkDrawableBackgroundColor(drawable, accentColor);
    }

    @Test
    public void testDrawNavbarButtons() {
        int layoutId = uiController.getTaskbarLayoutId(POSITION_BOTTOM_LEFT);
        LinearLayout layout = (LinearLayout) LayoutInflater.from(context).inflate(layoutId, null);
        prefs.edit()
                .remove(PREF_BUTTON_BACK)
                .remove(PREF_BUTTON_HOME)
                .remove(PREF_BUTTON_RECENTS)
                .apply();
        assertFalse(uiController.drawNavbarButtons(context, layout, prefs, Color.RED));

        prefs.edit().putBoolean(PREF_BUTTON_BACK, true).apply();
        assertTrue(uiController.drawNavbarButtons(context, layout, prefs, Color.RED));
        assertEquals(View.VISIBLE, layout.findViewById(R.id.button_back).getVisibility());
        prefs.edit().remove(PREF_BUTTON_BACK).apply();

        prefs.edit().putBoolean(PREF_BUTTON_HOME, true).apply();
        assertTrue(uiController.drawNavbarButtons(context, layout, prefs, Color.RED));
        assertEquals(View.VISIBLE, layout.findViewById(R.id.button_home).getVisibility());
        prefs.edit().remove(PREF_BUTTON_HOME).apply();

        prefs.edit().putBoolean(PREF_BUTTON_RECENTS, true).apply();
        assertTrue(uiController.drawNavbarButtons(context, layout, prefs, Color.RED));
        assertEquals(View.VISIBLE, layout.findViewById(R.id.button_recents).getVisibility());
        prefs.edit().remove(PREF_BUTTON_RECENTS).apply();
    }

    @Test
    public void testGetSearchInterval() {
        long permitTimeDeltaMillis = 100;
        prefs.edit().remove(PREF_RECENTS_AMOUNT).apply();
        long searchInterval = uiController.getSearchInterval(prefs);
        long lastDayTime = System.currentTimeMillis() - AlarmManager.INTERVAL_DAY;
        assertEquals(lastDayTime, searchInterval, permitTimeDeltaMillis);

        prefs.edit().putString(PREF_RECENTS_AMOUNT, PREF_RECENTS_AMOUNT_APP_START).apply();
        long deviceStartTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        // The service start time is larger than device start time
        long appStartTime = deviceStartTime * 2;
        prefs.edit().putLong(PREF_TIME_OF_SERVICE_START, appStartTime).apply();
        searchInterval = uiController.getSearchInterval(prefs);
        assertEquals(appStartTime, searchInterval);

        // The service start time is smaller than device start time
        prefs.edit().putLong(PREF_TIME_OF_SERVICE_START, deviceStartTime - 100).apply();
        searchInterval = uiController.getSearchInterval(prefs);
        deviceStartTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        assertEquals(deviceStartTime, searchInterval, permitTimeDeltaMillis);
        prefs.edit().remove(PREF_TIME_OF_SERVICE_START).apply();

        prefs.edit().putString(PREF_RECENTS_AMOUNT, PREF_RECENTS_AMOUNT_SHOW_ALL).apply();
        searchInterval = uiController.getSearchInterval(prefs);
        assertEquals(0, searchInterval);

        prefs.edit().putString(PREF_RECENTS_AMOUNT, PREF_RECENTS_AMOUNT_RUNNING_APPS_ONLY).apply();
        searchInterval = uiController.getSearchInterval(prefs);
        assertEquals(-1, searchInterval);

        prefs.edit().putString(PREF_RECENTS_AMOUNT, "unsupported").apply();
        searchInterval = uiController.getSearchInterval(prefs);
        assertEquals(-1, searchInterval);

        prefs.edit().remove(PREF_RECENTS_AMOUNT).apply();
    }

    @Test
    public void testDrawSysTrayOnClickListener() {
        PowerMockito.spy(U.class);
        BooleanAnswer isLibraryAnswer = new BooleanAnswer();
        when(U.isLibrary(context)).thenAnswer(isLibraryAnswer);

        isLibraryAnswer.answer = true;
        LinearLayout sysTrayLayout = initializeSysTrayLayout(POSITION_BOTTOM_RIGHT);
        assertFalse(sysTrayLayout.hasOnClickListeners());

        isLibraryAnswer.answer = false;
        sysTrayLayout = initializeSysTrayLayout(POSITION_BOTTOM_RIGHT);
        assertTrue(sysTrayLayout.hasOnClickListeners());
    }

    @Test
    public void testDrawSysTrayParentLayoutVisibility() {
        LinearLayout sysTrayLayout = initializeSysTrayLayout(POSITION_BOTTOM_RIGHT);
        ViewGroup parent = (ViewGroup) sysTrayLayout.getParent();
        assertEquals(View.VISIBLE, parent.getVisibility());
    }

    @Test
    public void testDrawSysTrayGravity() {
        checkDrawSysTrayGravity(POSITION_BOTTOM_LEFT, Gravity.END);
        checkDrawSysTrayGravity(POSITION_BOTTOM_RIGHT, Gravity.START);
    }

    @Test
    public void testDrawSysTrayTime() {
        checkDrawSysTrayTimeVisibility(POSITION_BOTTOM_LEFT, R.id.time_right);
        checkDrawSysTrayTimeVisibility(POSITION_BOTTOM_RIGHT, R.id.time_left);
    }

    @Test
    public void testCalculateScrollViewParams() {
        BooleanAnswer isVerticalAnswer = new BooleanAnswer();
        PowerMockito.spy(TaskbarPosition.class);
        when(TaskbarPosition.isVertical(context)).thenAnswer(isVerticalAnswer);

        DisplayInfo display = U.getDisplayInfo(context, true);
        int dividerSize = context.getResources().getDimensionPixelSize(R.dimen.tb_divider_size);

        int defaultSize = -1;
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(defaultSize, defaultSize);
        int iconSize = context.getResources().getDimensionPixelSize(R.dimen.tb_icon_size);

        isVerticalAnswer.answer = true;
        int maxScreenSize =
                Math.max(
                        0,
                        display.height
                                - U.getStatusBarHeight(context)
                                - U.getBaseTaskbarSize(context)
                );

        uiController.calculateScrollViewParams(context, prefs, params, true, 1);
        assertEquals(defaultSize, params.width);
        assertEquals(maxScreenSize + dividerSize, params.height);
        params.height = defaultSize;

        uiController.calculateScrollViewParams(context, prefs, params, false, 1);
        assertEquals(defaultSize, params.width);
        assertEquals(iconSize + dividerSize, params.height);
        params.height = defaultSize;

        uiController.calculateScrollViewParams(context, prefs, params, false, 10000);
        assertEquals(defaultSize, params.width);
        assertEquals(maxScreenSize + dividerSize, params.height);
        params.height = defaultSize;

        isVerticalAnswer.answer = false;
        maxScreenSize = Math.max(0, display.width - U.getBaseTaskbarSize(context));

        uiController.calculateScrollViewParams(context, prefs, params, true, 1);
        assertEquals(maxScreenSize + dividerSize, params.width);
        assertEquals(defaultSize, params.height);
        params.width = defaultSize;

        uiController.calculateScrollViewParams(context, prefs, params, false, 1);
        assertEquals(iconSize + dividerSize, params.width);
        assertEquals(defaultSize, params.height);
        params.width = defaultSize;

        uiController.calculateScrollViewParams(context, prefs, params, false, 10000);
        assertEquals(maxScreenSize + dividerSize, params.width);
        assertEquals(defaultSize, params.height);
    }

    @Test
    public void testScrollTaskbarForScrollViewVisibility() {
        int layoutId = uiController.getTaskbarLayoutId(POSITION_BOTTOM_LEFT);
        LinearLayout layout = (LinearLayout) LayoutInflater.from(context).inflate(layoutId, null);
        FrameLayout scrollView = layout.findViewById(R.id.taskbar_scrollview);
        LinearLayout taskbar = layout.findViewById(R.id.taskbar);
        uiController.scrollTaskbar(scrollView, taskbar, POSITION_BOTTOM_LEFT, "false", false);
        assertEquals(View.GONE, scrollView.getVisibility());

        uiController.scrollTaskbar(scrollView, taskbar, POSITION_BOTTOM_LEFT, "false", true);
        assertEquals(View.VISIBLE, scrollView.getVisibility());
    }

    @Test
    @Config(shadows = {TaskbarShadowScrollView.class})
    public void testScrollTaskbarForScrollViewLocation() {
        // We only provide enhanced ShadowScrollView with scrollTo supported, so we should
        // choose the layout uses the ScrollView instead of HorizontalScrollView.
        String taskbarPosition = POSITION_BOTTOM_VERTICAL_LEFT;
        int layoutId = uiController.getTaskbarLayoutId(taskbarPosition);
        LinearLayout layout = (LinearLayout) LayoutInflater.from(context).inflate(layoutId, null);
        FrameLayout scrollView = layout.findViewById(R.id.taskbar_scrollview);
        LinearLayout taskbar = layout.findViewById(R.id.taskbar);
        int taskbarWidth = 200;
        int taskbarHeight = 50;
        // Change LayoutParams doesn't work with robolectric, so we should use reflection
        // to change the location directly.
        ReflectionHelpers.setField(taskbar, "mLeft", 0);
        ReflectionHelpers.setField(taskbar, "mTop", 0);
        ReflectionHelpers.setField(taskbar, "mRight", taskbarWidth);
        ReflectionHelpers.setField(taskbar, "mBottom", taskbarHeight);

        BooleanAnswer isVerticalAnswer = new BooleanAnswer();
        PowerMockito.spy(TaskbarPosition.class);
        when(TaskbarPosition.isVertical(taskbarPosition)).thenAnswer(isVerticalAnswer);

        isVerticalAnswer.answer = false;
        uiController.scrollTaskbar(scrollView, taskbar, taskbarPosition, "false", true);
        assertEquals(0, scrollView.getScrollX());
        assertEquals(0, scrollView.getScrollY());
        uiController.scrollTaskbar(scrollView, taskbar, taskbarPosition, "true", true);
        assertEquals(taskbarWidth, scrollView.getScrollX());
        assertEquals(taskbarHeight, scrollView.getScrollY());

        isVerticalAnswer.answer = true;
        uiController.scrollTaskbar(scrollView, taskbar, taskbarPosition, "true", true);
        assertEquals(0, scrollView.getScrollX());
        assertEquals(0, scrollView.getScrollY());
        uiController.scrollTaskbar(scrollView, taskbar, taskbarPosition, "false", true);
        assertEquals(taskbarWidth, scrollView.getScrollX());
        assertEquals(taskbarHeight, scrollView.getScrollY());
    }

    private void checkDrawableBackgroundColor(Drawable drawable, int color) {
        assertTrue(drawable instanceof ColorDrawable);
        ColorDrawable colorDrawable = (ColorDrawable) drawable;
        assertEquals(color, colorDrawable.getColor());
    }

    private void checkDashboardEnabled(boolean expectedDashboardEnabled) {
        int layoutId = uiController.getTaskbarLayoutId(POSITION_BOTTOM_LEFT);
        LinearLayout layout = (LinearLayout) LayoutInflater.from(context).inflate(layoutId, null);
        FrameLayout dashboardButton = layout.findViewById(R.id.dashboard_button);
        boolean dashboardEnabled =
                uiController.drawDashboardButton(context, layout, dashboardButton, Color.RED);
        assertEquals(expectedDashboardEnabled, dashboardEnabled);
    }

    private void checkDrawSysTrayTimeVisibility(String position, int timeId) {
        LinearLayout sysTrayLayout = initializeSysTrayLayout(position);
        assertEquals(View.VISIBLE, sysTrayLayout.findViewById(timeId).getVisibility());
    }

    private void checkDrawSysTrayGravity(String position, int gravity) {
        LinearLayout sysTrayLayout = initializeSysTrayLayout(position);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) sysTrayLayout.getLayoutParams();
        assertEquals(gravity, params.gravity);
    }

    private LinearLayout initializeSysTrayLayout(String position) {
        int layoutId = uiController.getTaskbarLayoutId(position);
        LinearLayout layout = (LinearLayout) LayoutInflater.from(context).inflate(layoutId, null);
        uiController.drawSysTray(context, layoutId, layout);
        return getFieldSysTrayLayout(uiController);
    }

    private LinearLayout getFieldSysTrayLayout(TaskbarController uiController) {
        return ReflectionHelpers.getField(uiController, "sysTrayLayout");
    }

    private void checkStartButtonPadding(int padding, ImageView startButton) {
        assertEquals(padding, startButton.getPaddingLeft());
        assertEquals(padding, startButton.getPaddingTop());
        assertEquals(padding, startButton.getPaddingRight());
        assertEquals(padding, startButton.getPaddingBottom());
    }
}