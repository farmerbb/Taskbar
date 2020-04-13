package com.farmerbb.taskbar.util;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class SavedWindowSizeEntryTest {
    private static final String COMPONENT_NAME = "test-component-name";
    private static final String WINDOW_SIZE = "test-window-size";
    private SavedWindowSizesEntry entry = new SavedWindowSizesEntry(COMPONENT_NAME, WINDOW_SIZE);

    @Test
    public void testGetComponentName() {
        assertEquals(COMPONENT_NAME, entry.getComponentName());
    }

    @Test
    public void testGetWindowSize() {
        assertEquals(WINDOW_SIZE, entry.getWindowSize());
    }

    @Test
    public void testSerializable() {
        SavedWindowSizesEntry newEntry =
                SerializationUtils.deserialize(SerializationUtils.serialize(entry));
        assertEquals(entry.getComponentName(), newEntry.getComponentName());
        assertEquals(entry.getWindowSize(), newEntry.getWindowSize());
    }
}
