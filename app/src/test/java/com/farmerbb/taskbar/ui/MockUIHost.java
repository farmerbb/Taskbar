package com.farmerbb.taskbar.ui;

import android.view.View;

public class MockUIHost implements UIHost {

    @Override
    public void addView(View view, ViewParams params) {}

    @Override
    public void removeView(View view) {}

    @Override
    public void terminate() {}
}
