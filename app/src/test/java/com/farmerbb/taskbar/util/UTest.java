package com.farmerbb.taskbar.util;

import android.content.Context;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowDisplay;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class UTest {
    private static final String SP_KEY_ANCHOR = "anchor";
    private static final String SP_KEY_POSITION = "position";
    private static final String SP_POSITION_BOTTOM_LEFT = "bottom_left";
    private static final String SP_POSITION_BOTTOM_VERTICAL_LEFT = "bottom_vertical_left";
    private static final String SP_POSITION_BOTTOM_VERTICAL_RIGHT = "bottom_vertical_right";
    private static final String SP_POSITION_BOTTOM_RIGHT = "bottom_right";
    private static final String SP_POSITION_TOP_LEFT = "top_left";
    private static final String SP_POSITION_TOP_RIGHT = "top_right";
    private static final String SP_POSITION_TOP_VERTICAL_LEFT = "top_vertical_left";
    private static final String SP_POSITION_TOP_VERTICAL_RIGHT = "top_vertical_right";
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
        assertEquals(SP_POSITION_BOTTOM_LEFT, position);
    }

    @Test
    public void testGetTaskbarPositionWithAnchorAndPositionBottomLeft() {
        checkTaskbarPositionWithDifferentRotation(
                SP_POSITION_BOTTOM_LEFT,
                new ArrayList<String>() {{
                    add(SP_POSITION_BOTTOM_LEFT);
                    add(SP_POSITION_BOTTOM_VERTICAL_RIGHT);
                    add(SP_POSITION_TOP_RIGHT);
                    add(SP_POSITION_TOP_VERTICAL_LEFT);
                }}
        );
    }

    @Test
    public void testGetTaskbarPositionWithAnchorAndPositionBottomVerticalLeft() {
        checkTaskbarPositionWithDifferentRotation(
                SP_POSITION_BOTTOM_VERTICAL_LEFT,
                new ArrayList<String>() {{
                    add(SP_POSITION_BOTTOM_VERTICAL_LEFT);
                    add(SP_POSITION_BOTTOM_RIGHT);
                    add(SP_POSITION_TOP_VERTICAL_RIGHT);
                    add(SP_POSITION_TOP_LEFT);
                }}
        );
    }

    @Test
    public void testGetTaskbarPositionWithAnchorAndPositionBottomRight() {
        checkTaskbarPositionWithDifferentRotation(
                SP_POSITION_BOTTOM_RIGHT,
                new ArrayList<String>() {{
                    add(SP_POSITION_BOTTOM_RIGHT);
                    add(SP_POSITION_TOP_VERTICAL_RIGHT);
                    add(SP_POSITION_TOP_LEFT);
                    add(SP_POSITION_BOTTOM_VERTICAL_LEFT);
                }}
        );
    }

    @Test
    public void testGetTaskbarPositionWithAnchorAndPositionBottomVerticalRight() {
        checkTaskbarPositionWithDifferentRotation(
                SP_POSITION_BOTTOM_VERTICAL_RIGHT,
                new ArrayList<String>() {{
                    add(SP_POSITION_BOTTOM_VERTICAL_RIGHT);
                    add(SP_POSITION_TOP_RIGHT);
                    add(SP_POSITION_TOP_VERTICAL_LEFT);
                    add(SP_POSITION_BOTTOM_LEFT);
                }}
        );
    }

    @Test
    public void testGetTaskbarPositionWithAnchorAndPositionTopLeft() {
        checkTaskbarPositionWithDifferentRotation(
                SP_POSITION_TOP_LEFT,
                new ArrayList<String>() {{
                    add(SP_POSITION_TOP_LEFT);
                    add(SP_POSITION_BOTTOM_VERTICAL_LEFT);
                    add(SP_POSITION_BOTTOM_RIGHT);
                    add(SP_POSITION_TOP_VERTICAL_RIGHT);
                }}
        );
    }

    @Test
    public void testGetTaskbarPositionWithAnchorAndPositionTopVerticalLeft() {
        checkTaskbarPositionWithDifferentRotation(
                SP_POSITION_TOP_VERTICAL_LEFT,
                new ArrayList<String>() {{
                    add(SP_POSITION_TOP_VERTICAL_LEFT);
                    add(SP_POSITION_BOTTOM_LEFT);
                    add(SP_POSITION_BOTTOM_VERTICAL_RIGHT);
                    add(SP_POSITION_TOP_RIGHT);
                }}
        );
    }

    @Test
    public void testGetTaskbarPositionWithAnchorAndPositionTopRight() {
        checkTaskbarPositionWithDifferentRotation(
                SP_POSITION_TOP_RIGHT,
                new ArrayList<String>() {{
                    add(SP_POSITION_TOP_RIGHT);
                    add(SP_POSITION_TOP_VERTICAL_LEFT);
                    add(SP_POSITION_BOTTOM_LEFT);
                    add(SP_POSITION_BOTTOM_VERTICAL_RIGHT);
                }}
        );
    }

    @Test
    public void testGetTaskbarPositionWithAnchorAndPositionTopVerticalRight() {
        checkTaskbarPositionWithDifferentRotation(
                SP_POSITION_TOP_VERTICAL_RIGHT,
                new ArrayList<String>() {{
                    add(SP_POSITION_TOP_VERTICAL_RIGHT);
                    add(SP_POSITION_TOP_LEFT);
                    add(SP_POSITION_BOTTOM_VERTICAL_LEFT);
                    add(SP_POSITION_BOTTOM_RIGHT);
                }}
        );
    }

    private void checkTaskbarPositionWithDifferentRotation(String originPosition,
                                                           List<String> changedPositions) {
        assertEquals(4, changedPositions.size());
        String oldPosition =
                U.getSharedPreferences(context).getString(SP_KEY_POSITION, SP_POSITION_BOTTOM_LEFT);
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
