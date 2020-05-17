package com.farmerbb.taskbar.ui;

import android.widget.ScrollView;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowScrollView;

@Implements(ScrollView.class)
public class TaskbarShadowScrollView extends ShadowScrollView {

    @Implementation
    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
    }
}
