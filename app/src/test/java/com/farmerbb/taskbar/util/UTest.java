package com.farmerbb.taskbar.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.BuildConfig;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_ANDROID_X86_PREFS;
import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_AUTO_HIDE_NAVBAR;
import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_BLISS_OS_PREFS;
import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_BUTTON_BACK;
import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_BUTTON_HOME;
import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_BUTTON_RECENTS;
import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_FREEFORM_HACK;
import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_FREEFORM_HACK_OVERRIDE;
import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_MAX_NUM_OF_RECENTS;
import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_RECENTS_AMOUNT;
import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_REFRESH_FREQUENCY;
import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_SAVE_WINDOW_SIZES;
import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_SHORTCUT_ICON;
import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_SHOW_FREEFORM_DISABLED_MESSAGE;
import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_SORT_ORDER;
import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_START_BUTTON_IMAGE;
import static com.farmerbb.taskbar.util.SharedPreferenceConstant.SP_KEY_WINDOW_SIZE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
@PrepareForTest(U.class)
public class UTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        assertNotNull(context);
    }

    @Test
    public void testInitPrefsForBlissOS() {
        PowerMockito.spy(U.class);
        when(U.isBlissOs(any(Context.class))).thenReturn(true);
        assertTrue(U.isBlissOs(context));
        SharedPreferences prefs = U.getSharedPreferences(context);
        assertFalse(prefs.getBoolean(SP_KEY_BLISS_OS_PREFS, false));
        U.initPrefs(context);
        assertEquals(
                "running_apps_only",
                prefs.getString(SP_KEY_RECENTS_AMOUNT, "")
        );
        assertEquals("0", prefs.getString(SP_KEY_REFRESH_FREQUENCY, ""));
        assertEquals("2147483647", prefs.getString(SP_KEY_MAX_NUM_OF_RECENTS, ""));
        assertEquals("true", prefs.getString(SP_KEY_SORT_ORDER, ""));
        assertEquals("app_logo", prefs.getString(SP_KEY_START_BUTTON_IMAGE, ""));
        assertTrue(prefs.getBoolean(SP_KEY_BUTTON_BACK, false));
        assertTrue(prefs.getBoolean(SP_KEY_BUTTON_HOME, false));
        assertTrue(prefs.getBoolean(SP_KEY_BUTTON_RECENTS, false));
        assertTrue(prefs.getBoolean(SP_KEY_AUTO_HIDE_NAVBAR, false));
        assertFalse(prefs.getBoolean(SP_KEY_SHORTCUT_ICON, true));
        assertTrue(prefs.getBoolean(SP_KEY_BLISS_OS_PREFS, false));
        prefs.edit().putBoolean(SP_KEY_BLISS_OS_PREFS, false);
    }

    @Test
    public void testInitPrefsForNormalWithCanEnableFreeformAndHackOverrideFalse() {
        PowerMockito.spy(U.class);
        when(U.canEnableFreeform()).thenReturn(true);
        SharedPreferences prefs = U.getSharedPreferences(context);
        prefs.edit().putBoolean(SP_KEY_FREEFORM_HACK_OVERRIDE, false).apply();
        U.initPrefs(context);
        assertEquals(
                U.hasFreeformSupport(context) && !U.isSamsungDevice(),
                prefs.getBoolean(SP_KEY_FREEFORM_HACK, false)
        );
        assertFalse(prefs.getBoolean(SP_KEY_SAVE_WINDOW_SIZES, true));
        assertTrue(prefs.getBoolean(SP_KEY_FREEFORM_HACK_OVERRIDE, false));
    }

    @Test
    public void testInitPrefsForNormalWithCanEnableFreeformAndHackOverrideTrueButNoSupport() {
        PowerMockito.spy(U.class);
        when(U.canEnableFreeform()).thenReturn(true);
        SharedPreferences prefs = U.getSharedPreferences(context);
        prefs.edit().putBoolean(SP_KEY_FREEFORM_HACK_OVERRIDE, true).apply();
        when(U.hasFreeformSupport(context)).thenReturn(false);
        U.initPrefs(context);
        assertFalse(prefs.getBoolean(SP_KEY_FREEFORM_HACK, false));
    }

    @Test
    public void testInitPrefsForNormalWithCantEnableFreeform() {
        PowerMockito.spy(U.class);
        when(U.canEnableFreeform()).thenReturn(false);
        SharedPreferences prefs = U.getSharedPreferences(context);
        U.initPrefs(context);
        assertFalse(prefs.getBoolean(SP_KEY_FREEFORM_HACK, false));
        prefs.edit()
                .putBoolean(SP_KEY_FREEFORM_HACK, false)
                .putBoolean(SP_KEY_SHOW_FREEFORM_DISABLED_MESSAGE, false)
                .apply();
        U.initPrefs(context);
        assertFalse(prefs.getBoolean(SP_KEY_SHOW_FREEFORM_DISABLED_MESSAGE, false));
        prefs.edit()
                .putBoolean(SP_KEY_FREEFORM_HACK, true)
                .putBoolean(SP_KEY_SHOW_FREEFORM_DISABLED_MESSAGE, false)
                .apply();
        U.initPrefs(context);
        assertTrue(prefs.getBoolean(SP_KEY_SHOW_FREEFORM_DISABLED_MESSAGE, false));
        prefs.edit()
                .putBoolean(SP_KEY_FREEFORM_HACK, false)
                .putBoolean(SP_KEY_SHOW_FREEFORM_DISABLED_MESSAGE, true)
                .apply();
        U.initPrefs(context);
        assertTrue(prefs.getBoolean(SP_KEY_SHOW_FREEFORM_DISABLED_MESSAGE, false));
        prefs.edit()
                .putBoolean(SP_KEY_FREEFORM_HACK, true)
                .putBoolean(SP_KEY_SHOW_FREEFORM_DISABLED_MESSAGE, true)
                .apply();
        U.initPrefs(context);
        assertTrue(prefs.getBoolean(SP_KEY_SHOW_FREEFORM_DISABLED_MESSAGE, false));
    }
}

