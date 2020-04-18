package com.farmerbb.taskbar.ui;

import androidx.test.core.app.ApplicationProvider;

public class TestUIHostService extends UIHostService {
    TestUIController controller;

    @Override
    public UIController newController() {
        if (controller == null) {
            controller = new TestUIController(ApplicationProvider.getApplicationContext());
        }
        return controller;
    }
}
