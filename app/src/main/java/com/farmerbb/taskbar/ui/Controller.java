package com.farmerbb.taskbar.ui;

public interface Controller {
    void onCreateHost(Host host);
    void onRecreateHost(Host host);
    void onDestroyHost(Host host);
}