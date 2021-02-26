package com.farmerbb.taskbar.ui

import android.view.View

class MockUIHost : UIHost {
    override fun addView(view: View, params: ViewParams) {}
    override fun removeView(view: View) {}
    override fun terminate() {}
    override fun updateViewLayout(view: View, params: ViewParams) {}
}
