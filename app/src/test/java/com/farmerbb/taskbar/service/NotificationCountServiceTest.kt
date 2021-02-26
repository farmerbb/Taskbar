package com.farmerbb.taskbar.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.util.Constants
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class NotificationCountServiceTest {
    private val receiver = NotificationCountReceiver()
    private lateinit var context: Context
    private lateinit var service: NotificationCountService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        service = Robolectric.setupService(NotificationCountService::class.java)
        LocalBroadcastManager
                .getInstance(context)
                .registerReceiver(receiver,
                        IntentFilter(Constants.ACTION_NOTIFICATION_COUNT_CHANGED))
        receiver.reset()
    }

    @After
    fun tearDown() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
    }

    @Test
    fun testOnListenerConnected() {
        service.onListenerConnected()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Assert.assertTrue(receiver.count >= 0)
        receiver.reset()
        val intent = Intent()
        intent.action = Constants.ACTION_REQUEST_NOTIFICATION_COUNT
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Assert.assertTrue(receiver.count >= 0)
    }

    @Test
    fun testOnListenerDisconnected() {
        receiver.count = Int.MAX_VALUE
        service.onListenerDisconnected()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Assert.assertEquals(0, receiver.count.toLong())
    }

    @Test
    fun testOnNotificationPosted() {
        service.onNotificationPosted(null)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Assert.assertTrue(receiver.count >= 0)
    }

    @Test
    fun testOnNotificationRemoved() {
        service.onNotificationRemoved(null)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Assert.assertTrue(receiver.count >= 0)
    }

    @Test
    fun testGetValidCount() {
        Assert.assertEquals(0, service.getValidCount(0).toLong())
        Assert.assertEquals(60, service.getValidCount(60).toLong())
        Assert.assertEquals(99, service.getValidCount(99).toLong())
        Assert.assertEquals(99, service.getValidCount(1000).toLong())
        Assert.assertEquals(99, service.getValidCount(Int.MAX_VALUE).toLong())
    }

    private class NotificationCountReceiver : BroadcastReceiver() {
        var count = -1
        override fun onReceive(context: Context, intent: Intent) {
            count = intent.getIntExtra(Constants.EXTRA_COUNT, -1)
        }

        fun reset() {
            count = -1
        }
    }
}
