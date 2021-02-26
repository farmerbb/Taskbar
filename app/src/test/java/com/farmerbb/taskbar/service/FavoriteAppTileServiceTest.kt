package com.farmerbb.taskbar.service

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Process
import android.os.UserManager
import android.service.quicksettings.Tile
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.Constants.TEST_COMPONENT
import com.farmerbb.taskbar.Constants.TEST_LABEL
import com.farmerbb.taskbar.Constants.TEST_PACKAGE
import com.farmerbb.taskbar.Constants.TEST_WINDOW_SIZE
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.activity.PersistentShortcutLaunchActivity
import com.farmerbb.taskbar.activity.PersistentShortcutSelectAppActivity
import com.farmerbb.taskbar.util.Constants
import com.farmerbb.taskbar.util.U
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class FavoriteAppTileServiceTest {
    private lateinit var app1: FavoriteApp1
    private lateinit var prefs: SharedPreferences
    private lateinit var context: Context

    @Before
    fun setUp() {
        app1 = Robolectric.setupService(FavoriteApp1::class.java)
        prefs = U.getSharedPreferences(app1)
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testFavoriteApp1TileNumber() {
        Assert.assertEquals(1, app1.tileNumber().toLong())
    }

    @Test
    fun testFavoriteApp2TileNumber() {
        val app2 = Robolectric.setupService(FavoriteApp2::class.java)
        Assert.assertEquals(2, app2.tileNumber().toLong())
    }

    @Test
    fun testFavoriteApp3TileNumber() {
        val app3 = Robolectric.setupService(FavoriteApp3::class.java)
        Assert.assertEquals(3, app3.tileNumber().toLong())
    }

    @Test
    fun testFavoriteApp4TileNumber() {
        val app4 = Robolectric.setupService(FavoriteApp4::class.java)
        Assert.assertEquals(4, app4.tileNumber().toLong())
    }

    @Test
    fun testFavoriteApp5TileNumber() {
        val app5 = Robolectric.setupService(FavoriteApp5::class.java)
        Assert.assertEquals(5, app5.tileNumber().toLong())
    }

    @Test
    fun testOnTileRemoved() {
        app1.onTileRemoved()
        Assert.assertFalse(
                prefs.getBoolean(app1.prefix + Constants.PREF_ADDED_SUFFIX, true)
        )
    }

    @Test
    fun testOnClickWithoutAdded() {
        prefs.edit().putBoolean(app1.prefix + Constants.PREF_ADDED_SUFFIX, false).apply()
        app1.onClick()
        val startedActivityIntent = Shadows.shadowOf(context as Application?).nextStartedActivity
        Assert.assertNotNull(startedActivityIntent)
        Assert.assertEquals(
                app1.tileNumber().toLong(),
                startedActivityIntent.getIntExtra(Constants.PREF_QS_TILE, Int.MIN_VALUE)
                        .toLong())
        Assert.assertNotNull(startedActivityIntent.component)
        Assert.assertTrue(
                startedActivityIntent
                        .component
                        ?.className
                        ?.endsWith(PersistentShortcutSelectAppActivity::class.java.simpleName)!!
        )
    }

    @Test
    fun testOnClickWithAdded() {
        prefs.edit().putBoolean(app1.prefix + Constants.PREF_ADDED_SUFFIX, true).apply()
        Shadows.shadowOf(app1).setLocked(true)
        app1.onClick()
        Assert.assertFalse(app1.isLocked)
        Shadows.shadowOf(app1).setLocked(false)
        val testPackageName: String = TEST_PACKAGE
        prefs
                .edit()
                .putString(app1.prefix + Constants.PREF_PACKAGE_NAME_SUFFIX, testPackageName)
                .apply()
        val testComponentName: String = TEST_COMPONENT
        prefs
                .edit()
                .putString(app1.prefix + Constants.PREF_COMPONENT_NAME_SUFFIX, testComponentName)
                .apply()
        val testWindowSize: String = TEST_WINDOW_SIZE
        prefs
                .edit()
                .putString(app1.prefix + Constants.PREF_WINDOW_SIZE_SUFFIX, testWindowSize)
                .apply()
        app1.onClick()
        val startedActivityIntent = Shadows.shadowOf(context as Application?).nextStartedActivity
        Assert.assertNotNull(startedActivityIntent)
        Assert.assertNotNull(startedActivityIntent.component)
        Assert.assertTrue(
                startedActivityIntent
                        .component
                        ?.className
                        ?.endsWith(PersistentShortcutLaunchActivity::class.java.simpleName)!!
        )
        Assert.assertEquals(Intent.ACTION_MAIN, startedActivityIntent.action)
        Assert.assertEquals(
                testPackageName,
                startedActivityIntent.getStringExtra(Constants.EXTRA_PACKAGE_NAME)
        )
        Assert.assertEquals(
                testComponentName,
                startedActivityIntent.getStringExtra(Constants.EXTRA_COMPONENT_NAME)
        )
        Assert.assertEquals(
                testWindowSize,
                startedActivityIntent.getStringExtra(Constants.EXTRA_WINDOW_SIZE)
        )
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        Assert.assertEquals(
                userManager.getSerialNumberForUser(Process.myUserHandle()),
                startedActivityIntent.getLongExtra(Constants.EXTRA_USER_ID, Long.MIN_VALUE)
        )
    }

    @Test
    fun testOnStartListeningWithoutAdded() {
        prefs.edit().putBoolean(app1.prefix + Constants.PREF_ADDED_SUFFIX, false).apply()
        app1.onStartListening()
        val tile = app1.qsTile
        Assert.assertEquals(Tile.STATE_INACTIVE.toLong(), tile.state.toLong())
        Assert.assertEquals(app1.getString(R.string.tb_new_shortcut), tile.label)
    }

    @Test
    fun testOnStartListeningWithAdded() {
        prefs.edit().putBoolean(app1.prefix + Constants.PREF_ADDED_SUFFIX, true).apply()
        val testLabel: String = TEST_LABEL
        prefs.edit().putString(app1.prefix + Constants.PREF_LABEL_SUFFIX, testLabel).apply()
        app1.onStartListening()
        val tile = app1.qsTile
        Assert.assertEquals(Tile.STATE_ACTIVE.toLong(), tile.state.toLong())
        Assert.assertEquals(testLabel, tile.label)
    }
}
