package com.farmerbb.taskbar.receiver

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.Constants.UNSUPPORTED
import com.farmerbb.taskbar.util.Constants
import com.farmerbb.taskbar.util.U
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuitReceiverTest {
    private lateinit var quitReceiver: QuitReceiver
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        quitReceiver = QuitReceiver()
        prefs = U.getSharedPreferences(context)
    }

    @Test
    fun testSkipQuitReceiver() {
        prefs.edit().putBoolean(Constants.PREF_SKIP_QUIT_RECEIVER, true).apply()
        quitReceiver.onReceive(context, null)
        Assert.assertTrue(prefs.getBoolean(Constants.PREF_SKIP_QUIT_RECEIVER, true))
        val intent = Intent()
        quitReceiver.onReceive(context, intent)
        Assert.assertTrue(prefs.getBoolean(Constants.PREF_SKIP_QUIT_RECEIVER, true))
        intent.action = Constants.ACTION_QUIT + UNSUPPORTED
        quitReceiver.onReceive(context, intent)
        Assert.assertTrue(prefs.getBoolean(Constants.PREF_SKIP_QUIT_RECEIVER, true))
        intent.action = Constants.ACTION_QUIT
        quitReceiver.onReceive(context, intent)
        Assert.assertFalse(prefs.contains(Constants.PREF_SKIP_QUIT_RECEIVER))
    }

    @Test
    fun testNonSkipQuitReceiver() {
        prefs.edit().putBoolean(Constants.PREF_SKIP_QUIT_RECEIVER, false).apply()
        prefs.edit().putBoolean(Constants.PREF_TASKBAR_ACTIVE, true).apply()
        val intent = Intent(Constants.ACTION_QUIT)
        quitReceiver.onReceive(context, intent)
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_TASKBAR_ACTIVE, true))
    }
}
