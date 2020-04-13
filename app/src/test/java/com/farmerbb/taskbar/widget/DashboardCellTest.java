package com.farmerbb.taskbar.widget;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.widget.DashboardCell.OnInterceptedLongPressListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

@RunWith(RobolectricTestRunner.class)
public class DashboardCellTest {
    private DashboardCell cell;
    private Context context;
    private TestOnInterceptedLongPressListener listener;
    private int longPressTimeout;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        cell = new DashboardCell(context);
        listener = new TestOnInterceptedLongPressListener();
        cell.setOnInterceptedLongPressListener(listener);
        longPressTimeout = ViewConfiguration.getLongPressTimeout();
    }

    @Test
    public void testOnInterceptTouchEvent() {
        long downTime = SystemClock.uptimeMillis();
        dispatchTouchEvent(downTime, MotionEvent.ACTION_DOWN);
        dispatchTouchEvent(downTime + longPressTimeout - 1, MotionEvent.ACTION_UP);
        assertNull(listener.longPressedCell);
        downTime = SystemClock.uptimeMillis();
        dispatchTouchEvent(downTime, MotionEvent.ACTION_DOWN);
        dispatchTouchEvent(downTime + longPressTimeout, MotionEvent.ACTION_UP);
        assertNull(listener.longPressedCell);
        downTime = SystemClock.uptimeMillis();
        dispatchTouchEvent(downTime, MotionEvent.ACTION_DOWN);
        dispatchTouchEvent(downTime + longPressTimeout + 1, MotionEvent.ACTION_UP);
        assertNotNull(listener.longPressedCell);
        assertSame(cell, listener.longPressedCell);
    }

    private void dispatchTouchEvent(long downTime, int event) {
        float x = 0.0f;
        float y = 0.0f;
        int metaState = 0;
        MotionEvent motionEvent = MotionEvent.obtain(downTime, downTime, event, x, y, metaState);
        cell.onInterceptTouchEvent(motionEvent);
    }

    private static class TestOnInterceptedLongPressListener
            implements OnInterceptedLongPressListener {
        private DashboardCell longPressedCell;

        @Override
        public void onInterceptedLongPress(DashboardCell cell) {
            longPressedCell = cell;
        }
    }
}