package com.farmerbb.taskbar.widget;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.LinearLayout;

public class StartMenuLayout extends LinearLayout {
    private boolean viewHandlesBackButton = false;

    public StartMenuLayout(Context context) {
        super(context);
    }

    public StartMenuLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StartMenuLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void viewHandlesBackButton() {
        viewHandlesBackButton = true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(viewHandlesBackButton && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent("com.farmerbb.taskbar.HIDE_START_MENU"));
            return true;
        }

        return super.dispatchKeyEvent(event);
    }
}
