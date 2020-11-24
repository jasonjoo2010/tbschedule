package com.yoloho.schedule.storage.memory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.yoloho.enhanced.common.util.BeansUtil;
import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.interfaces.ScheduleFactory;
import com.yoloho.schedule.types.InitialResult;
import com.yoloho.schedule.types.ScheduleConfig;
import com.yoloho.schedule.types.ScheduleServer;
import com.yoloho.schedule.types.Strategy;
import com.yoloho.schedule.types.StrategyRuntime;
import com.yoloho.schedule.types.Task;
import com.yoloho.schedule.types.TaskItem;
import com.yoloho.schedule.types.TaskItemRuntime;
import com.yoloho.schedule.util.ScheduleUtil;

public class MemoryStorage implements IStorage {
    private static final Logger logger = LoggerFactory.getLogger(MemoryStorage.class.getSimpleName());
    private static final String SEPARATOR = "||";
    
    private final AtomicLong squenceCounter = new AtomicLong();
    private boolean initialized = false;
    private final ConcurrentHashMap<String, Task> taskMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, InitialResult> runningEntryInitialResult = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> runningEntryServerVersion = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> runningEntryServerList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, TaskItemRuntime>> runningEntryTaskItems = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduleServer> serverMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Strategy> strategyMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> factoryAllowExecuteMap = new ConcurrentHashMap<>();
    // strategyName-factoryUUID pair as key
    private final ConcurrentHashMap<String, StrategyRuntime> strategyRuntimeMap = new ConcurrentHashMap<>();
    
    @Override
    public String getName() {
        return "memory";
    }
    
    @Override
    public long getSequenceNumber() throws Exception {
        return squenceCounter.incrementAndGet();
    }

    @Override
    public long getGlobalTime() {
        return System.currentTimeMillis();
    }
    
    @Override
    public boolean test() {
        return initialized;
    }
    
    @Override
    public String dump() throws Exception {
        return "Not Implemented";
    }

    @Override
    public boolean init(final ScheduleConfig config, final OnConnected onConnected) {
        synchronized (this) {
            initialized = true;
            onConnected.connected(this);
        }
        logger.info("Local scheduling initialized");
        return true;
    }
    
    @Override
    public void shutdown() {
        synchronized (this) {
        }
    }
    
    @Override
    public void createTask(Task task) throws Exception {
        if (taskMap.containsKey(task.getName())) {
            throw new Exception("Task [" + task.getName() + "] has already existed.");
        }
        taskMap.putIfAbsent(task.getName(), task);
    }

    @Override
    public void updateTask(Task task) throws Exception {
        taskMap.put(task.getName(), task);
    }
    
    @Override
    public boolean removeTask(String taskName) throws Exception {
        if (taskMap.containsKey(taskName)) {
            taskMap.remove(taskName);
            return true;
        }
        return false;
    }
    
    @Override
    public void removeRunningEntry(String taskName, String ownSign) throws Exception {
        String runningEntryName = ScheduleUtil.runningEntryFromTaskName(taskName, ownSign);
        runningEntryInitialResult.remove(runningEntryName);
        runningEntryServerList.remove(runningEntryName);
        runningEntryServerVersion.remove(runningEntryName);
        runningEntryTaskItems.remove(runningEntryName);
    }
    
    @Override
    public Task getTask(String taskName) throws Exception {
        return taskMap.get(taskName);
    }
    
    @Override
    public List<String> getTaskNames() throws Exception {
        List<String> list = new ArrayList<>(taskMap.keySet());
        Collections.sort(list);
        return list;
    }
    
    @Override
    public void emptyTaskItems(String taskName, String ownSign) throws Exception {
        String runningEntryName = ScheduleUtil.runningEntryFromTaskName(taskName, ownSign);
        runningEntryTaskItems.remove(runningEntryName);
    }
    
    @Override
    public InitialResult getInitialRunningInfoResult(String taskName, String ownSign) throws Exception {
        return runningEntryInitialResult.get(ScheduleUtil.runningEntryFromTaskName(taskName, ownSign));
    }
    
