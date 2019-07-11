package com.yoloho.schedule.storage.zk.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PathUtilTest {
    @Test
    public void taskBasePath() {
        assertEquals("/baseTaskType", PathUtil.taskBasePath());
    }
    
    @Test
    public void strategyBasePath() {
        assertEquals("/strategy", PathUtil.strategyBasePath());
    }
    
    @Test
    public void factoryBasePath() {
        assertEquals("/factory", PathUtil.factoryBasePath());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void taskPathNull() {
        PathUtil.taskPath(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void taskPathEmpty() {
        PathUtil.taskPath("");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void taskPathIllegal1() {
        PathUtil.taskPath("////");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void taskPathIllegal2() {
        PathUtil.taskPath("as/df");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void taskPathIllegal3() {
        PathUtil.taskPath("as$df");
    }
    
    @Test
    public void taskPath() {
        assertEquals("/baseTaskType/test", PathUtil.taskPath("test"));
    }
    
    @Test
    public void runningEntryPath() {
        assertEquals("/baseTaskType/test/test$run", PathUtil.runningEntryPath("test", "run"));
    }
    
    @Test
    public void taskItemBasePath() {
        assertEquals("/baseTaskType/test/test/taskItem", PathUtil.taskItemBasePath("test", "BASE"));
    }
    
    @Test
    public void taskItemPath() {
        assertEquals("/baseTaskType/test/test$run/taskItem/item", PathUtil.taskItemPath("test", "run", "item"));
    }
    
    @Test
    public void serverBasePath() {
        assertEquals("/baseTaskType/test/test$run/server", PathUtil.serverBasePath("test", "run"));
    }
    
    @Test
    public void serverPath() {
        assertEquals("/baseTaskType/test/test$run/server/test", PathUtil.serverPath("test", "run", "test"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void strategyPathIllegal1() {
        PathUtil.strategyPath("as$d$f");
    }
    
    @Test
    public void strategyPath() {
        assertEquals("/strategy/as$d", PathUtil.strategyPath("as$d"));
    }
    
    @Test
    public void factoryPath() {
        assertEquals("/strategy/as$d", PathUtil.factoryPath("as$d"));
    }
}
