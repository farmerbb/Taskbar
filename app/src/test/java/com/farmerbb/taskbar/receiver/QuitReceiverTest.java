package com.farmerbb.taskbar.receiver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.util.U;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.farmerbb.taskbar.Constants.UNSUPPORTED;
import static com.farmerbb.taskbar.util.Constants.ACTION_QUIT;
import static com.farmerbb.taskbar.util.Constants.PREF_SKIP_QUIT_RECEIVER;
import static com.farmerbb.taskbar.util.Constants.PREF_TASKBAR_ACTIVE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class QuitReceiverTest {
    private QuitReceiver quitReceiver;
    private Context context;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        quitReceiver = new QuitReceiver();
        prefs = U.getSharedPreferences(context);
    }

    @Test
    public void testSkipQuitReceiver() {
        prefs.edit().putBoolean(PREF_SKIP_QUIT_RECEIVER, true).apply();

        quitReceiver.onReceive(context, null);
        assertTrue(prefs.getBoolean(PREF_SKIP_QUIT_RECEIVER, true));

        Intent intent = new Intent();
        quitReceiver.onReceive(context, intent);
        assertTrue(prefs.getBoolean(PREF_SKIP_QUIT_RECEIVER, true));

        intent.setAction(ACTION_QUIT + UNSUPPORTED);
        quitReceiver.onReceive(context, intent);
        assertTrue(prefs.getBoolean(PREF_SKIP_QUIT_RECEIVER, true));

        intent.setAction(ACTION_QUIT);
        quitReceiver.onReceive(context, intent);
        assertFalse(prefs.contains(PREF_SKIP_QUIT_RECEIVER));
    }

    @Test
    public void testNonSkipQuitReceiver() {
        prefs.edit().putBoolean(PREF_SKIP_QUIT_RECEIVER, false).apply();
        prefs.edit().putBoolean(PREF_TASKBAR_ACTIVE, true).apply();
        Intent intent = new Intent(ACTION_QUIT);
        quitReceiver.onReceive(context, intent);
        assertFalse(prefs.getBoolean(PREF_TASKBAR_ACTIVE, true));
    }
}
