package com.farmerbb.taskbar.util

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.ActivityOptions
import android.app.AlertDialog
import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.helper.FreeformHackHelper
import com.farmerbb.taskbar.mockito.BooleanAnswer
import com.farmerbb.taskbar.mockito.IntAnswer
import com.farmerbb.taskbar.service.PowerMenuService
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.rule.PowerMockRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSettings
import org.robolectric.shadows.ShadowToast
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@PowerMockIgnore("org.mockito.*", "org.robolectric.*", "android.*", "androidx.*")
@PrepareForTest(U::class)
@LooperMode(LooperMode.Mode.LEGACY)
class UTest {
    @get:Rule
    val rule = PowerMockRule()
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Assert.assertNotNull(context)
    }

    @Test
    @Throws(Exception::class)
    fun testShowPermissionDialogWithAndroidTVSettings() {
        testShowPermissionDialog(
                true,
                context.resources.getString(
                        R.string.tb_permission_dialog_message, U.getAppName(context)) +
                context.resources.getString(
                        R.string.tb_permission_dialog_instructions_tv, U.getAppName(context)),
                R.string.tb_action_open_settings
        )
    }

    @Test
    @Throws(Exception::class)
    fun testShowPermissionDialogNormal() {
        testShowPermissionDialog(
                false,
                context.resources.getString(
                        R.string.tb_permission_dialog_message, U.getAppName(context)) +
                context.resources.getString(
                        R.string.tb_permission_dialog_instructions_phone),
                R.string.tb_action_grant_permission
        )
    }

    @Throws(Exception::class)
    private fun testShowPermissionDialog(
        hasAndroidTVSettings: Boolean,
        message: String,
        buttonTextResId: Int
    ) {
        val onError = RunnableHooker()
        val onFinish = RunnableHooker()
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`<Any>(U::class.java, "hasAndroidTVSettings", context)
                .thenReturn(hasAndroidTVSettings)
        val dialog = U.showPermissionDialog(context, Callbacks(onError, onFinish))
        val shadowDialog = Shadows.shadowOf(dialog)
        val resources = context.resources
        Assert.assertEquals(
                resources.getString(R.string.tb_permission_dialog_title),
                shadowDialog.title
        )
        Assert.assertEquals(message, shadowDialog.message)
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        Assert.assertEquals(resources.getString(buttonTextResId), positiveButton.text)
        Assert.assertFalse(shadowDialog.isCancelable)
        positiveButton.performClick()
        Assert.assertTrue(onFinish.hasRun())
        Assert.assertFalse(onError.hasRun())
    }

    @Test
    fun testShowErrorDialog() {
        val onFinish = RunnableHooker()
        val appOpCommand = "app-op-command"
        val dialog = ReflectionHelpers.callStaticMethod<AlertDialog>(
                U::class.java,
                "showErrorDialog",
                ClassParameter.from(Context::class.java, context),
                ClassParameter.from(String::class.java, appOpCommand),
                ClassParameter.from(Callbacks::class.java, Callbacks(null, onFinish))
        )
        val shadowDialog = Shadows.shadowOf(dialog)
        val resources = context.resources
        Assert.assertEquals(
                resources.getString(R.string.tb_error_dialog_title),
                shadowDialog.title
        )
        Assert.assertEquals(
                resources.getString(
                        R.string.tb_error_dialog_message,
                        context.packageName,
                        appOpCommand
                ),
                shadowDialog.message
        )
        val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        Assert.assertEquals(resources.getString(R.string.tb_action_ok), button.text)
        Assert.assertFalse(shadowDialog.isCancelable)
        button.performClick()
        Assert.assertTrue(onFinish.hasRun())
    }

    @Test
    fun testSendAccessibilityActionWithServiceNotEnabledAndGrantedPermission() {
        testSendAccessibilityAction(false, true, true)
    }

    @Test
    fun testSendAccessibilityActionWithServiceEnabled() {
        testSendAccessibilityAction(true, false, true)
    }

    @Test
    fun testSendAccessibilityActionWithServiceNotEnabledAndWithoutPermission() {
        testSendAccessibilityAction(false, false, false)
    }

    private fun testSendAccessibilityAction(
        serviceEnabled: Boolean,
        hasPermission: Boolean,
        hasRun: Boolean
    ) {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.isAccessibilityServiceEnabled(context)).thenReturn(serviceEnabled)
        PowerMockito.`when`(U.hasWriteSecureSettingsPermission(context)).thenReturn(hasPermission)
        val onComplete = RunnableHooker()
        U.sendAccessibilityAction(
                context, AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, onComplete
        )
        // Run all delayed message.
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        Assert.assertEquals(hasRun, onComplete.hasRun())
    }

    @Test
    fun testIsAccessibilityServiceEnabled() {
        val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val componentName = ComponentName(context, PowerMenuService::class.java)
        val flattenString = componentName.flattenToString()
        val flattenShortString = componentName.flattenToShortString()
        val newEnabledService =
                enabledServices
                        ?.replace(":" + flattenString.toRegex(), "")
                        ?.replace(":" + flattenShortString.toRegex(), "")
                        ?.replace(flattenString.toRegex(), "")
                        ?.replace(flattenShortString.toRegex(), "")
                        ?: ""
        Settings.Secure.putString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                newEnabledService
        )
        Assert.assertFalse(U.isAccessibilityServiceEnabled(context))
        Settings.Secure.putString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                "$newEnabledService:$flattenString"
        )
        Assert.assertTrue(U.isAccessibilityServiceEnabled(context))
        Settings.Secure.putString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                "$newEnabledService:$flattenShortString"
        )
        Assert.assertTrue(U.isAccessibilityServiceEnabled(context))
        Settings.Secure.putString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                enabledServices
        )
    }

    @Test
    fun testHasWriteSecureSettingsPermissionForMarshmallowAndAboveVersion() {
        Assert.assertFalse(U.hasWriteSecureSettingsPermission(context))
        val application = ApplicationProvider.getApplicationContext<Application>()
        val shadowApplication = Shadows.shadowOf(application)
        shadowApplication.grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        Assert.assertTrue(U.hasWriteSecureSettingsPermission(context))
    }

    @Test
    @Config(sdk = [21])
    fun testHasWriteSecureSettingsPermissionVersionBelowMarshmallow() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val shadowApplication = Shadows.shadowOf(application)
        shadowApplication.grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        Assert.assertFalse(U.hasWriteSecureSettingsPermission(context))
    }

    @Test
    fun testShowToast() {
        U.showToast(context, R.string.tb_pin_shortcut_not_supported)
        val toast = ShadowToast.getLatestToast()
        Assert.assertEquals(Toast.LENGTH_SHORT.toLong(), toast.duration.toLong())
        Assert.assertEquals(
                context.resources.getString(R.string.tb_pin_shortcut_not_supported),
                ShadowToast.getTextOfLatestToast()
        )
    }

    @Test
    fun testShowLongToast() {
        U.showToastLong(context, R.string.tb_pin_shortcut_not_supported)
        val toast = ShadowToast.getLatestToast()
        Assert.assertEquals(Toast.LENGTH_LONG.toLong(), toast.duration.toLong())
        Assert.assertEquals(
                context.resources.getString(R.string.tb_pin_shortcut_not_supported),
                ShadowToast.getTextOfLatestToast()
        )
    }

    @Test
    fun testCancelToast() {
        U.showToastLong(context, R.string.tb_pin_shortcut_not_supported)
        val shadowToast = Shadows.shadowOf(ShadowToast.getLatestToast())
        Assert.assertFalse(shadowToast.isCancelled)
        U.cancelToast()
        Assert.assertTrue(shadowToast.isCancelled)
    }

    @Test
    fun testCanEnableFreeformWithNougatAndAboveVersion() {
        Assert.assertTrue(U.canEnableFreeform(context))
    }

    @Test
    @Config(sdk = [23])
    fun testCanEnableFreeformWithMarshmallowAndBelowVersion() {
        Assert.assertFalse(U.canEnableFreeform(context))
    }

    @Test
    fun testHasFreeformSupportWithoutFreeformEnabled() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.canEnableFreeform(context)).thenReturn(false)
        Assert.assertFalse(U.canEnableFreeform(context))
    }

    @Test
    fun testHasFreeformSupportWithFreeformEnabledAndNMR1AboveVersion() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.canEnableFreeform(context)).thenReturn(true)
        Assert.assertFalse(U.hasFreeformSupport(context))
        // Case 1, system has feature freeform.
        val packageManager = context.packageManager
        val shadowPackageManager = Shadows.shadowOf(packageManager)
        shadowPackageManager
                .setSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT, true)
        Assert.assertTrue(U.hasFreeformSupport(context))
        shadowPackageManager
                .setSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT, false)
        // Case 2, enable_freeform_support in Settings.Global is not 0
        Settings.Global.putInt(context.contentResolver, "enable_freeform_support", 1)
        Assert.assertTrue(U.hasFreeformSupport(context))
        Settings.Global.putInt(context.contentResolver, "enable_freeform_support", 0)
    }

    @Test
    @Config(sdk = [25])
    fun testHasFreeformSupportWithFreeformEnabledAndNMR1AndBelowVersion() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.canEnableFreeform(context)).thenReturn(true)
        Assert.assertFalse(U.hasFreeformSupport(context))
        // Case 3, version is less than or equal to N_MRI, and force_resizable_activities
        // in Settings.Global is not 0
        Settings.Global.putInt(context.contentResolver, "force_resizable_activities", 1)
        Assert.assertTrue(U.hasFreeformSupport(context))
        Settings.Global.putInt(context.contentResolver, "force_resizable_activities", 0)
    }

    @Test
    fun testCanBootToFreeform() {
        PowerMockito.spy(U::class.java)
        val hasFreeformSupportAnswer = BooleanAnswer()
        val isOverridingFreeformHackAnswer = BooleanAnswer()
        PowerMockito.`when`(U.hasFreeformSupport(context)).thenAnswer(hasFreeformSupportAnswer)
        PowerMockito.`when`(U.isOverridingFreeformHack(context, true))
                .thenAnswer(isOverridingFreeformHackAnswer)
        // Case 1, all return true
        hasFreeformSupportAnswer.answer = true
        isOverridingFreeformHackAnswer.answer = true
        Assert.assertFalse(U.canBootToFreeform(context))
        // Case 2, true, false
        hasFreeformSupportAnswer.answer = true
        isOverridingFreeformHackAnswer.answer = false
        Assert.assertTrue(U.canBootToFreeform(context))
        // Case 3, false, true
        hasFreeformSupportAnswer.answer = false
        isOverridingFreeformHackAnswer.answer = true
        Assert.assertFalse(U.canBootToFreeform(context))
        // Case 4, false, false
        hasFreeformSupportAnswer.answer = false
        isOverridingFreeformHackAnswer.answer = false
        Assert.assertFalse(U.canBootToFreeform(context))
    }

    @Test
    fun testIsSamsungDevice() {
        ShadowBuild.setManufacturer("Samsung")
        Assert.assertTrue(U.isSamsungDevice())
        ShadowBuild.setManufacturer("samsung")
        Assert.assertTrue(U.isSamsungDevice())
        ShadowBuild.setManufacturer("UnSamsung")
        Assert.assertFalse(U.isSamsungDevice())
    }

    @Test
    fun testGetBackgroundTint() {
        val prefs = U.getSharedPreferences(context)
        prefs.edit()
                .putInt(Constants.PREF_BACKGROUND_TINT, Color.GREEN)
                .putBoolean(Constants.PREF_SHOW_BACKGROUND, false)
                .apply()
        // If the SHOW_BACKGROUND is false, it use transparent to replace origin tint.
        Assert.assertEquals(Color.TRANSPARENT.toLong(), U.getBackgroundTint(context).toLong())
        prefs.edit()
                .putInt(Constants.PREF_BACKGROUND_TINT, Color.GREEN)
                .apply()
        Assert.assertEquals(Color.GREEN.toLong(), U.getBackgroundTint(context).toLong())
        prefs.edit().remove(Constants.PREF_BACKGROUND_TINT).apply()
        Assert.assertEquals(
                context.resources.getInteger(R.integer.tb_translucent_gray).toLong(),
                U.getBackgroundTint(context).toLong()
        )
    }

    @Test
    fun testAccentColor() {
        val prefs = U.getSharedPreferences(context)
        prefs.edit().remove(Constants.PREF_ACCENT_COLOR).apply()
        Assert.assertEquals(
                context.resources.getInteger(R.integer.tb_translucent_white).toLong(),
                U.getAccentColor(context).toLong()
        )
        prefs.edit().putInt(Constants.PREF_ACCENT_COLOR, Color.GREEN).apply()
        Assert.assertEquals(Color.GREEN.toLong(), U.getAccentColor(context).toLong())
    }

    @Test
    fun testCanDrawOverlaysWithMarshmallowAndAboveVersion() {
        ShadowSettings.setCanDrawOverlays(true)
        Assert.assertTrue(U.canDrawOverlays(context))
        ShadowSettings.setCanDrawOverlays(false)
        Assert.assertFalse(U.canDrawOverlays(context))
    }

    @Test
    @Config(sdk = [22])
    fun testCanDrawOverlaysWithMarshmallowBelowVersion() {
        Assert.assertTrue(U.canDrawOverlays(context))
    }

    @Test
    fun testIsGame() {
        // We only test for un-support launching games fullscreen, because of
        // we don't have a good method to test code with ApplicationInfo.
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putBoolean(Constants.PREF_LAUNCH_GAMES_FULLSCREEN, false).apply()
        Assert.assertFalse(U.isGame(context, context.packageName))
        prefs.edit().putBoolean(Constants.PREF_LAUNCH_GAMES_FULLSCREEN, true).apply()
        Assert.assertFalse(U.isGame(context, context.packageName))
        Assert.assertFalse(U.isGame(context, context.packageName + "un-exist-package"))
    }

    @Test
    fun testGetActivityOptionsWithQAndAboveVersion() {
        testGetActivityOptions(0, 5, 1, 1)
    }

    @Test
    @Config(sdk = [28])
    fun testGetActivityOptionsWithP() {
        // The stack id isn't changed from the default on Chrome OS with Android P
        val stackId = getActivityOptionsStackId(ActivityOptions.makeBasic())
        testGetActivityOptions(0, 5, 1, stackId)
    }

    @Test
    @Config(sdk = [27])
    fun testGetActivityOptionsWithPBelowVersion() {
        testGetActivityOptions(-1, 2, -1, -1)
    }

    private fun testGetActivityOptions(
        defaultStackId: Int,
        freeformStackId: Int,
        stackIdWithoutBrokenApi: Int,
        chromeOsStackId: Int
    ) {
        PowerMockito.spy(U::class.java)
        val hasBrokenSetLaunchBoundsApiAnswer = BooleanAnswer()
        val isChromeOsAnswer = BooleanAnswer()
        PowerMockito.`when`(U.hasBrokenSetLaunchBoundsApi())
                .thenAnswer(hasBrokenSetLaunchBoundsApiAnswer)
        PowerMockito.`when`(U.isChromeOs(context)).thenAnswer(isChromeOsAnswer)
        val originFreeformHackActive = FreeformHackHelper.getInstance().isFreeformHackActive
        checkActivityOptionsStackIdForNonContextMenu(
                context, null, false, defaultStackId
        )
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.APP_PORTRAIT, false, 1
        )
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.APP_PORTRAIT, true, freeformStackId
        )
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.APP_LANDSCAPE, false, 1
        )
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.APP_LANDSCAPE, true, freeformStackId
        )
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.APP_FULLSCREEN, false, 1
        )
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.FREEFORM_HACK, false, freeformStackId
        )
        FreeformHackHelper.getInstance().isFreeformHackActive = originFreeformHackActive
        hasBrokenSetLaunchBoundsApiAnswer.answer = true
        checkActivityOptionsStackIdForContextMenu(context, 1)
        hasBrokenSetLaunchBoundsApiAnswer.answer = false
        isChromeOsAnswer.answer = false
        checkActivityOptionsStackIdForContextMenu(context, stackIdWithoutBrokenApi)
        isChromeOsAnswer.answer = true
        checkActivityOptionsStackIdForContextMenu(context, chromeOsStackId)
    }

    private fun checkActivityOptionsStackIdForContextMenu(
        context: Context?,
        stackId: Int
    ) {
        val options = U.getActivityOptions(context, ApplicationType.CONTEXT_MENU, null)
        Assert.assertEquals(stackId.toLong(), getActivityOptionsStackId(options).toLong())
    }

    private fun checkActivityOptionsStackIdForNonContextMenu(
        context: Context?,
        applicationType: ApplicationType?,
        isFreeformHackActive: Boolean,
        stackId: Int
    ) {
        FreeformHackHelper.getInstance().isFreeformHackActive = isFreeformHackActive
        val options = U.getActivityOptions(context, applicationType, null)
        Assert.assertEquals(stackId.toLong(), getActivityOptionsStackId(options).toLong())
    }

    private fun getActivityOptionsStackId(options: ActivityOptions): Int {
        val methodName: String
        methodName = if (U.getCurrentApiVersion() >= 28.0f) {
            "getLaunchWindowingMode"
        } else {
            "getLaunchStackId"
        }
        return ReflectionHelpers.callInstanceMethod(options, methodName)
    }

    @Test
    fun testIsChromeOs() {
        val packageManager = context.packageManager
        val shadowPackageManager = Shadows.shadowOf(packageManager)
        shadowPackageManager.setSystemFeature("org.chromium.arc", true)
        Assert.assertTrue(U.isChromeOs(context))
        shadowPackageManager.setSystemFeature("org.chromium.arc", false)
        Assert.assertFalse(U.isChromeOs(context))
    }

    @Test
    @Config(qualifiers = "sw720dp")
    fun testGetBaseTaskbarSizeWithSW720dp() {
        PowerMockito.spy(U::class.java)
        val isSystemTrayEnabledAnswer = BooleanAnswer()
        PowerMockito.`when`(U.isSystemTrayEnabled(context)).thenAnswer(isSystemTrayEnabledAnswer)
        isSystemTrayEnabledAnswer.answer = false
        // The only difference of the different screen size, is the initial taskbar size.
        // So we only test the different in this test method.
        var initialSize = context.resources.getDimension(R.dimen.tb_base_size_start_plus_divider)
        initialSize += context.resources.getDimension(R.dimen.tb_base_size_collapse_button)
        initialSize += context.resources.getDimension(R.dimen.tb_dashboard_button_size)
        Assert.assertEquals(initialSize, U.getBaseTaskbarSize(context), 0f)
    }

    @Test
    fun testGetBaseTaskbarSizeWithNormalDimension() {
        PowerMockito.spy(U::class.java)
        val isSystemTrayEnabledAnswer = BooleanAnswer()
        PowerMockito.`when`(U.isSystemTrayEnabled(context)).thenAnswer(isSystemTrayEnabledAnswer)
        isSystemTrayEnabledAnswer.answer = false
        var initialSize = context.resources.getDimension(R.dimen.tb_base_size_start_plus_divider)
        initialSize += context.resources.getDimension(R.dimen.tb_base_size_collapse_button)
        Assert.assertEquals(initialSize, U.getBaseTaskbarSize(context), 0f)
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putBoolean(Constants.PREF_DASHBOARD, true).apply()
        val dashboardButtonSize = context.resources.getDimension(R.dimen.tb_dashboard_button_size)
        Assert.assertEquals(initialSize + dashboardButtonSize,
                U.getBaseTaskbarSize(context), 0f)
        prefs.edit().remove(Constants.PREF_DASHBOARD).apply()
        val navbarButtonsMargin = context.resources.getDimension(R.dimen.tb_navbar_buttons_margin)
        val iconSize = context.resources.getDimension(R.dimen.tb_icon_size)
        prefs.edit().putBoolean(Constants.PREF_BUTTON_BACK, true).apply()
        Assert.assertEquals(
                initialSize + navbarButtonsMargin + iconSize,
                U.getBaseTaskbarSize(context), 0f)
        prefs.edit().remove(Constants.PREF_BUTTON_BACK).apply()
        prefs.edit().putBoolean(Constants.PREF_BUTTON_HOME, true).apply()
        Assert.assertEquals(
                initialSize + navbarButtonsMargin + iconSize,
                U.getBaseTaskbarSize(context), 0f)
        prefs.edit().remove(Constants.PREF_BUTTON_HOME).apply()
        prefs.edit().putBoolean(Constants.PREF_BUTTON_RECENTS, true).apply()
        Assert.assertEquals(
                initialSize + navbarButtonsMargin + iconSize,
                U.getBaseTaskbarSize(context), 0f)
        prefs.edit().remove(Constants.PREF_BUTTON_RECENTS).apply()
        isSystemTrayEnabledAnswer.answer = true
        val systemTraySize = context.resources.getDimension(R.dimen.tb_systray_size)
        Assert.assertEquals(initialSize + systemTraySize,
                U.getBaseTaskbarSize(context), 0f)
    }

    @Test
    fun testInitPrefsForBlissOS() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.isBlissOs(ArgumentMatchers.any(Context::class.java)))
                .thenReturn(true)
        Assert.assertTrue(U.isBlissOs(context))
        val prefs = U.getSharedPreferences(context)
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_BLISS_OS_PREFS, false))
        U.initPrefs(context)
        Assert.assertEquals(
                Constants.PREF_RECENTS_AMOUNT_RUNNING_APPS_ONLY,
                prefs.getString(Constants.PREF_RECENTS_AMOUNT, "")
        )
        Assert.assertEquals("0",
                prefs.getString(Constants.PREF_REFRESH_FREQUENCY, ""))
        Assert.assertEquals("2147483647",
                prefs.getString(Constants.PREF_MAX_NUM_OF_RECENTS, ""))
        Assert.assertEquals("true",
                prefs.getString(Constants.PREF_SORT_ORDER, ""))
        Assert.assertEquals(
                Constants.PREF_START_BUTTON_IMAGE_APP_LOGO,
                prefs.getString(Constants.PREF_START_BUTTON_IMAGE, "")
        )
        Assert.assertTrue(prefs.getBoolean(Constants.PREF_BUTTON_BACK, false))
        Assert.assertTrue(prefs.getBoolean(Constants.PREF_BUTTON_HOME, false))
        Assert.assertTrue(prefs.getBoolean(Constants.PREF_BUTTON_RECENTS, false))
        Assert.assertTrue(prefs.getBoolean(Constants.PREF_AUTO_HIDE_NAVBAR, false))
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_SHORTCUT_ICON, true))
        Assert.assertTrue(prefs.getBoolean(Constants.PREF_BLISS_OS_PREFS, false))
        prefs.edit().putBoolean(Constants.PREF_BLISS_OS_PREFS, false)
    }

    @Test
    fun testInitPrefsForNormalWithCanEnableFreeformAndHackOverrideFalse() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.canEnableFreeform(context)).thenReturn(true)
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putBoolean(Constants.PREF_FREEFORM_HACK_OVERRIDE, false).apply()
        U.initPrefs(context)
        Assert.assertEquals(
                U.hasFreeformSupport(context) && !U.isSamsungDevice(),
                prefs.getBoolean(Constants.PREF_FREEFORM_HACK, false)
        )
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_SAVE_WINDOW_SIZES, true))
        Assert.assertTrue(prefs.getBoolean(Constants.PREF_FREEFORM_HACK_OVERRIDE, false))
    }

    @Test
    fun testInitPrefsForNormalWithCanEnableFreeformAndHackOverrideTrueButNoSupport() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.canEnableFreeform(context)).thenReturn(true)
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putBoolean(Constants.PREF_FREEFORM_HACK_OVERRIDE, true).apply()
        PowerMockito.`when`(U.hasFreeformSupport(context)).thenReturn(false)
        U.initPrefs(context)
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_FREEFORM_HACK, false))
    }

    @Test
    fun testInitPrefsForNormalWithCantEnableFreeform() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.canEnableFreeform(context)).thenReturn(false)
        val prefs = U.getSharedPreferences(context)
        U.initPrefs(context)
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_FREEFORM_HACK, false))
        prefs.edit()
                .putBoolean(Constants.PREF_FREEFORM_HACK, false)
                .putBoolean(Constants.PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false)
                .apply()
        U.initPrefs(context)
        Assert.assertFalse(prefs.getBoolean(
                Constants.PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false))
        prefs.edit()
                .putBoolean(Constants.PREF_FREEFORM_HACK, true)
                .putBoolean(Constants.PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false)
                .apply()
        U.initPrefs(context)
        Assert.assertTrue(prefs.getBoolean(
                Constants.PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false))
        prefs.edit()
                .putBoolean(Constants.PREF_FREEFORM_HACK, false)
                .putBoolean(Constants.PREF_SHOW_FREEFORM_DISABLED_MESSAGE, true)
                .apply()
        U.initPrefs(context)
        Assert.assertTrue(prefs.getBoolean(
                Constants.PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false))
        prefs.edit()
                .putBoolean(Constants.PREF_FREEFORM_HACK, true)
                .putBoolean(Constants.PREF_SHOW_FREEFORM_DISABLED_MESSAGE, true)
                .apply()
        U.initPrefs(context)
        Assert.assertTrue(prefs.getBoolean(
                Constants.PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false))
    }

    @Test
    fun testIsOverridingFreeformHackForPAndAboveVersion() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.isChromeOs(context)).thenReturn(false)
        // Check preferences
        Assert.assertFalse(U.isOverridingFreeformHack(context, true))
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putBoolean(Constants.PREF_FREEFORM_HACK, true).apply()
        Assert.assertTrue(U.isOverridingFreeformHack(context, true))
        prefs.edit().remove(Constants.PREF_FREEFORM_HACK).apply()

        // Don't check preferences
        Assert.assertTrue(U.isOverridingFreeformHack(context, false))
    }

    @Test
    @Config(sdk = [27])
    fun testIsOverridingFreeformHackForPBelowVersion() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.isChromeOs(context)).thenReturn(false)
        // Check preferences
        Assert.assertFalse(U.isOverridingFreeformHack(context, true))
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putBoolean(Constants.PREF_FREEFORM_HACK, true).apply()
        Assert.assertFalse(U.isOverridingFreeformHack(context, true))
        prefs.edit().remove(Constants.PREF_FREEFORM_HACK).apply()

        // Don't check preferences
        Assert.assertFalse(U.isOverridingFreeformHack(context, false))
    }

    @Test
    fun testIsOverridingFreeformHackForChromeOS() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.isChromeOs(context)).thenReturn(true)
        Assert.assertFalse(U.isOverridingFreeformHack(context, true))
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putBoolean(Constants.PREF_FREEFORM_HACK, true).apply()
        Assert.assertTrue(U.isOverridingFreeformHack(context, true))
        prefs.edit().remove(Constants.PREF_FREEFORM_HACK).apply()
    }

    @Test
    @Config(sdk = [28])
    fun testIsOverridingFreeformHackForChromeOSApi28() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.isChromeOs(context)).thenReturn(true)
        // Check preferences
        Assert.assertFalse(U.isOverridingFreeformHack(context, true))
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putBoolean(Constants.PREF_FREEFORM_HACK, true).apply()
        // The default PREF_CHROME_OS_CONTEXT_MENU_FIX is true
        Assert.assertTrue(U.isOverridingFreeformHack(context, true))
        prefs.edit().putBoolean(Constants.PREF_CHROME_OS_CONTEXT_MENU_FIX, false).apply()
        Assert.assertFalse(U.isOverridingFreeformHack(context, true))
        prefs.edit().putBoolean(Constants.PREF_CHROME_OS_CONTEXT_MENU_FIX, true).apply()
        Assert.assertTrue(U.isOverridingFreeformHack(context, true))
        prefs.edit().remove(Constants.PREF_FREEFORM_HACK).apply()
        prefs.edit().remove(Constants.PREF_CHROME_OS_CONTEXT_MENU_FIX).apply()

        // Don't check preferences
        Assert.assertTrue(U.isOverridingFreeformHack(context, false))
        prefs.edit().putBoolean(Constants.PREF_CHROME_OS_CONTEXT_MENU_FIX, false).apply()
        Assert.assertFalse(U.isOverridingFreeformHack(context, false))
        prefs.edit().putBoolean(Constants.PREF_CHROME_OS_CONTEXT_MENU_FIX, true).apply()
        Assert.assertTrue(U.isOverridingFreeformHack(context, false))
        prefs.edit().remove(Constants.PREF_CHROME_OS_CONTEXT_MENU_FIX).apply()
    }

    @Test
    @Config(sdk = [25])
    fun testHasBrokenSetLaunchBoundsApiForApi25() {
        Assert.assertFalse(U.hasBrokenSetLaunchBoundsApi())
    }

    @Test
    @Config(sdk = [26])
    @Throws(Exception::class)
    fun testHasBrokenSetLaunchBoundsApiForApi26() {
        testHasBrokenSetLaunchBoundsApiWithValidApiVersion()
    }

    @Test
    @Config(sdk = [27])
    @Throws(Exception::class)
    fun testHasBrokenSetLaunchBoundsApiForApi27() {
        testHasBrokenSetLaunchBoundsApiWithValidApiVersion()
        testHasBrokenSetLaunchBoundsApiWithValidApiVersion()
    }

    @Test
    @Config(sdk = [28])
    fun testHasBrokenSetLaunchBoundsApiForApi28() {
        Assert.assertFalse(U.hasBrokenSetLaunchBoundsApi())
    }

    @Throws(Exception::class)
    private fun testHasBrokenSetLaunchBoundsApiWithValidApiVersion() {
        PowerMockito.spy(U::class.java)
        val isSamsungDeviceAnswer = BooleanAnswer()
        val isNvidiaDevice = BooleanAnswer()
        PowerMockito.`when`(U.isSamsungDevice()).thenAnswer(isSamsungDeviceAnswer)
        PowerMockito.`when`<Any>(U::class.java, "isNvidiaDevice")
                .thenAnswer(isNvidiaDevice)
        isSamsungDeviceAnswer.answer = false
        isNvidiaDevice.answer = false
        Assert.assertTrue(U.hasBrokenSetLaunchBoundsApi())
        isSamsungDeviceAnswer.answer = false
        isNvidiaDevice.answer = true
        Assert.assertFalse(U.hasBrokenSetLaunchBoundsApi())
        isSamsungDeviceAnswer.answer = true
        isNvidiaDevice.answer = false
        Assert.assertFalse(U.hasBrokenSetLaunchBoundsApi())
        isSamsungDeviceAnswer.answer = true
        isNvidiaDevice.answer = true
        Assert.assertFalse(U.hasBrokenSetLaunchBoundsApi())
    }

    @Test
    fun testWrapContext() {
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putString(Constants.PREF_THEME, "light").apply()
        var newContext = U.wrapContext(context)
        var themeResource = ReflectionHelpers.getField<Int>(newContext, "mThemeResource")
        Assert.assertNotNull(themeResource)
        Assert.assertEquals(R.style.Taskbar.toLong(), (themeResource as Int).toLong())
        prefs.edit().putString(Constants.PREF_THEME, "dark").apply()
        newContext = U.wrapContext(context)
        themeResource = ReflectionHelpers.getField(newContext, "mThemeResource")
        Assert.assertNotNull(themeResource)
        Assert.assertEquals(R.style.Taskbar_Dark.toLong(), (themeResource as Int).toLong())
        prefs.edit().putString(Constants.PREF_THEME, "non-support").apply()
        newContext = U.wrapContext(context)
        Assert.assertTrue(newContext is ContextThemeWrapper)
        prefs.edit().remove(Constants.PREF_THEME).apply()
        newContext = U.wrapContext(context)
        themeResource = ReflectionHelpers.getField(newContext, "mThemeResource")
        Assert.assertNotNull(themeResource)
        Assert.assertEquals(R.style.Taskbar.toLong(), (themeResource as Int).toLong())
    }

    @Test
    fun testEnableFreeformModeShortcut() {
        PowerMockito.spy(U::class.java)
        val canEnableFreeformAnswer = BooleanAnswer()
        val isOverridingFreeformHackAnswer = BooleanAnswer()
        val isChromeOsAnswer = BooleanAnswer()
        PowerMockito.`when`(U.canEnableFreeform(context)).thenAnswer(canEnableFreeformAnswer)
        PowerMockito.`when`(U.isOverridingFreeformHack(context, false))
                .thenAnswer(isOverridingFreeformHackAnswer)
        PowerMockito.`when`(U.isChromeOs(context)).thenAnswer(isChromeOsAnswer)
        canEnableFreeformAnswer.answer = false
        isOverridingFreeformHackAnswer.answer = false
        isChromeOsAnswer.answer = false
        Assert.assertFalse(U.enableFreeformModeShortcut(context))
        canEnableFreeformAnswer.answer = false
        isOverridingFreeformHackAnswer.answer = false
        isChromeOsAnswer.answer = true
        Assert.assertFalse(U.enableFreeformModeShortcut(context))
        canEnableFreeformAnswer.answer = false
        isOverridingFreeformHackAnswer.answer = true
        isChromeOsAnswer.answer = false
        Assert.assertFalse(U.enableFreeformModeShortcut(context))
        canEnableFreeformAnswer.answer = false
        isOverridingFreeformHackAnswer.answer = true
        isChromeOsAnswer.answer = true
        Assert.assertFalse(U.enableFreeformModeShortcut(context))
        canEnableFreeformAnswer.answer = true
        isOverridingFreeformHackAnswer.answer = false
        isChromeOsAnswer.answer = false
        Assert.assertTrue(U.enableFreeformModeShortcut(context))
        canEnableFreeformAnswer.answer = true
        isOverridingFreeformHackAnswer.answer = false
        isChromeOsAnswer.answer = true
        Assert.assertFalse(U.enableFreeformModeShortcut(context))
        canEnableFreeformAnswer.answer = true
        isOverridingFreeformHackAnswer.answer = true
        isChromeOsAnswer.answer = false
        Assert.assertFalse(U.enableFreeformModeShortcut(context))
        canEnableFreeformAnswer.answer = true
        isOverridingFreeformHackAnswer.answer = true
        isChromeOsAnswer.answer = true
        Assert.assertFalse(U.enableFreeformModeShortcut(context))
    }

    @Test
    @Config(sdk = [26])
    fun testGetOverlayTypeForOAndAboveVersion() {
        Assert.assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY.toLong(),
                U.getOverlayType().toLong())
    }

    @Test
    @Config(sdk = [25])
    fun testGetOverlayTypeForOBelowVersion() {
        Assert.assertEquals(
                WindowManager.LayoutParams.TYPE_PHONE.toLong(),
                U.getOverlayType().toLong()
        )
    }

    @Test
    fun testGetDefaultStartButtonImage() {
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putBoolean(Constants.PREF_APP_DRAWER_ICON, true).apply()
        Assert.assertEquals(Constants.PREF_START_BUTTON_IMAGE_APP_LOGO,
                U.getDefaultStartButtonImage(context))
        prefs.edit().putBoolean(Constants.PREF_APP_DRAWER_ICON, false).apply()
        Assert.assertEquals(Constants.PREF_START_BUTTON_IMAGE_DEFAULT,
                U.getDefaultStartButtonImage(context))
        prefs.edit().remove(Constants.PREF_APP_DRAWER_ICON).apply()
        Assert.assertEquals(Constants.PREF_START_BUTTON_IMAGE_DEFAULT,
                U.getDefaultStartButtonImage(context))
    }

    @Test
    @Throws(Exception::class)
    fun testIsDesktopIconEnabled() {
        PowerMockito.spy(U::class.java)
        val canBootToFreeformAnswer = BooleanAnswer()
        val shouldLaunchTouchAbsorberAnswer = BooleanAnswer()
        PowerMockito.`when`<Any>(U::class.java, "canBootToFreeform", context, false)
                .thenAnswer(canBootToFreeformAnswer)
        PowerMockito.`when`<Any>(U::class.java, "shouldLaunchTouchAbsorber", context)
                .thenAnswer(shouldLaunchTouchAbsorberAnswer)
        canBootToFreeformAnswer.answer = false
        shouldLaunchTouchAbsorberAnswer.answer = false
        Assert.assertTrue(U.isDesktopIconsEnabled(context))
        canBootToFreeformAnswer.answer = false
        shouldLaunchTouchAbsorberAnswer.answer = true
        Assert.assertFalse(U.isDesktopIconsEnabled(context))
        canBootToFreeformAnswer.answer = true
        shouldLaunchTouchAbsorberAnswer.answer = false
        Assert.assertFalse(U.isDesktopIconsEnabled(context))
        canBootToFreeformAnswer.answer = true
        shouldLaunchTouchAbsorberAnswer.answer = true
        Assert.assertFalse(U.isDesktopIconsEnabled(context))
    }

    @Test
    @Config(sdk = [22])
    fun testIsSystemTrayEnabledForMBelowVersion() {
        val prefs = U.getSharedPreferences(context)
        prefs.edit()
                .putBoolean(Constants.PREF_SYS_TRAY, true)
                .putBoolean(Constants.PREF_FULL_LENGTH, true)
                .apply()
        Assert.assertFalse(U.isSystemTrayEnabled(context))
        prefs.edit().remove(Constants.PREF_SYS_TRAY).remove(Constants.PREF_FULL_LENGTH).apply()
    }

    @Test
    fun testIsSystemTrayEnabledForMAndAboveVersion() {
        val prefs = U.getSharedPreferences(context)
        Assert.assertFalse(U.isSystemTrayEnabled(context))
        prefs.edit().putBoolean(Constants.PREF_SYS_TRAY, true).apply()
        Assert.assertTrue(U.isSystemTrayEnabled(context))
        prefs.edit().putBoolean(Constants.PREF_FULL_LENGTH, false).apply()
        Assert.assertFalse(U.isSystemTrayEnabled(context))
        prefs.edit().putBoolean(Constants.PREF_FULL_LENGTH, true).apply()
        Assert.assertTrue(U.isSystemTrayEnabled(context))
        prefs.edit()
                .putString(Constants.PREF_POSITION, Constants.POSITION_BOTTOM_VERTICAL_LEFT)
                .putBoolean(Constants.PREF_ANCHOR, false)
                .apply()
        Assert.assertFalse(U.isSystemTrayEnabled(context))
        prefs.edit().remove(Constants.PREF_POSITION).remove(Constants.PREF_ANCHOR).apply()
    }

    @Test
    fun testApplyDisplayCutoutModeToWithPAndAboveVersion() {
        val layoutParams = WindowManager.LayoutParams()
        Assert.assertTrue(U.applyDisplayCutoutModeTo(layoutParams))
        Assert.assertEquals(
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES.toLong(),
                layoutParams.layoutInDisplayCutoutMode.toLong()
        )
    }

    @Test
    @Config(sdk = [27])
    fun testApplyDisplayCutoutModeToWithBelowVersion() {
        val layoutParams = WindowManager.LayoutParams()
        Assert.assertFalse(U.applyDisplayCutoutModeTo(layoutParams))
    }

    @Test
    fun testIsDesktopModeActive() {
        PowerMockito.spy(U::class.java)
        val isDesktopModeSupportedAnswer = BooleanAnswer()
        val getExternalDisplayIdAnswer = IntAnswer()
        PowerMockito.`when`(U.isDesktopModeSupported(context))
                .thenAnswer(isDesktopModeSupportedAnswer)
        PowerMockito.`when`(U.getExternalDisplayID(context))
                .thenAnswer(getExternalDisplayIdAnswer)
        isDesktopModeSupportedAnswer.answer = false
        Assert.assertFalse(U.isDesktopModeActive(context))
        isDesktopModeSupportedAnswer.answer = true
        Settings.Global.putInt(
                context.contentResolver,
                "force_desktop_mode_on_external_displays",
                0
        )
        Assert.assertFalse(U.isDesktopModeActive(context))
        Settings.Global.putInt(
                context.contentResolver,
                "force_desktop_mode_on_external_displays",
                1
        )
        Assert.assertFalse(U.isDesktopModeActive(context))
        getExternalDisplayIdAnswer.answer = 1
        Assert.assertTrue(U.isDesktopModeActive(context))
        Settings.Global.putInt(
                context.contentResolver,
                "force_desktop_mode_on_external_displays",
                0
        )
    }

    @Test
    fun testSendBroadcast() {
        val receiver = TestBroadcastReceiver()
        val filter = IntentFilter(TestBroadcastReceiver.ACTION)
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
        U.sendBroadcast(context, TestBroadcastReceiver.ACTION)
        Assert.assertTrue(receiver.onReceived)
        receiver.onReceived = false
        U.sendBroadcast(context, Intent(TestBroadcastReceiver.ACTION))
        Assert.assertTrue(receiver.onReceived)
    }

    private class TestBroadcastReceiver : BroadcastReceiver() {
        var onReceived = false
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION != intent.action) {
                return
            }
            onReceived = true
        }

        companion object {
            const val ACTION = "test-broadcast-receiver-action"
        }
    }
}
