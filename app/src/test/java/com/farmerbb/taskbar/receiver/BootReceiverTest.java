package com.farmerbb.taskbar.receiver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.mockito.BooleanAnswer;
import com.farmerbb.taskbar.service.NotificationService;
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

import static com.farmerbb.taskbar.util.Constants.PREF_BLISS_OS_PREFS;
import static com.farmerbb.taskbar.util.Constants.PREF_FREEFORM_HACK;
import static com.farmerbb.taskbar.util.Constants.PREF_FREEFORM_HACK_OVERRIDE;
import static com.farmerbb.taskbar.util.Constants.PREF_START_ON_BOOT;
import static com.farmerbb.taskbar.util.Constants.PREF_TASKBAR_ACTIVE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
@PrepareForTest(U.class)
public class BootReceiverTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private BootReceiver bootReceiver;
    private Context context;
    private Intent intent;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        bootReceiver = new BootReceiver();
        intent = new Intent(Intent.ACTION_BOOT_COMPLETED);
        prefs = U.getSharedPreferences(context);
    }

    @After
    public void tearDown() {
        prefs.edit().remove(PREF_FREEFORM_HACK).apply();
    }

    @Test
    public void testPrefsInit() {
        PowerMockito.mockStatic(U.class);
        when(U.getSharedPreferences(context)).thenReturn(prefs);
        when(U.canEnableFreeform()).thenReturn(false);

        prefs.edit().putBoolean(PREF_FREEFORM_HACK, true).apply();
        prefs.edit().putBoolean(PREF_BLISS_OS_PREFS, false).apply();
        when(U.isAndroidGeneric(context)).thenReturn(true);
        bootReceiver.onReceive(context, intent);
        assertFalse(prefs.getBoolean(PREF_FREEFORM_HACK, true));

        prefs.edit().putBoolean(PREF_FREEFORM_HACK, true).apply();
        prefs.edit().putBoolean(PREF_BLISS_OS_PREFS, true).apply();
        when(U.isAndroidGeneric(context)).thenReturn(true);
        bootReceiver.onReceive(context, intent);
        assertFalse(prefs.getBoolean(PREF_FREEFORM_HACK, false));

        prefs.edit().putBoolean(PREF_FREEFORM_HACK, true).apply();
        prefs.edit().putBoolean(PREF_BLISS_OS_PREFS, false).apply();
        when(U.isAndroidGeneric(context)).thenReturn(false);
        bootReceiver.onReceive(context, intent);
        assertFalse(prefs.getBoolean(PREF_FREEFORM_HACK, false));

        prefs.edit().putBoolean(PREF_FREEFORM_HACK, true).apply();
        prefs.edit().putBoolean(PREF_BLISS_OS_PREFS, true).apply();
        when(U.isAndroidGeneric(context)).thenReturn(false);
        bootReceiver.onReceive(context, intent);
        assertFalse(prefs.getBoolean(PREF_FREEFORM_HACK, false));
    }

    @Test
    public void testPrefFreeformHackInit() {
        PowerMockito.mockStatic(U.class);
        when(U.getSharedPreferences(context)).thenReturn(prefs);
        BooleanAnswer answer = new BooleanAnswer();
        when(U.hasFreeformSupport(context)).thenAnswer(answer);

        answer.answer = false;
        bootReceiver.onReceive(context, intent);
        assertFalse(prefs.getBoolean(PREF_FREEFORM_HACK, true));

        answer.answer = true;
        prefs.edit().putBoolean(PREF_FREEFORM_HACK, true).apply();
        bootReceiver.onReceive(context, intent);
        assertTrue(prefs.getBoolean(PREF_FREEFORM_HACK, false));
    }

    @Test
    public void testStartOnBootInit() {
        prefs.edit().putBoolean(PREF_START_ON_BOOT, true).apply();
        bootReceiver.onReceive(context, intent);
        assertTrue(prefs.getBoolean(PREF_TASKBAR_ACTIVE, false));

        prefs.edit().putBoolean(PREF_START_ON_BOOT, false).apply();
        prefs.edit().putBoolean(PREF_TASKBAR_ACTIVE, true).apply();
        PowerMockito.mockStatic(U.class);
        when(U.getSharedPreferences(context)).thenReturn(prefs);
        when(U.isServiceRunning(context, NotificationService.class)).thenReturn(false);
        bootReceiver.onReceive(context, intent);
        assertFalse(prefs.getBoolean(PREF_TASKBAR_ACTIVE, false));
    }
}
