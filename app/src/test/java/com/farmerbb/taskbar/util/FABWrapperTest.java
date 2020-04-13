package com.farmerbb.taskbar.util;

import android.content.Context;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.HomeActivity;
import com.farmerbb.taskbar.activity.MainActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class FABWrapperTest {
    private Context context;
    private FABWrapper wrapper;
    private ActivityScenario<MainActivity> scenario;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        scenario = ActivityScenario.launch(MainActivity.class);
        scenario.moveToState(Lifecycle.State.RESUMED);
        scenario.onActivity(activity -> wrapper = new FABWrapper(activity));
    }

    @After
    public void tearDown() {
        scenario.moveToState(Lifecycle.State.DESTROYED);
    }

    @Test
    public void testViewForNonAndroidX86Version() {
        assertTrue(wrapper.view instanceof FloatingActionButton);
        int padding = context.getResources().getDimensionPixelSize(R.dimen.tb_fake_fab_padding);
        assertEquals(padding, wrapper.view.getPaddingLeft());
        assertEquals(padding, wrapper.view.getPaddingTop());
        assertEquals(padding, wrapper.view.getPaddingRight());
        assertEquals(padding, wrapper.view.getPaddingBottom());
    }

    @Test
    public void testShow() {
        wrapper.show();
        assertEquals(View.VISIBLE, wrapper.view.getVisibility());
    }

    @Test
    public void testHide() {
        wrapper.show();
        wrapper.hide();
        assertEquals(View.GONE, wrapper.view.getVisibility());
    }
}