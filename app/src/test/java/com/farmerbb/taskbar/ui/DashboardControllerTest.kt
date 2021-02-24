package com.farmerbb.taskbar.ui

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Process
import android.view.View
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.Constants.DEFAULT_TEST_CELL_ID
import com.farmerbb.taskbar.Constants.TEST_LABEL
import com.farmerbb.taskbar.Constants.TEST_NAME
import com.farmerbb.taskbar.Constants.TEST_PACKAGE
import com.farmerbb.taskbar.Constants.UNSUPPORTED
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.helper.LauncherHelper
import com.farmerbb.taskbar.mockito.BooleanAnswer
import com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_LEFT
import com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_RIGHT
import com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_VERTICAL_LEFT
import com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_VERTICAL_RIGHT
import com.farmerbb.taskbar.util.Constants.POSITION_TOP_LEFT
import com.farmerbb.taskbar.util.Constants.POSITION_TOP_RIGHT
import com.farmerbb.taskbar.util.Constants.POSITION_TOP_VERTICAL_LEFT
import com.farmerbb.taskbar.util.Constants.POSITION_TOP_VERTICAL_RIGHT
import com.farmerbb.taskbar.util.Constants.PREF_DASHBOARD_TUTORIAL_SHOWN
import com.farmerbb.taskbar.util.Constants.PREF_DASHBOARD_WIDGET_PLACEHOLDER_SUFFIX
import com.farmerbb.taskbar.util.Constants.PREF_DASHBOARD_WIDGET_PREFIX
import com.farmerbb.taskbar.util.Constants.PREF_DASHBOARD_WIDGET_PROVIDER_SUFFIX
import com.farmerbb.taskbar.util.Constants.PREF_DEFAULT_NULL
import com.farmerbb.taskbar.util.Constants.PREF_DONT_STOP_DASHBOARD
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
import org.robolectric.shadows.AppWidgetProviderInfoBuilder
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@PowerMockIgnore("org.mockito.*", "org.robolectric.*", "android.*", "androidx.*",
        "com.farmerbb.taskbar.shadow.*")
@PrepareForTest(value = [U::class, TaskbarPosition::class, DashboardController::class,
    LauncherHelper::class])
