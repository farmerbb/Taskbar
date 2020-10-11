package com.farmerbb.taskbar.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static android.os.Looper.getMainLooper;
import static com.farmerbb.taskbar.util.Constants.ACTION_NOTIFICATION_COUNT_CHANGED;
import static com.farmerbb.taskbar.util.Constants.ACTION_REQUEST_NOTIFICATION_COUNT;
import static com.farmerbb.taskbar.util.Constants.EXTRA_COUNT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class NotificationCountServiceTest {
    private NotificationCountReceiver receiver = new NotificationCountReceiver();
    private Context context;
    private NotificationCountService service;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        service = Robolectric.setupService(NotificationCountService.class);
        LocalBroadcastManager
                .getInstance(context)
                .registerReceiver(receiver, new IntentFilter(ACTION_NOTIFICATION_COUNT_CHANGED));
        receiver.reset();
    }

    @After
    public void tearDown() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }

    @Test
    public void testOnListenerConnected() {
        service.onListenerConnected();
        shadowOf(getMainLooper()).idle();
        assertTrue(receiver.count >= 0);

        receiver.reset();
        Intent intent = new Intent();
        intent.setAction(ACTION_REQUEST_NOTIFICATION_COUNT);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        shadowOf(getMainLooper()).idle();
        assertTrue(receiver.count >= 0);
    }

    @Test
    public void testOnListenerDisconnected() {
        receiver.count = Integer.MAX_VALUE;
        service.onListenerDisconnected();
        shadowOf(getMainLooper()).idle();
        assertEquals(0, receiver.count);
    }

    @Test
    public void testOnNotificationPosted() {
        service.onNotificationPosted(null);
        shadowOf(getMainLooper()).idle();
        assertTrue(receiver.count >= 0);
    }

    @Test
    public void testOnNotificationRemoved() {
        service.onNotificationRemoved(null);
        shadowOf(getMainLooper()).idle();
        assertTrue(receiver.count >= 0);
    }

    @Test
    public void testGetValidCount() {
        assertEquals(0, service.getValidCount(0));
        assertEquals(60, service.getValidCount(60));
        assertEquals(99, service.getValidCount(99));
        assertEquals(99, service.getValidCount(1000));
        assertEquals(99, service.getValidCount(Integer.MAX_VALUE));
    }

    private static class NotificationCountReceiver extends BroadcastReceiver {
        int count = -1;

        @Override
        public void onReceive(Context context, Intent intent) {
            count = intent.getIntExtra(EXTRA_COUNT, -1);
        }

        void reset() {
            count = -1;
        }
    }
}
