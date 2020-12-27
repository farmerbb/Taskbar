package com.farmerbb.taskbar.receiver;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.util.U;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowSettings;

import static com.farmerbb.taskbar.Constants.UNSUPPORTED;
import static com.farmerbb.taskbar.util.Constants.ACTION_SHOW_HIDE_TASKBAR;
import static com.farmerbb.taskbar.util.Constants.EXTRA_START_SERVICES;
import static com.farmerbb.taskbar.util.Constants.PREF_IS_HIDDEN;
import static com.farmerbb.taskbar.util.Constants.PREF_TASKBAR_ACTIVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class ShowHideTaskbarReceiverTest {
    private ShowHideTaskbarReceiver showHideTaskbarReceiver;
    private Application application;
    private Context context;
    private SharedPreferences prefs;
    private Intent notificationIntent;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        application = (Application) context;
        notificationIntent = new Intent(context, NotificationService.class);
        prefs = U.getSharedPreferences(context);
        showHideTaskbarReceiver = new ShowHideTaskbarReceiver();
    }

    @Test
    public void testSkipShowHideTaskbar() {
        ShadowSettings.setCanDrawOverlays(true);

        shadowOf(application).clearStartedServices();
        prefs.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();

        showHideTaskbarReceiver.onReceive(context, null);
        assertNull(shadowOf(application).peekNextStartedService());

        Intent intent = new Intent();
        showHideTaskbarReceiver.onReceive(context, intent);
        assertNull(shadowOf(application).peekNextStartedService());

        intent.setAction(ACTION_SHOW_HIDE_TASKBAR + UNSUPPORTED);
        showHideTaskbarReceiver.onReceive(context, intent);
        assertNull(shadowOf(application).peekNextStartedService());

        intent.setAction(ACTION_SHOW_HIDE_TASKBAR);
        showHideTaskbarReceiver.onReceive(context, intent);
        assertNull(shadowOf(application).peekNextStartedService());

        prefs.edit().putBoolean(PREF_TASKBAR_ACTIVE, true).apply();
        showHideTaskbarReceiver.onReceive(context, intent);
        Intent startedServiceIntent = shadowOf(application).peekNextStartedService();
        assertNotNull(startedServiceIntent);
        assertEquals(notificationIntent.getComponent(), startedServiceIntent.getComponent());
    }

    @Test
    public void testShowHideTaskbar() {
        Intent intent = new Intent(ACTION_SHOW_HIDE_TASKBAR);
        ShadowSettings.setCanDrawOverlays(true);
        prefs.edit().putBoolean(PREF_TASKBAR_ACTIVE, true).apply();

        shadowOf(application).clearStartedServices();
        prefs.edit().putBoolean(PREF_IS_HIDDEN, true).apply();
        showHideTaskbarReceiver.onReceive(context, intent);
        Intent startedServiceIntent = shadowOf(application).peekNextStartedService();
        assertNotNull(startedServiceIntent);
        assertEquals(notificationIntent.getComponent(), startedServiceIntent.getComponent());
        assertTrue(startedServiceIntent.getBooleanExtra(EXTRA_START_SERVICES, false));

        shadowOf(application).clearStartedServices();
        prefs.edit().putBoolean(PREF_IS_HIDDEN, false).apply();
        showHideTaskbarReceiver.onReceive(context, intent);
        startedServiceIntent = shadowOf(application).peekNextStartedService();
        assertNotNull(startedServiceIntent);
        assertEquals(notificationIntent.getComponent(), startedServiceIntent.getComponent());
        assertNull(startedServiceIntent.getExtras());
        assertTrue(prefs.getBoolean(PREF_IS_HIDDEN, false));
    }
}
