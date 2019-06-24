package com.taobao.pamirs.schedule.zk;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionTest {
    @Test
    public void getVersionTest() {
        assertTrue(DataVersion.getVersion().contains("tbschedule"));
    }
    
    @Test
    public void isCompatibleTest() {
        assertTrue(DataVersion.isCompatible(DataVersion.getVersion()));
        assertFalse(DataVersion.isCompatible("tbschedule-3.2.13")); //不兼容高版本
        assertTrue(DataVersion.isCompatible("tbschedule-3.2.11")); //兼容低版本
    }
}
