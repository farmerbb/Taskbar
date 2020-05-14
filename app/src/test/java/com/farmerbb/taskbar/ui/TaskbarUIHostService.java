package com.farmerbb.taskbar.ui;

import androidx.test.core.app.ApplicationProvider;

public class TaskbarUIHostService extends UIHostService {
    TaskbarController controller;

    @Override
    public UIController newController() {
        if (controller == null) {
            controller = new TaskbarController(ApplicationProvider.getApplicationContext());
        }
        return controller;
    }
}
