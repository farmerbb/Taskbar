package com.farmerbb.taskbar.ui

import android.app.AlarmManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.os.UserHandle
import android.os.UserManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.Constants
import com.farmerbb.taskbar.LauncherAppsHelper.generateTestLauncherActivityInfo
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.activity.HomeActivity
import com.farmerbb.taskbar.activity.HomeActivityDelegate
import com.farmerbb.taskbar.activity.InvisibleActivityFreeform
import com.farmerbb.taskbar.activity.MainActivity
import com.farmerbb.taskbar.activity.SecondaryHomeActivity
import com.farmerbb.taskbar.mockito.BooleanAnswer
import com.farmerbb.taskbar.mockito.StringAnswer
import com.farmerbb.taskbar.shadow.TaskbarShadowScrollView
import com.farmerbb.taskbar.util.AppEntry
import com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_LEFT
import com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_RIGHT
import com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_VERTICAL_LEFT
import com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_VERTICAL_RIGHT
import com.farmerbb.taskbar.util.Constants.POSITION_TOP_LEFT
import com.farmerbb.taskbar.util.Constants.POSITION_TOP_RIGHT
import com.farmerbb.taskbar.util.Constants.POSITION_TOP_VERTICAL_LEFT
import com.farmerbb.taskbar.util.Constants.POSITION_TOP_VERTICAL_RIGHT
import com.farmerbb.taskbar.util.Constants.PREF_BUTTON_BACK
import com.farmerbb.taskbar.util.Constants.PREF_BUTTON_HOME
import com.farmerbb.taskbar.util.Constants.PREF_BUTTON_RECENTS
import com.farmerbb.taskbar.util.Constants.PREF_DASHBOARD
import com.farmerbb.taskbar.util.Constants.PREF_HIDE_FOREGROUND
import com.farmerbb.taskbar.util.Constants.PREF_RECENTS_AMOUNT
import com.farmerbb.taskbar.util.Constants.PREF_RECENTS_AMOUNT_APP_START
import com.farmerbb.taskbar.util.Constants.PREF_RECENTS_AMOUNT_RUNNING_APPS_ONLY
import com.farmerbb.taskbar.util.Constants.PREF_RECENTS_AMOUNT_SHOW_ALL
import com.farmerbb.taskbar.util.Constants.PREF_START_BUTTON_IMAGE
import com.farmerbb.taskbar.util.Constants.PREF_START_BUTTON_IMAGE_APP_LOGO
import com.farmerbb.taskbar.util.Constants.PREF_START_BUTTON_IMAGE_CUSTOM
import com.farmerbb.taskbar.util.Constants.PREF_START_BUTTON_IMAGE_DEFAULT
import com.farmerbb.taskbar.util.Constants.PREF_TIME_OF_SERVICE_START
import com.farmerbb.taskbar.util.TaskbarPosition
import com.farmerbb.taskbar.util.U
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.rule.PowerMockRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowUsageStatsManager
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@PowerMockIgnore("org.mockito.*", "org.robolectric.*", "android.*", "androidx.*")
@PrepareForTest(value = [U::class, TaskbarPosition::class])
class TaskbarControllerTest {
    @get:Rule
    val rule = PowerMockRule()
    private lateinit var uiController: TaskbarController
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private val host: UIHost = MockUIHost()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        uiController = TaskbarController(context)
        prefs = U.getSharedPreferences(context)
        uiController.onCreateHost(host)
    }

    @After
    fun tearDown() {
        prefs.edit().remove(PREF_START_BUTTON_IMAGE).apply()
        uiController.onDestroyHost(host)
    }

    @Test
    fun testInitialization() {
        Assert.assertNotNull(uiController)
    }

    @Test
    fun testDrawStartButtonPadding() {
        val startButton = ImageView(context)
        prefs = U.getSharedPreferences(context)
        prefs.edit().putString(PREF_START_BUTTON_IMAGE, PREF_START_BUTTON_IMAGE_DEFAULT).apply()
        uiController.drawStartButton(context, startButton, prefs)
        var padding = context.resources.getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding)
        checkStartButtonPadding(padding, startButton)
        PowerMockito.spy(U::class.java)
        // Use bliss os logic to avoid using LauncherApps, that robolectric doesn't support
        PowerMockito.`when`(U.isBlissOs(context)).thenReturn(true)
        prefs.edit().putString(PREF_START_BUTTON_IMAGE, PREF_START_BUTTON_IMAGE_APP_LOGO).apply()
        uiController.drawStartButton(context, startButton, prefs)
        padding = context.resources.getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding_alt)
        checkStartButtonPadding(padding, startButton)
        prefs.edit().putString(PREF_START_BUTTON_IMAGE, PREF_START_BUTTON_IMAGE_CUSTOM).apply()
        uiController.drawStartButton(context, startButton, prefs)
        padding = context.resources.getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding)
        checkStartButtonPadding(padding, startButton)
        prefs.edit().putString(PREF_START_BUTTON_IMAGE, "non-support").apply()
        uiController.drawStartButton(context, startButton, prefs)
        checkStartButtonPadding(0, startButton)
    }

    @Test
    fun testGetTaskbarGravity() {
        Assert.assertEquals(
                (Gravity.BOTTOM or Gravity.LEFT).toLong(),
                uiController.getTaskbarGravity(POSITION_BOTTOM_LEFT).toLong())
        Assert.assertEquals(
                (Gravity.BOTTOM or Gravity.LEFT).toLong(),
                uiController.getTaskbarGravity(POSITION_BOTTOM_VERTICAL_LEFT).toLong())
        Assert.assertEquals(
                (Gravity.BOTTOM or Gravity.RIGHT).toLong(),
                uiController.getTaskbarGravity(POSITION_BOTTOM_RIGHT).toLong())
        Assert.assertEquals(
                (Gravity.BOTTOM or Gravity.RIGHT).toLong(),
                uiController.getTaskbarGravity(POSITION_BOTTOM_VERTICAL_RIGHT).toLong())
        Assert.assertEquals(
                (Gravity.TOP or Gravity.LEFT).toLong(),
                uiController.getTaskbarGravity(POSITION_TOP_LEFT).toLong())
        Assert.assertEquals(
                (Gravity.TOP or Gravity.LEFT).toLong(),
                uiController.getTaskbarGravity(POSITION_TOP_VERTICAL_LEFT).toLong())
        Assert.assertEquals(
                (Gravity.TOP or Gravity.RIGHT).toLong(),
                uiController.getTaskbarGravity(POSITION_TOP_RIGHT).toLong())
        Assert.assertEquals(
                (Gravity.TOP or Gravity.RIGHT).toLong(),
                uiController.getTaskbarGravity(POSITION_TOP_VERTICAL_RIGHT).toLong())
        Assert.assertEquals(
                (Gravity.BOTTOM or Gravity.LEFT).toLong(),
                uiController.getTaskbarGravity(Constants.UNSUPPORTED).toLong())
    }

    @Test
    fun testGetTaskbarLayoutId() {
        Assert.assertEquals(
                R.layout.tb_taskbar_left.toLong(),
                uiController.getTaskbarLayoutId(POSITION_BOTTOM_LEFT).toLong())
        Assert.assertEquals(
                R.layout.tb_taskbar_vertical.toLong(),
                uiController.getTaskbarLayoutId(POSITION_BOTTOM_VERTICAL_LEFT).toLong())
        Assert.assertEquals(
                R.layout.tb_taskbar_right.toLong(),
                uiController.getTaskbarLayoutId(POSITION_BOTTOM_RIGHT).toLong())
        Assert.assertEquals(
                R.layout.tb_taskbar_vertical.toLong(),
                uiController.getTaskbarLayoutId(POSITION_BOTTOM_VERTICAL_RIGHT).toLong())
        Assert.assertEquals(
                R.layout.tb_taskbar_left.toLong(),
                uiController.getTaskbarLayoutId(POSITION_TOP_LEFT).toLong())
        Assert.assertEquals(
                R.layout.tb_taskbar_top_vertical.toLong(),
                uiController.getTaskbarLayoutId(POSITION_TOP_VERTICAL_LEFT).toLong())
        Assert.assertEquals(
                R.layout.tb_taskbar_right.toLong(),
                uiController.getTaskbarLayoutId(POSITION_TOP_RIGHT).toLong())
        Assert.assertEquals(
                R.layout.tb_taskbar_top_vertical.toLong(),
                uiController.getTaskbarLayoutId(POSITION_TOP_VERTICAL_RIGHT).toLong())
        Assert.assertEquals(
                R.layout.tb_taskbar_left.toLong(),
                uiController.getTaskbarLayoutId(Constants.UNSUPPORTED).toLong())
    }

    @Test
    fun testDrawDashboardButtonWithDefaultConfig() {
        prefs.edit().remove(PREF_DASHBOARD).apply()
        checkDashboardEnabled(false)
    }

    @Test
    @Config(qualifiers = "sw540dp")
    fun testDrawDashboardButtonWithDefaultConfigForSw540dp() {
        prefs.edit().remove(PREF_DASHBOARD).apply()
        checkDashboardEnabled(false)
    }

    @Test
    @Config(qualifiers = "sw720dp")
    fun testDrawDashboardButtonWithDefaultConfigForSw720dp() {
        prefs.edit().remove(PREF_DASHBOARD).apply()
        checkDashboardEnabled(true)
    }

    @Test
    fun testDrawDashboardButtonForDashboardButton() {
        val accentColor = Color.RED
        val layoutId = uiController.getTaskbarLayoutId(POSITION_BOTTOM_LEFT)
        val layout = LayoutInflater.from(context).inflate(layoutId, null) as LinearLayout
        val dashboardButton = layout.findViewById<FrameLayout>(R.id.dashboard_button)
        prefs.edit().putBoolean(PREF_DASHBOARD, false).apply()
        var dashboardEnabled = uiController.drawDashboardButton(context, layout,
                dashboardButton, accentColor)
        Assert.assertFalse(dashboardEnabled)
        Assert.assertEquals(View.GONE.toLong(), dashboardButton.visibility.toLong())
        prefs.edit().putBoolean(PREF_DASHBOARD, true).apply()
        dashboardEnabled = uiController.drawDashboardButton(context, layout, dashboardButton,
                accentColor)
        Assert.assertTrue(dashboardEnabled)
        Assert.assertTrue(dashboardButton.hasOnClickListeners())
        Assert.assertEquals(View.VISIBLE.toLong(), dashboardButton.visibility.toLong())
        var drawable = layout.findViewById<View>(R.id.square1).background
        checkDrawableBackgroundColor(drawable, accentColor)
        drawable = layout.findViewById<View>(R.id.square2).background
        checkDrawableBackgroundColor(drawable, accentColor)
        drawable = layout.findViewById<View>(R.id.square3).background
        checkDrawableBackgroundColor(drawable, accentColor)
        drawable = layout.findViewById<View>(R.id.square4).background
        checkDrawableBackgroundColor(drawable, accentColor)
        drawable = layout.findViewById<View>(R.id.square5).background
        checkDrawableBackgroundColor(drawable, accentColor)
        drawable = layout.findViewById<View>(R.id.square6).background
        checkDrawableBackgroundColor(drawable, accentColor)
    }

    @Test
    fun testDrawNavbarButtons() {
        val layoutId = uiController.getTaskbarLayoutId(POSITION_BOTTOM_LEFT)
        val layout = LayoutInflater.from(context).inflate(layoutId, null) as LinearLayout
        prefs.edit()
                .remove(PREF_BUTTON_BACK)
                .remove(PREF_BUTTON_HOME)
                .remove(PREF_BUTTON_RECENTS)
                .apply()
        Assert.assertFalse(uiController.drawNavbarButtons(context, layout, prefs, Color.RED))
        prefs.edit().putBoolean(PREF_BUTTON_BACK, true).apply()
        Assert.assertTrue(uiController.drawNavbarButtons(context, layout, prefs, Color.RED))
        Assert.assertEquals(View.VISIBLE.toLong(), layout.findViewById<View>(R.id.button_back)
                .visibility.toLong())
        prefs.edit().remove(PREF_BUTTON_BACK).apply()
        prefs.edit().putBoolean(PREF_BUTTON_HOME, true).apply()
        Assert.assertTrue(uiController.drawNavbarButtons(context, layout, prefs, Color.RED))
        Assert.assertEquals(View.VISIBLE.toLong(), layout.findViewById<View>(R.id.button_home)
                .visibility.toLong())
        prefs.edit().remove(PREF_BUTTON_HOME).apply()
        prefs.edit().putBoolean(PREF_BUTTON_RECENTS, true).apply()
        Assert.assertTrue(uiController.drawNavbarButtons(context, layout, prefs, Color.RED))
        Assert.assertEquals(View.VISIBLE.toLong(), layout.findViewById<View>(R.id.button_recents)
                .visibility.toLong())
        prefs.edit().remove(PREF_BUTTON_RECENTS).apply()
    }

    @Test
    fun testGetSearchInterval() {
        val permitTimeDeltaMillis: Long = 100
        prefs.edit().remove(PREF_RECENTS_AMOUNT).apply()
        var searchInterval = uiController.getSearchInterval(prefs)
        val lastDayTime = System.currentTimeMillis() - AlarmManager.INTERVAL_DAY
        Assert.assertEquals(lastDayTime.toFloat(), searchInterval.toFloat(),
                permitTimeDeltaMillis.toFloat())
        prefs.edit().putString(PREF_RECENTS_AMOUNT, PREF_RECENTS_AMOUNT_APP_START).apply()
        var deviceStartTime = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        // The service start time is larger than device start time
        val appStartTime = deviceStartTime * 2
        prefs.edit().putLong(PREF_TIME_OF_SERVICE_START, appStartTime).apply()
        searchInterval = uiController.getSearchInterval(prefs)
        Assert.assertEquals(appStartTime, searchInterval)

        // The service start time is smaller than device start time
        prefs.edit().putLong(PREF_TIME_OF_SERVICE_START, deviceStartTime - 100).apply()
        searchInterval = uiController.getSearchInterval(prefs)
        deviceStartTime = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        Assert.assertEquals(deviceStartTime.toFloat(), searchInterval.toFloat(),
                permitTimeDeltaMillis.toFloat())
        prefs.edit().remove(PREF_TIME_OF_SERVICE_START).apply()
        prefs.edit().putString(PREF_RECENTS_AMOUNT, PREF_RECENTS_AMOUNT_SHOW_ALL).apply()
        searchInterval = uiController.getSearchInterval(prefs)
        Assert.assertEquals(0, searchInterval)
        prefs.edit().putString(PREF_RECENTS_AMOUNT, PREF_RECENTS_AMOUNT_RUNNING_APPS_ONLY).apply()
        searchInterval = uiController.getSearchInterval(prefs)
        Assert.assertEquals(-1, searchInterval)
        prefs.edit().putString(PREF_RECENTS_AMOUNT, Constants.UNSUPPORTED).apply()
        searchInterval = uiController.getSearchInterval(prefs)
        Assert.assertEquals(-1, searchInterval)
        prefs.edit().remove(PREF_RECENTS_AMOUNT).apply()
    }

    @Test
    fun testDrawSysTrayOnClickListener() {
        PowerMockito.spy(U::class.java)
        val isLibraryAnswer = BooleanAnswer()
        PowerMockito.`when`(U.isLibrary(context)).thenAnswer(isLibraryAnswer)
        isLibraryAnswer.answer = true
        var sysTrayLayout = initializeSysTrayLayout(POSITION_BOTTOM_RIGHT)
        Assert.assertFalse(sysTrayLayout.hasOnClickListeners())
        isLibraryAnswer.answer = false
        sysTrayLayout = initializeSysTrayLayout(POSITION_BOTTOM_RIGHT)
        Assert.assertTrue(sysTrayLayout.hasOnClickListeners())
    }

    @Test
    fun testDrawSysTrayParentLayoutVisibility() {
        val sysTrayLayout = initializeSysTrayLayout(POSITION_BOTTOM_RIGHT)
        val parent = sysTrayLayout.parent as ViewGroup
        Assert.assertEquals(View.VISIBLE.toLong(), parent.visibility.toLong())
    }

    @Test
    fun testDrawSysTrayGravity() {
        checkDrawSysTrayGravity(POSITION_BOTTOM_LEFT, Gravity.END)
        checkDrawSysTrayGravity(POSITION_BOTTOM_RIGHT, Gravity.START)
    }

    @Test
    fun testDrawSysTrayTime() {
        checkDrawSysTrayTimeVisibility(POSITION_BOTTOM_LEFT, R.id.time_right)
        checkDrawSysTrayTimeVisibility(POSITION_BOTTOM_RIGHT, R.id.time_left)
    }

    @Test
    fun testCalculateScrollViewParams() {
        val isVerticalAnswer = BooleanAnswer()
        PowerMockito.spy(TaskbarPosition::class.java)
        PowerMockito.`when`(TaskbarPosition.isVertical(context)).thenAnswer(isVerticalAnswer)
        val display = U.getDisplayInfo(context)
        val dividerSize = context.resources.getDimensionPixelSize(R.dimen.tb_divider_size)
        val defaultSize = -1
        val params = ViewGroup.LayoutParams(defaultSize, defaultSize)
        val iconSize = context.resources.getDimensionPixelSize(R.dimen.tb_icon_size)
        isVerticalAnswer.answer = true
        var maxScreenSize = Math.max(
                0,
                display.height -
                        U.getStatusBarHeight(context) -
                        Math.round(U.getBaseTaskbarSize(context))
        )
        uiController.calculateScrollViewParams(context, prefs, params, true, 1)
        Assert.assertEquals(defaultSize.toLong(), params.width.toLong())
        Assert.assertEquals(maxScreenSize + dividerSize.toLong(), params.height.toLong())
        params.height = defaultSize
        uiController.calculateScrollViewParams(context, prefs, params, false, 1)
        Assert.assertEquals(defaultSize.toLong(), params.width.toLong())
        Assert.assertEquals(iconSize + dividerSize.toLong(), params.height.toLong())
        params.height = defaultSize
        uiController.calculateScrollViewParams(context, prefs, params, false, 10000)
        Assert.assertEquals(defaultSize.toLong(), params.width.toLong())
        Assert.assertEquals(maxScreenSize + dividerSize.toLong(), params.height.toLong())
        params.height = defaultSize
        isVerticalAnswer.answer = false
        maxScreenSize = Math.max(0, display.width - Math.round(U.getBaseTaskbarSize(context)))
        uiController.calculateScrollViewParams(context, prefs, params, true, 1)
        Assert.assertEquals(maxScreenSize + dividerSize.toLong(), params.width.toLong())
        Assert.assertEquals(defaultSize.toLong(), params.height.toLong())
        params.width = defaultSize
        uiController.calculateScrollViewParams(context, prefs, params, false, 1)
        Assert.assertEquals(iconSize + dividerSize.toLong(), params.width.toLong())
        Assert.assertEquals(defaultSize.toLong(), params.height.toLong())
        params.width = defaultSize
        uiController.calculateScrollViewParams(context, prefs, params, false, 10000)
        Assert.assertEquals(maxScreenSize + dividerSize.toLong(), params.width.toLong())
        Assert.assertEquals(defaultSize.toLong(), params.height.toLong())
    }

    @Test
    fun testScrollTaskbarForScrollViewVisibility() {
        val layoutId = uiController.getTaskbarLayoutId(POSITION_BOTTOM_LEFT)
        val layout = LayoutInflater.from(context).inflate(layoutId, null) as LinearLayout
        val scrollView = layout.findViewById<FrameLayout>(R.id.taskbar_scrollview)
        val taskbar = layout.findViewById<LinearLayout>(R.id.taskbar)
        uiController.scrollTaskbar(scrollView, taskbar, POSITION_BOTTOM_LEFT, "false", false)
        Assert.assertEquals(View.GONE.toLong(), scrollView.visibility.toLong())
        uiController.scrollTaskbar(scrollView, taskbar, POSITION_BOTTOM_LEFT, "false", true)
        Assert.assertEquals(View.VISIBLE.toLong(), scrollView.visibility.toLong())
    }

    @Test
    @Config(shadows = [TaskbarShadowScrollView::class])
    fun testScrollTaskbarForScrollViewLocation() {
        // We only provide enhanced ShadowScrollView with scrollTo supported, so we should
        // choose the layout uses the ScrollView instead of HorizontalScrollView.
        val taskbarPosition = POSITION_BOTTOM_VERTICAL_LEFT
        val layoutId = uiController.getTaskbarLayoutId(taskbarPosition)
        val layout = LayoutInflater.from(context).inflate(layoutId, null) as LinearLayout
        val scrollView = layout.findViewById<FrameLayout>(R.id.taskbar_scrollview)
        val taskbar = layout.findViewById<LinearLayout>(R.id.taskbar)
        val taskbarWidth = 200
        val taskbarHeight = 50
        // Change LayoutParams doesn't work with robolectric, so we should use reflection
        // to change the location directly.
        ReflectionHelpers.setField(taskbar, "mLeft", 0)
        ReflectionHelpers.setField(taskbar, "mTop", 0)
        ReflectionHelpers.setField(taskbar, "mRight", taskbarWidth)
        ReflectionHelpers.setField(taskbar, "mBottom", taskbarHeight)
        val isVerticalAnswer = BooleanAnswer()
        PowerMockito.spy(TaskbarPosition::class.java)
        PowerMockito.`when`(TaskbarPosition.isVertical(taskbarPosition))
                .thenAnswer(isVerticalAnswer)
        isVerticalAnswer.answer = false
        uiController.scrollTaskbar(scrollView, taskbar, taskbarPosition, "false", true)
        Assert.assertEquals(0, scrollView.scrollX.toLong())
        Assert.assertEquals(0, scrollView.scrollY.toLong())
        uiController.scrollTaskbar(scrollView, taskbar, taskbarPosition, "true", true)
        Assert.assertEquals(taskbarWidth.toLong(), scrollView.scrollX.toLong())
        Assert.assertEquals(taskbarHeight.toLong(), scrollView.scrollY.toLong())
        isVerticalAnswer.answer = true
        uiController.scrollTaskbar(scrollView, taskbar, taskbarPosition, "true", true)
        Assert.assertEquals(0, scrollView.scrollX.toLong())
        Assert.assertEquals(0, scrollView.scrollY.toLong())
        uiController.scrollTaskbar(scrollView, taskbar, taskbarPosition, "false", true)
        Assert.assertEquals(taskbarWidth.toLong(), scrollView.scrollX.toLong())
        Assert.assertEquals(taskbarHeight.toLong(), scrollView.scrollY.toLong())
    }

    @Test
    fun testFilterForegroundApp() {
        prefs.edit().putBoolean(PREF_HIDE_FOREGROUND, true).apply()
        val searchInterval = 0L
        val applicationIdsToRemove: MutableList<String> = ArrayList()
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
                as UsageStatsManager
        val entryTestPackage1 = Constants.TEST_PACKAGE + "-1"
        var event = ShadowUsageStatsManager.EventBuilder
                .buildEvent()
                .setEventType(UsageEvents.Event.MOVE_TO_FOREGROUND)
                .setTimeStamp(100L)
                .setPackage(entryTestPackage1)
                .build()
        Shadows.shadowOf(usageStatsManager).addEvent(event)
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove)
        Assert.assertEquals(entryTestPackage1, applicationIdsToRemove.removeAt(0))
        event = ShadowUsageStatsManager.EventBuilder
                .buildEvent()
                .setEventType(UsageEvents.Event.MOVE_TO_BACKGROUND)
                .setTimeStamp(200L)
                .setPackage(Constants.TEST_PACKAGE + "-2")
                .build()
        Shadows.shadowOf(usageStatsManager).addEvent(event)
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove)
        Assert.assertEquals(entryTestPackage1, applicationIdsToRemove.removeAt(0))
        event = buildTaskbarForegroundAppEvent(MainActivity::class.java.canonicalName, 300L)
        Shadows.shadowOf(usageStatsManager).addEvent(event)
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove)
        Assert.assertEquals(MainActivity::class.java.canonicalName,
                applicationIdsToRemove.removeAt(0))
        event = buildTaskbarForegroundAppEvent(HomeActivity::class.java.canonicalName, 400L)
        Shadows.shadowOf(usageStatsManager).addEvent(event)
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove)
        Assert.assertEquals(HomeActivity::class.java.canonicalName,
                applicationIdsToRemove.removeAt(0))
        event = buildTaskbarForegroundAppEvent(
                HomeActivityDelegate::class.java.canonicalName, 500L)
        Shadows.shadowOf(usageStatsManager).addEvent(event)
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove)
        Assert.assertEquals(HomeActivityDelegate::class.java.canonicalName,
                applicationIdsToRemove.removeAt(0))
        event = buildTaskbarForegroundAppEvent(
                SecondaryHomeActivity::class.java.canonicalName, 600L)
        Shadows.shadowOf(usageStatsManager).addEvent(event)
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove)
        Assert.assertEquals(SecondaryHomeActivity::class.java.canonicalName,
                applicationIdsToRemove.removeAt(0))
        event = buildTaskbarForegroundAppEvent(
                InvisibleActivityFreeform::class.java.canonicalName, 700L)
        Shadows.shadowOf(usageStatsManager).addEvent(event)
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove)
        Assert.assertEquals(InvisibleActivityFreeform::class.java.canonicalName,
                applicationIdsToRemove.removeAt(0))
        event = buildTaskbarForegroundAppEvent(Constants.UNSUPPORTED, 800L)
        Shadows.shadowOf(usageStatsManager).addEvent(event)
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove)
        Assert.assertEquals(Constants.UNSUPPORTED, applicationIdsToRemove.removeAt(0))
        prefs.edit().remove(PREF_HIDE_FOREGROUND).apply()
        uiController.filterForegroundApp(context, prefs, searchInterval, applicationIdsToRemove)
        Assert.assertEquals(0, applicationIdsToRemove.size.toLong())
    }

    @Test
    fun testNeedToReverseOrder() {
        PowerMockito.spy(TaskbarPosition::class.java)
        val positions: MutableList<String> = ArrayList()
        positions.add(POSITION_BOTTOM_LEFT)
        positions.add(POSITION_BOTTOM_RIGHT)
        positions.add(POSITION_BOTTOM_VERTICAL_LEFT)
        positions.add(POSITION_BOTTOM_VERTICAL_RIGHT)
        positions.add(POSITION_TOP_LEFT)
        positions.add(POSITION_TOP_RIGHT)
        positions.add(POSITION_TOP_VERTICAL_LEFT)
        positions.add(POSITION_TOP_VERTICAL_RIGHT)
        positions.add(Constants.UNSUPPORTED)
        var sortOrder = "false"
        for (position in positions) {
            PowerMockito.`when`(TaskbarPosition.getTaskbarPosition(context))
                    .thenAnswer(StringAnswer(position))
            if (POSITION_BOTTOM_RIGHT == position || POSITION_TOP_RIGHT == position) {
                Assert.assertTrue(uiController.needToReverseOrder(context, sortOrder))
            } else {
                Assert.assertFalse(uiController.needToReverseOrder(context, sortOrder))
            }
        }
        sortOrder = "true"
        for (position in positions) {
            PowerMockito.`when`(TaskbarPosition.getTaskbarPosition(context))
                    .thenAnswer(StringAnswer(position))
            if (POSITION_BOTTOM_RIGHT == position || POSITION_TOP_RIGHT == position) {
                Assert.assertFalse(uiController.needToReverseOrder(context, sortOrder))
            } else {
                Assert.assertTrue(uiController.needToReverseOrder(context, sortOrder))
            }
        }
        sortOrder = Constants.UNSUPPORTED
        for (position in positions) {
            PowerMockito.`when`(TaskbarPosition.getTaskbarPosition(context))
                    .thenAnswer(StringAnswer(position))
            Assert.assertFalse(uiController.needToReverseOrder(context, sortOrder))
        }
    }

    @Test
    fun testFilterRealPinnedApps() {
        val pinnedApps: MutableList<AppEntry> = ArrayList()
        val entries: MutableList<AppEntry> = ArrayList()
        val applicationIdsToRemove: MutableList<String> = ArrayList()
        var realNumOfPinnedApps = uiController.filterRealPinnedApps(
                context, pinnedApps, entries, applicationIdsToRemove
        )
        Assert.assertEquals(0, realNumOfPinnedApps.toLong())
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val shadowLauncherApps = Shadows.shadowOf(launcherApps)
        var appEntry = generateTestAppEntry(1)
        pinnedApps.add(appEntry)
        shadowLauncherApps
                .addEnabledPackage(
                        UserHandle.getUserHandleForUid(Constants.DEFAULT_TEST_USER_ID),
                        appEntry.packageName
                )
        realNumOfPinnedApps = uiController.filterRealPinnedApps(
                context, pinnedApps, entries, applicationIdsToRemove
        )
        Assert.assertEquals(1, realNumOfPinnedApps.toLong())
        Assert.assertEquals(appEntry.packageName, applicationIdsToRemove[0])
        Assert.assertEquals(appEntry, entries[0])
        applicationIdsToRemove.clear()
        entries.clear()
        appEntry = generateTestAppEntry(2)
        pinnedApps.add(appEntry)
        realNumOfPinnedApps = uiController.filterRealPinnedApps(
                context, pinnedApps, entries, applicationIdsToRemove
        )
        Assert.assertEquals(1, realNumOfPinnedApps.toLong())
        Assert.assertEquals(2, applicationIdsToRemove.size.toLong())
        Assert.assertEquals(1, entries.size.toLong())
    }

    @Test
    fun testGenerateAppEntries() {
        val usageStatsList: MutableList<AppEntry> = ArrayList()
        val entries: MutableList<AppEntry> = ArrayList()
        val launcherAppCache: MutableList<LauncherActivityInfo> = ArrayList()
        uiController.generateAppEntries(context, -1, usageStatsList, entries, launcherAppCache)
        Assert.assertEquals(0, entries.size.toLong())
        uiController.generateAppEntries(context, 0, usageStatsList, entries, launcherAppCache)
        Assert.assertEquals(0, entries.size.toLong())
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        Shadows.shadowOf(userManager)
                .addUserProfile(UserHandle.getUserHandleForUid(Constants.DEFAULT_TEST_USER_ID))
        Shadows.shadowOf(userManager)
                .addUserProfile(
                        UserHandle.getUserHandleForUid(Constants.DEFAULT_TEST_USER_PROFILE_ID)
                )
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val shadowLauncherApps = Shadows.shadowOf(launcherApps)
        var appEntry = generateTestAppEntry(0)
        usageStatsList.add(appEntry)
        uiController.generateAppEntries(context, 1, usageStatsList, entries, launcherAppCache)
        Assert.assertEquals(0, entries.size.toLong())
        val info = ActivityInfo()
        info.packageName = appEntry.packageName
        info.name = appEntry.label
        info.nonLocalizedLabel = appEntry.label
        val launcherActivityInfo = generateTestLauncherActivityInfo(
                context, info, Constants.DEFAULT_TEST_USER_ID
        )
        shadowLauncherApps.addActivity(launcherActivityInfo.user, launcherActivityInfo)
        uiController.generateAppEntries(context, 1, usageStatsList, entries, launcherAppCache)
        Assert.assertEquals(1, entries.size.toLong())
        Assert.assertEquals(1, launcherAppCache.size.toLong())
        Assert.assertEquals(appEntry.packageName, entries[0].packageName)
        Assert.assertSame(launcherActivityInfo, launcherAppCache[0])
        entries.clear()
        launcherAppCache.clear()
        val launcherActivityInfoForProfile = generateTestLauncherActivityInfo(
                context, info, Constants.DEFAULT_TEST_USER_PROFILE_ID
        )
        shadowLauncherApps.addActivity(
                UserHandle.getUserHandleForUid(Constants.DEFAULT_TEST_USER_PROFILE_ID),
                launcherActivityInfoForProfile
        )
        uiController.generateAppEntries(context, 1, usageStatsList, entries, launcherAppCache)
        Assert.assertEquals(1, launcherAppCache.size.toLong())
        Assert.assertEquals(1, entries.size.toLong())
        Assert.assertEquals(appEntry.packageName, entries[0].packageName)
        Assert.assertSame(launcherActivityInfo, launcherAppCache[0])
        entries.clear()
        launcherAppCache.clear()
        usageStatsList.clear()
        appEntry = AppEntry(
                "com.google.android.googlequicksearchbox",
                Constants.TEST_COMPONENT,
                Constants.TEST_LABEL,
                null,
                false
        )
        usageStatsList.add(appEntry)
        val thirdInfo = ActivityInfo()
        thirdInfo.packageName = appEntry.packageName
        thirdInfo.name = appEntry.label
        thirdInfo.nonLocalizedLabel = appEntry.label
        val thirdLauncherActivityInfo = generateTestLauncherActivityInfo(
                context, thirdInfo, Constants.DEFAULT_TEST_USER_ID
        )
        shadowLauncherApps
                .addActivity(thirdLauncherActivityInfo.user, thirdLauncherActivityInfo)
        uiController.generateAppEntries(context, 1, usageStatsList, entries, launcherAppCache)
        Assert.assertSame(thirdLauncherActivityInfo, launcherAppCache[0])
        entries.clear()
        launcherAppCache.clear()
        val forthInfo = ActivityInfo(thirdInfo)
        forthInfo.name = "com.google.android.googlequicksearchbox.SearchActivity"
        val forthLauncherActivityInfo = generateTestLauncherActivityInfo(
                context, forthInfo, Constants.DEFAULT_TEST_USER_ID
        )
        shadowLauncherApps
                .addActivity(forthLauncherActivityInfo.user, forthLauncherActivityInfo)
        uiController.generateAppEntries(context, 1, usageStatsList, entries, launcherAppCache)
        Assert.assertSame(forthLauncherActivityInfo, launcherAppCache[0])
    }

    @Test
    fun testPopulateAppEntries() {
        val entries: MutableList<AppEntry> = ArrayList()
        val pm = context.packageManager
        val launcherAppCache: MutableList<LauncherActivityInfo> = ArrayList()
        uiController.populateAppEntries(context, pm, entries, launcherAppCache)
        Assert.assertEquals(0, entries.size.toLong())
        var appEntry = generateTestAppEntry(1)
        entries.add(appEntry)
        uiController.populateAppEntries(context, pm, entries, launcherAppCache)
        Assert.assertEquals(1, entries.size.toLong())
        Assert.assertSame(appEntry, entries[0])
        val firstEntry = appEntry
        appEntry = AppEntry(Constants.TEST_PACKAGE, null, null, null, false)
        appEntry.lastTimeUsed = System.currentTimeMillis()
        entries.add(appEntry)
        val info = ActivityInfo()
        info.packageName = appEntry.packageName
        info.name = Constants.TEST_NAME
        info.nonLocalizedLabel = Constants.TEST_LABEL
        val launcherActivityInfo = generateTestLauncherActivityInfo(
                context, info, Constants.DEFAULT_TEST_USER_ID
        )
        launcherAppCache.add(launcherActivityInfo)
        uiController.populateAppEntries(context, pm, entries, launcherAppCache)
        Assert.assertEquals(2, entries.size.toLong())
        Assert.assertSame(firstEntry, entries[0])
        val populatedEntry = entries[1]
        Assert.assertEquals(info.packageName, populatedEntry.packageName)
        Assert.assertEquals(
                launcherActivityInfo.componentName.flattenToString(),
                populatedEntry.componentName
        )
        Assert.assertEquals(info.nonLocalizedLabel.toString(), populatedEntry.label)
        Assert.assertEquals(Constants.DEFAULT_TEST_USER_ID.toLong(),
                populatedEntry.getUserId(context))
        Assert.assertEquals(appEntry.lastTimeUsed, populatedEntry.lastTimeUsed)
    }

    private fun generateTestAppEntry(index: Int): AppEntry {
        val appEntry = AppEntry(
                Constants.TEST_PACKAGE + "-" + index,
                Constants.TEST_COMPONENT + "-" + index,
                Constants.TEST_LABEL + "-" + index,
                null,
                false
        )
        appEntry.setUserId(Constants.DEFAULT_TEST_USER_ID.toLong())
        return appEntry
    }

    private fun buildTaskbarForegroundAppEvent(className: String, timestamp: Long):
            UsageEvents.Event {
        return ShadowUsageStatsManager.EventBuilder
                .buildEvent()
                .setPackage(className)
                .setTimeStamp(timestamp)
                .setClass(className)
                .setEventType(UsageEvents.Event.MOVE_TO_FOREGROUND)
                .build()
    }

    private fun checkDrawableBackgroundColor(drawable: Drawable, color: Int) {
        Assert.assertTrue(drawable is ColorDrawable)
        val colorDrawable = drawable as ColorDrawable
        Assert.assertEquals(color.toLong(), colorDrawable.color.toLong())
    }

    private fun checkDashboardEnabled(expectedDashboardEnabled: Boolean) {
        val layoutId = uiController.getTaskbarLayoutId(POSITION_BOTTOM_LEFT)
        val layout = LayoutInflater.from(context).inflate(layoutId, null) as LinearLayout
        val dashboardButton = layout.findViewById<FrameLayout>(R.id.dashboard_button)
        val dashboardEnabled = uiController.drawDashboardButton(context, layout,
                dashboardButton, Color.RED)
        Assert.assertEquals(expectedDashboardEnabled, dashboardEnabled)
    }

    private fun checkDrawSysTrayTimeVisibility(position: String, timeId: Int) {
        val sysTrayLayout = initializeSysTrayLayout(position)
        Assert.assertEquals(View.VISIBLE.toLong(), sysTrayLayout.findViewById<View>(timeId)
                .visibility.toLong())
    }

    private fun checkDrawSysTrayGravity(position: String, gravity: Int) {
        val sysTrayLayout = initializeSysTrayLayout(position)
        val params = sysTrayLayout.layoutParams as FrameLayout.LayoutParams
        Assert.assertEquals(gravity.toLong(), params.gravity.toLong())
    }

    private fun initializeSysTrayLayout(position: String): LinearLayout {
        val layoutId = uiController.getTaskbarLayoutId(position)
        val layout = LayoutInflater.from(context).inflate(layoutId, null) as LinearLayout
        uiController.drawSysTray(context, layoutId, layout)
        return getFieldSysTrayLayout(uiController)
    }

    private fun getFieldSysTrayLayout(uiController: TaskbarController?): LinearLayout {
        return ReflectionHelpers.getField(uiController, "sysTrayLayout")
    }

    private fun checkStartButtonPadding(padding: Int, startButton: ImageView) {
        Assert.assertEquals(padding.toLong(), startButton.paddingLeft.toLong())
        Assert.assertEquals(padding.toLong(), startButton.paddingTop.toLong())
        Assert.assertEquals(padding.toLong(), startButton.paddingRight.toLong())
        Assert.assertEquals(padding.toLong(), startButton.paddingBottom.toLong())
    }
}
