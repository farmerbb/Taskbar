package com.farmerbb.taskbar.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.KeyEvent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.farmerbb.taskbar.util.Constants.ACTION_HIDE_START_MENU;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class StartMenuLayoutTest {
    private Context context;
    private StartMenuLayout layout;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        layout = new StartMenuLayout(context);
    }

    @Test
    public void testDispatchKeyEvent() {
        IntentFilter filter = new IntentFilter(ACTION_HIDE_START_MENU);
        TestBroadcastReceiver receiver = new TestBroadcastReceiver();
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK);
        layout.dispatchKeyEvent(keyEvent);
        assertFalse(receiver.onReceived);
        layout.viewHandlesBackButton();
        layout.dispatchKeyEvent(keyEvent);
        assertTrue(receiver.onReceived);
    }

    private static class TestBroadcastReceiver extends BroadcastReceiver {
        private boolean onReceived;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !ACTION_HIDE_START_MENU.equals(intent.getAction())) {
                return;
            }
            onReceived = true;
        }
    }
}
