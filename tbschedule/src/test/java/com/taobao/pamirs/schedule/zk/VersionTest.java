package com.taobao.pamirs.schedule.zk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionTest {
    @Test
    public void getVersionTest() {
        assertEquals(Version.VERSION, Version.getVersion());
    }
    
    @Test
    public void isCompatibleTest() {
        assertTrue(Version.isCompatible(Version.getVersion()));
        assertFalse(Version.isCompatible("tbschedule-3.2.13")); //不兼容高版本
        assertTrue(Version.isCompatible("tbschedule-3.2.11")); //兼容低版本
    }
}
