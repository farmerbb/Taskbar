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
import static com.farmerbb.taskbar.content.TaskbarPosition.transferPositionWithRotation;
import static org.junit.Assert.assertEquals;

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
