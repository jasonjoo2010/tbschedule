package com.taobao.pamirs.schedule;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.Date;

import org.junit.Test;

public class ScheduleUtilTest {

    @Test
    public void getLocalHostNameTest() {
        assertNotNull(ScheduleUtil.getLocalHostName());
    }
    
    @Test
    public void getFreeSocketPortTest() {
        assertTrue(ScheduleUtil.getFreeSocketPort() > 0);
    }
    
    @Test
    public void getLocalIPTest() {
        assertNotNull(ScheduleUtil.getLocalIP());
    }
    
    @Test
    public void transferDataToStringTest() {
        assertEquals("2019-01-02 17:59:25", ScheduleUtil.transferDataToString(new Date(1546423165000L)));
    }
    
    @Test
    public void transferStringToDateTest() {
        try {
            assertEquals(1546423165000L, ScheduleUtil.transferStringToDate("2019-01-02 17:59:25").getTime());
        } catch (ParseException e) {
            assertTrue(false);
        }
    }
    
    @Test
    public void getTaskTypeByBaseAndOwnSignTest() {
        assertEquals("test", ScheduleUtil.getTaskTypeByBaseAndOwnSign("test", "BASE"));
        assertEquals("test$base", ScheduleUtil.getTaskTypeByBaseAndOwnSign("test", "base"));
        assertEquals("$base", ScheduleUtil.getTaskTypeByBaseAndOwnSign("", "base"));
    }
    
    @Test
    public void splitBaseTaskTypeFromTaskTypeTest() {
        assertEquals("a", ScheduleUtil.splitBaseTaskTypeFromTaskType("a$b"));
        assertEquals("", ScheduleUtil.splitBaseTaskTypeFromTaskType("$b"));
        assertEquals("b", ScheduleUtil.splitBaseTaskTypeFromTaskType("b"));
    }
    
    @Test
    public void splitOwnsignFromTaskTypeTest() {
        assertEquals("b", ScheduleUtil.splitOwnsignFromTaskType("a$b"));
        assertEquals("b", ScheduleUtil.splitOwnsignFromTaskType("$b"));
        assertEquals("BASE", ScheduleUtil.splitOwnsignFromTaskType("b"));
    }
    
    @Test
    public void assignTaskNumberTest() {
        assertArrayEquals(new int[] { 10 }, ScheduleUtil.assignTaskNumber(1, 10, 0));
        assertArrayEquals(new int[] { 5, 5 }, ScheduleUtil.assignTaskNumber(2, 10, 0));
        assertArrayEquals(new int[] { 4, 3, 3 }, ScheduleUtil.assignTaskNumber(3, 10, 0));
        assertArrayEquals(new int[] { 3, 3, 2, 2 }, ScheduleUtil.assignTaskNumber(4, 10, 0));
        assertArrayEquals(new int[] { 2, 2, 2, 2, 2 }, ScheduleUtil.assignTaskNumber(5, 10, 0));
        assertArrayEquals(new int[] { 2, 2, 2, 2, 1, 1 }, ScheduleUtil.assignTaskNumber(6, 10, 0));
        assertArrayEquals(new int[] { 2, 2, 2, 1, 1, 1, 1 }, ScheduleUtil.assignTaskNumber(7, 10, 0));
        assertArrayEquals(new int[] { 2, 2, 1, 1, 1, 1, 1, 1 }, ScheduleUtil.assignTaskNumber(8, 10, 0));
        assertArrayEquals(new int[] { 2, 1, 1, 1, 1, 1, 1, 1, 1 }, ScheduleUtil.assignTaskNumber(9, 10, 0));
        assertArrayEquals(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }, ScheduleUtil.assignTaskNumber(10, 10, 0));
        assertArrayEquals(new int[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ScheduleUtil.assignTaskNumber(10, 1, 0));
        assertArrayEquals(new int[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ScheduleUtil.assignTaskNumber(10, 1, 0));
        assertArrayEquals(new int[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ScheduleUtil.assignTaskNumber(10, 1, 0));
        assertArrayEquals(new int[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ScheduleUtil.assignTaskNumber(10, 1, 0));
        assertArrayEquals(new int[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ScheduleUtil.assignTaskNumber(10, 1, 0));
        assertArrayEquals(new int[] { 10 }, ScheduleUtil.assignTaskNumber(1, 10, 3));
        assertArrayEquals(new int[] { 5, 5 }, ScheduleUtil.assignTaskNumber(2, 10, 3));
        assertArrayEquals(new int[] { 4, 3, 3 }, ScheduleUtil.assignTaskNumber(3, 10, 3));
        assertArrayEquals(new int[] { 3, 3, 2, 2 }, ScheduleUtil.assignTaskNumber(4, 10, 3));
        assertArrayEquals(new int[] { 2, 2, 2, 2, 2 }, ScheduleUtil.assignTaskNumber(5, 10, 3));
        assertArrayEquals(new int[] { 2, 2, 2, 2, 1, 1 }, ScheduleUtil.assignTaskNumber(6, 10, 3));
        assertArrayEquals(new int[] { 2, 2, 2, 1, 1, 1, 1 }, ScheduleUtil.assignTaskNumber(7, 10, 3));
        assertArrayEquals(new int[] { 2, 2, 1, 1, 1, 1, 1, 1 }, ScheduleUtil.assignTaskNumber(8, 10, 3));
        assertArrayEquals(new int[] { 2, 1, 1, 1, 1, 1, 1, 1, 1 }, ScheduleUtil.assignTaskNumber(9, 10, 3));
        assertArrayEquals(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }, ScheduleUtil.assignTaskNumber(10,10,3));
    }
}
