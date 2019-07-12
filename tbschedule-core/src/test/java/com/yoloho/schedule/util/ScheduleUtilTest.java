package com.yoloho.schedule.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

public class ScheduleUtilTest {

    @Test
    public void getLocalHostNameTest() {
        assertNotNull(ScheduleUtil.getLocalHostName());
    }
    
    @Test
    public void getLocalIPTest() {
        assertNotNull(ScheduleUtil.getLocalIP());
    }
    
    @Test
    public void dataToString() {
        assertEquals("2019-01-02 17:59:25", ScheduleUtil.dataToString(new Date(1546423165000L)));
    }
    
    @Test
    public void runningEntryFromTaskName() {
        assertEquals("test", ScheduleUtil.runningEntryFromTaskName("test", "BASE"));
        assertEquals("test$base", ScheduleUtil.runningEntryFromTaskName("test", "base"));
        assertEquals("$base", ScheduleUtil.runningEntryFromTaskName("", "base"));
    }
    
    @Test
    public void taskNameFromRunningEntry() {
        assertEquals("a", ScheduleUtil.taskNameFromRunningEntry("a$b"));
        assertEquals("", ScheduleUtil.taskNameFromRunningEntry("$b"));
        assertEquals("b", ScheduleUtil.taskNameFromRunningEntry("b"));
    }
    
    @Test
    public void ownsignFromRunningEntry() {
        assertEquals("b", ScheduleUtil.ownsignFromRunningEntry("a$b"));
        assertEquals("b", ScheduleUtil.ownsignFromRunningEntry("$b"));
        assertEquals("BASE", ScheduleUtil.ownsignFromRunningEntry("b"));
    }
    
    @Test
    public void assignTaskNumberTest() {
        assertArrayEquals(new int[] { 10 }, ScheduleUtil.generateSequence(1, 10, 0));
        assertArrayEquals(new int[] { 5, 5 }, ScheduleUtil.generateSequence(2, 10, 0));
        assertArrayEquals(new int[] { 4, 3, 3 }, ScheduleUtil.generateSequence(3, 10, 0));
        assertArrayEquals(new int[] { 3, 3, 2, 2 }, ScheduleUtil.generateSequence(4, 10, 0));
        assertArrayEquals(new int[] { 2, 2, 2, 2, 2 }, ScheduleUtil.generateSequence(5, 10, 0));
        assertArrayEquals(new int[] { 2, 2, 2, 2, 1, 1 }, ScheduleUtil.generateSequence(6, 10, 0));
        assertArrayEquals(new int[] { 2, 2, 2, 1, 1, 1, 1 }, ScheduleUtil.generateSequence(7, 10, 0));
        assertArrayEquals(new int[] { 2, 2, 1, 1, 1, 1, 1, 1 }, ScheduleUtil.generateSequence(8, 10, 0));
        assertArrayEquals(new int[] { 2, 1, 1, 1, 1, 1, 1, 1, 1 }, ScheduleUtil.generateSequence(9, 10, 0));
        assertArrayEquals(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }, ScheduleUtil.generateSequence(10, 10, 0));
        assertArrayEquals(new int[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ScheduleUtil.generateSequence(10, 1, 0));
        assertArrayEquals(new int[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ScheduleUtil.generateSequence(10, 1, 0));
        assertArrayEquals(new int[] { 3 }, ScheduleUtil.generateSequence(1, 10, 3));
        assertArrayEquals(new int[] { 3, 3 }, ScheduleUtil.generateSequence(2, 10, 3));
        assertArrayEquals(new int[] { 3, 3, 3 }, ScheduleUtil.generateSequence(3, 10, 3));
        assertArrayEquals(new int[] { 3, 3, 2, 2 }, ScheduleUtil.generateSequence(4, 10, 3));
        assertArrayEquals(new int[] { 2, 2, 2, 2, 2 }, ScheduleUtil.generateSequence(5, 10, 3));
        assertArrayEquals(new int[] { 2, 2, 2, 2, 1, 1 }, ScheduleUtil.generateSequence(6, 10, 3));
        assertArrayEquals(new int[] { 2, 2, 2, 1, 1, 1, 1 }, ScheduleUtil.generateSequence(7, 10, 3));
        assertArrayEquals(new int[] { 2, 2, 1, 1, 1, 1, 1, 1 }, ScheduleUtil.generateSequence(8, 10, 3));
        assertArrayEquals(new int[] { 2, 1, 1, 1, 1, 1, 1, 1, 1 }, ScheduleUtil.generateSequence(9, 10, 3));
        assertArrayEquals(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }, ScheduleUtil.generateSequence(10, 10, 3));
    }
    
    @Test
    public void getLeader() {
        List<String> serverList = new ArrayList<>();
        serverList.add("$000005");
        serverList.add("$000003");
        serverList.add("$000001");
        serverList.add("$000002");
        assertEquals("$000001", ScheduleUtil.getLeader(serverList));
    }
    
    @Test
    public void isLeader() {
        List<String> serverList = new ArrayList<>();
        serverList.add("$000005");
        serverList.add("$000003");
        serverList.add("$000001");
        serverList.add("$000002");
        assertFalse(ScheduleUtil.isLeader("asdf", serverList));
        assertFalse(ScheduleUtil.isLeader("$000002", serverList));
        assertTrue(ScheduleUtil.isLeader("$000001", serverList));
    }
}
