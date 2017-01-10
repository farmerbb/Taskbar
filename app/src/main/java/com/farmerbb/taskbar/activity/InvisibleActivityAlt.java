package com.farmerbb.taskbar.activity;

import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.farmerbb.taskbar.R;

public class InvisibleActivityAlt extends InvisibleActivity {
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayManager dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);

        setContentView(R.layout.incognito);

        LinearLayout layout = (LinearLayout) findViewById(R.id.incognitoLayout);
        layout.setLayoutParams(new FrameLayout.LayoutParams(display.getWidth(), display.getHeight()));
    }
}
