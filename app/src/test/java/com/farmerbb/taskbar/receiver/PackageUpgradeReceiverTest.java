package com.farmerbb.taskbar.receiver;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.activity.DummyActivity;
import com.farmerbb.taskbar.mockito.BooleanAnswer;
import com.farmerbb.taskbar.service.NotificationService;
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
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSettings;

import static com.farmerbb.taskbar.util.Constants.EXTRA_START_FREEFORM_HACK;
import static com.farmerbb.taskbar.util.Constants.EXTRA_START_SERVICES;
import static com.farmerbb.taskbar.util.Constants.PREF_IS_HIDDEN;
import static com.farmerbb.taskbar.util.Constants.PREF_TASKBAR_ACTIVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
@PrepareForTest(U.class)
public class PackageUpgradeReceiverTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private PackageUpgradeReceiver packageUpgradeReceiver;
    private Context context;
    private Intent intent;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        packageUpgradeReceiver = new PackageUpgradeReceiver();
        intent = new Intent(Intent.ACTION_MY_PACKAGE_REPLACED);
        prefs = U.getSharedPreferences(context);
    }

    @Test
    public void testStartDummyActivity() {
        PowerMockito.mockStatic(U.class);
        when(U.getSharedPreferences(context)).thenReturn(prefs);
        BooleanAnswer answer = new BooleanAnswer();
        when(U.hasFreeformSupport(context)).thenAnswer(answer);
        when(U.isFreeformModeEnabled(context)).thenAnswer(answer);

        Application application = (Application) context;
        shadowOf(application).clearNextStartedActivities();

        prefs.edit().putBoolean(PREF_TASKBAR_ACTIVE, true).apply();
        prefs.edit().putBoolean(PREF_IS_HIDDEN, false).apply();
        answer.answer = true;
        packageUpgradeReceiver.onReceive(context, intent);
        Intent startedIntent = shadowOf(application).peekNextStartedActivity();
        assertNotNull(startedIntent);
        assertTrue(startedIntent.getBooleanExtra(EXTRA_START_FREEFORM_HACK, false));
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, startedIntent.getFlags());
        assertNotNull(startedIntent.getComponent());
        assertEquals(DummyActivity.class.getCanonicalName(), startedIntent.getComponent().getClassName());

        shadowOf(application).clearNextStartedActivities();
        answer.answer = false;
        packageUpgradeReceiver.onReceive(context, this.intent);
        assertNull(shadowOf(application).peekNextStartedActivity());

        shadowOf(application).clearNextStartedActivities();
        answer.answer = true;
        prefs.edit().putBoolean(PREF_IS_HIDDEN, true).apply();
        packageUpgradeReceiver.onReceive(context, this.intent);
        assertNull(shadowOf(application).peekNextStartedActivity());

        shadowOf(application).clearNextStartedActivities();
        prefs.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();
        prefs.edit().putBoolean(PREF_IS_HIDDEN, false).apply();
        packageUpgradeReceiver.onReceive(context, this.intent);
        assertNull(shadowOf(application).peekNextStartedActivity());
    }

    @Test
    public void testStartNotificationService() {
        Application application = (Application) context;

        shadowOf(application).clearStartedServices();
        ShadowSettings.setCanDrawOverlays(true);
        prefs.edit().putBoolean(PREF_TASKBAR_ACTIVE, true).apply();
        prefs.edit().putBoolean(PREF_IS_HIDDEN, false).apply();
        packageUpgradeReceiver.onReceive(context, intent);
        Intent startedIntent = shadowOf(application).peekNextStartedService();
        assertNotNull(startedIntent);
        assertTrue(startedIntent.getBooleanExtra(EXTRA_START_SERVICES, false));
        assertNotNull(startedIntent.getComponent());
        assertEquals(
                NotificationService.class.getCanonicalName(),
                startedIntent.getComponent().getClassName()
        );

        shadowOf(application).clearStartedServices();
        ShadowSettings.setCanDrawOverlays(false);
        prefs.edit().putBoolean(PREF_TASKBAR_ACTIVE, true).apply();
        prefs.edit().putBoolean(PREF_IS_HIDDEN, false).apply();
        packageUpgradeReceiver.onReceive(context, intent);
        startedIntent = shadowOf(application).peekNextStartedService();
        assertNull(startedIntent);

        shadowOf(application).clearStartedServices();
        ShadowSettings.setCanDrawOverlays(true);
        prefs.edit().putBoolean(PREF_TASKBAR_ACTIVE, true).apply();
        prefs.edit().putBoolean(PREF_IS_HIDDEN, true).apply();
        packageUpgradeReceiver.onReceive(context, intent);
        startedIntent = shadowOf(application).peekNextStartedService();
        assertNotNull(startedIntent);
        assertFalse(startedIntent.getBooleanExtra(EXTRA_START_SERVICES, true));
        assertNotNull(startedIntent.getComponent());
        assertEquals(
                NotificationService.class.getCanonicalName(),
                startedIntent.getComponent().getClassName()
        );

        shadowOf(application).clearStartedServices();
        prefs.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();
        packageUpgradeReceiver.onReceive(context, intent);
        assertNull(shadowOf(application).peekNextStartedService());
    }
}
