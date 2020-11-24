package com.yoloho.schedule.interfaces;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.yoloho.schedule.types.ScheduleServer;
import com.yoloho.schedule.types.Strategy;
import com.yoloho.schedule.types.StrategyKind;
import com.yoloho.schedule.types.StrategyRuntime;
import com.yoloho.schedule.types.Task;
import com.yoloho.schedule.types.TaskItemRuntime;
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
    public void misc() {
        assertTrue(StringUtils.isNotEmpty(this.storage.getName()));
        assertTrue(this.storage.test());
    }
    
    @Test
    public void sequence() throws Exception {
        long s0 = this.storage.getSequenceNumber();
        long s1 = this.storage.getSequenceNumber();
        assertTrue(s0 < s1);
    }
    
    @Test
    public void getGlobalTime() throws InterruptedException {
        long s0 = this.storage.getGlobalTime();
        Thread.sleep(1000);
        long s1 = this.storage.getGlobalTime();
        assertTrue(s1 - s0 > 990);
    }
    
    private ScheduleFactory newFactory(
            final String uuid, final String ip, final String hostname
            ) {
        ScheduleFactory f = new ScheduleFactory() {
            
            @Override
            public String getUuid() {
                return uuid;
            }
            
            @Override
            public String getIp() {
                return ip;
            }
            
            @Override
            public String getHostName() {
                return hostname;
            }
        };
        return f;
    }
    
    private Strategy newStrategy(String name, String taskName) {
        Strategy strategy = new Strategy();
        strategy.setName(name);
        strategy.setTaskName(taskName);
        strategy.setKind(StrategyKind.Schedule);
        strategy.setAssignNum(1);
        strategy.setIPList(new String[] {"127.0.0.1"});
        strategy.setSts(Strategy.STS_RESUME);
        return strategy;
    }
    
    private Task newTask(String name) {
        Task task = new Task();
        task.setName(name);
        task.setTaskItems(new String[] {"a", "b:{c=1}"});
        task.setDealBeanName("testBean");
        task.setSleepTimeNoData(60000);
        task.setSleepTimeInterval(500);
        task.setTaskKind(Task.TASKKIND_STATIC);
        return task;
    }
    
    private ScheduleServer newServer(String taskName, String ownSign, String serverUUID, String uuid) {
        ScheduleServer server = new ScheduleServer();
        server.setUuid(serverUUID);
        server.setManagerFactoryUUID(uuid);
        server.setThreadNum(1);
        server.setTaskName(taskName);
        server.setOwnSign(ownSign);
        server.setHostName("localhost");
        server.setIp("127.0.0.1");
        return server;
    }
    
    @Test
    public void taskItems() throws Exception {
        this.storage.removeTask("t0");
        this.storage.removeTask("t1");
        this.storage.emptyTaskItems("t0", null);
        this.storage.emptyTaskItems("t0", "dev");
        this.storage.emptyTaskItems("t1", "");
        
        // init tasks
        Task t0 = newTask("t0");
        t0.setTaskItems(new String[] {"a", "b"});
        Task t1 = newTask("t1");
        t1.setTaskItems(new String[] {"p0:{a=1,b=2}", "p1:{a=0,b=3}", "p2:{a=3,b=0}"});
        this.storage.createTask(t0);
        this.storage.createTask(t1);
        
        // verify they are empty
        assertEquals(0, this.storage.getTaskItems("t0", null).size());
        assertEquals(0, this.storage.getTaskItems("t0", "dev").size());
        assertEquals(0, this.storage.getTaskItems("t1", "").size());
        
        assertNull(this.storage.getInitialRunningInfoResult("t0", null));
        assertNull(this.storage.getInitialRunningInfoResult("t0", "dev"));
        assertNull(this.storage.getInitialRunningInfoResult("t1", null));
        
        // initial
        this.storage.initTaskItems("t0", null);
        this.storage.initTaskItems("t0", "dev");
        this.storage.initTaskItems("t1", null);
        this.storage.updateTaskItemsInitialResult("t0", null, "uuid0");
        this.storage.updateTaskItemsInitialResult("t0", "dev", "uuid1");
        this.storage.updateTaskItemsInitialResult("t1", null, "uuid2");
        
        // verify
        assertEquals("uuid0", this.storage.getInitialRunningInfoResult("t0", null).getUuid());
        assertEquals("uuid1", this.storage.getInitialRunningInfoResult("t0", "dev").getUuid());
        assertEquals("uuid2", this.storage.getInitialRunningInfoResult("t1", null).getUuid());
        
        assertEquals(2, this.storage.getTaskItems("t0", null).size());
        assertEquals(2, this.storage.getTaskItems("t0", "dev").size());
        assertEquals(3, this.storage.getTaskItems("t1", "").size());
        
        {
            List<TaskItemRuntime> items = this.storage.getTaskItems("t0", null);
            TaskItemRuntime item = items.get(0);
            assertTrue(StringUtils.isEmpty(item.getRequestScheduleServer()));
            assertTrue(StringUtils.isEmpty(item.getCurrentScheduleServer()));
            
            // asssign
            this.storage.updateTaskItemCurrentServer(item.getTaskName(), item.getOwnSign(), item.getTaskItem(), "server1");
            this.storage.updateTaskItemRequestServer(item.getTaskName(), item.getOwnSign(), item.getTaskItem(), "server2");
            
            // verify
            items = this.storage.getTaskItems("t0", null);
            item = items.get(0);
            assertEquals("server1", item.getCurrentScheduleServer());
            assertEquals("server2", item.getRequestScheduleServer());
            
            // release
            this.storage.releaseTaskItemByOwner("t0", null, "server2");
            
            items = this.storage.getTaskItems("t0", null);
            item = items.get(0);
            assertEquals("server1", item.getCurrentScheduleServer());
            assertEquals("server2", item.getRequestScheduleServer());
            
            this.storage.releaseTaskItemByOwner("t0", null, "server1");
            
            items = this.storage.getTaskItems("t0", null);
            item = items.get(0);
            assertEquals("server2", item.getCurrentScheduleServer());
            assertEquals("", item.getRequestScheduleServer());
        }
        
        // empty them
        this.storage.removeRunningEntry("t0", null); // different way
        this.storage.emptyTaskItems("t0", "dev");
        this.storage.emptyTaskItems("t1", null);
        
        // verify
        assertEquals(0, this.storage.getTaskItems("t0", null).size());
        assertEquals(0, this.storage.getTaskItems("t0", "dev").size());
        assertEquals(0, this.storage.getTaskItems("t1", "").size());
        
        // remove tasks
        this.storage.removeTask("t0");
        this.storage.removeTask("t1");
    }
    
    @Test
    public void server() throws Exception {
        this.storage.removeRunningEntry("t0", null);
        this.storage.removeRunningEntry("t0", "dev");
        this.storage.removeRunningEntry("t1", null);
        
        ScheduleServer server1 = newServer("t0", null, "server1", "host1");
        ScheduleServer server2 = newServer("t0", null, "server2", "host2");
        ScheduleServer server3 = newServer("t0", "dev", "server3", "host1");
        ScheduleServer server4 = newServer("t0", "dev", "server4", "host2");
        ScheduleServer server5 = newServer("t1", null, "server5", "host3");
        ScheduleServer server6 = newServer("t1", null, "server6", "host4");
        
        assertNull(this.storage.getServer("t0", null, "server1"));
        assertNull(this.storage.getServer("t0", null, "server2"));
        assertNull(this.storage.getServer("t0", "dev", "server3"));
        assertNull(this.storage.getServer("t0", "dev", "server4"));
        assertNull(this.storage.getServer("t1", null, "server5"));
        assertNull(this.storage.getServer("t1", null, "server6"));
        
        assertEquals(Collections.emptyList(), this.storage.getServerUuidList("t0", null));
        assertEquals(Collections.emptyList(), this.storage.getServerUuidList("t0", "dev"));
        assertEquals(Collections.emptyList(), this.storage.getServerUuidList("t1", null));
        
        // get versions
        assertEquals(0, this.storage.getServerSchedulingVersion("t0", null));
        assertEquals(0, this.storage.getServerSchedulingVersion("t0", "dev"));
        assertEquals(0, this.storage.getServerSchedulingVersion("t1", null));
        
        // increase some of them
        this.storage.increaseServerSchedulingVersion("t0", "dev");
        this.storage.increaseServerSchedulingVersion("t1", "");
        
        // verify
        assertEquals(0, this.storage.getServerSchedulingVersion("t0", null));
        assertEquals(1, this.storage.getServerSchedulingVersion("t0", "dev"));
        assertEquals(1, this.storage.getServerSchedulingVersion("t1", null));
        
        // create servers
        this.storage.createServer(server6);
        this.storage.createServer(server5);
        this.storage.createServer(server3);
        this.storage.createServer(server1);
        this.storage.createServer(server2);
        this.storage.createServer(server4);
        
        // verify
        assertNotNull(this.storage.getServer("t0", null, "server1"));
        assertNotNull(this.storage.getServer("t0", null, "server2"));
        assertNotNull(this.storage.getServer("t0", "dev", "server3"));
        assertNotNull(this.storage.getServer("t0", "dev", "server4"));
        assertNotNull(this.storage.getServer("t1", null, "server5"));
        assertNotNull(this.storage.getServer("t1", null, "server6"));
        
        assertEquals(Arrays.asList("server1", "server2"), this.storage.getServerUuidList("t0", null));
        assertEquals(Arrays.asList("server3", "server4"), this.storage.getServerUuidList("t0", "dev"));
        assertEquals(Arrays.asList("server5", "server6"), this.storage.getServerUuidList("t1", null));
        
        // remove partial
        this.storage.removeServer("t0", null, "server1");
        this.storage.removeServer("t0", "dev", "server1"); // wrong
        this.storage.removeServer("t1", null, "server5");
        
        // verify
        assertNull(this.storage.getServer("t0", null, "server1"));
        assertNotNull(this.storage.getServer("t0", null, "server2"));
        assertNotNull(this.storage.getServer("t0", "dev", "server3"));
        assertNotNull(this.storage.getServer("t0", "dev", "server4"));
        assertNull(this.storage.getServer("t1", null, "server5"));
        assertNotNull(this.storage.getServer("t1", null, "server6"));
        
        // remove
        this.storage.removeServer("t0", "dev", "server4"); // right
        
        // verify
        assertNull(this.storage.getServer("t0", null, "server1"));
        assertNotNull(this.storage.getServer("t0", null, "server2"));
        assertNotNull(this.storage.getServer("t0", "dev", "server3"));
        assertNull(this.storage.getServer("t0", "dev", "server4"));
        assertNull(this.storage.getServer("t1", null, "server5"));
        assertNotNull(this.storage.getServer("t1", null, "server6"));
        
        // clear all
        this.storage.removeRunningEntry("t0", null);
        this.storage.removeRunningEntry("t0", "dev");
        this.storage.removeRunningEntry("t1", null);
        
        assertNull(this.storage.getServer("t0", null, "server1"));
        assertNull(this.storage.getServer("t0", null, "server2"));
        assertNull(this.storage.getServer("t0", "dev", "server3"));
        assertNull(this.storage.getServer("t0", "dev", "server4"));
        assertNull(this.storage.getServer("t1", null, "server5"));
        assertNull(this.storage.getServer("t1", null, "server6"));
    }
    
    @Test
    public void factory() throws Exception {
        ScheduleFactory f1 = newFactory("uuid1", "127.0.0.1", "localhost");
        ScheduleFactory f2 = newFactory("uuid2", "127.0.0.1", "localhost");
        ScheduleFactory f3 = newFactory("uuid3", "127.0.0.1", "localhost");
        
        // clear
        this.storage.unregisterFactory(f1);
        this.storage.unregisterFactory(f2);
        this.storage.unregisterFactory(f3);
        this.storage.removeStrategy("s0");
        
        assertEquals(Collections.emptyList(), this.storage.getFactoryUuidList());
        
        assertTrue(this.storage.isFactoryAllowExecute("uuid1"));
        assertTrue(this.storage.isFactoryAllowExecute("uuid2"));
        assertTrue(this.storage.isFactoryAllowExecute("uuid3"));
        
        Strategy strategy = newStrategy("s0", "t0");
        this.storage.createStrategy(strategy);
        
        // register
        this.storage.registerFactory(f1);
        this.storage.registerFactory(f2);
        this.storage.registerFactory(f3);
        
        // runtime verify
        assertNotNull(this.storage.getStrategyRuntime("s0", "uuid1"));
        assertNotNull(this.storage.getStrategyRuntime("s0", "uuid2"));
        assertNotNull(this.storage.getStrategyRuntime("s0", "uuid3"));
        // runtime verify
        assertEquals(3, this.storage.getRuntimesOfStrategy("s0").size());
        
        // update runtime
        StrategyRuntime runtime = this.storage.getStrategyRuntime("s0", "uuid1");
        assertEquals(0, runtime.getCurrentNum());
        runtime.setCurrentNum(222);
        this.storage.updateRuntimeOfStrategy(runtime);
        runtime = this.storage.getStrategyRuntime("s0", "uuid1");
        assertEquals(222, runtime.getCurrentNum());
        
        // verify
        assertTrue(this.storage.isFactoryAllowExecute("uuid1"));
        assertTrue(this.storage.isFactoryAllowExecute("uuid2"));
        assertTrue(this.storage.isFactoryAllowExecute("uuid3"));
        assertEquals(Arrays.asList("uuid1", "uuid2", "uuid3"), this.storage.getFactoryUuidList());
        
        // disable 2
        this.storage.setFactoryAllowExecute(f1.getUuid(), false);
        this.storage.setFactoryAllowExecute(f3.getUuid(), false);
        
        // verify
        assertFalse(this.storage.isFactoryAllowExecute("uuid1"));
        assertTrue(this.storage.isFactoryAllowExecute("uuid2"));
        assertFalse(this.storage.isFactoryAllowExecute("uuid3"));
        
        // stop strategy
        strategy.setSts(Strategy.STS_PAUSE);
        this.storage.updateStrategy(strategy);
        this.storage.registerFactory(f1);
        this.storage.registerFactory(f2);
        this.storage.registerFactory(f3);
        // verify
        assertEquals(0, this.storage.getRuntimesOfStrategy("s0").size());
        
        // resume strategy
        strategy.setSts(Strategy.STS_RESUME);
        this.storage.updateStrategy(strategy);
        this.storage.registerFactory(f1);
        this.storage.registerFactory(f2);
        this.storage.registerFactory(f3);
        // verify
        assertEquals(3, this.storage.getRuntimesOfStrategy("s0").size());
        
        // clear runtimes
        this.storage.clearStrategiesOfFactory("uuid1");
        // verify
        assertEquals(2, this.storage.getRuntimesOfStrategy("s0").size());
        
        // clear
        this.storage.unregisterFactory(f1);
        this.storage.unregisterFactory(f2);
        this.storage.unregisterFactory(f3);
        assertEquals(Collections.emptyList(), this.storage.getFactoryUuidList());
        assertNull(this.storage.getStrategyRuntime("s0", "uuid1"));
        assertNull(this.storage.getStrategyRuntime("s0", "uuid2"));
        assertNull(this.storage.getStrategyRuntime("s0", "uuid3"));
        this.storage.removeStrategy("s0");
    }
    
    @Test
    public void strategy() throws Exception {
        // clear
        this.storage.removeStrategy("s0");
        this.storage.removeStrategy("s2");
        this.storage.removeStrategy("s1");
        
        assertEquals(Collections.emptyList(), this.storage.getStrategyNames());
        
        // create
        this.storage.createStrategy(newStrategy("s2", "t"));
        this.storage.createStrategy(newStrategy("s0", "t"));
        this.storage.createStrategy(newStrategy("s1", "t"));
        
        // verify
        assertEquals(Arrays.asList("s0", "s1", "s2"), this.storage.getStrategyNames());
        
        // update
        Strategy s = this.storage.getStrategy("s1");
        assertEquals("t", s.getTaskName());
        s.setTaskName("t111");
        this.storage.updateStrategy(s);
        s = this.storage.getStrategy("s1");
        assertEquals("t111", s.getTaskName());
        
        // remove
        this.storage.removeStrategy("s1");
        
        // verify
        assertEquals(Arrays.asList("s0", "s2"), this.storage.getStrategyNames());
        
        // remove remained
        this.storage.removeStrategy("s0");
        this.storage.removeStrategy("s2");
        
        // verify
        assertEquals(Collections.emptyList(), this.storage.getStrategyNames());
    }
    
    @Test
    public void runningEntries() throws Exception {
        String taskName = "task1";
        
        // clear first
        this.storage.emptyTaskItems(taskName, null);
        this.storage.emptyTaskItems(taskName, "test");
        
        // set initial result
        this.storage.updateTaskItemsInitialResult(taskName, null, "uuid1");
        this.storage.updateTaskItemsInitialResult(taskName, "test", "uuid2");
        
        // verify
        List<String> entryList = this.storage.getRunningEntryList(taskName);
        assertNotNull(entryList);
        assertEquals(2, entryList.size());
        assertTrue(entryList.contains(ScheduleUtil.runningEntryFromTaskName(taskName, "")));
        assertTrue(entryList.contains(ScheduleUtil.runningEntryFromTaskName(taskName, "test")));
        
        // clear them
        this.storage.emptyTaskItems(taskName, null);
        this.storage.emptyTaskItems(taskName, "test");
        
        // verify
        assertEquals(Collections.emptyList(), this.storage.getRunningEntryList(taskName));
    }
    
    @Test
    public void task() throws Exception {
        String taskName = "task1";
        String taskName2 = "task2";
        String taskName3 = "task3";
        
        // remove first
        this.storage.removeTask(taskName);
        this.storage.removeTask(taskName2);
        this.storage.removeTask(taskName3);
        
        Task task = newTask(taskName);
        this.storage.createTask(task);
        this.storage.createTask(newTask(taskName3));
        this.storage.createTask(newTask(taskName2));
        
        task = this.storage.getTask(taskName);
        assertNotNull(task);
        assertEquals("task1", task.getName());
        assertEquals("testBean", task.getDealBeanName());
        assertEquals(2, task.getTaskItemList().length);
        assertEquals("b", task.getTaskItemList()[1].getTaskItemId());
        
        task.setDealBeanName("testBean1");
        this.storage.updateTask(task);
        
        task = this.storage.getTask(taskName);
        assertNotNull(task);
        assertEquals("task1", task.getName());
        assertEquals("testBean1", task.getDealBeanName());
        assertEquals(2, task.getTaskItemList().length);
        assertEquals("b", task.getTaskItemList()[1].getTaskItemId());
        
        assertEquals(Arrays.asList("task1", "task2", "task3"), this.storage.getTaskNames());
        
        assertTrue(this.storage.removeTask(taskName));
        
        assertEquals(Arrays.asList("task2", "task3"), this.storage.getTaskNames());
        
        task = this.storage.getTask(taskName);
        assertNull(this.storage.getTask(taskName));
        
        assertTrue(this.storage.dump().length() > 10);
        
        // remove remained
        assertTrue(this.storage.removeTask(taskName2));
        assertTrue(this.storage.removeTask(taskName3));
        
        // verify
        assertEquals(Collections.emptyList(), this.storage.getTaskNames());
    }
}
