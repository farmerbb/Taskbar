package com.farmerbb.taskbar.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.ImageView;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.U;

import org.bouncycastle.jcajce.provider.symmetric.Grain128;
import org.junit.After;
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
import org.robolectric.android.controller.ServiceController;

import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_RIGHT;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_VERTICAL_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_VERTICAL_RIGHT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_RIGHT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_VERTICAL_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_VERTICAL_RIGHT;
import static com.farmerbb.taskbar.util.Constants.PREF_START_BUTTON_IMAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.robolectric.util.ReflectionHelpers.ClassParameter.from;
import static org.robolectric.util.ReflectionHelpers.callInstanceMethod;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
@PrepareForTest(U.class)
public class TaskbarControllerTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private ServiceController<TaskbarUIHostService> controller;
    private TaskbarUIHostService hostService;
    private TaskbarController uiController;
    private Context context;
    SharedPreferences prefs;

    @Before
    public void setUp() {
        controller = Robolectric.buildService(TaskbarUIHostService.class);
        hostService = controller.create().get();
        uiController = hostService.controller;
        context = ApplicationProvider.getApplicationContext();
        prefs = U.getSharedPreferences(context);
    }

    @After
    public void tearDown() {
        prefs.edit().remove(PREF_START_BUTTON_IMAGE).apply();
    }

    @Test
    public void testInitialization() {
        assertNotNull(uiController);
    }

    @Test
    public void testDrawStartButtonPadding() {
        ImageView startButton = new ImageView(context);
        prefs = U.getSharedPreferences(context);
        prefs.edit().putString(PREF_START_BUTTON_IMAGE, "default").apply();
        callDrawStartButton(context, startButton, prefs);
        int padding =
                context.getResources().getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding);
        checkStartButtonPadding(padding, startButton);

        PowerMockito.spy(U.class);
        // Use bliss os logic to avoid using LauncherApps, that robolectric doesn't support
        when(U.isBlissOs(context)).thenReturn(true);
        prefs.edit().putString(PREF_START_BUTTON_IMAGE, "app_logo").apply();
        callDrawStartButton(context, startButton, prefs);
        padding =
                context.getResources().getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding_alt);
        checkStartButtonPadding(padding, startButton);

        prefs.edit().putString(PREF_START_BUTTON_IMAGE, "custom").apply();
        callDrawStartButton(context, startButton, prefs);
        padding = context.getResources().getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding);
        checkStartButtonPadding(padding, startButton);

        prefs.edit().putString(PREF_START_BUTTON_IMAGE, "non-support").apply();
        callDrawStartButton(context, startButton, prefs);
        checkStartButtonPadding(0, startButton);
    }

    @Test
    public void testGetTaskbarGravity() {
        assertEquals(
                Gravity.BOTTOM | Gravity.LEFT,
                callGetTaskbarGravity(POSITION_BOTTOM_LEFT)
        );
        assertEquals(
                Gravity.BOTTOM | Gravity.LEFT,
                callGetTaskbarGravity(POSITION_BOTTOM_VERTICAL_LEFT)
        );
        assertEquals(
                Gravity.BOTTOM | Gravity.RIGHT,
                callGetTaskbarGravity(POSITION_BOTTOM_RIGHT)
        );
        assertEquals(
                Gravity.BOTTOM | Gravity.RIGHT,
                callGetTaskbarGravity(POSITION_BOTTOM_VERTICAL_RIGHT)
        );
        assertEquals(
                Gravity.TOP | Gravity.LEFT,
                callGetTaskbarGravity(POSITION_TOP_LEFT)
        );
        assertEquals(
                Gravity.TOP | Gravity.LEFT,
                callGetTaskbarGravity(POSITION_TOP_VERTICAL_LEFT)
        );
        assertEquals(
                Gravity.TOP | Gravity.RIGHT,
                callGetTaskbarGravity(POSITION_TOP_RIGHT)
        );
        assertEquals(
                Gravity.TOP | Gravity.RIGHT,
                callGetTaskbarGravity(POSITION_TOP_VERTICAL_RIGHT)
        );
        assertEquals(
                Gravity.BOTTOM | Gravity.LEFT,
                callGetTaskbarGravity("unsupported")
        );
    }

    @Test
    public void testGetTaskbarLayoutId() {
        assertEquals(
                R.layout.tb_taskbar_left,
                callGetTaskbarLayoutId(POSITION_BOTTOM_LEFT)
        );
        assertEquals(
                R.layout.tb_taskbar_vertical,
                callGetTaskbarLayoutId(POSITION_BOTTOM_VERTICAL_LEFT)
        );
        assertEquals(
                R.layout.tb_taskbar_right,
                callGetTaskbarLayoutId(POSITION_BOTTOM_RIGHT)
        );
        assertEquals(
                R.layout.tb_taskbar_vertical,
                callGetTaskbarLayoutId(POSITION_BOTTOM_VERTICAL_RIGHT)
        );
        assertEquals(
                R.layout.tb_taskbar_left,
                callGetTaskbarLayoutId(POSITION_TOP_LEFT)
        );
        assertEquals(
                R.layout.tb_taskbar_top_vertical,
                callGetTaskbarLayoutId(POSITION_TOP_VERTICAL_LEFT)
        );
        assertEquals(
                R.layout.tb_taskbar_right,
                callGetTaskbarLayoutId(POSITION_TOP_RIGHT)
        );
        assertEquals(
                R.layout.tb_taskbar_top_vertical,
                callGetTaskbarLayoutId(POSITION_TOP_VERTICAL_RIGHT)
        );
        assertEquals(
                R.layout.tb_taskbar_left,
                callGetTaskbarLayoutId("unsupported")
        );
    }

    private int callGetTaskbarLayoutId(String taskbarPosition) {
        return callInstanceMethod(
                uiController,
                "getTaskbarLayoutId",
                from(String.class, taskbarPosition)
        );
    }

    private int callGetTaskbarGravity(String taskbarPosition) {
        return callInstanceMethod(
                uiController,
                "getTaskbarGravity",
                from(String.class, taskbarPosition)
        );
    }

    private void callDrawStartButton(Context context,
                                     ImageView startButton,
                                     SharedPreferences prefs) {
        callInstanceMethod(
                uiController,
                "drawStartButton",
                from(Context.class, context),
                from(ImageView.class, startButton),
                from(SharedPreferences.class, prefs),
                from(int.class, Color.RED)
        );
    }


    private void checkStartButtonPadding(int padding, ImageView startButton) {
        assertEquals(padding, startButton.getPaddingLeft());
        assertEquals(padding, startButton.getPaddingTop());
        assertEquals(padding, startButton.getPaddingRight());
        assertEquals(padding, startButton.getPaddingBottom());
    }
}