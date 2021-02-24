package com.farmerbb.taskbar.util

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.R
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class ToastFrameworkImplTest {
    private lateinit var context: Context
    private lateinit var impl: ToastFrameworkImpl
    private val message = "test-message"
    private val length = Toast.LENGTH_LONG

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        impl = ToastFrameworkImpl(context, message, length)
    }

    @Test
    fun testShow() {
        impl.show()
        Assert.assertEquals(message, ShadowToast.getTextOfLatestToast())
        val toast = ShadowToast.getLatestToast()
        Assert.assertEquals(length.toLong(), toast.duration.toLong())
        Assert.assertEquals((Gravity.BOTTOM or Gravity.CENTER_VERTICAL).toLong(),
                toast.gravity.toLong())
        Assert.assertEquals(0, toast.xOffset.toLong())
        Assert.assertEquals(
                context.resources.getDimensionPixelSize(R.dimen.tb_toast_y_offset).toLong(),
                toast.yOffset
                        .toLong())
    }

    @Test
    fun testCancel() {
        impl.show()
        val toast = Shadows.shadowOf(ShadowToast.getLatestToast())
        Assert.assertFalse(toast.isCancelled)
        impl.cancel()
        Assert.assertTrue(toast.isCancelled)
    }
}
