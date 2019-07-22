package com.yoloho.schedule.interfaces;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.types.Task;
import com.yoloho.schedule.util.ScheduleUtil;

public abstract class IStorageTest {
    private IStorage storage;
    
    protected abstract IStorage getStorage();
    
    @Before
    public void init() {
        storage = getStorage();
    }
    
    @After
    public void deinit() {
        storage.shutdown();
    }
    
    @Test
    public void sequence() throws Exception {
        long s0 = this.storage.getSequenceNumber();
        long s1 = this.storage.getSequenceNumber();
        assertTrue(s0 < s1);
    }
    
    @Test
    public void task() throws Exception {
        String taskName = "task1";
        
        // remove first
        this.storage.removeTask(taskName);
        
        Task task = new Task();
        task.setName(taskName);
        task.setTaskItems(new String[] {"a", "b:{c=1}"});
        task.setDealBeanName("testBean");
        task.setSleepTimeNoData(60000);
        task.setSleepTimeInterval(500);
        task.setTaskKind("Schedule");
        this.storage.createTask(task);
        
        task = this.storage.getTask(taskName);
        assertNotNull(task);
        assertEquals("task1", task.getName());
        assertEquals("testBean", task.getDealBeanName());
        assertEquals(2, task.getTaskItemList().length);
        assertEquals("b", task.getTaskItemList()[1].getTaskItemId());
        
        task.setDealBeanName("testBean1");
        this.storage.updateTask(task);
        
        // running entry
        this.storage.emptyTaskItems(taskName, null);
        this.storage.emptyTaskItems(taskName, "test");
        this.storage.updateTaskItemsInitialResult(taskName, null, "uuid1");
        this.storage.updateTaskItemsInitialResult(taskName, "test", "uuid2");
        List<String> entryList = this.storage.getRunningEntryList(taskName);
        assertNotNull(entryList);
        assertEquals(2, entryList.size());
        assertTrue(entryList.contains(taskName));
        assertTrue(entryList.contains(ScheduleUtil.runningEntryFromTaskName(taskName, "test")));
        
        assertTrue(this.storage.dump().length() > 10);
        
        task = this.storage.getTask(taskName);
        assertNotNull(task);
        assertEquals("task1", task.getName());
        assertEquals("testBean1", task.getDealBeanName());
        assertEquals(2, task.getTaskItemList().length);
        assertEquals("b", task.getTaskItemList()[1].getTaskItemId());
        
        assertTrue(this.storage.removeTask(taskName));
        
        task = this.storage.getTask(taskName);
        assertNull(this.storage.getTask(taskName));
    }
}
