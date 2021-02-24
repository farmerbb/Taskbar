package com.farmerbb.taskbar.widget

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.widget.DashboardCell.OnInterceptedLongPressListener
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DashboardCellTest {
    private lateinit var cell: DashboardCell
    private lateinit var context: Context
    private lateinit var listener: TestOnInterceptedLongPressListener
    private var longPressTimeout = 0

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cell = DashboardCell(context)
        listener = TestOnInterceptedLongPressListener()
        cell.setOnInterceptedLongPressListener(listener)
        longPressTimeout = ViewConfiguration.getLongPressTimeout()
    }

    @Test
    fun testOnInterceptTouchEvent() {
        var downTime = SystemClock.uptimeMillis()
        dispatchTouchEvent(downTime, MotionEvent.ACTION_DOWN)
        dispatchTouchEvent(downTime + longPressTimeout - 1, MotionEvent.ACTION_UP)
        Assert.assertNull(listener.longPressedCell)
        downTime = SystemClock.uptimeMillis()
        dispatchTouchEvent(downTime, MotionEvent.ACTION_DOWN)
        dispatchTouchEvent(downTime + longPressTimeout, MotionEvent.ACTION_UP)
        Assert.assertNull(listener.longPressedCell)
        downTime = SystemClock.uptimeMillis()
        dispatchTouchEvent(downTime, MotionEvent.ACTION_DOWN)
        dispatchTouchEvent(downTime + longPressTimeout + 1, MotionEvent.ACTION_UP)
        Assert.assertNotNull(listener.longPressedCell)
        Assert.assertSame(cell, listener.longPressedCell)
    }

    private fun dispatchTouchEvent(downTime: Long, event: Int) {
        val x = 0.0f
        val y = 0.0f
        val metaState = 0
        val motionEvent = MotionEvent.obtain(downTime, downTime, event, x, y, metaState)
        cell.onInterceptTouchEvent(motionEvent)
    }

    private class TestOnInterceptedLongPressListener : OnInterceptedLongPressListener {
        var longPressedCell: DashboardCell? = null
        override fun onInterceptedLongPress(cell: DashboardCell) {
            longPressedCell = cell
        }
    }
}
