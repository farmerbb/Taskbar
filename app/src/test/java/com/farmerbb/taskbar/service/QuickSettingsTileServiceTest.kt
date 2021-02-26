package com.farmerbb.taskbar.service

import android.content.SharedPreferences
import android.service.quicksettings.Tile
import com.farmerbb.taskbar.Constants.UNSUPPORTED
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.mockito.BooleanAnswer
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
@PowerMockIgnore("org.mockito.*", "org.robolectric.*", "android.*", "androidx.*")
@PrepareForTest(value = [U::class])
class QuickSettingsTileServiceTest {
    @get:Rule
    val rule = PowerMockRule()
    private lateinit var tileService: QuickSettingsTileService
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        tileService = Robolectric.setupService(QuickSettingsTileService::class.java)
        prefs = U.getSharedPreferences(tileService)
    }

    @Test
    fun testUpdateStateForIcon() {
        prefs.edit().putString(Constants.PREF_START_BUTTON_IMAGE,
                Constants.PREF_START_BUTTON_IMAGE_APP_LOGO).apply()
        tileService.updateState()
        var icon = tileService.qsTile.icon
        Assert.assertEquals(R.drawable.tb_system.toLong(), Shadows.shadowOf(icon).resId.toLong())
        prefs.edit().putString(Constants.PREF_START_BUTTON_IMAGE, UNSUPPORTED).apply()
        tileService.updateState()
        icon = tileService.qsTile.icon
        Assert.assertEquals(R.drawable.tb_allapps.toLong(), Shadows.shadowOf(icon).resId.toLong())
    }

    @Test
    fun testUpdateStateForState() {
        PowerMockito.spy(U::class.java)
        val canDrawOverlaysAnswer = BooleanAnswer()
        PowerMockito.`when`(U.canDrawOverlays(tileService)).thenAnswer(canDrawOverlaysAnswer)
        val isServiceRunningAnswer = BooleanAnswer()
        PowerMockito.`when`(U.isServiceRunning(tileService, NotificationService::class.java))
                .thenAnswer(isServiceRunningAnswer)
        canDrawOverlaysAnswer.answer = true
        isServiceRunningAnswer.answer = true
        tileService.updateState()
        Assert.assertEquals(Tile.STATE_ACTIVE.toLong(), tileService.qsTile.state.toLong())
        canDrawOverlaysAnswer.answer = true
        isServiceRunningAnswer.answer = false
        tileService.updateState()
        Assert.assertEquals(Tile.STATE_INACTIVE.toLong(), tileService.qsTile.state.toLong())
        canDrawOverlaysAnswer.answer = false
        isServiceRunningAnswer.answer = true
        tileService.updateState()
        Assert.assertEquals(Tile.STATE_UNAVAILABLE.toLong(), tileService.qsTile.state.toLong())
        canDrawOverlaysAnswer.answer = false
        isServiceRunningAnswer.answer = false
        tileService.updateState()
        Assert.assertEquals(Tile.STATE_UNAVAILABLE.toLong(), tileService.qsTile.state.toLong())
    }
}
