package com.farmerbb.taskbar.util

import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.activity.MainActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FABWrapperTest {
    private lateinit var context: Context
    private lateinit var wrapper: FABWrapper
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity { activity: MainActivity? -> wrapper = FABWrapper(activity) }
    }

    @After
    fun tearDown() {
        scenario.moveToState(Lifecycle.State.DESTROYED)
    }

    @Test
    fun testViewForNonAndroidX86Version() {
        Assert.assertTrue(wrapper.view is FloatingActionButton)
        val padding = context.resources.getDimensionPixelSize(R.dimen.tb_fake_fab_padding)
        Assert.assertEquals(padding.toLong(), wrapper.view.paddingLeft.toLong())
        Assert.assertEquals(padding.toLong(), wrapper.view.paddingTop.toLong())
        Assert.assertEquals(padding.toLong(), wrapper.view.paddingRight.toLong())
        Assert.assertEquals(padding.toLong(), wrapper.view.paddingBottom.toLong())
    }

    @Test
    fun testShow() {
        wrapper.show()
        Assert.assertEquals(View.VISIBLE.toLong(), wrapper.view.visibility.toLong())
    }

    @Test
    fun testHide() {
        wrapper.show()
        wrapper.hide()
        Assert.assertEquals(View.GONE.toLong(), wrapper.view.visibility.toLong())
    }
}
