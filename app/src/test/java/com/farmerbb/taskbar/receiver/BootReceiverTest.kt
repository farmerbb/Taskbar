package com.farmerbb.taskbar.receiver

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.mockito.BooleanAnswer
import com.farmerbb.taskbar.service.NotificationService
import com.farmerbb.taskbar.util.Constants
import com.farmerbb.taskbar.util.U
import org.junit.After
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

@RunWith(RobolectricTestRunner::class)
@PowerMockIgnore("org.mockito.*", "org.robolectric.*", "android.*", "androidx.*")
@PrepareForTest(U::class)
class BootReceiverTest {
    @get:Rule
    val rule = PowerMockRule()
    private lateinit var bootReceiver: BootReceiver
    private lateinit var context: Context
    private lateinit var intent: Intent
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        bootReceiver = BootReceiver()
        intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        prefs = U.getSharedPreferences(context)
    }

    @After
    fun tearDown() {
        prefs.edit().remove(Constants.PREF_FREEFORM_HACK).apply()
    }

    @Test
    fun testPrefsInit() {
        PowerMockito.mockStatic(U::class.java)
        PowerMockito.`when`(U.getSharedPreferences(context)).thenReturn(prefs)
        PowerMockito.`when`(U.canEnableFreeform(context)).thenReturn(false)
        prefs.edit().putBoolean(Constants.PREF_FREEFORM_HACK, true).apply()
        prefs.edit().putBoolean(Constants.PREF_BLISS_OS_PREFS, false).apply()
        PowerMockito.`when`(U.isAndroidGeneric(context)).thenReturn(true)
        bootReceiver.onReceive(context, intent)
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_FREEFORM_HACK, true))
        prefs.edit().putBoolean(Constants.PREF_FREEFORM_HACK, true).apply()
        prefs.edit().putBoolean(Constants.PREF_BLISS_OS_PREFS, true).apply()
        PowerMockito.`when`(U.isAndroidGeneric(context)).thenReturn(true)
        bootReceiver.onReceive(context, intent)
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_FREEFORM_HACK, false))
        prefs.edit().putBoolean(Constants.PREF_FREEFORM_HACK, true).apply()
        prefs.edit().putBoolean(Constants.PREF_BLISS_OS_PREFS, false).apply()
        PowerMockito.`when`(U.isAndroidGeneric(context)).thenReturn(false)
        bootReceiver.onReceive(context, intent)
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_FREEFORM_HACK, false))
        prefs.edit().putBoolean(Constants.PREF_FREEFORM_HACK, true).apply()
        prefs.edit().putBoolean(Constants.PREF_BLISS_OS_PREFS, true).apply()
        PowerMockito.`when`(U.isAndroidGeneric(context)).thenReturn(false)
        bootReceiver.onReceive(context, intent)
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_FREEFORM_HACK, false))
    }

    @Test
    fun testPrefFreeformHackInit() {
        PowerMockito.mockStatic(U::class.java)
        PowerMockito.`when`(U.getSharedPreferences(context)).thenReturn(prefs)
        val answer = BooleanAnswer()
        PowerMockito.`when`(U.hasFreeformSupport(context)).thenAnswer(answer)
        answer.answer = false
        bootReceiver.onReceive(context, intent)
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_FREEFORM_HACK, true))
        answer.answer = true
        prefs.edit().putBoolean(Constants.PREF_FREEFORM_HACK, true).apply()
        bootReceiver.onReceive(context, intent)
        Assert.assertTrue(prefs.getBoolean(Constants.PREF_FREEFORM_HACK, false))
    }

    @Test
    fun testStartOnBootInit() {
        prefs.edit().putBoolean(Constants.PREF_START_ON_BOOT, true).apply()
        bootReceiver.onReceive(context, intent)
        Assert.assertTrue(prefs.getBoolean(Constants.PREF_TASKBAR_ACTIVE, false))
        prefs.edit().putBoolean(Constants.PREF_START_ON_BOOT, false).apply()
        prefs.edit().putBoolean(Constants.PREF_TASKBAR_ACTIVE, true).apply()
        PowerMockito.mockStatic(U::class.java)
        PowerMockito.`when`(U.getSharedPreferences(context)).thenReturn(prefs)
        PowerMockito.`when`(U.isServiceRunning(context, NotificationService::class.java))
                .thenReturn(false)
        bootReceiver.onReceive(context, intent)
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_TASKBAR_ACTIVE, false))
    }
}
