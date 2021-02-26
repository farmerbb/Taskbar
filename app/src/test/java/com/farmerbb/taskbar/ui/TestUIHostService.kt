package com.farmerbb.taskbar.ui

import androidx.test.core.app.ApplicationProvider

class TestUIHostService : UIHostService() {
    var controller: TestUIController =
            TestUIController(ApplicationProvider.getApplicationContext())
    override fun newController(): UIController {
        return controller
    }
}
