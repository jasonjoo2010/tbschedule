package com.yoloho.schedule.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DataVersionTest {
    @Test
    public void getVersionTest() {
        assertTrue(DataVersion.getCurrentVersion().contains("tbschedule"));
    }
    
    @Test
    public void isCompatibleTest() {
        assertTrue(DataVersion.isCompatible(DataVersion.getCurrentVersion()));
        assertFalse(DataVersion.isCompatible("tbschedule-3.2.13"));
        assertFalse(DataVersion.isCompatible("tbschedule-3.2.11"));
        
        assertFalse(DataVersion.isCompatible("tbschedule-4.0.0"));
        assertFalse(DataVersion.isCompatible("tbschedule-4.0.2"));
        assertFalse(DataVersion.isCompatible("tbschedule-4.0.2.asdfs"));
        assertFalse(DataVersion.isCompatible("tbschedule-4.1.0-asdfs"));
        assertTrue(DataVersion.isCompatible("tbschedule-4.1.0"));
    }
}
