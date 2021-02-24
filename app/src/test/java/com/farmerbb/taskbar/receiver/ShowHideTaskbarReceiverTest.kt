package com.farmerbb.taskbar.receiver

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.Constants.UNSUPPORTED
import com.farmerbb.taskbar.service.NotificationService
import com.farmerbb.taskbar.util.Constants
import com.farmerbb.taskbar.util.U
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
class ShowHideTaskbarReceiverTest {
    private lateinit var showHideTaskbarReceiver: ShowHideTaskbarReceiver
    private lateinit var application: Application
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var notificationIntent: Intent

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        notificationIntent = Intent(context, NotificationService::class.java)
        prefs = U.getSharedPreferences(context)
        showHideTaskbarReceiver = ShowHideTaskbarReceiver()
    }

    @Test
    fun testSkipShowHideTaskbar() {
        ShadowSettings.setCanDrawOverlays(true)
        Shadows.shadowOf(application).clearStartedServices()
        prefs.edit().putBoolean(Constants.PREF_TASKBAR_ACTIVE, false).apply()
        showHideTaskbarReceiver.onReceive(context, null)
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedService())
        val intent = Intent()
        showHideTaskbarReceiver.onReceive(context, intent)
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedService())
        intent.action = Constants.ACTION_SHOW_HIDE_TASKBAR + UNSUPPORTED
        showHideTaskbarReceiver.onReceive(context, intent)
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedService())
        intent.action = Constants.ACTION_SHOW_HIDE_TASKBAR
        showHideTaskbarReceiver.onReceive(context, intent)
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedService())
        prefs.edit().putBoolean(Constants.PREF_TASKBAR_ACTIVE, true).apply()
        showHideTaskbarReceiver.onReceive(context, intent)
        val startedServiceIntent = Shadows.shadowOf(application).peekNextStartedService()
        Assert.assertNotNull(startedServiceIntent)
        Assert.assertEquals(notificationIntent.component, startedServiceIntent.component)
    }

    @Test
    fun testShowHideTaskbar() {
        val intent = Intent(Constants.ACTION_SHOW_HIDE_TASKBAR)
        ShadowSettings.setCanDrawOverlays(true)
        prefs.edit().putBoolean(Constants.PREF_TASKBAR_ACTIVE, true).apply()
        Shadows.shadowOf(application).clearStartedServices()
        prefs.edit().putBoolean(Constants.PREF_IS_HIDDEN, true).apply()
        showHideTaskbarReceiver.onReceive(context, intent)
        var startedServiceIntent = Shadows.shadowOf(application).peekNextStartedService()
        Assert.assertNotNull(startedServiceIntent)
        Assert.assertEquals(notificationIntent.component, startedServiceIntent.component)
        Assert.assertTrue(startedServiceIntent.getBooleanExtra(
                Constants.EXTRA_START_SERVICES, false))
        Shadows.shadowOf(application).clearStartedServices()
        prefs.edit().putBoolean(Constants.PREF_IS_HIDDEN, false).apply()
        showHideTaskbarReceiver.onReceive(context, intent)
        startedServiceIntent = Shadows.shadowOf(application).peekNextStartedService()
        Assert.assertNotNull(startedServiceIntent)
        Assert.assertEquals(notificationIntent.component, startedServiceIntent.component)
        Assert.assertNull(startedServiceIntent.extras)
        Assert.assertTrue(prefs.getBoolean(Constants.PREF_IS_HIDDEN, false))
    }
}