    @Override
    public void updateTaskItemsInitialResult(String taskName, String ownSign, 
            String initializerUuid) throws Exception {
        String runningEntryName = ScheduleUtil.runningEntryFromTaskName(taskName, ownSign);
        InitialResult result = getInitialRunningInfoResult(taskName, ownSign);
        if (result == null) {
            result = new InitialResult();
        }
        result.setVersion(result.getVersion() + 1);
        result.setUpdateTime(getGlobalTime());
        result.setUuid(initializerUuid);
        runningEntryInitialResult.put(runningEntryName, result);
    }
    
    @Override
    public void initTaskItems(String taskName, String ownSign) throws Exception {
        String runningEntryName = ScheduleUtil.runningEntryFromTaskName(taskName, ownSign);
        Map<String, TaskItemRuntime> taskItemMap = runningEntryTaskItems.get(runningEntryName);
        if (taskItemMap == null) {
            runningEntryTaskItems.putIfAbsent(runningEntryName, new HashMap<String, TaskItemRuntime>());
            taskItemMap = runningEntryTaskItems.get(runningEntryName);
        }
        Task task = getTask(taskName);
        if (task != null) {
            // init task items
            synchronized (taskItemMap) {
                TaskItem[] items = task.getTaskItemList();
                for (TaskItem define : items) {
                    // init other properties
                    TaskItemRuntime item = new TaskItemRuntime();
                    item.setTaskName(taskName);
                    item.setOwnSign(ownSign);
                    item.setDealParameter(define.getParameter());
                    item.setCurrentScheduleServer("");
                    item.setRequestScheduleServer("");
                    item.setTaskItem(define.getTaskItemId());
                    taskItemMap.put(item.getTaskItem(), item);
                }
            }
        }
    }
    
    @Override
    public List<String> getRunningEntryList(String taskName) throws Exception {
        List<String> result = new ArrayList<>();
        // consider to multiple maps
        Set<String> entryNameSet = runningEntryInitialResult.keySet();
        for (String entryName : entryNameSet) {
            String name = ScheduleUtil.taskNameFromRunningEntry(entryName);
            if (StringUtils.equals(taskName, name)) {
                result.add(entryName);
            }
        }
        if (!result.isEmpty()) {
            Collections.sort(result);
        }
        return result;
    }
    
    @Override
    public List<TaskItemRuntime> getTaskItems(String taskName, String ownSign) throws Exception {
        String runningEntryName = ScheduleUtil.runningEntryFromTaskName(taskName, ownSign);
        if (!runningEntryTaskItems.containsKey(runningEntryName)) {
            return Collections.emptyList();
        }
        Map<String, TaskItemRuntime> items = runningEntryTaskItems.get(runningEntryName);
        if (items == null) {
            return Collections.emptyList();
        }
        List<TaskItemRuntime> result = new ArrayList<>(items.size());
        synchronized (items) {
            for (Entry<String, TaskItemRuntime> entry : items.entrySet()) {
                TaskItemRuntime runtime = new TaskItemRuntime();
                BeansUtil.copyBean(entry.getValue(), runtime);
                result.add(runtime);
            }
        }
        if (!result.isEmpty()) {
            Collections.sort(result, COMPARATOR_TASK_ITEM_RUNTIME);
        }
        return result;
    }
    
    @Override
    public int releaseTaskItemByOwner(String taskName, String ownSign, String ownerUuid) throws Exception {
        String runningEntryName = ScheduleUtil.runningEntryFromTaskName(taskName, ownSign);
        Map<String, TaskItemRuntime> items = runningEntryTaskItems.get(runningEntryName);
        int released = 0;
        if (items == null) {
            return released;
        }
        synchronized (items) {
            for (Entry<String, TaskItemRuntime> entry : items.entrySet()) {
                TaskItemRuntime item = entry.getValue();
                if (StringUtils.isNotEmpty(item.getCurrentScheduleServer()) && StringUtils.isNotEmpty(item.getRequestScheduleServer())) {
                    if (StringUtils.equals(ownerUuid, item.getCurrentScheduleServer())) {
                        item.setCurrentScheduleServer(item.getRequestScheduleServer());
                        item.setRequestScheduleServer("");
                        released ++;
                    }
                }
            }
        }
        return released;
    }
    
