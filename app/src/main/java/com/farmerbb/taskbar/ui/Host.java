package com.farmerbb.taskbar.ui;

import android.view.View;

public interface Host {
    void addView(View view, ViewParams params);
    void removeView(View view);
    void terminate();
}