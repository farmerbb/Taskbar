package com.farmerbb.taskbar.shadow

import android.widget.ScrollView
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowScrollView

@Implements(ScrollView::class)
class TaskbarShadowScrollView : ShadowScrollView() {
    @Implementation
    public override fun scrollTo(x: Int, y: Int) {
        super.scrollTo(x, y)
    }
}
