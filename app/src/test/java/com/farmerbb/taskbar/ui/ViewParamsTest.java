package com.farmerbb.taskbar.ui;

import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.WindowManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ViewParamsTest {
    @Test
    public void testToWindowManagerParams() {
        int width = 100;
        int height = 101;
        int gravity = Gravity.BOTTOM;
        int flags = 1001;
        ViewParams params = new ViewParams(width, height, gravity, flags);
        WindowManager.LayoutParams generatedParams = params.toWindowManagerParams();
        assertEquals(width, generatedParams.width);
        assertEquals(height, generatedParams.height);
        assertEquals(gravity, generatedParams.gravity);
        assertEquals(flags, generatedParams.flags);
        assertEquals(PixelFormat.TRANSLUCENT, generatedParams.format);
        assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, generatedParams.type);
        params.gravity = -1;
        generatedParams = params.toWindowManagerParams();
        assertEquals(0, generatedParams.gravity);
    }
}
