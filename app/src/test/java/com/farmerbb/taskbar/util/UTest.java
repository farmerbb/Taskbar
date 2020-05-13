package com.farmerbb.taskbar.util;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.provider.Settings;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.mockito.BooleanAnswer;
import com.farmerbb.taskbar.mockito.IntAnswer;
import com.farmerbb.taskbar.service.PowerMenuService;

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
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowBuild;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.shadows.ShadowSettings;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ReflectionHelpers;

import static android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static org.robolectric.util.ReflectionHelpers.ClassParameter.from;

import static com.farmerbb.taskbar.util.Constants.*;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
@PrepareForTest(U.class)
public class UTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        assertNotNull(context);
    }

    @Test
    public void testShowPermissionDialogWithAndroidTVSettings() throws Exception {
        testShowPermissionDialog(
                true,
                R.string.tb_permission_dialog_message_alt,
                R.string.tb_action_open_settings
        );
    }

    @Test
    public void testShowPermissionDialogNormal() throws Exception {
        testShowPermissionDialog(
                false,
                R.string.tb_permission_dialog_message,
                R.string.tb_action_grant_permission
        );
    }

    private void testShowPermissionDialog(boolean hasAndroidTVSettings,
                                          int messageResId,
                                          int buttonTextResId) throws Exception {
        RunnableHooker onError = new RunnableHooker();
        RunnableHooker onFinish = new RunnableHooker();
        PowerMockito.spy(U.class);
        when(U.class, "hasAndroidTVSettings", context).thenReturn(hasAndroidTVSettings);
        AlertDialog dialog = U.showPermissionDialog(context, onError, onFinish);
        ShadowAlertDialog shadowDialog = Shadows.shadowOf(dialog);
        Resources resources = context.getResources();
        assertEquals(
                resources.getString(R.string.tb_permission_dialog_title),
                shadowDialog.getTitle()
        );
        assertEquals(resources.getString(messageResId), shadowDialog.getMessage());
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        assertEquals(resources.getString(buttonTextResId), positiveButton.getText());
        assertFalse(shadowDialog.isCancelable());
        positiveButton.performClick();
        assertTrue(onFinish.hasRun());
        assertFalse(onError.hasRun());
    }

    @Test
    public void testShowErrorDialog() {
        RunnableHooker onFinish = new RunnableHooker();
        String appOpCommand = "app-op-command";
        AlertDialog dialog =
                ReflectionHelpers.callStaticMethod(
                        U.class,
                        "showErrorDialog",
                        from(Context.class, context),
                        from(String.class, appOpCommand),
                        from(Runnable.class, onFinish)
                );
        ShadowAlertDialog shadowDialog = Shadows.shadowOf(dialog);
        Resources resources = context.getResources();
        assertEquals(
                resources.getString(R.string.tb_error_dialog_title),
                shadowDialog.getTitle()
        );
        assertEquals(
                resources.getString(
                        R.string.tb_error_dialog_message,
                        context.getPackageName(),
                        appOpCommand
                ),
                shadowDialog.getMessage()
        );
        Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        assertEquals(resources.getString(R.string.tb_action_ok), button.getText());
        assertFalse(shadowDialog.isCancelable());
        button.performClick();
        assertTrue(onFinish.hasRun());
    }

    @Test
    public void testSendAccessibilityActionWithServiceNotEnabledAndGrantedPermission() {
        testSendAccessibilityAction(false, true, true);
    }

    @Test
    public void testSendAccessibilityActionWithServiceEnabled() {
        testSendAccessibilityAction(true, false, true);
    }

    @Test
    public void testSendAccessibilityActionWithServiceNotEnabledAndWithoutPermission() {
        testSendAccessibilityAction(false, false, false);
    }

    private void testSendAccessibilityAction(boolean serviceEnabled,
                                             boolean hasPermission,
                                             boolean hasRun) {
        PowerMockito.spy(U.class);
        when(U.isAccessibilityServiceEnabled(context)).thenReturn(serviceEnabled);
        when(U.hasWriteSecureSettingsPermission(context)).thenReturn(hasPermission);
        RunnableHooker onComplete = new RunnableHooker();
        U.sendAccessibilityAction(
                context, AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, onComplete
        );
        // Run all delayed message.
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertEquals(hasRun, onComplete.hasRun());
    }

    @Test
    public void testIsAccessibilityServiceEnabled() {
        String enabledServices =
                Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                );
        ComponentName componentName = new ComponentName(context, PowerMenuService.class);
        String flattenString = componentName.flattenToString();
        String flattenShortString = componentName.flattenToShortString();
        String newEnabledService =
                enabledServices == null ?
                        "" :
                        enabledServices
                                .replaceAll(":" + flattenString, "")
                                .replaceAll(":" + flattenShortString, "")
                                .replaceAll(flattenString, "")
                                .replaceAll(flattenShortString, "");
        Settings.Secure.putString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                newEnabledService
        );
        assertFalse(U.isAccessibilityServiceEnabled(context));
        Settings.Secure.putString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                newEnabledService + ":" + flattenString
        );
        assertTrue(U.isAccessibilityServiceEnabled(context));
        Settings.Secure.putString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                newEnabledService + ":" + flattenShortString
        );
        assertTrue(U.isAccessibilityServiceEnabled(context));
        Settings.Secure.putString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                enabledServices
        );
    }

    @Test
    public void testHasWriteSecureSettingsPermissionForMarshmallowAndAboveVersion() {
        assertFalse(U.hasWriteSecureSettingsPermission(context));
        Application application = ApplicationProvider.getApplicationContext();
        ShadowApplication shadowApplication = Shadows.shadowOf(application);
        shadowApplication.grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS);
        assertTrue(U.hasWriteSecureSettingsPermission(context));
    }

    @Test
    @Config(sdk = 21)
    public void testHasWriteSecureSettingsPermissionVersionBelowMarshmallow() {
        Application application = ApplicationProvider.getApplicationContext();
        ShadowApplication shadowApplication = Shadows.shadowOf(application);
        shadowApplication.grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS);
        assertFalse(U.hasWriteSecureSettingsPermission(context));
    }

    @Test
    public void testShowToast() {
        U.showToast(context, R.string.tb_pin_shortcut_not_supported);
        Toast toast = ShadowToast.getLatestToast();
        assertEquals(Toast.LENGTH_SHORT, toast.getDuration());
        assertEquals(
                context.getResources().getString(R.string.tb_pin_shortcut_not_supported),
                ShadowToast.getTextOfLatestToast()
        );
    }

    @Test
    public void testShowLongToast() {
        U.showToastLong(context, R.string.tb_pin_shortcut_not_supported);
        Toast toast = ShadowToast.getLatestToast();
        assertEquals(Toast.LENGTH_LONG, toast.getDuration());
        assertEquals(
                context.getResources().getString(R.string.tb_pin_shortcut_not_supported),
                ShadowToast.getTextOfLatestToast()
        );
    }

    @Test
    public void testCancelToast() {
        U.showToastLong(context, R.string.tb_pin_shortcut_not_supported);
        ShadowToast shadowToast = Shadows.shadowOf(ShadowToast.getLatestToast());
        assertFalse(shadowToast.isCancelled());
        U.cancelToast();
        assertTrue(shadowToast.isCancelled());
    }

    @Test
    public void testCanEnableFreeformWithNougatAndAboveVersion() {
        assertTrue(U.canEnableFreeform());
    }

    @Test
    @Config(sdk = 23)
    public void testCanEnableFreeformWithMarshmallowAndBelowVersion() {
        assertFalse(U.canEnableFreeform());
    }

    @Test
    public void testHasFreeformSupportWithoutFreeformEnabled() {
        PowerMockito.spy(U.class);
        when(U.canEnableFreeform()).thenReturn(false);
        assertFalse(U.canEnableFreeform());
    }

    @Test
    public void testHasFreeformSupportWithFreeformEnabledAndNMR1AboveVersion() {
        PowerMockito.spy(U.class);
        when(U.canEnableFreeform()).thenReturn(true);
        assertFalse(U.hasFreeformSupport(context));
        // Case 1, system has feature freeform.
        PackageManager packageManager = context.getPackageManager();
        ShadowPackageManager shadowPackageManager = Shadows.shadowOf(packageManager);
        shadowPackageManager
                .setSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT, true);
        assertTrue(U.hasFreeformSupport(context));
        shadowPackageManager
                .setSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT, false);
        // Case 2, enable_freeform_support in Settings.Global is not 0
        Settings.Global.putInt(context.getContentResolver(), "enable_freeform_support", 1);
        assertTrue(U.hasFreeformSupport(context));
        Settings.Global.putInt(context.getContentResolver(), "enable_freeform_support", 0);
    }

    @Test
    @Config(sdk = 25)
    public void testHasFreeformSupportWithFreeformEnabledAndNMR1AndBelowVersion() {
        PowerMockito.spy(U.class);
        when(U.canEnableFreeform()).thenReturn(true);
        assertFalse(U.hasFreeformSupport(context));
        // Case 3, version is less than or equal to N_MRI, and force_resizable_activities
        // in Settings.Global is not 0
        Settings.Global.putInt(context.getContentResolver(), "force_resizable_activities", 1);
        assertTrue(U.hasFreeformSupport(context));
        Settings.Global.putInt(context.getContentResolver(), "force_resizable_activities", 0);
    }

    @Test
    public void testCanBootToFreeform() {
        PowerMockito.spy(U.class);
        BooleanAnswer hasFreeformSupportAnswer = new BooleanAnswer();
        BooleanAnswer isOverridingFreeformHackAnswer = new BooleanAnswer();
        when(U.hasFreeformSupport(context)).thenAnswer(hasFreeformSupportAnswer);
        when(U.isOverridingFreeformHack(context, true)).thenAnswer(isOverridingFreeformHackAnswer);
        // Case 1, all return true
        hasFreeformSupportAnswer.answer = true;
        isOverridingFreeformHackAnswer.answer = true;
        assertFalse(U.canBootToFreeform(context));
        // Case 2, true, false
        hasFreeformSupportAnswer.answer = true;
        isOverridingFreeformHackAnswer.answer = false;
        assertTrue(U.canBootToFreeform(context));
        // Case 3, false, true
        hasFreeformSupportAnswer.answer = false;
        isOverridingFreeformHackAnswer.answer = true;
        assertFalse(U.canBootToFreeform(context));
        // Case 4, false, false
        hasFreeformSupportAnswer.answer = false;
        isOverridingFreeformHackAnswer.answer = false;
        assertFalse(U.canBootToFreeform(context));
    }

    @Test
    public void testIsSamsungDevice() {
        ShadowBuild.setManufacturer("Samsung");
        assertTrue(U.isSamsungDevice());
        ShadowBuild.setManufacturer("samsung");
        assertTrue(U.isSamsungDevice());
        ShadowBuild.setManufacturer("UnSamsung");
        assertFalse(U.isSamsungDevice());
    }

    @Test
    public void testGetBackgroundTint() {
        SharedPreferences prefs = U.getSharedPreferences(context);
        prefs.edit()
                .putInt(PREF_BACKGROUND_TINT, Color.GREEN)
                .putBoolean(PREF_SHOW_BACKGROUND, false)
                .apply();
        // If the SHOW_BACKGROUND is false, it use transparent to replace origin tint.
        assertEquals(Color.TRANSPARENT, U.getBackgroundTint(context));
        prefs.edit()
                .putInt(PREF_BACKGROUND_TINT, Color.GREEN)
                .apply();
        assertEquals(Color.GREEN, U.getBackgroundTint(context));
        prefs.edit().remove(PREF_BACKGROUND_TINT).apply();
        assertEquals(
                context.getResources().getInteger(R.integer.tb_translucent_gray),
                U.getBackgroundTint(context)
        );
    }

    @Test
    public void testAccentColor() {
        SharedPreferences prefs = U.getSharedPreferences(context);
        prefs.edit().remove(PREF_ACCENT_COLOR).apply();
        assertEquals(
                context.getResources().getInteger(R.integer.tb_translucent_white),
                U.getAccentColor(context)
        );
        prefs.edit().putInt(PREF_ACCENT_COLOR, Color.GREEN).apply();
        assertEquals(Color.GREEN, U.getAccentColor(context));
    }

    @Test
    public void testCanDrawOverlaysWithMarshmallowAndAboveVersion() {
        ShadowSettings.setCanDrawOverlays(true);
        assertTrue(U.canDrawOverlays(context));
        ShadowSettings.setCanDrawOverlays(false);
        assertFalse(U.canDrawOverlays(context));
    }

    @Test
    @Config(sdk = 22)
    public void testCanDrawOverlaysWithMarshmallowBelowVersion() {
        assertTrue(U.canDrawOverlays(context));
    }

    @Test
    public void testIsGame() {
        // We only test for un-support launching games fullscreen, because of
        // we don't have a good method to test code with ApplicationInfo.
        SharedPreferences prefs = U.getSharedPreferences(context);
        prefs.edit().putBoolean(PREF_LAUNCH_GAMES_FULLSCREEN, false).apply();
        assertFalse(U.isGame(context, context.getPackageName()));
        prefs.edit().putBoolean(PREF_LAUNCH_GAMES_FULLSCREEN, true).apply();
        assertFalse(U.isGame(context, context.getPackageName()));
        assertFalse(U.isGame(context, context.getPackageName() + "un-exist-package"));
    }

    @Test
    public void testGetActivityOptionsWithPAndAboveVersion() {
        testGetActivityOptions(0, 5, 1);
    }

    @Test
    @Config(sdk = 27)
    public void testGetActivityOptionsWithPBelowVersion() {
        testGetActivityOptions(-1, 2, -1);
    }

    private void testGetActivityOptions(int defaultStackId,
                                        int freeformStackId,
                                        int stackIdWithoutBrokenApi) {
        PowerMockito.spy(U.class);
        BooleanAnswer hasBrokenSetLaunchBoundsApiAnswer = new BooleanAnswer();
        BooleanAnswer isChromeOsAnswer = new BooleanAnswer();
        when(U.hasBrokenSetLaunchBoundsApi()).thenAnswer(hasBrokenSetLaunchBoundsApiAnswer);
        when(U.isChromeOs(context)).thenAnswer(isChromeOsAnswer);
        boolean originFreeformHackActive = FreeformHackHelper.getInstance().isFreeformHackActive();
        checkActivityOptionsStackIdForNonContextMenu(
                context, null, false, defaultStackId
        );
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.APP_PORTRAIT, false, 1
        );
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.APP_PORTRAIT, true, freeformStackId
        );
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.APP_LANDSCAPE, false, 1
        );
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.APP_LANDSCAPE, true, freeformStackId
        );
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.APP_FULLSCREEN, false, 1
        );
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.FREEFORM_HACK, false, freeformStackId
        );
        FreeformHackHelper.getInstance().setFreeformHackActive(originFreeformHackActive);
        hasBrokenSetLaunchBoundsApiAnswer.answer = true;
        checkActivityOptionsStackIdForContextMenu(context, 1);
        hasBrokenSetLaunchBoundsApiAnswer.answer = false;
        isChromeOsAnswer.answer = false;
        checkActivityOptionsStackIdForContextMenu(context, stackIdWithoutBrokenApi);
        isChromeOsAnswer.answer = true;
        checkActivityOptionsStackIdForContextMenu(context, -1);
    }

    private void checkActivityOptionsStackIdForContextMenu(Context context,
                                                           int stackId) {
        ActivityOptions options = U.getActivityOptions(context, ApplicationType.CONTEXT_MENU, null);
        assertEquals(stackId, getActivityOptionsStackId(options));
    }

    private void checkActivityOptionsStackIdForNonContextMenu(Context context,
                                                              ApplicationType applicationType,
                                                              boolean isFreeformHackActive,
                                                              int stackId) {
        FreeformHackHelper.getInstance().setFreeformHackActive(isFreeformHackActive);
        ActivityOptions options = U.getActivityOptions(context, applicationType, null);
        assertEquals(stackId, getActivityOptionsStackId(options));
    }

    private int getActivityOptionsStackId(ActivityOptions options) {
        String methodName;
        if (U.getCurrentApiVersion() >= 28.0f) {
            methodName = "getLaunchWindowingMode";
        } else {
            methodName = "getLaunchStackId";
        }
        return ReflectionHelpers.callInstanceMethod(options, methodName);
    }

    @Test
    public void testIsChromeOs() {
        PackageManager packageManager = context.getPackageManager();
        ShadowPackageManager shadowPackageManager = Shadows.shadowOf(packageManager);
        shadowPackageManager.setSystemFeature("org.chromium.arc", true);
        assertTrue(U.isChromeOs(context));
        shadowPackageManager.setSystemFeature("org.chromium.arc", false);
        assertFalse(U.isChromeOs(context));
    }

    @Test
    @Config(qualifiers = "sw720dp")
    public void testGetBaseTaskbarSizeWithSW720dp() {
        PowerMockito.spy(U.class);
        BooleanAnswer isSystemTrayEnabledAnswer = new BooleanAnswer();
        when(U.isSystemTrayEnabled(context)).thenAnswer(isSystemTrayEnabledAnswer);
        isSystemTrayEnabledAnswer.answer = false;
        // The only difference of the different screen size, is the initial taskbar size.
        // So we only test the different in this test method.
        float initialSize = context.getResources().getDimension(R.dimen.tb_base_taskbar_size);
        initialSize += context.getResources().getDimension(R.dimen.tb_dashboard_button_size);
        assertEquals(Math.round(initialSize), U.getBaseTaskbarSize(context));
    }

    @Test
    public void testGetBaseTaskbarSizeWithNormalDimension() {
        PowerMockito.spy(U.class);
        BooleanAnswer isSystemTrayEnabledAnswer = new BooleanAnswer();
        when(U.isSystemTrayEnabled(context)).thenAnswer(isSystemTrayEnabledAnswer);
        isSystemTrayEnabledAnswer.answer = false;
        float initialSize = context.getResources().getDimension(R.dimen.tb_base_taskbar_size);
        assertEquals(Math.round(initialSize), U.getBaseTaskbarSize(context));
        SharedPreferences prefs = U.getSharedPreferences(context);
        prefs.edit().putBoolean(PREF_DASHBOARD, true).apply();
        float dashboardButtonSize =
                context.getResources().getDimension(R.dimen.tb_dashboard_button_size);
        assertEquals(Math.round(initialSize + dashboardButtonSize), U.getBaseTaskbarSize(context));
        prefs.edit().remove(PREF_DASHBOARD).apply();
        float navbarButtonsMargin =
                context.getResources().getDimension(R.dimen.tb_navbar_buttons_margin);
        float iconSize =
                context.getResources().getDimension(R.dimen.tb_icon_size);
        prefs.edit().putBoolean(PREF_BUTTON_BACK, true).apply();
        assertEquals(
                Math.round(initialSize + navbarButtonsMargin + iconSize),
                U.getBaseTaskbarSize(context)
        );
        prefs.edit().remove(PREF_BUTTON_BACK).apply();
        prefs.edit().putBoolean(PREF_BUTTON_HOME, true).apply();
        assertEquals(
                Math.round(initialSize + navbarButtonsMargin + iconSize),
                U.getBaseTaskbarSize(context)
        );
        prefs.edit().remove(PREF_BUTTON_HOME).apply();
        prefs.edit().putBoolean(PREF_BUTTON_RECENTS, true).apply();
        assertEquals(
                Math.round(initialSize + navbarButtonsMargin + iconSize),
                U.getBaseTaskbarSize(context)
        );
        prefs.edit().remove(PREF_BUTTON_RECENTS).apply();
        isSystemTrayEnabledAnswer.answer = true;
        float systemTraySize = context.getResources().getDimension(R.dimen.tb_systray_size);
        assertEquals(Math.round(initialSize + systemTraySize), U.getBaseTaskbarSize(context));
    }

    @Test
    public void testInitPrefsForBlissOS() {
        PowerMockito.spy(U.class);
        when(U.isBlissOs(any(Context.class))).thenReturn(true);
        assertTrue(U.isBlissOs(context));
        SharedPreferences prefs = U.getSharedPreferences(context);
        assertFalse(prefs.getBoolean(PREF_BLISS_OS_PREFS, false));
        U.initPrefs(context);
        assertEquals(
                "running_apps_only",
                prefs.getString(PREF_RECENTS_AMOUNT, "")
        );
        assertEquals("0", prefs.getString(PREF_REFRESH_FREQUENCY, ""));
        assertEquals("2147483647", prefs.getString(PREF_MAX_NUM_OF_RECENTS, ""));
        assertEquals("true", prefs.getString(PREF_SORT_ORDER, ""));
        assertEquals("app_logo", prefs.getString(PREF_START_BUTTON_IMAGE, ""));
        assertTrue(prefs.getBoolean(PREF_BUTTON_BACK, false));
        assertTrue(prefs.getBoolean(PREF_BUTTON_HOME, false));
        assertTrue(prefs.getBoolean(PREF_BUTTON_RECENTS, false));
        assertTrue(prefs.getBoolean(PREF_AUTO_HIDE_NAVBAR, false));
        assertFalse(prefs.getBoolean(PREF_SHORTCUT_ICON, true));
        assertTrue(prefs.getBoolean(PREF_BLISS_OS_PREFS, false));
        prefs.edit().putBoolean(PREF_BLISS_OS_PREFS, false);
    }

    @Test
    public void testInitPrefsForNormalWithCanEnableFreeformAndHackOverrideFalse() {
        PowerMockito.spy(U.class);
        when(U.canEnableFreeform()).thenReturn(true);
        SharedPreferences prefs = U.getSharedPreferences(context);
        prefs.edit().putBoolean(PREF_FREEFORM_HACK_OVERRIDE, false).apply();
        U.initPrefs(context);
        assertEquals(
                U.hasFreeformSupport(context) && !U.isSamsungDevice(),
                prefs.getBoolean(PREF_FREEFORM_HACK, false)
        );
        assertFalse(prefs.getBoolean(PREF_SAVE_WINDOW_SIZES, true));
        assertTrue(prefs.getBoolean(PREF_FREEFORM_HACK_OVERRIDE, false));
    }

    @Test
    public void testInitPrefsForNormalWithCanEnableFreeformAndHackOverrideTrueButNoSupport() {
        PowerMockito.spy(U.class);
        when(U.canEnableFreeform()).thenReturn(true);
        SharedPreferences prefs = U.getSharedPreferences(context);
        prefs.edit().putBoolean(PREF_FREEFORM_HACK_OVERRIDE, true).apply();
        when(U.hasFreeformSupport(context)).thenReturn(false);
        U.initPrefs(context);
        assertFalse(prefs.getBoolean(PREF_FREEFORM_HACK, false));
    }

    @Test
    public void testInitPrefsForNormalWithCantEnableFreeform() {
        PowerMockito.spy(U.class);
        when(U.canEnableFreeform()).thenReturn(false);
        SharedPreferences prefs = U.getSharedPreferences(context);
        U.initPrefs(context);
        assertFalse(prefs.getBoolean(PREF_FREEFORM_HACK, false));
        prefs.edit()
                .putBoolean(PREF_FREEFORM_HACK, false)
                .putBoolean(PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false)
                .apply();
        U.initPrefs(context);
        assertFalse(prefs.getBoolean(PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false));
        prefs.edit()
                .putBoolean(PREF_FREEFORM_HACK, true)
                .putBoolean(PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false)
                .apply();
        U.initPrefs(context);
        assertTrue(prefs.getBoolean(PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false));
        prefs.edit()
                .putBoolean(PREF_FREEFORM_HACK, false)
                .putBoolean(PREF_SHOW_FREEFORM_DISABLED_MESSAGE, true)
                .apply();
        U.initPrefs(context);
        assertTrue(prefs.getBoolean(PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false));
        prefs.edit()
                .putBoolean(PREF_FREEFORM_HACK, true)
                .putBoolean(PREF_SHOW_FREEFORM_DISABLED_MESSAGE, true)
                .apply();
        U.initPrefs(context);
        assertTrue(prefs.getBoolean(PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false));
    }

    @Test
    public void testIsOverridingFreeformHackForPAndAboveVersion() {
        PowerMockito.spy(U.class);
        when(U.isChromeOs(context)).thenReturn(false);
        // Check preferences
        assertFalse(U.isOverridingFreeformHack(context, true));
        SharedPreferences prefs = U.getSharedPreferences(context);
        prefs.edit().putBoolean(PREF_FREEFORM_HACK, true).apply();
        assertTrue(U.isOverridingFreeformHack(context, true));
        prefs.edit().remove(PREF_FREEFORM_HACK).apply();

        // Don't check preferences
        assertTrue(U.isOverridingFreeformHack(context, false));
    }

    @Test
    @Config(sdk = 27)
    public void testIsOverridingFreeformHackForPBelowVersion() {
        PowerMockito.spy(U.class);
        when(U.isChromeOs(context)).thenReturn(false);
        // Check preferences
        assertFalse(U.isOverridingFreeformHack(context, true));
        SharedPreferences prefs = U.getSharedPreferences(context);
        prefs.edit().putBoolean(PREF_FREEFORM_HACK, true).apply();
        assertFalse(U.isOverridingFreeformHack(context, true));
        prefs.edit().remove(PREF_FREEFORM_HACK).apply();

        // Don't check preferences
        assertFalse(U.isOverridingFreeformHack(context, false));
    }

    @Test
    public void testIsOverridingFreeformHackForChromeOS() {
        PowerMockito.spy(U.class);
        when(U.isChromeOs(context)).thenReturn(true);
        // Check preferences
        assertFalse(U.isOverridingFreeformHack(context, true));
        SharedPreferences prefs = U.getSharedPreferences(context);
        prefs.edit().putBoolean(PREF_FREEFORM_HACK, true).apply();
        // The default PREF_CHROME_OS_CONTEXT_MENU_FIX is true
        assertTrue(U.isOverridingFreeformHack(context, true));
        prefs.edit().putBoolean(PREF_CHROME_OS_CONTEXT_MENU_FIX, false).apply();
        assertFalse(U.isOverridingFreeformHack(context, true));
        prefs.edit().putBoolean(PREF_CHROME_OS_CONTEXT_MENU_FIX, true).apply();
        assertTrue(U.isOverridingFreeformHack(context, true));
        prefs.edit().remove(PREF_FREEFORM_HACK).apply();
        prefs.edit().remove(PREF_CHROME_OS_CONTEXT_MENU_FIX).apply();

        // Don't check preferences
        assertTrue(U.isOverridingFreeformHack(context, false));
        prefs.edit().putBoolean(PREF_CHROME_OS_CONTEXT_MENU_FIX, false).apply();
        assertFalse(U.isOverridingFreeformHack(context, false));
        prefs.edit().putBoolean(PREF_CHROME_OS_CONTEXT_MENU_FIX, true).apply();
        assertTrue(U.isOverridingFreeformHack(context, false));
        prefs.edit().remove(PREF_CHROME_OS_CONTEXT_MENU_FIX).apply();
    }

    @Test
    @Config(sdk = 25)
    public void testHasBrokenSetLaunchBoundsApiForApi25() {
        assertFalse(U.hasBrokenSetLaunchBoundsApi());
    }

    @Test
    @Config(sdk = 26)
    public void testHasBrokenSetLaunchBoundsApiForApi26() throws Exception {
        testHasBrokenSetLaunchBoundsApiWithValidApiVersion();
    }

    @Test
    @Config(sdk = 27)
    public void testHasBrokenSetLaunchBoundsApiForApi27() throws Exception {
        testHasBrokenSetLaunchBoundsApiWithValidApiVersion();
        testHasBrokenSetLaunchBoundsApiWithValidApiVersion();
    }

    @Test
    @Config(sdk = 28)
    public void testHasBrokenSetLaunchBoundsApiForApi28() {
        assertFalse(U.hasBrokenSetLaunchBoundsApi());
    }

    private void testHasBrokenSetLaunchBoundsApiWithValidApiVersion() throws Exception {
        PowerMockito.spy(U.class);
        BooleanAnswer isSamsungDeviceAnswer = new BooleanAnswer();
        BooleanAnswer isNvidiaDevice = new BooleanAnswer();
        when(U.isSamsungDevice()).thenAnswer(isSamsungDeviceAnswer);
        when(U.class, "isNvidiaDevice").thenAnswer(isNvidiaDevice);
        isSamsungDeviceAnswer.answer = false;
        isNvidiaDevice.answer = false;
        assertTrue(U.hasBrokenSetLaunchBoundsApi());
        isSamsungDeviceAnswer.answer = false;
        isNvidiaDevice.answer = true;
        assertFalse(U.hasBrokenSetLaunchBoundsApi());
        isSamsungDeviceAnswer.answer = true;
        isNvidiaDevice.answer = false;
        assertFalse(U.hasBrokenSetLaunchBoundsApi());
        isSamsungDeviceAnswer.answer = true;
        isNvidiaDevice.answer = true;
        assertFalse(U.hasBrokenSetLaunchBoundsApi());
    }

    @Test
    public void testWrapContext() {
        SharedPreferences prefs = U.getSharedPreferences(context);
        prefs.edit().putString(PREF_THEME, "light").apply();
        Context newContext = U.wrapContext(context);
        Integer themeResource = ReflectionHelpers.getField(newContext, "mThemeResource");
        assertNotNull(themeResource);
        assertEquals(R.style.Taskbar, (int) themeResource);
        prefs.edit().putString(PREF_THEME, "dark").apply();
        newContext = U.wrapContext(context);
        themeResource = ReflectionHelpers.getField(newContext, "mThemeResource");
        assertNotNull(themeResource);
        assertEquals(R.style.Taskbar_Dark, (int) themeResource);
        prefs.edit().putString(PREF_THEME, "non-support").apply();
        newContext = U.wrapContext(context);
        assertTrue(newContext instanceof ContextThemeWrapper);
        prefs.edit().remove(PREF_THEME).apply();
        newContext = U.wrapContext(context);
        themeResource = ReflectionHelpers.getField(newContext, "mThemeResource");
        assertNotNull(themeResource);
        assertEquals(R.style.Taskbar, (int) themeResource);
    }

    @Test
    public void testEnableFreeformModeShortcut() {
        PowerMockito.spy(U.class);
        BooleanAnswer canEnableFreeformAnswer = new BooleanAnswer();
        BooleanAnswer isOverridingFreeformHackAnswer = new BooleanAnswer();
        BooleanAnswer isChromeOsAnswer = new BooleanAnswer();
        when(U.canEnableFreeform()).thenAnswer(canEnableFreeformAnswer);
        when(U.isOverridingFreeformHack(context, false))
                .thenAnswer(isOverridingFreeformHackAnswer);
        when(U.isChromeOs(context)).thenAnswer(isChromeOsAnswer);

        canEnableFreeformAnswer.answer = false;
        isOverridingFreeformHackAnswer.answer = false;
        isChromeOsAnswer.answer = false;
        assertFalse(U.enableFreeformModeShortcut(context));

        canEnableFreeformAnswer.answer = false;
        isOverridingFreeformHackAnswer.answer = false;
        isChromeOsAnswer.answer = true;
        assertFalse(U.enableFreeformModeShortcut(context));

        canEnableFreeformAnswer.answer = false;
        isOverridingFreeformHackAnswer.answer = true;
        isChromeOsAnswer.answer = false;
        assertFalse(U.enableFreeformModeShortcut(context));

        canEnableFreeformAnswer.answer = false;
        isOverridingFreeformHackAnswer.answer = true;
        isChromeOsAnswer.answer = true;
        assertFalse(U.enableFreeformModeShortcut(context));

        canEnableFreeformAnswer.answer = true;
        isOverridingFreeformHackAnswer.answer = false;
        isChromeOsAnswer.answer = false;
        assertTrue(U.enableFreeformModeShortcut(context));

        canEnableFreeformAnswer.answer = true;
        isOverridingFreeformHackAnswer.answer = false;
        isChromeOsAnswer.answer = true;
        assertFalse(U.enableFreeformModeShortcut(context));

        canEnableFreeformAnswer.answer = true;
        isOverridingFreeformHackAnswer.answer = true;
        isChromeOsAnswer.answer = false;
        assertFalse(U.enableFreeformModeShortcut(context));

        canEnableFreeformAnswer.answer = true;
        isOverridingFreeformHackAnswer.answer = true;
        isChromeOsAnswer.answer = true;
        assertFalse(U.enableFreeformModeShortcut(context));
    }

    @Test
    @Config(sdk = 26)
    public void testGetOverlayTypeForOAndAboveVersion() {
        assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, U.getOverlayType());
    }

    @Test
    @Config(sdk = 25)
    public void testGetOverlayTypeForOBelowVersion() {
        assertEquals(WindowManager.LayoutParams.TYPE_PHONE, U.getOverlayType());
    }

    @Test
    public void testGetDefaultStartButtonImage() {
        SharedPreferences prefs = U.getSharedPreferences(context);
        prefs.edit().putBoolean(PREF_APP_DRAWER_ICON, true).apply();
        assertEquals("app_logo", U.getDefaultStartButtonImage(context));
        prefs.edit().putBoolean(PREF_APP_DRAWER_ICON, false).apply();
        assertEquals("default", U.getDefaultStartButtonImage(context));
        prefs.edit().remove(PREF_APP_DRAWER_ICON).apply();
        assertEquals("default", U.getDefaultStartButtonImage(context));
    }

    @Test
    public void testIsDesktopIconEnabled() throws Exception {
        PowerMockito.spy(U.class);
        BooleanAnswer canBootToFreeformAnswer = new BooleanAnswer();
        BooleanAnswer shouldLaunchTouchAbsorberAnswer = new BooleanAnswer();
        when(U.class, "canBootToFreeform", context, false)
                .thenAnswer(canBootToFreeformAnswer);
        when(U.class, "shouldLaunchTouchAbsorber", context)
                .thenAnswer(shouldLaunchTouchAbsorberAnswer);

        canBootToFreeformAnswer.answer = false;
        shouldLaunchTouchAbsorberAnswer.answer = false;
        assertTrue(U.isDesktopIconsEnabled(context));

        canBootToFreeformAnswer.answer = false;
        shouldLaunchTouchAbsorberAnswer.answer = true;
        assertFalse(U.isDesktopIconsEnabled(context));

        canBootToFreeformAnswer.answer = true;
        shouldLaunchTouchAbsorberAnswer.answer = false;
        assertFalse(U.isDesktopIconsEnabled(context));

        canBootToFreeformAnswer.answer = true;
        shouldLaunchTouchAbsorberAnswer.answer = true;
        assertFalse(U.isDesktopIconsEnabled(context));
    }

    @Test
    @Config(sdk = 22)
    public void testIsSystemTrayEnabledForMBelowVersion() {
        SharedPreferences prefs = U.getSharedPreferences(context);
        prefs.edit()
                .putBoolean(PREF_SYS_TRAY, true)
                .putBoolean(PREF_FULL_LENGTH, true)
                .apply();
        assertFalse(U.isSystemTrayEnabled(context));
        prefs.edit().remove(PREF_SYS_TRAY).remove(PREF_FULL_LENGTH).apply();
    }

    @Test
    public void testIsSystemTrayEnabledForMAndAboveVersion() {
        SharedPreferences prefs = U.getSharedPreferences(context);
        assertFalse(U.isSystemTrayEnabled(context));
        prefs.edit().putBoolean(PREF_SYS_TRAY, true).apply();
        assertTrue(U.isSystemTrayEnabled(context));
        prefs.edit().putBoolean(PREF_FULL_LENGTH, false).apply();
        assertFalse(U.isSystemTrayEnabled(context));
        prefs.edit().putBoolean(PREF_FULL_LENGTH, true).apply();
        assertTrue(U.isSystemTrayEnabled(context));
        prefs.edit()
                .putString(PREF_POSITION, POSITION_BOTTOM_VERTICAL_LEFT)
                .putBoolean(PREF_ANCHOR, false)
                .apply();
        assertFalse(U.isSystemTrayEnabled(context));
        prefs.edit().remove(PREF_POSITION).remove(PREF_ANCHOR).apply();
    }

    @Test
    public void testApplyDisplayCutoutModeToWithPAndAboveVersion() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        assertTrue(U.applyDisplayCutoutModeTo(layoutParams));
        assertEquals(
                LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES,
                layoutParams.layoutInDisplayCutoutMode
        );
    }

    @Test
    @Config(sdk = 27)
    public void testApplyDisplayCutoutModeToWithBelowVersion() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        assertFalse(U.applyDisplayCutoutModeTo(layoutParams));
    }

    @Test
    public void testIsDesktopModeActive() {
        PowerMockito.spy(U.class);
        BooleanAnswer isDesktopModeSupportedAnswer = new BooleanAnswer();
        IntAnswer getExternalDisplayIdAnswer = new IntAnswer();
        when(U.isDesktopModeSupported(context)).thenAnswer(isDesktopModeSupportedAnswer);
        when(U.getExternalDisplayID(context)).thenAnswer(getExternalDisplayIdAnswer);

        isDesktopModeSupportedAnswer.answer = false;
        assertFalse(U.isDesktopModeActive(context));

        isDesktopModeSupportedAnswer.answer = true;
        Settings.Global.putInt(
                context.getContentResolver(),
                "force_desktop_mode_on_external_displays",
                0
        );
        assertFalse(U.isDesktopModeActive(context));
        Settings.Global.putInt(
                context.getContentResolver(),
                "force_desktop_mode_on_external_displays",
                1
        );
        assertFalse(U.isDesktopModeActive(context));
        getExternalDisplayIdAnswer.answer = 1;
        assertTrue(U.isDesktopModeActive(context));
        Settings.Global.putInt(
                context.getContentResolver(),
                "force_desktop_mode_on_external_displays",
                0
        );
    }

    @Test
    public void testSendBroadcast() {
        TestBroadcastReceiver receiver = new TestBroadcastReceiver();
        IntentFilter filter = new IntentFilter(TestBroadcastReceiver.ACTION);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);
        U.sendBroadcast(context, TestBroadcastReceiver.ACTION);
        assertTrue(receiver.onReceived);
        receiver.onReceived = false;
        U.sendBroadcast(context, new Intent(TestBroadcastReceiver.ACTION));
        assertTrue(receiver.onReceived);
    }

    private static final class TestBroadcastReceiver extends BroadcastReceiver {
        private static final String ACTION = "test-broadcast-receiver-action";
        private boolean onReceived;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !ACTION.equals(intent.getAction())) {
                return;
            }
            onReceived = true;
        }
    }
}