class DashboardControllerTest {
    @get:Rule
    val rule = PowerMockRule()
    private lateinit var uiController: DashboardController
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private val host: UIHost = MockUIHost()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        uiController = DashboardController(context)
        prefs = U.getSharedPreferences(context)
        uiController.onCreateHost(host)
    }

    @After
    fun tearDown() {
        uiController.onDestroyHost(host)
        prefs.edit().remove(PREF_DASHBOARD_TUTORIAL_SHOWN).apply()
    }

    @Test
    fun testUpdatePaddingSize() {
        val paddingDefault = Int.MAX_VALUE
        val layout = LinearLayout(context)
        layout.setPadding(paddingDefault, paddingDefault, paddingDefault, paddingDefault)
        uiController.updatePaddingSize(context, layout, UNSUPPORTED)
        verifyViewPadding(layout, paddingDefault, paddingDefault, paddingDefault, paddingDefault)
        val paddingSize = context.resources.getDimensionPixelSize(R.dimen.tb_icon_size)
        uiController.updatePaddingSize(context, layout, POSITION_TOP_VERTICAL_LEFT)
        verifyViewPadding(layout, paddingSize, 0, 0, 0)
        uiController.updatePaddingSize(context, layout, POSITION_BOTTOM_VERTICAL_LEFT)
        verifyViewPadding(layout, paddingSize, 0, 0, 0)
        uiController.updatePaddingSize(context, layout, POSITION_TOP_LEFT)
        verifyViewPadding(layout, 0, paddingSize, 0, 0)
        uiController.updatePaddingSize(context, layout, POSITION_TOP_RIGHT)
        verifyViewPadding(layout, 0, paddingSize, 0, 0)
        uiController.updatePaddingSize(context, layout, POSITION_TOP_VERTICAL_RIGHT)
        verifyViewPadding(layout, 0, 0, paddingSize, 0)
        uiController.updatePaddingSize(context, layout, POSITION_BOTTOM_VERTICAL_RIGHT)
        verifyViewPadding(layout, 0, 0, paddingSize, 0)
        uiController.updatePaddingSize(context, layout, POSITION_BOTTOM_LEFT)
        verifyViewPadding(layout, 0, 0, 0, paddingSize)
        uiController.updatePaddingSize(context, layout, POSITION_BOTTOM_RIGHT)
        verifyViewPadding(layout, 0, 0, 0, paddingSize)
    }

    @Test
    fun testShouldSendDisappearingBroadcast() {
        val helper = PowerMockito.mock(LauncherHelper::class.java)
        val isOnSecondaryHomeScreenAnswer = BooleanAnswer()
        PowerMockito.`when`(helper.isOnSecondaryHomeScreen(context))
                .thenAnswer(isOnSecondaryHomeScreenAnswer)
        PowerMockito.mockStatic(LauncherHelper::class.java)
        PowerMockito.`when`(LauncherHelper.getInstance()).thenReturn(helper)
        isOnSecondaryHomeScreenAnswer.answer = true
        prefs.edit().putBoolean(PREF_DONT_STOP_DASHBOARD, true).apply()
        Assert.assertFalse(uiController.shouldSendDisappearingBroadcast(context, prefs))
        isOnSecondaryHomeScreenAnswer.answer = true
        prefs.edit().putBoolean(PREF_DONT_STOP_DASHBOARD, false).apply()
        Assert.assertTrue(uiController.shouldSendDisappearingBroadcast(context, prefs))
        isOnSecondaryHomeScreenAnswer.answer = false
        prefs.edit().putBoolean(PREF_DONT_STOP_DASHBOARD, true).apply()
        Assert.assertTrue(uiController.shouldSendDisappearingBroadcast(context, prefs))
        isOnSecondaryHomeScreenAnswer.answer = false
        prefs.edit().putBoolean(PREF_DONT_STOP_DASHBOARD, false).apply()
        Assert.assertTrue(uiController.shouldSendDisappearingBroadcast(context, prefs))
    }

    @Test
    fun testSaveWidgetInfo() {
        val info = AppWidgetProviderInfo()
        info.provider = ComponentName(TEST_PACKAGE, TEST_NAME)
        val cellId: Int = DEFAULT_TEST_CELL_ID
        val appWidgetId = 100
        prefs.edit().putString(uiController.generateProviderPlaceholderPrefKey(cellId), "").apply()
        uiController.saveWidgetInfo(context, info, cellId, appWidgetId)
        Assert.assertEquals(
                appWidgetId.toLong(),
                prefs.getInt(PREF_DASHBOARD_WIDGET_PREFIX + cellId, -1).toLong())
        Assert.assertEquals(
                info.provider.flattenToString(),
                prefs.getString(uiController.generateProviderPrefKey(cellId), "")
        )
        Assert.assertFalse(prefs.contains(uiController.generateProviderPlaceholderPrefKey(cellId)))
    }

    @Test
    fun testShowDashboardTutorialToast() {
        prefs.edit().putBoolean(PREF_DASHBOARD_TUTORIAL_SHOWN, true).apply()
        uiController.showDashboardTutorialToast(context)
        Assert.assertNull(ShadowToast.getTextOfLatestToast())
        prefs.edit().putBoolean(PREF_DASHBOARD_TUTORIAL_SHOWN, false).apply()
        uiController.showDashboardTutorialToast(context)
        Assert.assertTrue(prefs.getBoolean(PREF_DASHBOARD_TUTORIAL_SHOWN, false))
        val toastText = ShadowToast.getTextOfLatestToast()
        Assert.assertEquals(context.getString(R.string.tb_dashboard_tutorial, toastText), toastText)
    }

    @Test
    fun testGenerateProviderPrefKey() {
        Assert.assertEquals(
                PREF_DASHBOARD_WIDGET_PREFIX +
                        DEFAULT_TEST_CELL_ID +
                        PREF_DASHBOARD_WIDGET_PROVIDER_SUFFIX,
                uiController.generateProviderPrefKey(DEFAULT_TEST_CELL_ID)
        )
    }

    @Test
    fun testGenerateProviderPlaceholderPrefKey() {
        Assert.assertEquals(
                PREF_DASHBOARD_WIDGET_PREFIX +
                        DEFAULT_TEST_CELL_ID +
                        PREF_DASHBOARD_WIDGET_PLACEHOLDER_SUFFIX,
                uiController.generateProviderPlaceholderPrefKey(DEFAULT_TEST_CELL_ID)
        )
    }

    @Test
    fun testShowPlaceholderToast() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val cellId: Int = DEFAULT_TEST_CELL_ID
        val providerPrefKey = uiController.generateProviderPrefKey(cellId)
        val shadowAppWidgetManager = Shadows.shadowOf(appWidgetManager)
        val providerInfo = ActivityInfo()
        providerInfo.nonLocalizedLabel = TEST_LABEL
        val info = AppWidgetProviderInfoBuilder.newBuilder().setProviderInfo(providerInfo).build()
        info.provider = ComponentName(TEST_PACKAGE, TEST_NAME)
        shadowAppWidgetManager.addInstalledProvidersForProfile(Process.myUserHandle(), info)
        prefs.edit().putString(providerPrefKey, null).apply()
        uiController.showPlaceholderToast(context, appWidgetManager, cellId, prefs)
        Assert.assertNull(ShadowToast.getLatestToast())
        prefs.edit().putString(providerPrefKey, PREF_DEFAULT_NULL).apply()
        uiController.showPlaceholderToast(context, appWidgetManager, cellId, prefs)
        Assert.assertNull(ShadowToast.getLatestToast())
        prefs
                .edit()
                .putString(providerPrefKey, info.provider.flattenToString() + UNSUPPORTED)
                .apply()
        uiController.showPlaceholderToast(context, appWidgetManager, cellId, prefs)
        Assert.assertNull(ShadowToast.getLatestToast())
        prefs.edit().putString(providerPrefKey, info.provider.flattenToString()).apply()
        uiController.showPlaceholderToast(context, appWidgetManager, cellId, prefs)
        val lastToast = ShadowToast.getTextOfLatestToast()
        Assert.assertNotNull(lastToast)
        val expectedText = context.getString(R.string.tb_widget_restore_toast, TEST_LABEL)
        Assert.assertEquals(expectedText, lastToast)
    }

    private fun verifyViewPadding(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        Assert.assertEquals(left.toLong(), view.paddingLeft.toLong())
        Assert.assertEquals(top.toLong(), view.paddingTop.toLong())
        Assert.assertEquals(right.toLong(), view.paddingRight.toLong())
        Assert.assertEquals(bottom.toLong(), view.paddingBottom.toLong())
    }
}
