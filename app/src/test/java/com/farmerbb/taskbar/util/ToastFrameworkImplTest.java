package com.farmerbb.taskbar.util;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowToast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ToastFrameworkImplTest {
    private Context context;
    private ToastFrameworkImpl impl;
    private String message = "test-message";
    private int length = Toast.LENGTH_LONG;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        impl = new ToastFrameworkImpl(context, message, length);
    }

    @Test
    public void testShow() {
        impl.show();
        assertEquals(message, ShadowToast.getTextOfLatestToast());
        Toast toast = ShadowToast.getLatestToast();
        assertEquals(length, toast.getDuration());
        assertEquals(Gravity.BOTTOM | Gravity.CENTER_VERTICAL, toast.getGravity());
        assertEquals(0, toast.getXOffset());
        assertEquals(
                context.getResources().getDimensionPixelSize(R.dimen.tb_toast_y_offset),
                toast.getYOffset()
        );
    }

    @Test
    public void testCancel() {
        impl.show();
        ShadowToast toast = Shadows.shadowOf(ShadowToast.getLatestToast());
        assertFalse(toast.isCancelled());
        impl.cancel();
        assertTrue(toast.isCancelled());
    }
}
