package com.farmerbb.taskbar.ui

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ViewParamsTest {
    @Test
    fun testToWindowManagerParams() {
        val width = 100
        val height = 101
        val gravity = Gravity.BOTTOM
        val flags = 1001
        val bottomMargin = 102
        val params = ViewParams(width, height, gravity, flags, bottomMargin)
        var generatedParams = params.toWindowManagerParams()
        Assert.assertEquals(width.toLong(), generatedParams.width.toLong())
        Assert.assertEquals(height.toLong(), generatedParams.height.toLong())
        Assert.assertEquals(gravity.toLong(), generatedParams.gravity.toLong())
        Assert.assertEquals(flags.toLong(), generatedParams.flags.toLong())
        Assert.assertEquals(bottomMargin.toLong(), generatedParams.y.toLong())
        Assert.assertEquals(PixelFormat.TRANSLUCENT.toLong(), generatedParams.format.toLong())
        Assert.assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY.toLong(),
                generatedParams.type.toLong())
        params.gravity = -1
        generatedParams = params.toWindowManagerParams()
        Assert.assertEquals(0, generatedParams.gravity.toLong())
        params.bottomMargin = -1
        generatedParams = params.toWindowManagerParams()
        Assert.assertEquals(0, generatedParams.y.toLong())
    }
}
