package com.farmerbb.taskbar.ui;

import android.app.AlarmManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.os.UserHandle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.HomeActivity;
import com.farmerbb.taskbar.activity.HomeActivityDelegate;
import com.farmerbb.taskbar.activity.InvisibleActivityFreeform;
import com.farmerbb.taskbar.activity.MainActivity;
import com.farmerbb.taskbar.activity.SecondaryHomeActivity;
import com.farmerbb.taskbar.mockito.BooleanAnswer;
import com.farmerbb.taskbar.mockito.StringAnswer;
import com.farmerbb.taskbar.shadow.TaskbarShadowLauncherApps;
import com.farmerbb.taskbar.shadow.TaskbarShadowScrollView;
import com.farmerbb.taskbar.util.AppEntry;
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
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowUsageStatsManager.EventBuilder;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

import static android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND;
import static android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND;
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
import static com.farmerbb.taskbar.util.Constants.PREF_HIDE_FOREGROUND;
import static com.farmerbb.taskbar.util.Constants.PREF_RECENTS_AMOUNT;
import static com.farmerbb.taskbar.util.Constants.PREF_RECENTS_AMOUNT_APP_START;
import static com.farmerbb.taskbar.util.Constants.PREF_RECENTS_AMOUNT_RUNNING_APPS_ONLY;
import static com.farmerbb.taskbar.util.Constants.PREF_RECENTS_AMOUNT_SHOW_ALL;
import static com.farmerbb.taskbar.util.Constants.PREF_START_BUTTON_IMAGE;
import static com.farmerbb.taskbar.util.Constants.PREF_TIME_OF_SERVICE_START;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.util.ReflectionHelpers.ClassParameter.from;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*",
        "android.*", "androidx.*", "com.farmerbb.taskbar.shadow.*"})
@PrepareForTest(value = {U.class, TaskbarPosition.class})
public class TaskbarControllerTest {
    private static final int DEFAULT_TEST_USER_ID = 0;
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

    @Test
    public void testFilterForegroundApp() {
        prefs.edit().putBoolean(PREF_HIDE_FOREGROUND, true).apply();

        long searchInterval = 0L;
        List<String> applicationIdsToRemove = new ArrayList<>();
        UsageStatsManager usageStatsManager =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents.Event event =
                EventBuilder
                        .buildEvent()
                        .setEventType(MOVE_TO_FOREGROUND)
                        .setTimeStamp(100L)
                        .setPackage("test-package-1")
                        .build();
        shadowOf(usageStatsManager).addEvent(event);
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove);
        assertEquals("test-package-1", applicationIdsToRemove.remove(0));

