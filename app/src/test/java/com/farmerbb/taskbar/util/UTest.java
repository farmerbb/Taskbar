package com.farmerbb.taskbar.util;

import android.content.Context;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.content.TaskbarPosition;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class UTest {
    private static final String SP_KEY_ANCHOR = "anchor";
    private static final String SP_KEY_POSITION = "position";
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        assertNotNull(context);
    }

    @Test
    public void testGetTaskbarPositionWithoutAnchor() {
        String position = U.getTaskbarPosition(context);
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
        assertEquals(changedPositions.get(0), U.getTaskbarPosition(context));
        initializeRotation(Surface.ROTATION_90);
        assertEquals(changedPositions.get(1), U.getTaskbarPosition(context));
        initializeRotation(Surface.ROTATION_180);
        assertEquals(changedPositions.get(2), U.getTaskbarPosition(context));
        initializeRotation(Surface.ROTATION_270);
        assertEquals(changedPositions.get(3), U.getTaskbarPosition(context));
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
