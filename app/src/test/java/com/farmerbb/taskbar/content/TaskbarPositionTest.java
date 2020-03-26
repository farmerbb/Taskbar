package com.farmerbb.taskbar.content;

import android.view.Surface;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static com.farmerbb.taskbar.content.TaskbarPosition.POSITION_BOTTOM_LEFT;
import static com.farmerbb.taskbar.content.TaskbarPosition.POSITION_BOTTOM_RIGHT;
import static com.farmerbb.taskbar.content.TaskbarPosition.POSITION_BOTTOM_VERTICAL_LEFT;
import static com.farmerbb.taskbar.content.TaskbarPosition.POSITION_BOTTOM_VERTICAL_RIGHT;
import static com.farmerbb.taskbar.content.TaskbarPosition.POSITION_TOP_LEFT;
import static com.farmerbb.taskbar.content.TaskbarPosition.POSITION_TOP_RIGHT;
import static com.farmerbb.taskbar.content.TaskbarPosition.POSITION_TOP_VERTICAL_LEFT;
import static com.farmerbb.taskbar.content.TaskbarPosition.POSITION_TOP_VERTICAL_RIGHT;
import static com.farmerbb.taskbar.content.TaskbarPosition.isBottom;
import static com.farmerbb.taskbar.content.TaskbarPosition.isLeft;
import static com.farmerbb.taskbar.content.TaskbarPosition.isRight;
import static com.farmerbb.taskbar.content.TaskbarPosition.isVertical;
import static com.farmerbb.taskbar.content.TaskbarPosition.isVerticalLeft;
import static com.farmerbb.taskbar.content.TaskbarPosition.isVerticalRight;
import static com.farmerbb.taskbar.content.TaskbarPosition.transferPositionWithRotation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class TaskbarPositionTest {
    @Test
    public void testTransferPositionWithPositionBottomVerticalLeft() {
        checkTaskbarPositionWithDifferentRotation(
                POSITION_BOTTOM_VERTICAL_LEFT,
                new ArrayList<String>() {{
                    add(POSITION_BOTTOM_VERTICAL_LEFT);
                    add(POSITION_BOTTOM_RIGHT);
                    add(POSITION_TOP_VERTICAL_RIGHT);
                    add(POSITION_TOP_LEFT);
                }}
        );
    }

    @Test
    public void testTransferPositionWithPositionBottomRight() {
        checkTaskbarPositionWithDifferentRotation(
                POSITION_BOTTOM_RIGHT,
                new ArrayList<String>() {{
                    add(POSITION_BOTTOM_RIGHT);
                    add(POSITION_TOP_VERTICAL_RIGHT);
                    add(POSITION_TOP_LEFT);
                    add(POSITION_BOTTOM_VERTICAL_LEFT);
                }}
        );
    }

    @Test
    public void testTransferPositionWithPositionBottomVerticalRight() {
        checkTaskbarPositionWithDifferentRotation(
                POSITION_BOTTOM_VERTICAL_RIGHT,
                new ArrayList<String>() {{
                    add(POSITION_BOTTOM_VERTICAL_RIGHT);
                    add(POSITION_TOP_RIGHT);
                    add(POSITION_TOP_VERTICAL_LEFT);
                    add(POSITION_BOTTOM_LEFT);
                }}
        );
    }

    @Test
    public void testTransferPositionWithPositionTopLeft() {
        checkTaskbarPositionWithDifferentRotation(
                POSITION_TOP_LEFT,
                new ArrayList<String>() {{
                    add(POSITION_TOP_LEFT);
                    add(POSITION_BOTTOM_VERTICAL_LEFT);
                    add(POSITION_BOTTOM_RIGHT);
                    add(POSITION_TOP_VERTICAL_RIGHT);
                }}
        );
    }

    @Test
    public void testTransferPositionWithPositionTopVerticalLeft() {
        checkTaskbarPositionWithDifferentRotation(
                POSITION_TOP_VERTICAL_LEFT,
                new ArrayList<String>() {{
                    add(POSITION_TOP_VERTICAL_LEFT);
                    add(POSITION_BOTTOM_LEFT);
                    add(POSITION_BOTTOM_VERTICAL_RIGHT);
                    add(POSITION_TOP_RIGHT);
                }}
        );
    }

    @Test
    public void testTransferPositionWithPositionTopRight() {
        checkTaskbarPositionWithDifferentRotation(
                POSITION_TOP_RIGHT,
                new ArrayList<String>() {{
                    add(POSITION_TOP_RIGHT);
                    add(POSITION_TOP_VERTICAL_LEFT);
                    add(POSITION_BOTTOM_LEFT);
                    add(POSITION_BOTTOM_VERTICAL_RIGHT);
                }}
        );
    }

    @Test
    public void testTransferPositionWithPositionTopVerticalRight() {
        checkTaskbarPositionWithDifferentRotation(
                POSITION_TOP_VERTICAL_RIGHT,
                new ArrayList<String>() {{
                    add(POSITION_TOP_VERTICAL_RIGHT);
                    add(POSITION_TOP_LEFT);
                    add(POSITION_BOTTOM_VERTICAL_LEFT);
                    add(POSITION_BOTTOM_RIGHT);
                }}
        );
    }

    @Test
    public void testIsVertical() {
        assertFalse(isVertical(POSITION_BOTTOM_LEFT));
        assertFalse(isVertical(POSITION_BOTTOM_RIGHT));
        assertTrue(isVertical(POSITION_BOTTOM_VERTICAL_LEFT));
        assertTrue(isVertical(POSITION_BOTTOM_VERTICAL_RIGHT));
        assertFalse(isVertical(POSITION_TOP_LEFT));
        assertFalse(isVertical(POSITION_TOP_RIGHT));
        assertTrue(isVertical(POSITION_TOP_VERTICAL_LEFT));
        assertTrue(isVertical(POSITION_TOP_VERTICAL_RIGHT));
    }

    @Test
    public void testIsLeft() {
        assertTrue(isLeft(POSITION_BOTTOM_LEFT));
        assertFalse(isLeft(POSITION_BOTTOM_RIGHT));
        assertTrue(isLeft(POSITION_BOTTOM_VERTICAL_LEFT));
        assertFalse(isLeft(POSITION_BOTTOM_VERTICAL_RIGHT));
        assertTrue(isLeft(POSITION_TOP_LEFT));
        assertFalse(isLeft(POSITION_TOP_RIGHT));
        assertTrue(isLeft(POSITION_TOP_VERTICAL_LEFT));
        assertFalse(isLeft(POSITION_TOP_VERTICAL_RIGHT));
    }

    @Test
    public void testIsRight() {
        assertFalse(isRight(POSITION_BOTTOM_LEFT));
        assertTrue(isRight(POSITION_BOTTOM_RIGHT));
        assertFalse(isRight(POSITION_BOTTOM_VERTICAL_LEFT));
        assertTrue(isRight(POSITION_BOTTOM_VERTICAL_RIGHT));
        assertFalse(isRight(POSITION_TOP_LEFT));
        assertTrue(isRight(POSITION_TOP_RIGHT));
        assertFalse(isRight(POSITION_TOP_VERTICAL_LEFT));
        assertTrue(isRight(POSITION_TOP_VERTICAL_RIGHT));
    }

    @Test
    public void testIsBottom() {
        assertTrue(isBottom(POSITION_BOTTOM_LEFT));
        assertTrue(isBottom(POSITION_BOTTOM_RIGHT));
        assertTrue(isBottom(POSITION_BOTTOM_VERTICAL_LEFT));
        assertTrue(isBottom(POSITION_BOTTOM_VERTICAL_RIGHT));
        assertFalse(isBottom(POSITION_TOP_LEFT));
        assertFalse(isBottom(POSITION_TOP_RIGHT));
        assertFalse(isBottom(POSITION_TOP_VERTICAL_LEFT));
        assertFalse(isBottom(POSITION_TOP_VERTICAL_RIGHT));
    }

    @Test
    public void testIsVerticalLeft() {
        assertFalse(isVerticalLeft(POSITION_BOTTOM_LEFT));
        assertFalse(isVerticalLeft(POSITION_BOTTOM_RIGHT));
        assertTrue(isVerticalLeft(POSITION_BOTTOM_VERTICAL_LEFT));
        assertFalse(isVerticalLeft(POSITION_BOTTOM_VERTICAL_RIGHT));
        assertFalse(isVerticalLeft(POSITION_TOP_LEFT));
        assertFalse(isVerticalLeft(POSITION_TOP_RIGHT));
        assertTrue(isVerticalLeft(POSITION_TOP_VERTICAL_LEFT));
        assertFalse(isVerticalLeft(POSITION_TOP_VERTICAL_RIGHT));
    }

    @Test
    public void testIsVerticalRight() {
        assertFalse(isVerticalRight(POSITION_BOTTOM_LEFT));
        assertFalse(isVerticalRight(POSITION_BOTTOM_RIGHT));
        assertFalse(isVerticalRight(POSITION_BOTTOM_VERTICAL_LEFT));
        assertTrue(isVerticalRight(POSITION_BOTTOM_VERTICAL_RIGHT));
        assertFalse(isVerticalRight(POSITION_TOP_LEFT));
        assertFalse(isVerticalRight(POSITION_TOP_RIGHT));
        assertFalse(isVerticalRight(POSITION_TOP_VERTICAL_LEFT));
        assertTrue(isVerticalRight(POSITION_TOP_VERTICAL_RIGHT));
    }

    private void checkTaskbarPositionWithDifferentRotation(String originPosition,
                                                           List<String> changedPositions) {
        assertEquals(4, changedPositions.size());
        assertEquals(
                changedPositions.get(0),
                transferPositionWithRotation(originPosition, Surface.ROTATION_0)
        );
        assertEquals(
                changedPositions.get(1),
                transferPositionWithRotation(originPosition, Surface.ROTATION_90)
        );
        assertEquals(
                changedPositions.get(2),
                transferPositionWithRotation(originPosition, Surface.ROTATION_180)
        );
        assertEquals(
                changedPositions.get(3),
                transferPositionWithRotation(originPosition, Surface.ROTATION_270)
        );
    }
}
