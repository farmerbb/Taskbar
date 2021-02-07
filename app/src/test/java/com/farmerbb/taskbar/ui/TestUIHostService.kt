package com.farmerbb.taskbar.ui

import androidx.test.core.app.ApplicationProvider

class TestUIHostService : UIHostService() {
    var controller: TestUIController? = null
    override fun newController(): UIController {
        if (controller == null) {
            controller = TestUIController(ApplicationProvider.getApplicationContext())
        }
        return controller!!
    }
}