    @Override
    public void updateTaskItemRequestServer(String taskName, String ownSign, String taskItem, String server) throws Exception {
        String runningEntryName = ScheduleUtil.runningEntryFromTaskName(taskName, ownSign);
        Map<String, TaskItemRuntime> items = runningEntryTaskItems.get(runningEntryName);
        if (items == null) {
            runningEntryTaskItems.putIfAbsent(runningEntryName, new HashMap<String, TaskItemRuntime>());
            items = runningEntryTaskItems.get(runningEntryName);
        }
        synchronized (items) {
            TaskItemRuntime item = items.get(taskItem);
            if (item == null) {
                // should not happen
                item = new TaskItemRuntime();
                item.setTaskName(taskName);
                item.setOwnSign(ownSign);
                item.setDealParameter("");
                item.setCurrentScheduleServer("");
                item.setRequestScheduleServer("");
                item.setTaskItem(taskItem);
                items.put(taskItem, item);
            }
            item.setRequestScheduleServer(server);
        }
    }
    
    @Override
    public void updateTaskItemCurrentServer(String taskName, String ownSign, String taskItem, String server) throws Exception {
        String runningEntryName = ScheduleUtil.runningEntryFromTaskName(taskName, ownSign);
        Map<String, TaskItemRuntime> items = runningEntryTaskItems.get(runningEntryName);
        if (items == null) {
            runningEntryTaskItems.putIfAbsent(runningEntryName, new HashMap<String, TaskItemRuntime>());
            items = runningEntryTaskItems.get(runningEntryName);
        }
        synchronized (items) {
            TaskItemRuntime item = items.get(taskItem);
            if (item == null) {
                // should not happen
                item = new TaskItemRuntime();
                item.setTaskName(taskName);
                item.setOwnSign(ownSign);
                item.setDealParameter("");
                item.setCurrentScheduleServer("");
                item.setRequestScheduleServer("");
                item.setTaskItem(taskItem);
                items.put(taskItem, item);
            }
            item.setCurrentScheduleServer(server);
        }
    }
    
    @Override
    public long getServerSchedulingVersion(String taskName, String ownSign) throws Exception {
        String runningEntryName = ScheduleUtil.runningEntryFromTaskName(taskName, ownSign);
        AtomicLong atomic = runningEntryServerVersion.get(runningEntryName);
        if (atomic == null) {
            return 0;
        }
        return atomic.get();
    }
    
    @Override
    public void increaseServerSchedulingVersion(String taskName, String ownSign) throws Exception {
        String runningEntryName = ScheduleUtil.runningEntryFromTaskName(taskName, ownSign);
        AtomicLong atomic = runningEntryServerVersion.get(runningEntryName);
        if (atomic == null) {
            runningEntryServerVersion.putIfAbsent(runningEntryName, new AtomicLong());
            atomic = runningEntryServerVersion.get(runningEntryName);
        }
        atomic.incrementAndGet();
    }
    
    @Override
    public List<String> getServerUuidList(String taskName, String ownSign) throws Exception {
        String runningEntry = ScheduleUtil.runningEntryFromTaskName(taskName, ownSign);
        Set<String> servers = runningEntryServerList.get(runningEntry);
        if (servers == null) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>(servers);
        Collections.sort(list, COMPARATOR_UUID);
        return list;
    }
    
    @Override
    public void removeServer(String taskName, String ownSign, String serverUuid) throws Exception {
        String runningEntry = ScheduleUtil.runningEntryFromTaskName(taskName, ownSign);
        Set<String> servers = runningEntryServerList.get(runningEntry);
        if (servers != null) {
            servers.remove(serverUuid);
        }
        serverMap.remove(serverUuid);
    }
    
    @Override
    public boolean updateServer(ScheduleServer server) throws Exception {
        String runningEntry = ScheduleUtil.runningEntryFromTaskName(server.getTaskName(), server.getOwnSign());
        Set<String> servers = runningEntryServerList.get(runningEntry);
        ScheduleServer curServer = serverMap.get(server.getUuid());
        if (server == null || servers == null || !servers.contains(server.getUuid())) {
            return false;
        }
        BeansUtil.copyBean(server, curServer);
        return true;
    }
    
