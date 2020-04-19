package com.farmerbb.taskbar.ui;

import android.content.Context;

public class TestUIController extends UIController {
    UIHost onCreateHost;
    UIHost onRecreateHost;
    UIHost onDestroyHost;

    public TestUIController(Context context) {
        super(context);
    }

    @Override
    void onCreateHost(UIHost host) {
        onCreateHost = host;
    }

    @Override
    void onRecreateHost(UIHost host) {
        onRecreateHost = host;
    }

    @Override
    void onDestroyHost(UIHost host) {
        onDestroyHost = host;
    }
}
