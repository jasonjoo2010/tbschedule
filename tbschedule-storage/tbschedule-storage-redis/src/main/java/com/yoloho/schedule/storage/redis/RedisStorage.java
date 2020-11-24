package com.yoloho.schedule.storage.redis;

import java.io.Closeable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.yoloho.enhanced.cache.config.EnableRedisTemplateConfiguration.ClientConfiguration;
import com.yoloho.enhanced.data.cache.lock.DistributedLock;
import com.yoloho.enhanced.data.cache.redis.RedisServiceImpl;
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
import com.yoloho.schedule.util.DataVersion;
import com.yoloho.schedule.util.ScheduleUtil;

import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis backend
 * <p>
 * Structure(All based on prefix): <br>
 * <pre>
 * / -> storage version
 * /sequence -> xxxx
 * /task -> {"a" => {}, "b" => {}}
 * /task/a -> ["a", "a$test"]
 * /task/a/a/servers/version -> 1
 * /task/a/a/servers -> {uuid1 => {}, uuid2 => {}}
 * /task/a/a/items/initializer
 * /task/a/a/items
 * ...
 * /strategy -> {"a" => {}, "b" => {}}
 * /strategy/a/runtime -> {uuid1 => {}, uuid2 => {}}              [recyclable]
 * /factory -> {uuid1 => {run: true, heartbeat: 123332222}}       [recyclable]
 * </pre>
 * 
 * @author jason
 *
 */
public class RedisStorage implements IStorage {
    private static final Logger logger = LoggerFactory.getLogger(RedisStorage.class.getSimpleName());
    
    private boolean initialized = false;
    private DistributedLock<String> lock = null;
    private StringRedisTemplate redisTemplate;
    private RedisServiceImpl redisService;
    private String prefixKey = "schedule:redis:";
    
    private Closeable lockStrategyRuntime(String strategyName) throws TimeoutException, InterruptedException {
        String lockKey = keyStrategyRuntime(strategyName);
        return lock.lock(lockKey, 2, TimeUnit.SECONDS);
    }
    
    private Closeable lockFactory() throws TimeoutException, InterruptedException {
        String lockKey = keyFactory();
        return lock.lock(lockKey, 2, TimeUnit.SECONDS);
    }
    
    private Closeable lockTask(String taskName) throws TimeoutException, InterruptedException {
        String lockKey = keyRunningEntries(taskName);
        return lock.lock(lockKey, 2, TimeUnit.SECONDS);
    }
    
