package com.farmerbb.taskbar.service;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Process;
import android.os.UserManager;
import android.service.quicksettings.Tile;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.PersistentShortcutLaunchActivity;
import com.farmerbb.taskbar.activity.PersistentShortcutSelectAppActivity;
import com.farmerbb.taskbar.shadow.TaskbarShadowTileService;
import com.farmerbb.taskbar.util.Constants;
import com.farmerbb.taskbar.util.U;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static android.content.Context.USER_SERVICE;
import static com.farmerbb.taskbar.Constants.TEST_COMPONENT;
import static com.farmerbb.taskbar.Constants.TEST_LABEL;
import static com.farmerbb.taskbar.Constants.TEST_PACKAGE;
import static com.farmerbb.taskbar.Constants.TEST_WINDOW_SIZE;
import static com.farmerbb.taskbar.util.Constants.EXTRA_COMPONENT_NAME;
import static com.farmerbb.taskbar.util.Constants.EXTRA_PACKAGE_NAME;
import static com.farmerbb.taskbar.util.Constants.EXTRA_USER_ID;
import static com.farmerbb.taskbar.util.Constants.EXTRA_WINDOW_SIZE;
import static com.farmerbb.taskbar.util.Constants.PREF_COMPONENT_NAME_SUFFIX;
import static com.farmerbb.taskbar.util.Constants.PREF_LABEL_SUFFIX;
import static com.farmerbb.taskbar.util.Constants.PREF_PACKAGE_NAME_SUFFIX;
import static com.farmerbb.taskbar.util.Constants.PREF_QS_TILE;
import static com.farmerbb.taskbar.util.Constants.PREF_WINDOW_SIZE_SUFFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class FavoriteAppTileServiceTest {
    private FavoriteApp1 app1;
    private SharedPreferences prefs;
    private Context context;

    @Before
    public void setUp() {
        app1 = Robolectric.setupService(FavoriteApp1.class);
        prefs = U.getSharedPreferences(app1);
        context = ApplicationProvider.getApplicationContext();
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
        assertFalse(
                prefs.getBoolean(app1.getPrefix() + Constants.PREF_ADDED_SUFFIX, true)
        );
    }

    @Test
    public void testOnClickWithoutAdded() {
        prefs.edit().putBoolean(app1.getPrefix() + Constants.PREF_ADDED_SUFFIX, false).apply();
        app1.onClick();
        Intent startedActivityIntent = shadowOf((Application) context).getNextStartedActivity();
        assertNotNull(startedActivityIntent);
        assertEquals(
                app1.tileNumber(),
                startedActivityIntent.getIntExtra(PREF_QS_TILE, Integer.MIN_VALUE)
        );
        assertNotNull(startedActivityIntent.getComponent());
        assertTrue(
                startedActivityIntent
                        .getComponent()
                        .getClassName()
                        .endsWith(PersistentShortcutSelectAppActivity.class.getSimpleName())
        );
    }

    @Test
    @Config(shadows = {TaskbarShadowTileService.class})
    public void testOnClickWithAdded() {
        prefs.edit().putBoolean(app1.getPrefix() + Constants.PREF_ADDED_SUFFIX, true).apply();

        shadowOf(app1).setLocked(true);
        app1.onClick();
        assertFalse(app1.isLocked());

        shadowOf(app1).setLocked(false);
        String testPackageName = TEST_PACKAGE;
        prefs
                .edit()
                .putString(app1.getPrefix() + PREF_PACKAGE_NAME_SUFFIX, testPackageName)
                .apply();
        String testComponentName = TEST_COMPONENT;
        prefs
                .edit()
                .putString(app1.getPrefix() + PREF_COMPONENT_NAME_SUFFIX, testComponentName)
                .apply();
        String testWindowSize = TEST_WINDOW_SIZE;
        prefs
                .edit()
                .putString(app1.getPrefix() + PREF_WINDOW_SIZE_SUFFIX, testWindowSize)
                .apply();
        app1.onClick();
        Intent startedActivityIntent = shadowOf((Application) context).getNextStartedActivity();
        assertNotNull(startedActivityIntent);
        assertNotNull(startedActivityIntent.getComponent());
        assertTrue(
                startedActivityIntent
                        .getComponent()
                        .getClassName()
                        .endsWith(PersistentShortcutLaunchActivity.class.getSimpleName())
        );
        assertEquals(Intent.ACTION_MAIN, startedActivityIntent.getAction());
        assertEquals(
                testPackageName,
                startedActivityIntent.getStringExtra(EXTRA_PACKAGE_NAME)
        );
        assertEquals(
                testComponentName,
                startedActivityIntent.getStringExtra(EXTRA_COMPONENT_NAME)
        );
        assertEquals(
                testWindowSize,
                startedActivityIntent.getStringExtra(EXTRA_WINDOW_SIZE)
        );
        UserManager userManager = (UserManager) context.getSystemService(USER_SERVICE);
        assertEquals(
                userManager.getSerialNumberForUser(Process.myUserHandle()),
                startedActivityIntent.getLongExtra(EXTRA_USER_ID, Long.MIN_VALUE)
        );
    }

    @Test
    public void testOnStartListeningWithoutAdded() {
        prefs.edit().putBoolean(app1.getPrefix() + Constants.PREF_ADDED_SUFFIX, false).apply();
        app1.onStartListening();
        Tile tile = app1.getQsTile();
        assertEquals(Tile.STATE_INACTIVE, tile.getState());
        assertEquals(app1.getString(R.string.tb_new_shortcut), tile.getLabel());
    }

    @Test
    public void testOnStartListeningWithAdded() {
        prefs.edit().putBoolean(app1.getPrefix() + Constants.PREF_ADDED_SUFFIX, true).apply();
        String testLabel = TEST_LABEL;
        prefs.edit().putString(app1.getPrefix() + PREF_LABEL_SUFFIX, testLabel).apply();
        app1.onStartListening();
        Tile tile = app1.getQsTile();
        assertEquals(Tile.STATE_ACTIVE, tile.getState());
        assertEquals(testLabel, tile.getLabel());
    }
}