        event =
                EventBuilder
                        .buildEvent()
                        .setEventType(MOVE_TO_BACKGROUND)
                        .setTimeStamp(200L)
                        .setPackage("test-package-2")
                        .build();
        shadowOf(usageStatsManager).addEvent(event);
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove);
        assertEquals("test-package-1", applicationIdsToRemove.remove(0));

        event = buildTaskbarForegroundAppEvent(MainActivity.class.getCanonicalName(), 300L);
        shadowOf(usageStatsManager).addEvent(event);
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove);
        assertEquals(MainActivity.class.getCanonicalName(), applicationIdsToRemove.remove(0));

        event = buildTaskbarForegroundAppEvent(HomeActivity.class.getCanonicalName(), 400L);
        shadowOf(usageStatsManager).addEvent(event);
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove);
        assertEquals(HomeActivity.class.getCanonicalName(), applicationIdsToRemove.remove(0));

        event = buildTaskbarForegroundAppEvent(HomeActivityDelegate.class.getCanonicalName(), 500L);
        shadowOf(usageStatsManager).addEvent(event);
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove);
        assertEquals(HomeActivityDelegate.class.getCanonicalName(), applicationIdsToRemove.remove(0));

        event = buildTaskbarForegroundAppEvent(SecondaryHomeActivity.class.getCanonicalName(), 600L);
        shadowOf(usageStatsManager).addEvent(event);
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove);
        assertEquals(SecondaryHomeActivity.class.getCanonicalName(), applicationIdsToRemove.remove(0));

        event = buildTaskbarForegroundAppEvent(InvisibleActivityFreeform.class.getCanonicalName(), 700L);
        shadowOf(usageStatsManager).addEvent(event);
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove);
        assertEquals(InvisibleActivityFreeform.class.getCanonicalName(), applicationIdsToRemove.remove(0));

        event = buildTaskbarForegroundAppEvent("unsupported", 800L);
        shadowOf(usageStatsManager).addEvent(event);
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove);
        assertEquals("unsupported", applicationIdsToRemove.remove(0));

        prefs.edit().remove(PREF_HIDE_FOREGROUND).apply();
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove);
        assertEquals(0, applicationIdsToRemove.size());
    }

    @Test
    public void testNeedToReverseOrder() {
        PowerMockito.spy(TaskbarPosition.class);
        StringAnswer positionAnswer = new StringAnswer();
        when(TaskbarPosition.getTaskbarPosition(context)).thenAnswer(positionAnswer);

        List<String> positions = new ArrayList<>();
        positions.add(POSITION_BOTTOM_LEFT);
        positions.add(POSITION_BOTTOM_RIGHT);
        positions.add(POSITION_BOTTOM_VERTICAL_LEFT);
        positions.add(POSITION_BOTTOM_VERTICAL_RIGHT);
        positions.add(POSITION_TOP_LEFT);
        positions.add(POSITION_TOP_RIGHT);
        positions.add(POSITION_TOP_VERTICAL_LEFT);
        positions.add(POSITION_TOP_VERTICAL_RIGHT);
        positions.add("unsupported");

        String sortOrder = "false";
        for (String position : positions) {
            positionAnswer.answer = position;
            if (POSITION_BOTTOM_RIGHT.equals(position) || POSITION_TOP_RIGHT.equals(position)) {
                assertTrue(uiController.needToReverseOrder(context, sortOrder));
            } else {
                assertFalse(uiController.needToReverseOrder(context, sortOrder));
            }
        }

        sortOrder = "true";
        for (String position : positions) {
            positionAnswer.answer = position;
            if (POSITION_BOTTOM_RIGHT.equals(position) || POSITION_TOP_RIGHT.equals(position)) {
                assertFalse(uiController.needToReverseOrder(context, sortOrder));
            } else {
                assertTrue(uiController.needToReverseOrder(context, sortOrder));
            }
        }

        sortOrder = "unsupported";
        for (String position : positions) {
            positionAnswer.answer = position;
            assertFalse(uiController.needToReverseOrder(context, sortOrder));
        }
    }

    @Test
    @Config(shadows = TaskbarShadowLauncherApps.class)
    public void testFilterRealPinnedApps() {
        List<AppEntry> pinnedApps = new ArrayList<>();
        List<AppEntry> entries = new ArrayList<>();
        List<String> applicationIdsToRemove = new ArrayList<>();

        int realNumOfPinnedApps = uiController.filterRealPinnedApps(
                context, pinnedApps, entries, applicationIdsToRemove
        );
        assertEquals(0, realNumOfPinnedApps);

        LauncherApps launcherApps =
                (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        TaskbarShadowLauncherApps taskbarShadowLauncherApps =
                (TaskbarShadowLauncherApps) shadowOf(launcherApps);
        AppEntry appEntry = generateTestAppEntry(1);
        pinnedApps.add(appEntry);
        taskbarShadowLauncherApps
                .addEnabledPackage(
                        UserHandle.getUserHandleForUid(DEFAULT_TEST_USER_ID),
                        appEntry.getPackageName()
                );
        realNumOfPinnedApps = uiController.filterRealPinnedApps(
                context, pinnedApps, entries, applicationIdsToRemove
        );
        assertEquals(1, realNumOfPinnedApps);
        assertEquals(appEntry.getPackageName(), applicationIdsToRemove.get(0));
        assertEquals(appEntry, entries.get(0));
        applicationIdsToRemove.clear();
        entries.clear();

        appEntry = generateTestAppEntry(2);
        pinnedApps.add(appEntry);
        realNumOfPinnedApps = uiController.filterRealPinnedApps(
                context, pinnedApps, entries, applicationIdsToRemove
        );
        assertEquals(1, realNumOfPinnedApps);
        assertEquals(2, applicationIdsToRemove.size());
        assertEquals(1, entries.size());

        taskbarShadowLauncherApps.reset();
    }

    @Test
    public void testPopulateAppEntry() {
        List<AppEntry> entries = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<LauncherActivityInfo> launcherAppCache = new ArrayList<>();

        uiController.populateAppEntry(context, pm, entries, launcherAppCache);
        assertEquals(0, entries.size());

        AppEntry appEntry = generateTestAppEntry(1);
        entries.add(appEntry);
        uiController.populateAppEntry(context, pm, entries, launcherAppCache);
        assertEquals(1, entries.size());
        assertSame(appEntry, entries.get(0));

        AppEntry firstEntry = appEntry;
        appEntry = new AppEntry("test-package", null, null, null, false);
        appEntry.setLastTimeUsed(System.currentTimeMillis());
        entries.add(appEntry);
        ActivityInfo info = new ActivityInfo();
        info.packageName = appEntry.getPackageName();
        info.name = "test-name";
        info.nonLocalizedLabel = "test-label";
        LauncherActivityInfo launcherActivityInfo =
                ReflectionHelpers.callConstructor(
                        LauncherActivityInfo.class,
                        from(Context.class, context),
                        from(ActivityInfo.class, info),
                        from(UserHandle.class, UserHandle.getUserHandleForUid(DEFAULT_TEST_USER_ID))
                );
        launcherAppCache.add(launcherActivityInfo);
        uiController.populateAppEntry(context, pm, entries, launcherAppCache);
        assertEquals(2, entries.size());
        assertSame(firstEntry, entries.get(0));
        AppEntry populatedEntry = entries.get(1);
        assertEquals(info.packageName, populatedEntry.getPackageName());
        assertEquals(
                launcherActivityInfo.getComponentName().flattenToString(),
                populatedEntry.getComponentName()
        );
        assertEquals(info.nonLocalizedLabel.toString(), populatedEntry.getLabel());
        assertEquals(DEFAULT_TEST_USER_ID, populatedEntry.getUserId(context));
        assertEquals(appEntry.getLastTimeUsed(), populatedEntry.getLastTimeUsed());
    }

    private AppEntry generateTestAppEntry(int index) {
        AppEntry appEntry =
                new AppEntry(
                        "test-package-" + index,
                        "test-component" + index,
                        "test-label-" + index,
                        null,
                        false
                );
        appEntry.setUserId(DEFAULT_TEST_USER_ID);
        return appEntry;
    }

    private UsageEvents.Event buildTaskbarForegroundAppEvent(String className, long timestamp) {
        return EventBuilder
                .buildEvent()
                .setPackage(className)
                .setTimeStamp(timestamp)
                .setClass(className)
                .setEventType(MOVE_TO_FOREGROUND)
                .build();
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