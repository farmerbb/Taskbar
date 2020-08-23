package com.farmerbb.taskbar.service;

import android.content.SharedPreferences;

import com.farmerbb.taskbar.util.U;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class FavoriteAppTileServiceTest {
    private FavoriteApp1 app1;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        app1 = Robolectric.setupService(FavoriteApp1.class);
        prefs = U.getSharedPreferences(app1);
    }

    @Test
    public void testFavoriteApp1TileNumber() {
        assertEquals(1, app1.tileNumber());
    }

    @Test
    public void testFavoriteApp2TileNumber() {
        FavoriteApp2 app2 = Robolectric.setupService(FavoriteApp2.class);
        assertEquals(2, app2.tileNumber());
    }

    @Test
    public void testFavoriteApp3TileNumber() {
        FavoriteApp3 app3 = Robolectric.setupService(FavoriteApp3.class);
        assertEquals(3, app3.tileNumber());
    }

    @Test
    public void testFavoriteApp4TileNumber() {
        FavoriteApp4 app4 = Robolectric.setupService(FavoriteApp4.class);
        assertEquals(4, app4.tileNumber());
    }

    @Test
    public void testFavoriteApp5TileNumber() {
        FavoriteApp5 app5 = Robolectric.setupService(FavoriteApp5.class);
        assertEquals(5, app5.tileNumber());
    }

    @Test
    public void testOnTileRemoved() {
        app1.onTileRemoved();
        assertFalse(prefs.getBoolean("qs_tile_" + app1.tileNumber() + "_" + "added", true));
    }
}