    @Override
    public void createServer(ScheduleServer server) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotEmpty(server.getUuid()));
        String runningEntry = ScheduleUtil.runningEntryFromTaskName(server.getTaskName(), server.getOwnSign());
        // uuid should be unique
        if (serverMap.containsKey(server.getUuid())) {
            throw new Exception(server.getUuid() + " duplicated");
        }
        try {
            Set<String> servers = runningEntryServerList.get(runningEntry);
            if (servers == null) {
                runningEntryServerList.putIfAbsent(runningEntry, new ConcurrentSkipListSet<String>());
                servers = runningEntryServerList.get(runningEntry);
            }
            servers.add(server.getUuid());
            Timestamp heartBeatTime = new Timestamp(getGlobalTime());
            ScheduleServer newServer = new ScheduleServer();
            BeansUtil.copyBean(server, newServer);
            newServer.setHeartBeatTime(heartBeatTime);
            serverMap.put(server.getUuid(), newServer);
        } catch (Exception e) {
            // rollback
            Set<String> servers = runningEntryServerList.get(runningEntry);
            if (servers != null) {
                servers.remove(server.getUuid());
            }
            serverMap.remove(server.getUuid());
            throw e;
        }
    }
    
    @Override
    public ScheduleServer getServer(String taskName, String ownSign, String serverUuid) throws Exception {
        String runningEntry = ScheduleUtil.runningEntryFromTaskName(taskName, ownSign);
        Set<String> servers = runningEntryServerList.get(runningEntry);
        ScheduleServer server = serverMap.get(serverUuid);
        if (server == null || servers == null || !servers.contains(serverUuid)) {
            return null;
        }
        return server;
    }
    
    @Override
    public List<String> getStrategyNames() throws Exception {
        List<String> list = new ArrayList<>(strategyMap.keySet());
        Collections.sort(list);
        return list;
    }
    
    @Override
    public Strategy getStrategy(String strategyName) throws Exception {
        return strategyMap.get(strategyName);
    }
    
    @Override
    public void createStrategy(Strategy strategy) throws Exception {
        if (strategyMap.containsKey(strategy.getName())) {
            throw new Exception("Srategy [" + strategy.getName() + "] has already existed.");
        }
        strategyMap.put(strategy.getName(), strategy);
    }
    
    @Override
    public void updateStrategy(Strategy strategy) throws Exception {
        strategyMap.put(strategy.getName(), strategy);
    }
    
    @Override
    public boolean removeStrategy(String strategyName) throws Exception {
        if (strategyMap.containsKey(strategyName)) {
            // check if it has any server registered
            for (String key : new CopyOnWriteArrayList<>(strategyRuntimeMap.keySet())) {
                String name = strategyNameFromKey(key);
                if (StringUtils.equals(name, strategyName)) {
                    throw new RuntimeException("Can not remove running strategy");
                }
            }
            strategyMap.remove(strategyName);
            return true;
        }
        return false;
    }
    
    @Override
    public void clearStrategiesOfFactory(String factoryUUID) throws Exception {
        for (String key : new CopyOnWriteArrayList<>(strategyRuntimeMap.keySet())) {
            String factory = factoryUuidFromKey(key);
            if (StringUtils.equals(factoryUUID, factory)) {
                strategyRuntimeMap.remove(key);
            }
        }
    }
    
    @Override
    public boolean isFactoryAllowExecute(String factoryUUID) throws Exception {
        if (!factoryAllowExecuteMap.containsKey(factoryUUID)) {
            throw new RuntimeException("Factory doesn't exist");
        }
        return factoryAllowExecuteMap.get(factoryUUID);
    }
    
    @Override
    public void setFactoryAllowExecute(String factoryUUID, boolean allow) throws Exception {
        if (!factoryAllowExecuteMap.containsKey(factoryUUID)) {
            throw new RuntimeException("Factory doesn't exist");
        }
        factoryAllowExecuteMap.put(factoryUUID, allow);
    }
    
    @Override
    public List<String> getFactoryUuidList() throws Exception {
        List<String> list = new ArrayList<>(factoryAllowExecuteMap.keySet());
        Collections.sort(list, COMPARATOR_UUID);
        return list;
    }
    
    private String strategyNameFromKey(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        int pos = key.indexOf(SEPARATOR);
        if (pos <= 0) {
            return null;
        }
        return key.substring(0, pos);
    }
    
    private String factoryUuidFromKey(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        int pos = key.indexOf(SEPARATOR);
        if (pos <= 0) {
            return null;
        }
        return key.substring(pos + 2);
    }
    
    @Override
    public List<String> registerFactory(ScheduleFactory factory) throws Exception {
        // register to strategy
        List<String> unregistered = new ArrayList<>();
        List<String> names = getStrategyNames();
        if (!factoryAllowExecuteMap.contains(factory.getUuid())) {
            factoryAllowExecuteMap.putIfAbsent(factory.getUuid(), true);
        }
        for (String strategyName : names) {
            Strategy strategy = getStrategy(strategyName);
            boolean isFind = false;
            String key = strategy.getName() + SEPARATOR + factory.getUuid();
            if (Strategy.STS_PAUSE.equalsIgnoreCase(strategy.getSts()) == false
                    && strategy.getIPList() != null) {
                for (String ip : strategy.getIPList()) {
                    if (ip.equals("127.0.0.1") || ip.equalsIgnoreCase("localhost") || ip.equals(factory.getIp())
                            || ip.equalsIgnoreCase(factory.getHostName())) {
                        // Can be scheduled in this factory
                        if (!strategyRuntimeMap.containsKey(key)) {
                            StrategyRuntime runtime = new StrategyRuntime();
                            runtime.setStrategyName(strategyName);
                            runtime.setFactoryUuid(factory.getUuid());
                            runtime.setRequestNum(0);
                            strategyRuntimeMap.putIfAbsent(key, runtime);
                        }
                        isFind = true;
                        break;
                    }
                }
            }
            if (isFind == false) {
                if (strategyRuntimeMap.containsKey(key)) {
                    strategyRuntimeMap.remove(key);
                    unregistered.add(strategyName);
                }
            }
        }
        return unregistered;
    }
    
    @Override
    public void unregisterFactory(ScheduleFactory factory) throws Exception {
        factoryAllowExecuteMap.remove(factory.getUuid());
        List<String> names = getStrategyNames();
        for (String strategyName : names) {
            strategyRuntimeMap.remove(strategyName + SEPARATOR + factory.getUuid());
        }
    }
    
    @Override
    public List<StrategyRuntime> getRuntimesOfStrategy(String strategyName) throws Exception {
        List<String> factoryUuidList = new ArrayList<>();
        for (String key : new CopyOnWriteArrayList<>(strategyRuntimeMap.keySet())) {
            String name = strategyNameFromKey(key);
            if (StringUtils.equals(name, strategyName)) {
                factoryUuidList.add(factoryUuidFromKey(key));
            }
        }
        Collections.sort(factoryUuidList, COMPARATOR_UUID);
        List<StrategyRuntime> result = new ArrayList<>(factoryUuidList.size());
        for (String factoryUUID : factoryUuidList) {
            StrategyRuntime runtime = getStrategyRuntime(strategyName, factoryUUID);
            if (runtime != null) {
                result.add(runtime);
            }
        }
        return result;
    }
    
    @Override
    public StrategyRuntime getStrategyRuntime(String strategyName, String factoryUUID) throws Exception {
        String key = strategyName + SEPARATOR + factoryUUID;
        StrategyRuntime runtime = strategyRuntimeMap.get(key);
        if (runtime == null) {
            return null;
        }
        Preconditions.checkArgument(StringUtils.isNotEmpty(runtime.getFactoryUuid()), "Factory UUID of runtime is empty");
        Preconditions.checkArgument(StringUtils.isNotEmpty(runtime.getStrategyName()), "Strategy name of runtime is empty");
        return runtime;
    }
    
    @Override
    public void updateRuntimeOfStrategy(StrategyRuntime runtime) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotEmpty(runtime.getFactoryUuid()), "Factory UUID of runtime is empty");
        Preconditions.checkArgument(StringUtils.isNotEmpty(runtime.getStrategyName()), "Strategy name of runtime is empty");
        String key = runtime.getStrategyName() + SEPARATOR + runtime.getFactoryUuid();
        strategyRuntimeMap.put(key, runtime);
    }
}