    private void initRedis(ScheduleConfig config) {
        // pool
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(3);
        poolConfig.setMaxTotal(10);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTimeBetweenEvictionRunsMillis(60000);
        poolConfig.setMinEvictableIdleTimeMillis(120000);
        // client
        ClientConfiguration clientConfiguration = new ClientConfiguration(poolConfig, 5000, 10000);
        String addr = config.getAddress();
        Preconditions.checkArgument(StringUtils.isNotEmpty(addr), "Redis server address can not be empty.");
        int pos = addr.indexOf(':');
        int port = 6379;
        String host = addr;
        if (pos > 0) {
            host = addr.substring(0, pos);
            port = NumberUtils.toInt(addr.substring(pos + 1));
        }
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(host, port);
        if (StringUtils.isNotEmpty(config.getPassword())) {
            redisStandaloneConfiguration.setPassword(config.getPassword());
        }
        JedisConnectionFactory connectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration, clientConfiguration);
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisService = new RedisServiceImpl();
        redisService.setRedisTemplate(redisTemplate);
        prefixKey = "schedule:redis:" + config.getRootPath() + ":";
        lock = new DistributedLock<>(redisService, prefixKey + "/lock", 5);
    }
    
    private void checkStorage() throws Exception {
        // set version
        String key = key("/");
        String storeVersion = redisService.get(key);
        if (StringUtils.isNotEmpty(storeVersion)) {
            if (!DataVersion.isCompatible(storeVersion)) {
                throw new Exception("Current Version " + DataVersion.getCurrentVersion()
                        + " don't be compatible with the version " + storeVersion + " in storage");
            }
            logger.info(
                    "Current Data Structure Version: " + DataVersion.getCurrentVersion() + " Data in Storage: " + storeVersion);
        } else {
            redisService.set(key, DataVersion.getCurrentVersion());
        }
    }

    @Override
    public boolean init(final ScheduleConfig config, final OnConnected onConnected) {
        synchronized (this) {
            if (initialized) {
                return true;
            }
            initRedis(config);
            try {
                checkStorage();
            } catch (Exception e) {
                logger.error("Initializing redis storage structure failed", e);
                return false;
            }
            initialized = true;
            onConnected.connected(this);
        }
        logger.info("Redis storage initialized");
        return true;
    }
    
    @Override
    public void shutdown() {
        synchronized (this) {
            // Seem nothing needs to be done?
        }
    }
    
    private String key(String key) {
        return prefixKey + key;
    }
    
    private String keyFactory() {
        return key("/factory");
    }
    
    private String keyStrategy() {
        return key("/strategy");
    }
    
    private String keyTask() {
        return key("/task");
    }
    
    private String keyRunningEntries(String taskName) {
        return keyTask() + "/" + taskName;
    }
    
    private String keyServerVersion(String taskName, String ownSign) {
        return keyTask() + "/" + taskName + "/" + ScheduleUtil.runningEntryFromTaskName(taskName, ownSign)
                + "/servers/version";
    }
    
    private String keyServers(String taskName, String ownSign) {
        return keyTask() + "/" + taskName + "/" + ScheduleUtil.runningEntryFromTaskName(taskName, ownSign)
                + "/servers";
    }
    
    private String keyTaskItemsInitializer(String taskName, String ownSign) {
        return keyTask() + "/" + taskName + "/" + ScheduleUtil.runningEntryFromTaskName(taskName, ownSign)
                + "/items/initializer";
    }
    
    private String keyTaskItems(String taskName, String ownSign) {
        return keyTask() + "/" + taskName + "/" + ScheduleUtil.runningEntryFromTaskName(taskName, ownSign)
                + "/items";
    }
    
    private String keyStrategyRuntime(String strategyName) {
        return key("/strategy/" + strategyName + "/runtime");
    }
    
    @Override
    public String getName() {
        return "redis";
    }
    
    @Override
    public long getSequenceNumber() throws Exception {
        return redisService.increaseAndGet(key("/sequence"), 1);
    }
    
    /**
     * Fetch the global time based on zookeeper server
     * 
     * @return
     */
    @Override
    public long getGlobalTime() {
        return redisTemplate.execute(new RedisCallback<Long>() {

            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.time();
            }
        });
    }
    
    @Override
    public boolean test() {
        return initialized && getGlobalTime() > 0;
    }
    
    @Override
    public String dump() throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("/: ").append(redisService.get(key("/")));
        builder.append("\n/sequence: ").append(redisService.get(key("/sequence")));
        // task
        Map<String, String> taskMap = redisService.hashGetAll(keyTask());
        for (Entry<String, String> entry : taskMap.entrySet()) {
            String taskName = entry.getKey();
            builder.append("\n/task/").append(taskName).append(": ").append(entry.getValue());
            // running entries
            List<String> runningEntryNameList = getRunningEntryList(taskName);
            for (String runningEntry : runningEntryNameList) {
                String ownSign = ScheduleUtil.ownsignFromRunningEntry(runningEntry);
                // servers
                builder.append("\n/task/").append(taskName).append("/").append(runningEntry)
                        .append("/servers/version: ").append(getServerSchedulingVersion(taskName, ownSign));
                List<String> serverUuidList = getServerUuidList(taskName, ownSign);
                for (String serverUUID : serverUuidList) {
                    builder.append("\n/task/").append(taskName).append("/").append(runningEntry).append("/servers/")
                            .append(serverUUID).append(": ").append(JSON.toJSONString(getServer(taskName, ownSign, serverUUID)));
                }
                // task items
                builder.append("\n/task/").append(taskName).append("/").append(runningEntry)
                        .append("/items/initializer: ")
                        .append(JSON.toJSONString(getInitialRunningInfoResult(taskName, ownSign)));
                List<TaskItemRuntime> items = getTaskItems(taskName, ownSign);
                for (TaskItemRuntime item : items) {
                    builder.append("\n/task/").append(taskName).append("/").append(runningEntry).append("/items/")
                            .append(item.getTaskItem()).append(": ")
                            .append(JSON.toJSONString(item));
                }
            }
        }
        // strategy
        Map<String, String> strategyMap = redisService.hashGetAll(keyStrategy());
        for (Entry<String, String> entry : strategyMap.entrySet()) {
            builder.append("\n/strategy/").append(entry.getKey()).append(": ").append(entry.getValue());
            // strategy runtime
            Map<String, String> strategyRuntimeMap = redisService.hashGetAll(keyStrategyRuntime(entry.getKey()));
            for (Entry<String, String> runtimeEntry : strategyRuntimeMap.entrySet()) {
                builder.append("\n/strategy/")
                    .append(entry.getKey())
                    .append("/runtime/")
                    .append(runtimeEntry.getKey())
                    .append(": ")
                    .append(runtimeEntry.getValue());
            }
        }
        // factory
        Map<String, String> factoryMap = redisService.hashGetAll(keyFactory());
        for (Entry<String, String> entry : factoryMap.entrySet()) {
            builder.append("\n/factory/").append(entry.getKey()).append(": ").append(entry.getValue());
        }
        return builder.toString();
    }
    /////////////
    
    @Override
    public void createTask(Task task) throws Exception {
        String key = keyTask();
        lock.lock(key, 2, TimeUnit.SECONDS);
        try {
            if (redisService.hashExists(key, task.getName())) {
                throw new Exception("Task [" + task.getName() + "] has already existed.");
            }
            redisService.hashPutIfAbsent(key, task.getName(), task);
        } finally {
            lock.unlock(key);
        }
    }

    @Override
    public void updateTask(Task task) throws Exception {
        String key = keyTask();
        lock.lock(key, 2, TimeUnit.SECONDS);
        try {
            redisService.hashPut(key, task.getName(), task);
        } finally {
            lock.unlock(key);
        }
    }
    
    @Override
    public boolean removeTask(String taskName) throws Exception {
        String key = keyTask();
        lock.lock(key, 2, TimeUnit.SECONDS);
        try {
            if (redisService.hashExists(key, taskName)) {
                redisService.hashRemove(key, taskName);
                return true;
            }
            return false;
        } finally {
            lock.unlock(key);
        }
    }
    
    @Override
    public void removeRunningEntry(String taskName, String ownSign) throws Exception {
        try (Closeable c = lockTask(taskName)) {
            List<String> keys = new ArrayList<>(4);
            keys.add(keyServerVersion(taskName, ownSign));
            keys.add(keyServers(taskName, ownSign));
            keys.add(keyTaskItems(taskName, ownSign));
            keys.add(keyTaskItemsInitializer(taskName, ownSign));
            redisService.delete(keys);
            redisService.unsortedSetRemove(keyRunningEntries(taskName),
                    ScheduleUtil.runningEntryFromTaskName(taskName, ownSign));
        }
    }
    
    @Override
    public Task getTask(String taskName) throws Exception {
        String key = keyTask();
        return redisService.hashGet(key, taskName, Task.class);
    }
    
    @Override
    public List<String> getTaskNames() throws Exception {
        List<String> list = new ArrayList<>(redisService.hashKeys(keyTask()));
        Collections.sort(list);
        return list;
    }
    
    @Override
    public void emptyTaskItems(String taskName, String ownSign) throws Exception {
        String key = keyTaskItems(taskName, ownSign);
        try (Closeable c = lockTask(taskName)) {
            redisService.delete(key);
        }
        try (Closeable c = lockTask(taskName)) {
            redisService.unsortedSetAdd(keyRunningEntries(taskName),
                    ScheduleUtil.runningEntryFromTaskName(taskName, ownSign));
        }
    }
    
    @Override
    public InitialResult getInitialRunningInfoResult(String taskName, String ownSign) throws Exception {
        return redisService.get(keyTaskItemsInitializer(taskName, ownSign), InitialResult.class);
    }
    
    @Override
    public void updateTaskItemsInitialResult(String taskName, String ownSign, 
            String initializerUuid) throws Exception {
        try (Closeable c = lockTask(taskName)) {
            InitialResult result = getInitialRunningInfoResult(taskName, ownSign);
            if (result == null) {
                result = new InitialResult();
            }
            result.setVersion(result.getVersion() + 1);
            result.setUpdateTime(getGlobalTime());
            result.setUuid(initializerUuid);
            redisService.set(keyTaskItemsInitializer(taskName, ownSign), result);
        }
    }
    
    @Override
    public void initTaskItems(String taskName, String ownSign) throws Exception {
        try (Closeable c = lockTask(taskName)) {
            Task task = getTask(taskName);
            if (task != null) {
                // init task items
                TaskItem[] items = task.getTaskItemList();
                String key = keyTaskItems(taskName, ownSign);
                Map<String, TaskItemRuntime> map = new HashMap<>(items.length);
                for (TaskItem define : items) {
                    // init other properties
                    TaskItemRuntime item = new TaskItemRuntime();
                    item.setTaskName(taskName);
                    item.setOwnSign(ownSign);
                    item.setDealParameter(define.getParameter());
                    item.setCurrentScheduleServer("");
                    item.setRequestScheduleServer("");
                    item.setTaskItem(define.getTaskItemId());
                    map.put(item.getTaskItem(), item);
                }
                redisService.hashPutAll(key, map);
            } else {
                logger.warn("Try to initial a non-existed task's item");
            }
        }
    }
    
    @Override
    public List<String> getRunningEntryList(String taskName) throws Exception {
        String key = keyRunningEntries(taskName);
        List<String> result = new ArrayList<>(redisService.unsortedSetGetAll(key));
        if (!result.isEmpty()) {
            Collections.sort(result);
        }
        return result;
    }
    
    @Override
    public List<TaskItemRuntime> getTaskItems(String taskName, String ownSign) throws Exception {
        List<TaskItemRuntime> items = redisService.hashValues(keyTaskItems(taskName, ownSign), TaskItemRuntime.class);
        List<TaskItemRuntime> result = new ArrayList<>(items);
        if (!result.isEmpty()) {
            Collections.sort(result, COMPARATOR_TASK_ITEM_RUNTIME);
        }
        return result;
    }
    
    @Override
    public int releaseTaskItemByOwner(String taskName, String ownSign, String ownerUuid) throws Exception {
        try (Closeable c = lockTask(taskName)) {
            String key = keyTaskItems(taskName, ownSign);
            Map<String, TaskItemRuntime> items = redisService.hashGetAll(key, TaskItemRuntime.class);
            int released = 0;
            if (items == null || items.isEmpty()) {
                return released;
            }
            for (Entry<String, TaskItemRuntime> entry : items.entrySet()) {
                TaskItemRuntime item = entry.getValue();
                if (StringUtils.isNotEmpty(item.getCurrentScheduleServer()) && StringUtils.isNotEmpty(item.getRequestScheduleServer())) {
                    if (StringUtils.equals(ownerUuid, item.getCurrentScheduleServer())) {
                        // release
                        item.setCurrentScheduleServer(item.getRequestScheduleServer());
                        item.setRequestScheduleServer("");
                        redisService.hashPut(key, item.getTaskItem(), item);
                        released ++;
                    }
                }
            }
            return released;
        }
    }
    
    @Override
    public void updateTaskItemRequestServer(String taskName, String ownSign, String taskItem, String server) throws Exception {
        try (Closeable c = lockTask(taskName)) {
            String key = keyTaskItems(taskName, ownSign);
            TaskItemRuntime item = redisService.hashGet(key, taskItem, TaskItemRuntime.class);
            if (item == null) {
                logger.warn("Lack of runtime of taskItem, initializing");
                // should not happen
                item = new TaskItemRuntime();
                item.setTaskName(taskName);
                item.setOwnSign(ownSign);
                item.setDealParameter("");
                item.setCurrentScheduleServer("");
                item.setRequestScheduleServer("");
                item.setTaskItem(taskItem);
            }
            item.setRequestScheduleServer(server);
            redisService.hashPut(key, taskItem, item);
        }
    }
    
    @Override
    public void updateTaskItemCurrentServer(String taskName, String ownSign, String taskItem, String server) throws Exception {
        try (Closeable c = lockTask(taskName)) {
            String key = keyTaskItems(taskName, ownSign);
            TaskItemRuntime item = redisService.hashGet(key, taskItem, TaskItemRuntime.class);
            if (item == null) {
                logger.warn("Lack of runtime of taskItem, initializing");
                // should not happen
                item = new TaskItemRuntime();
                item.setTaskName(taskName);
                item.setOwnSign(ownSign);
                item.setDealParameter("");
                item.setCurrentScheduleServer("");
                item.setRequestScheduleServer("");
                item.setTaskItem(taskItem);
            }
            item.setCurrentScheduleServer(server);
            redisService.hashPut(key, taskItem, item);
        }
    }
    
    @Override
    public long getServerSchedulingVersion(String taskName, String ownSign) throws Exception {
        String key = keyServerVersion(taskName, ownSign);
        Long version = redisService.get(key, Long.class);
        if (version == null) {
            return 0;
        }
        return version;
    }
    
    @Override
    public void increaseServerSchedulingVersion(String taskName, String ownSign) throws Exception {
        String key = keyServerVersion(taskName, ownSign);
        redisService.increaseAndGet(key);
    }
    
    @Override
    public List<String> getServerUuidList(String taskName, String ownSign) throws Exception {
        String key = keyServers(taskName, ownSign);
        Set<String> uuids = redisService.hashKeys(key);
        if (uuids.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>(uuids);
        Collections.sort(list, COMPARATOR_UUID);
        return list;
    }
    
    @Override
    public void removeServer(String taskName, String ownSign, String serverUuid) throws Exception {
        String key = keyServers(taskName, ownSign);
        try (Closeable c = lockTask(taskName)) {
            if (redisService.hashExists(key, serverUuid)) {
                redisService.hashRemove(key, serverUuid);
            }
        }
    }
    
    @Override
    public boolean updateServer(ScheduleServer server) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotEmpty(server.getUuid()));
        String key = keyServers(server.getTaskName(), server.getOwnSign());
        try (Closeable c = lockTask(server.getTaskName())) {
            if (!redisService.hashExists(key, server.getUuid())) {
                return false;
            }
            redisService.hashPut(key, server.getUuid(), server);
            return true;
        }
    }
    
    @Override
    public void createServer(ScheduleServer server) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotEmpty(server.getUuid()));
        String key = keyServers(server.getTaskName(), server.getOwnSign());
        try (Closeable c = lockTask(server.getTaskName())) {
            if (redisService.hashExists(key, server.getUuid())) {
                throw new Exception(server.getUuid() + " duplicated");
            }
            Timestamp heartBeatTime = new Timestamp(getGlobalTime());
            server.setHeartBeatTime(heartBeatTime);
            redisService.hashPut(key, server.getUuid(), server);
        }
    }
    
    @Override
    public ScheduleServer getServer(String taskName, String ownSign, String serverUuid) throws Exception {
        String key = keyServers(taskName, ownSign);
        return redisService.hashGet(key, serverUuid, ScheduleServer.class);
    }
    
    @Override
    public List<String> getStrategyNames() throws Exception {
        Set<String> keys = redisService.hashKeys(keyStrategy());
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>(keys);
        Collections.sort(list);
        return list;
    }
    
    @Override
    public Strategy getStrategy(String strategyName) throws Exception {
        String key = keyStrategy();
        return redisService.hashGet(key, strategyName, Strategy.class);
    }
    
    @Override
    public void createStrategy(Strategy strategy) throws Exception {
        String key = keyStrategy();
        lock.lock(key, 2, TimeUnit.SECONDS);
        try {
            if (redisService.hashExists(key, strategy.getName())) {
                throw new Exception("Srategy [" + strategy.getName() + "] has already existed.");
            }
            redisService.hashPut(key, strategy.getName(), strategy);
        } finally {
            lock.unlock(key);
        }
    }
    
    @Override
    public void updateStrategy(Strategy strategy) throws Exception {
        String key = keyStrategy();
        lock.lock(key, 2, TimeUnit.SECONDS);
        try {
            redisService.hashPut(key, strategy.getName(), strategy);
        } finally {
            lock.unlock(key);
        }
    }
    
    @Override
    public boolean removeStrategy(String strategyName) throws Exception {
        String key = keyStrategy();
        lock.lock(key, 2, TimeUnit.SECONDS);
        try {
            if (!redisService.hashExists(key, strategyName)) {
                return false;
            }
            String runtimeKey = keyStrategyRuntime(strategyName);
            // check if it has any server registered
            if (redisService.hashSize(runtimeKey) > 0) {
                throw new RuntimeException("Can not remove running strategy");
            }
            redisService.hashRemove(key, strategyName);
            return true;
        } finally {
            lock.unlock(key);
        }
    }
    
    @Override
    public void clearStrategiesOfFactory(String factoryUUID) throws Exception {
        List<String> names = getStrategyNames();
        for (String strategyName : names) {
            try (Closeable c = lockStrategyRuntime(strategyName)) {
                String key = keyStrategyRuntime(strategyName);
                redisService.hashRemove(key, factoryUUID);
            }
        }
    }
    
    @Override
    public boolean isFactoryAllowExecute(String factoryUUID) throws Exception {
        String factoryKey = keyFactory();
        if (!redisService.hashExists(factoryKey, factoryUUID)) {
            throw new RuntimeException("Factory doesn't exist");
        }
        FactoryInfo info = redisService.hashGet(factoryKey, factoryUUID, FactoryInfo.class);
        return info == null ? true : info.isAllow();
    }
    
    @Override
    public void setFactoryAllowExecute(String factoryUUID, boolean allow) throws Exception {
        String factoryKey = keyFactory();
        lock.lock(factoryKey, 2, TimeUnit.SECONDS);
        try {
            if (!redisService.hashExists(factoryKey, factoryUUID)) {
                throw new RuntimeException("Factory doesn't exist");
            }
            FactoryInfo info = redisService.hashGet(factoryKey, factoryUUID, FactoryInfo.class);
            if (info == null) {
                info = new FactoryInfo();
                info.setHeartbeat(getGlobalTime());
            }
            info.setAllow(allow);
            redisService.hashPut(factoryKey, factoryUUID, info);
        } finally {
            lock.unlock(factoryKey);
        }
    }
    
    @Override
    public List<String> getFactoryUuidList() throws Exception {
        Set<String> keys = redisService.hashKeys(keyFactory());
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>(keys);
        Collections.sort(list, COMPARATOR_UUID);
        return list;
    }
    
    private void cleanExpiredFactory() throws Exception {
        List<String> factoryExpired = new ArrayList<>();
        String key = keyFactory();
        Map<String, FactoryInfo> map = redisService.hashGetAll(key, FactoryInfo.class);
        long now = getGlobalTime();
        for (Entry<String, FactoryInfo> entry : map.entrySet()) {
            // 30 seconds ttl
            long diff = now - entry.getValue().getHeartbeat();
            if (diff > 30000) {
                // expired
                factoryExpired.add(entry.getKey());
            }
        }
        if (factoryExpired.isEmpty()) {
            return;
        }
        List<String> strategyNameList = getStrategyNames();
        try (Closeable c = lockFactory()) {
            // clean
            for (String uuid : factoryExpired) {
                redisService.hashRemove(key, uuid);
                for (String strategyName : strategyNameList) {
                    redisService.hashRemove(keyStrategyRuntime(strategyName), uuid);
                }
            }
        }
        logger.info("Clean {} expired factories", factoryExpired.size());
    }
    
    @Override
    public List<String> registerFactory(ScheduleFactory factory) throws Exception {
        List<String> unregistered = new ArrayList<>();
        String factoryKey = keyFactory();
        // heartbeat
        FactoryInfo info = null;
        try (Closeable c = lockFactory()) {
            info = redisService.hashGet(factoryKey, factory.getUuid(), FactoryInfo.class);
            if (info == null) {
                info = new FactoryInfo();
                info.setAllow(true);
            }
            info.setHeartbeat(getGlobalTime());
            redisService.hashPut(factoryKey, factory.getUuid(), info);
        }
        cleanExpiredFactory();
        // register to strategy
        List<String> names = getStrategyNames();
        for (String strategyName : names) {
            try (Closeable c = lockStrategyRuntime(strategyName)) {
                Strategy strategy = getStrategy(strategyName);
                boolean isFind = false;
                String key = keyStrategyRuntime(strategy.getName());
                if (Strategy.STS_PAUSE.equalsIgnoreCase(strategy.getSts()) == false
                        && strategy.getIPList() != null) {
                    for (String ip : strategy.getIPList()) {
                        if (ip.equals("127.0.0.1") || ip.equalsIgnoreCase("localhost") || ip.equals(factory.getIp())
                                || ip.equalsIgnoreCase(factory.getHostName())) {
                            // Can be scheduled in this factory
                            if (getStrategyRuntime(strategy.getName(), factory.getUuid()) == null) {
                                StrategyRuntime runtime = new StrategyRuntime();
                                runtime.setStrategyName(strategyName);
                                runtime.setFactoryUuid(factory.getUuid());
                                runtime.setRequestNum(0);
                                updateRuntimeOfStrategy(runtime);
                            }
                            isFind = true;
                            break;
                        }
                    }
                }
                if (isFind == false) {
                    if (getStrategyRuntime(strategy.getName(), factory.getUuid()) != null) {
                        redisService.hashRemove(key, factory.getUuid());
                        unregistered.add(strategyName);
                    }
                }
            }
        }
        return unregistered;
    }
    
    @Override
    public void unregisterFactory(ScheduleFactory factory) throws Exception {
        List<String> names = getStrategyNames();
        for (String strategyName : names) {
            try (Closeable c = lockStrategyRuntime(strategyName)) {
                redisService.hashRemove(keyStrategyRuntime(strategyName), factory.getUuid());
            }
        }
        try (Closeable c = lockFactory()) {
            redisService.hashRemove(keyFactory(), factory.getUuid());
        }
    }
    
    @Override
    public List<StrategyRuntime> getRuntimesOfStrategy(String strategyName) throws Exception {
        String key = keyStrategyRuntime(strategyName);
        Map<String, StrategyRuntime> map = redisService.hashGetAll(key, StrategyRuntime.class);
        if (map == null || map.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> factoryUuidList = new ArrayList<>(map.keySet());
        Collections.sort(factoryUuidList, COMPARATOR_UUID);
        List<StrategyRuntime> result = new ArrayList<>(factoryUuidList.size());
        for (String factoryUUID : factoryUuidList) {
            StrategyRuntime runtime = map.get(factoryUUID);
            if (runtime != null) {
                result.add(runtime);
            }
        }
        return result;
    }
    
    @Override
    public StrategyRuntime getStrategyRuntime(String strategyName, String factoryUUID) throws Exception {
        String key = keyStrategyRuntime(strategyName);
        StrategyRuntime runtime = redisService.hashGet(key, factoryUUID, StrategyRuntime.class);
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
        String key = keyStrategyRuntime(runtime.getStrategyName());
        redisService.hashPut(key, runtime.getFactoryUuid(), runtime);
    }
}
