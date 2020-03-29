package com.farmerbb.taskbar.content;

import android.content.Context;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.util.U;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowDisplay;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class TaskbarPositionTest {
    private static final String SP_KEY_ANCHOR = "anchor";
    private static final String SP_KEY_POSITION = "position";
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        assertNotNull(context);
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

    @Test
    public void testGetTaskbarPositionWithoutAnchor() {
        String position = TaskbarPosition.getTaskbarPosition(context);
        // The default position is bottom_left
        assertEquals(POSITION_BOTTOM_LEFT, position);
    }

    @Test
    public void testGetTaskbarPositionWithAnchorAndPositionBottomLeft() {
        checkTaskbarPositionWithDifferentRotation(
                POSITION_BOTTOM_LEFT,
                new ArrayList<String>() {{
                    add(POSITION_BOTTOM_LEFT);
                    add(POSITION_BOTTOM_VERTICAL_RIGHT);
                    add(POSITION_TOP_RIGHT);
                    add(POSITION_TOP_VERTICAL_LEFT);
                }}
        );
    }

    @Test
    public void testGetTaskbarPositionWithAnchorAndPositionBottomVerticalLeft() {
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
    public void testGetTaskbarPositionWithAnchorAndPositionBottomRight() {
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
    public void testGetTaskbarPositionWithAnchorAndPositionBottomVerticalRight() {
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
    public void testGetTaskbarPositionWithAnchorAndPositionTopLeft() {
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
    public void testGetTaskbarPositionWithAnchorAndPositionTopVerticalLeft() {
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
    public void testGetTaskbarPositionWithAnchorAndPositionTopRight() {
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
    public void testGetTaskbarPositionWithAnchorAndPositionTopVerticalRight() {
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
    public void testGetTaskbarPositionWithAnchorAndInvalidPosition() {
        checkTaskbarPositionWithDifferentRotation(
                "invalid-position",
                new ArrayList<String>() {{
                    add(POSITION_BOTTOM_LEFT);
                    add(POSITION_BOTTOM_LEFT);
                    add(POSITION_BOTTOM_LEFT);
                    add(POSITION_BOTTOM_LEFT);
                }}
        );
    }

    private void checkTaskbarPositionWithDifferentRotation(String originPosition,
                                                           List<String> changedPositions) {
        assertEquals(4, changedPositions.size());
        String oldPosition =
                U.getSharedPreferences(context).getString(SP_KEY_POSITION, POSITION_BOTTOM_LEFT);
        boolean oldAnchor =
                U.getSharedPreferences(context).getBoolean(SP_KEY_ANCHOR, false);
        initializeTaskbarPosition(originPosition);
        initializeRotation(Surface.ROTATION_0);
        assertEquals(changedPositions.get(0), TaskbarPosition.getTaskbarPosition(context));
        initializeRotation(Surface.ROTATION_90);
        assertEquals(changedPositions.get(1), TaskbarPosition.getTaskbarPosition(context));
        initializeRotation(Surface.ROTATION_180);
        assertEquals(changedPositions.get(2), TaskbarPosition.getTaskbarPosition(context));
        initializeRotation(Surface.ROTATION_270);
        assertEquals(changedPositions.get(3), TaskbarPosition.getTaskbarPosition(context));
        U.getSharedPreferences(context).edit().putBoolean(SP_KEY_ANCHOR, oldAnchor).apply();
        U.getSharedPreferences(context).edit().putString(SP_KEY_POSITION, oldPosition).apply();
    }

    private void initializeTaskbarPosition(String position) {
        U.getSharedPreferences(context).edit().putBoolean(SP_KEY_ANCHOR, true).apply();
        U.getSharedPreferences(context).edit().putString(SP_KEY_POSITION, position).apply();
    }

    private void initializeRotation(int rotation) {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        ShadowDisplay shadowDisplay = Shadows.shadowOf(display);
        shadowDisplay.setRotation(rotation);
    }
}
