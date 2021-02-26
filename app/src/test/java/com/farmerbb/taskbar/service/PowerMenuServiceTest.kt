package com.farmerbb.taskbar.service

import android.content.Intent
import com.farmerbb.taskbar.util.Constants
import com.farmerbb.taskbar.util.U
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class PowerMenuServiceTest {
    @Test
    fun testAccessibilityActionPerforming() {
        val service = Robolectric.setupService(PowerMenuService::class.java)
        val intent = Intent(Constants.ACTION_ACCESSIBILITY_ACTION)
        val testAction = 1000
        intent.putExtra(Constants.EXTRA_ACTION, testAction)
        U.sendBroadcast(service, intent)
        val shadowService = Shadows.shadowOf(service)
        Assert.assertTrue(shadowService.globalActionsPerformed.contains(testAction))
    }
}
