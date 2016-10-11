package com.farmerbb.taskbar.util;

import java.io.Serializable;

public class SavedWindowSizesEntry implements Serializable {
    static final long serialVersionUID = 4308104486351313269L;

    private String componentName;
    private String windowSize;

    SavedWindowSizesEntry(String componentName, String label) {
        this.componentName = componentName;
        this.windowSize = label;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getWindowSize() {
        return windowSize;
    }
}