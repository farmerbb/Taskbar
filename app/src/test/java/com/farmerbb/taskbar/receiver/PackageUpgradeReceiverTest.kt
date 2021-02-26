package com.farmerbb.taskbar.receiver

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.activity.DummyActivity
import com.farmerbb.taskbar.mockito.BooleanAnswer
import com.farmerbb.taskbar.service.NotificationService
import com.farmerbb.taskbar.util.Constants
import com.farmerbb.taskbar.util.U
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.rule.PowerMockRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
@PowerMockIgnore("org.mockito.*", "org.robolectric.*", "android.*", "androidx.*")
@PrepareForTest(U::class)
class PackageUpgradeReceiverTest {
    @get:Rule
    val rule = PowerMockRule()
    private lateinit var packageUpgradeReceiver: PackageUpgradeReceiver
    private lateinit var context: Context
    private lateinit var intent: Intent
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        packageUpgradeReceiver = PackageUpgradeReceiver()
        intent = Intent(Intent.ACTION_MY_PACKAGE_REPLACED)
        prefs = U.getSharedPreferences(context)
    }

    @Test
    fun testStartDummyActivity() {
        PowerMockito.mockStatic(U::class.java)
        PowerMockito.`when`(U.getSharedPreferences(context)).thenReturn(prefs)
        val answer = BooleanAnswer()
        PowerMockito.`when`(U.hasFreeformSupport(context)).thenAnswer(answer)
        PowerMockito.`when`(U.isFreeformModeEnabled(context)).thenAnswer(answer)
        val application = context as Application?
        Shadows.shadowOf(application).clearNextStartedActivities()
        prefs.edit().putBoolean(Constants.PREF_TASKBAR_ACTIVE, true).apply()
        prefs.edit().putBoolean(Constants.PREF_IS_HIDDEN, false).apply()
        answer.answer = true
        packageUpgradeReceiver.onReceive(context, intent)
        val startedIntent = Shadows.shadowOf(application).peekNextStartedActivity()
        Assert.assertNotNull(startedIntent)
        Assert.assertTrue(startedIntent.getBooleanExtra(Constants.EXTRA_START_FREEFORM_HACK, false))
        Assert.assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK.toLong(), startedIntent.flags.toLong())
        Assert.assertNotNull(startedIntent.component)
        Assert.assertEquals(DummyActivity::class.java.canonicalName,
                startedIntent.component!!.className)
        Shadows.shadowOf(application).clearNextStartedActivities()
        answer.answer = false
        packageUpgradeReceiver.onReceive(context, intent)
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedActivity())
        Shadows.shadowOf(application).clearNextStartedActivities()
        answer.answer = true
        prefs.edit().putBoolean(Constants.PREF_IS_HIDDEN, true).apply()
        packageUpgradeReceiver.onReceive(context, intent)
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedActivity())
        Shadows.shadowOf(application).clearNextStartedActivities()
        prefs.edit().putBoolean(Constants.PREF_TASKBAR_ACTIVE, false).apply()
        prefs.edit().putBoolean(Constants.PREF_IS_HIDDEN, false).apply()
        packageUpgradeReceiver.onReceive(context, intent)
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedActivity())
    }

    @Test
    fun testStartNotificationService() {
        val application = context as Application?
        Shadows.shadowOf(application).clearStartedServices()
        ShadowSettings.setCanDrawOverlays(true)
        prefs.edit().putBoolean(Constants.PREF_TASKBAR_ACTIVE, true).apply()
        prefs.edit().putBoolean(Constants.PREF_IS_HIDDEN, false).apply()
        packageUpgradeReceiver.onReceive(context, intent)
        var startedIntent = Shadows.shadowOf(application).peekNextStartedService()
        Assert.assertNotNull(startedIntent)
        Assert.assertTrue(startedIntent.getBooleanExtra(Constants.EXTRA_START_SERVICES, false))
        Assert.assertNotNull(startedIntent.component)
        Assert.assertEquals(
                NotificationService::class.java.canonicalName,
                startedIntent.component!!.className
        )
        Shadows.shadowOf(application).clearStartedServices()
        ShadowSettings.setCanDrawOverlays(false)
        prefs.edit().putBoolean(Constants.PREF_TASKBAR_ACTIVE, true).apply()
        prefs.edit().putBoolean(Constants.PREF_IS_HIDDEN, false).apply()
        packageUpgradeReceiver.onReceive(context, intent)
        startedIntent = Shadows.shadowOf(application).peekNextStartedService()
        Assert.assertNull(startedIntent)
        Shadows.shadowOf(application).clearStartedServices()
        ShadowSettings.setCanDrawOverlays(true)
        prefs.edit().putBoolean(Constants.PREF_TASKBAR_ACTIVE, true).apply()
        prefs.edit().putBoolean(Constants.PREF_IS_HIDDEN, true).apply()
        packageUpgradeReceiver.onReceive(context, intent)
        startedIntent = Shadows.shadowOf(application).peekNextStartedService()
        Assert.assertNotNull(startedIntent)
        Assert.assertFalse(startedIntent.getBooleanExtra(Constants.EXTRA_START_SERVICES, true))
        Assert.assertNotNull(startedIntent.component)
        Assert.assertEquals(
                NotificationService::class.java.canonicalName,
                startedIntent.component!!.className
        )
        Shadows.shadowOf(application).clearStartedServices()
        prefs.edit().putBoolean(Constants.PREF_TASKBAR_ACTIVE, false).apply()
        packageUpgradeReceiver.onReceive(context, intent)
        Assert.assertNull(Shadows.shadowOf(application).peekNextStartedService())
    }
}
