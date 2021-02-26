package com.farmerbb.taskbar.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.KeyEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.util.Constants
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class StartMenuLayoutTest {
    private lateinit var context: Context
    private lateinit var layout: StartMenuLayout

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        layout = StartMenuLayout(context)
    }

    @Test
    fun testDispatchKeyEvent() {
        val filter = IntentFilter(Constants.ACTION_HIDE_START_MENU)
        val receiver = TestBroadcastReceiver()
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)
        layout.dispatchKeyEvent(keyEvent)
        Assert.assertFalse(receiver.onReceived)
        layout.viewHandlesBackButton()
        layout.dispatchKeyEvent(keyEvent)
        Assert.assertTrue(receiver.onReceived)
    }

    private class TestBroadcastReceiver : BroadcastReceiver() {
        var onReceived = false
        override fun onReceive(context: Context, intent: Intent) {
            if (Constants.ACTION_HIDE_START_MENU != intent.action) {
                return
            }
            onReceived = true
        }
    }
}
