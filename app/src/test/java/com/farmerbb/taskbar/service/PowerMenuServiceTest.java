package com.farmerbb.taskbar.service;

import android.content.Intent;

import com.farmerbb.taskbar.util.U;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowAccessibilityService;

import static com.farmerbb.taskbar.util.Constants.ACTION_ACCESSIBILITY_ACTION;
import static com.farmerbb.taskbar.util.Constants.EXTRA_ACTION;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class PowerMenuServiceTest {
    @Test
    public void testAccessibilityActionPerforming() {
        PowerMenuService service = Robolectric.setupService(PowerMenuService.class);
        Intent intent = new Intent(ACTION_ACCESSIBILITY_ACTION);
        int testAction = 1000;
        intent.putExtra(EXTRA_ACTION, testAction);
        U.sendBroadcast(service, intent);
        ShadowAccessibilityService shadowService = shadowOf(service);
        assertTrue(shadowService.getGlobalActionsPerformed().contains(testAction));
    }
}
