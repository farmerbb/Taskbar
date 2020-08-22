package com.farmerbb.taskbar.service;

import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.mockito.BooleanAnswer;
import com.farmerbb.taskbar.util.U;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static com.farmerbb.taskbar.Constants.UNSUPPORTED;
import static com.farmerbb.taskbar.util.Constants.PREF_START_BUTTON_IMAGE;
import static com.farmerbb.taskbar.util.Constants.PREF_START_BUTTON_IMAGE_APP_LOGO;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
@PrepareForTest(value = {U.class})
public class QuickSettingsTileServiceTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private QuickSettingsTileService tileService;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        tileService = Robolectric.setupService(QuickSettingsTileService.class);
        prefs = U.getSharedPreferences(tileService);
    }

    @Test
    public void testUpdateStateForIcon() {
        prefs.edit().putString(PREF_START_BUTTON_IMAGE, PREF_START_BUTTON_IMAGE_APP_LOGO).apply();
        tileService.updateState();
        Icon icon = tileService.getQsTile().getIcon();
        assertEquals(R.drawable.tb_system, shadowOf(icon).getResId());

        prefs.edit().putString(PREF_START_BUTTON_IMAGE, UNSUPPORTED).apply();
        tileService.updateState();
        icon = tileService.getQsTile().getIcon();
        assertEquals(R.drawable.tb_allapps, shadowOf(icon).getResId());
    }

    @Test
    public void testUpdateStateForState() {
        PowerMockito.spy(U.class);
        BooleanAnswer canDrawOverlaysAnswer = new BooleanAnswer();
        when(U.canDrawOverlays(tileService)).thenAnswer(canDrawOverlaysAnswer);
        BooleanAnswer isServiceRunningAnswer = new BooleanAnswer();
        when(U.isServiceRunning(tileService, NotificationService.class))
                .thenAnswer(isServiceRunningAnswer);

        canDrawOverlaysAnswer.answer = true;
        isServiceRunningAnswer.answer = true;
        tileService.updateState();
        assertEquals(Tile.STATE_ACTIVE, tileService.getQsTile().getState());

        canDrawOverlaysAnswer.answer = true;
        isServiceRunningAnswer.answer = false;
        tileService.updateState();
        assertEquals(Tile.STATE_INACTIVE, tileService.getQsTile().getState());

        canDrawOverlaysAnswer.answer = false;
        isServiceRunningAnswer.answer = true;
        tileService.updateState();
        assertEquals(Tile.STATE_UNAVAILABLE, tileService.getQsTile().getState());

        canDrawOverlaysAnswer.answer = false;
        isServiceRunningAnswer.answer = false;
        tileService.updateState();
        assertEquals(Tile.STATE_UNAVAILABLE, tileService.getQsTile().getState());
    }
}
