package com.farmerbb.taskbar.util

import android.content.Context
import android.view.Surface
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import java.util.function.Predicate
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class TaskbarPositionTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Assert.assertNotNull(context)
    }

    @Test
    fun testIsVertical() {
        Assert.assertFalse(TaskbarPosition.isVertical(Constants.POSITION_BOTTOM_LEFT))
        Assert.assertFalse(TaskbarPosition.isVertical(Constants.POSITION_BOTTOM_RIGHT))
        Assert.assertTrue(TaskbarPosition.isVertical(Constants.POSITION_BOTTOM_VERTICAL_LEFT))
        Assert.assertTrue(TaskbarPosition.isVertical(Constants.POSITION_BOTTOM_VERTICAL_RIGHT))
        Assert.assertFalse(TaskbarPosition.isVertical(Constants.POSITION_TOP_LEFT))
        Assert.assertFalse(TaskbarPosition.isVertical(Constants.POSITION_TOP_RIGHT))
        Assert.assertTrue(TaskbarPosition.isVertical(Constants.POSITION_TOP_VERTICAL_LEFT))
        Assert.assertTrue(TaskbarPosition.isVertical(Constants.POSITION_TOP_VERTICAL_RIGHT))
    }

    @Test
    fun testIsVerticalWithContext() {
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_LEFT,
                { context: Context? -> TaskbarPosition.isVertical(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_RIGHT,
                { context: Context? -> TaskbarPosition.isVertical(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_VERTICAL_LEFT,
                { context: Context? -> TaskbarPosition.isVertical(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_VERTICAL_RIGHT,
                { context: Context? -> TaskbarPosition.isVertical(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_LEFT,
                { context: Context? -> TaskbarPosition.isVertical(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_RIGHT,
                { context: Context? -> TaskbarPosition.isVertical(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_VERTICAL_LEFT,
                { context: Context? -> TaskbarPosition.isVertical(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_VERTICAL_RIGHT,
                { context: Context? -> TaskbarPosition.isVertical(context) },
                true
        )
    }

    @Test
    fun testIsLeft() {
        Assert.assertTrue(TaskbarPosition.isLeft(Constants.POSITION_BOTTOM_LEFT))
        Assert.assertFalse(TaskbarPosition.isLeft(Constants.POSITION_BOTTOM_RIGHT))
        Assert.assertTrue(TaskbarPosition.isLeft(Constants.POSITION_BOTTOM_VERTICAL_LEFT))
        Assert.assertFalse(TaskbarPosition.isLeft(Constants.POSITION_BOTTOM_VERTICAL_RIGHT))
        Assert.assertTrue(TaskbarPosition.isLeft(Constants.POSITION_TOP_LEFT))
        Assert.assertFalse(TaskbarPosition.isLeft(Constants.POSITION_TOP_RIGHT))
        Assert.assertTrue(TaskbarPosition.isLeft(Constants.POSITION_TOP_VERTICAL_LEFT))
        Assert.assertFalse(TaskbarPosition.isLeft(Constants.POSITION_TOP_VERTICAL_RIGHT))
    }

    @Test
    fun testIsLeftWithContext() {
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_LEFT,
                { context: Context? -> TaskbarPosition.isLeft(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_RIGHT,
                { context: Context? -> TaskbarPosition.isLeft(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_VERTICAL_LEFT,
                { context: Context? -> TaskbarPosition.isLeft(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_VERTICAL_RIGHT,
                { context: Context? -> TaskbarPosition.isLeft(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_LEFT,
                { context: Context? -> TaskbarPosition.isLeft(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_RIGHT,
                { context: Context? -> TaskbarPosition.isLeft(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_VERTICAL_LEFT,
                { context: Context? -> TaskbarPosition.isLeft(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_VERTICAL_RIGHT,
                { context: Context? -> TaskbarPosition.isLeft(context) },
                false
        )
    }

    @Test
    fun testIsRight() {
        Assert.assertFalse(TaskbarPosition.isRight(Constants.POSITION_BOTTOM_LEFT))
        Assert.assertTrue(TaskbarPosition.isRight(Constants.POSITION_BOTTOM_RIGHT))
        Assert.assertFalse(TaskbarPosition.isRight(Constants.POSITION_BOTTOM_VERTICAL_LEFT))
        Assert.assertTrue(TaskbarPosition.isRight(Constants.POSITION_BOTTOM_VERTICAL_RIGHT))
        Assert.assertFalse(TaskbarPosition.isRight(Constants.POSITION_TOP_LEFT))
        Assert.assertTrue(TaskbarPosition.isRight(Constants.POSITION_TOP_RIGHT))
        Assert.assertFalse(TaskbarPosition.isRight(Constants.POSITION_TOP_VERTICAL_LEFT))
        Assert.assertTrue(TaskbarPosition.isRight(Constants.POSITION_TOP_VERTICAL_RIGHT))
    }

    @Test
    fun testIsRightWithContext() {
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_LEFT,
                { context: Context? -> TaskbarPosition.isRight(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_RIGHT,
                { context: Context? -> TaskbarPosition.isRight(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_VERTICAL_LEFT,
                { context: Context? -> TaskbarPosition.isRight(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_VERTICAL_RIGHT,
                { context: Context? -> TaskbarPosition.isRight(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_LEFT,
                { context: Context? -> TaskbarPosition.isRight(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_RIGHT,
                { context: Context? -> TaskbarPosition.isRight(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_VERTICAL_LEFT,
                { context: Context? -> TaskbarPosition.isRight(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_VERTICAL_RIGHT,
                { context: Context? -> TaskbarPosition.isRight(context) },
                true
        )
    }

    @Test
    fun testIsBottom() {
        Assert.assertTrue(TaskbarPosition.isBottom(Constants.POSITION_BOTTOM_LEFT))
        Assert.assertTrue(TaskbarPosition.isBottom(Constants.POSITION_BOTTOM_RIGHT))
        Assert.assertTrue(TaskbarPosition.isBottom(Constants.POSITION_BOTTOM_VERTICAL_LEFT))
        Assert.assertTrue(TaskbarPosition.isBottom(Constants.POSITION_BOTTOM_VERTICAL_RIGHT))
        Assert.assertFalse(TaskbarPosition.isBottom(Constants.POSITION_TOP_LEFT))
        Assert.assertFalse(TaskbarPosition.isBottom(Constants.POSITION_TOP_RIGHT))
        Assert.assertFalse(TaskbarPosition.isBottom(Constants.POSITION_TOP_VERTICAL_LEFT))
        Assert.assertFalse(TaskbarPosition.isBottom(Constants.POSITION_TOP_VERTICAL_RIGHT))
    }

    @Test
    fun testIsBottomWithContext() {
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_LEFT,
                { context: Context? -> TaskbarPosition.isBottom(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_RIGHT,
                { context: Context? -> TaskbarPosition.isBottom(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_VERTICAL_LEFT,
                { context: Context? -> TaskbarPosition.isBottom(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_VERTICAL_RIGHT,
                { context: Context? -> TaskbarPosition.isBottom(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_LEFT,
                { context: Context? -> TaskbarPosition.isBottom(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_RIGHT,
                { context: Context? -> TaskbarPosition.isBottom(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_VERTICAL_LEFT,
                { context: Context? -> TaskbarPosition.isBottom(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_VERTICAL_RIGHT,
                { context: Context? -> TaskbarPosition.isBottom(context) },
                false
        )
    }

    @Test
    fun testIsVerticalLeft() {
        Assert.assertFalse(TaskbarPosition.isVerticalLeft(Constants.POSITION_BOTTOM_LEFT))
        Assert.assertFalse(TaskbarPosition.isVerticalLeft(Constants.POSITION_BOTTOM_RIGHT))
        Assert.assertTrue(TaskbarPosition.isVerticalLeft(Constants.POSITION_BOTTOM_VERTICAL_LEFT))
        Assert.assertFalse(TaskbarPosition.isVerticalLeft(Constants.POSITION_BOTTOM_VERTICAL_RIGHT))
        Assert.assertFalse(TaskbarPosition.isVerticalLeft(Constants.POSITION_TOP_LEFT))
        Assert.assertFalse(TaskbarPosition.isVerticalLeft(Constants.POSITION_TOP_RIGHT))
        Assert.assertTrue(TaskbarPosition.isVerticalLeft(Constants.POSITION_TOP_VERTICAL_LEFT))
        Assert.assertFalse(TaskbarPosition.isVerticalLeft(Constants.POSITION_TOP_VERTICAL_RIGHT))
    }

    @Test
    fun testIsVerticalLeftWithContext() {
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_LEFT,
                { context: Context? -> TaskbarPosition.isVerticalLeft(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_RIGHT,
                { context: Context? -> TaskbarPosition.isVerticalLeft(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_VERTICAL_LEFT,
                { context: Context? -> TaskbarPosition.isVerticalLeft(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_VERTICAL_RIGHT,
                { context: Context? -> TaskbarPosition.isVerticalLeft(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_LEFT,
                { context: Context? -> TaskbarPosition.isVerticalLeft(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_RIGHT,
                { context: Context? -> TaskbarPosition.isVerticalLeft(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_VERTICAL_LEFT,
                { context: Context? -> TaskbarPosition.isVerticalLeft(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_VERTICAL_RIGHT,
                { context: Context? -> TaskbarPosition.isVerticalLeft(context) },
                false
        )
    }

    @Test
    fun testIsVerticalRight() {
        Assert.assertFalse(TaskbarPosition.isVerticalRight(Constants.POSITION_BOTTOM_LEFT))
        Assert.assertFalse(TaskbarPosition.isVerticalRight(Constants.POSITION_BOTTOM_RIGHT))
        Assert.assertFalse(TaskbarPosition.isVerticalRight(Constants.POSITION_BOTTOM_VERTICAL_LEFT))
        Assert.assertTrue(TaskbarPosition.isVerticalRight(Constants.POSITION_BOTTOM_VERTICAL_RIGHT))
        Assert.assertFalse(TaskbarPosition.isVerticalRight(Constants.POSITION_TOP_LEFT))
        Assert.assertFalse(TaskbarPosition.isVerticalRight(Constants.POSITION_TOP_RIGHT))
        Assert.assertFalse(TaskbarPosition.isVerticalRight(Constants.POSITION_TOP_VERTICAL_LEFT))
        Assert.assertTrue(TaskbarPosition.isVerticalRight(Constants.POSITION_TOP_VERTICAL_RIGHT))
    }

    @Test
    fun testIsVerticalRightWithContext() {
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_LEFT,
                { context: Context? -> TaskbarPosition.isVerticalRight(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_RIGHT,
                { context: Context? -> TaskbarPosition.isVerticalRight(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_VERTICAL_LEFT,
                { context: Context? -> TaskbarPosition.isVerticalRight(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_BOTTOM_VERTICAL_RIGHT,
                { context: Context? -> TaskbarPosition.isVerticalRight(context) },
                true
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_LEFT,
                { context: Context? -> TaskbarPosition.isVerticalRight(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_RIGHT,
                { context: Context? -> TaskbarPosition.isVerticalRight(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_VERTICAL_LEFT,
                { context: Context? -> TaskbarPosition.isVerticalRight(context) },
                false
        )
        checkTaskbarPositionGroup(
                Constants.POSITION_TOP_VERTICAL_RIGHT,
                { context: Context? -> TaskbarPosition.isVerticalRight(context) },
                true
        )
    }

    @Test
    fun testGetTaskbarPositionWithoutAnchor() {
        val position = TaskbarPosition.getTaskbarPosition(context)
        // The default position is bottom_left
        Assert.assertEquals(Constants.POSITION_BOTTOM_LEFT, position)
    }

    @Test
    fun testGetTaskbarPositionWithAnchorAndPositionBottomLeft() {
        checkTaskbarPositionWithDifferentRotation(
                Constants.POSITION_BOTTOM_LEFT,
                object : ArrayList<String>() {
                    init {
                        add(Constants.POSITION_BOTTOM_LEFT)
                        add(Constants.POSITION_BOTTOM_VERTICAL_RIGHT)
                        add(Constants.POSITION_TOP_RIGHT)
                        add(Constants.POSITION_TOP_VERTICAL_LEFT)
                    }
                }
        )
    }

    @Test
    fun testGetTaskbarPositionWithAnchorAndPositionBottomVerticalLeft() {
        checkTaskbarPositionWithDifferentRotation(
                Constants.POSITION_BOTTOM_VERTICAL_LEFT,
                object : ArrayList<String>() {
                    init {
                        add(Constants.POSITION_BOTTOM_VERTICAL_LEFT)
                        add(Constants.POSITION_BOTTOM_RIGHT)
                        add(Constants.POSITION_TOP_VERTICAL_RIGHT)
                        add(Constants.POSITION_TOP_LEFT)
                    }
                }
        )
    }

    @Test
    fun testGetTaskbarPositionWithAnchorAndPositionBottomRight() {
        checkTaskbarPositionWithDifferentRotation(
                Constants.POSITION_BOTTOM_RIGHT,
                object : ArrayList<String>() {
                    init {
                        add(Constants.POSITION_BOTTOM_RIGHT)
                        add(Constants.POSITION_TOP_VERTICAL_RIGHT)
                        add(Constants.POSITION_TOP_LEFT)
                        add(Constants.POSITION_BOTTOM_VERTICAL_LEFT)
                    }
                }
        )
    }

    @Test
    fun testGetTaskbarPositionWithAnchorAndPositionBottomVerticalRight() {
        checkTaskbarPositionWithDifferentRotation(
                Constants.POSITION_BOTTOM_VERTICAL_RIGHT,
                object : ArrayList<String>() {
                    init {
                        add(Constants.POSITION_BOTTOM_VERTICAL_RIGHT)
                        add(Constants.POSITION_TOP_RIGHT)
                        add(Constants.POSITION_TOP_VERTICAL_LEFT)
                        add(Constants.POSITION_BOTTOM_LEFT)
                    }
                }
        )
    }

    @Test
    fun testGetTaskbarPositionWithAnchorAndPositionTopLeft() {
        checkTaskbarPositionWithDifferentRotation(
                Constants.POSITION_TOP_LEFT,
                object : ArrayList<String>() {
                    init {
                        add(Constants.POSITION_TOP_LEFT)
                        add(Constants.POSITION_BOTTOM_VERTICAL_LEFT)
                        add(Constants.POSITION_BOTTOM_RIGHT)
                        add(Constants.POSITION_TOP_VERTICAL_RIGHT)
                    }
                }
        )
    }

    @Test
    fun testGetTaskbarPositionWithAnchorAndPositionTopVerticalLeft() {
        checkTaskbarPositionWithDifferentRotation(
                Constants.POSITION_TOP_VERTICAL_LEFT,
                object : ArrayList<String>() {
                    init {
                        add(Constants.POSITION_TOP_VERTICAL_LEFT)
                        add(Constants.POSITION_BOTTOM_LEFT)
                        add(Constants.POSITION_BOTTOM_VERTICAL_RIGHT)
                        add(Constants.POSITION_TOP_RIGHT)
                    }
                }
        )
    }

    @Test
    fun testGetTaskbarPositionWithAnchorAndPositionTopRight() {
        checkTaskbarPositionWithDifferentRotation(
                Constants.POSITION_TOP_RIGHT,
                object : ArrayList<String>() {
                    init {
                        add(Constants.POSITION_TOP_RIGHT)
                        add(Constants.POSITION_TOP_VERTICAL_LEFT)
                        add(Constants.POSITION_BOTTOM_LEFT)
                        add(Constants.POSITION_BOTTOM_VERTICAL_RIGHT)
                    }
                }
        )
    }

    @Test
    fun testGetTaskbarPositionWithAnchorAndPositionTopVerticalRight() {
        checkTaskbarPositionWithDifferentRotation(
                Constants.POSITION_TOP_VERTICAL_RIGHT,
                object : ArrayList<String>() {
                    init {
                        add(Constants.POSITION_TOP_VERTICAL_RIGHT)
                        add(Constants.POSITION_TOP_LEFT)
                        add(Constants.POSITION_BOTTOM_VERTICAL_LEFT)
                        add(Constants.POSITION_BOTTOM_RIGHT)
                    }
                }
        )
    }

    @Test
    fun testGetTaskbarPositionWithAnchorAndInvalidPosition() {
        checkTaskbarPositionWithDifferentRotation(
                "invalid-position",
                object : ArrayList<String>() {
                    init {
                        add(Constants.POSITION_BOTTOM_LEFT)
                        add(Constants.POSITION_BOTTOM_LEFT)
                        add(Constants.POSITION_BOTTOM_LEFT)
                        add(Constants.POSITION_BOTTOM_LEFT)
                    }
                }
        )
    }

    private fun checkTaskbarPositionWithDifferentRotation(
        originPosition: String,
        changedPositions: List<String>
    ) {
        Assert.assertEquals(4, changedPositions.size.toLong())
        val oldPosition = U.getSharedPreferences(context)
                .getString(Constants.PREF_POSITION, Constants.POSITION_BOTTOM_LEFT)
        val oldAnchor = U.getSharedPreferences(context)
                .getBoolean(Constants.PREF_ANCHOR, false)
        initializeTaskbarPosition(originPosition)
        initializeRotation(Surface.ROTATION_0)
        Assert.assertEquals(changedPositions[0], TaskbarPosition.getTaskbarPosition(context))
        initializeRotation(Surface.ROTATION_90)
        Assert.assertEquals(changedPositions[1], TaskbarPosition.getTaskbarPosition(context))
        initializeRotation(Surface.ROTATION_180)
        Assert.assertEquals(changedPositions[2], TaskbarPosition.getTaskbarPosition(context))
        initializeRotation(Surface.ROTATION_270)
        Assert.assertEquals(changedPositions[3], TaskbarPosition.getTaskbarPosition(context))
        U.getSharedPreferences(context).edit()
                .putBoolean(Constants.PREF_ANCHOR, oldAnchor).apply()
        U.getSharedPreferences(context).edit()
                .putString(Constants.PREF_POSITION, oldPosition).apply()
    }

    private fun checkTaskbarPositionGroup(
        originPosition: String,
        predicate: Predicate<Context?>,
        expectedResult: Boolean
    ) {
        val oldPosition = U.getSharedPreferences(context)
                .getString(Constants.PREF_POSITION, Constants.POSITION_BOTTOM_LEFT)
        val oldAnchor = U.getSharedPreferences(context)
                .getBoolean(Constants.PREF_ANCHOR, false)
        initializeTaskbarPosition(originPosition)
        Assert.assertEquals(expectedResult, predicate.test(context))
        U.getSharedPreferences(context).edit()
                .putBoolean(Constants.PREF_ANCHOR, oldAnchor).apply()
        U.getSharedPreferences(context).edit()
                .putString(Constants.PREF_POSITION, oldPosition).apply()
    }

    private fun initializeTaskbarPosition(position: String) {
        U.getSharedPreferences(context).edit()
                .putBoolean(Constants.PREF_ANCHOR, true).apply()
        U.getSharedPreferences(context).edit()
                .putString(Constants.PREF_POSITION, position).apply()
    }

    private fun initializeRotation(rotation: Int) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val shadowDisplay = Shadows.shadowOf(display)
        shadowDisplay.setRotation(rotation)
    }
}
