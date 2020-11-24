package com.yoloho.schedule.storage.jdbc;

import java.io.Closeable;
import java.io.Serializable;
import java.net.URI;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.yoloho.enhanced.common.util.HttpClientUtil;
import com.yoloho.enhanced.data.cache.lock.DistributedLock;
import com.yoloho.enhanced.data.dao.api.EnhancedDao;
import com.yoloho.enhanced.data.dao.api.UpdateEntry;
import com.yoloho.enhanced.data.dao.api.filter.DynamicQueryFilter;
import com.yoloho.enhanced.data.dao.api.filter.QueryData;
import com.yoloho.enhanced.data.dao.impl.EnhancedDaoImpl;
import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.interfaces.ScheduleFactory;
import com.yoloho.schedule.storage.jdbc.model.StorageInfo;
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

/**
 * Jdbc(test against mysql) backend
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
public class JdbcStorage implements IStorage {
    private static final Logger logger = LoggerFactory.getLogger(JdbcStorage.class.getSimpleName());
    
    private boolean initialized = false;
    private DistributedLock<String> lock = null;
    private String prefixKey = "default::";
    
    private DruidDataSource dataSource = null;
    private SqlSessionFactory sqlSessionFactory = null;
    
    // dao
    private EnhancedDao<StorageInfo, Long> storageInfoDao = null;

    
    private Closeable lockStrategyRuntime(String strategyName) throws TimeoutException, InterruptedException {
        String lockKey = keyStrategyRuntimePrefix(strategyName);
        return lock.lock(lockKey, 2, TimeUnit.SECONDS);
    }
    
    private Closeable lockFactories() throws TimeoutException, InterruptedException {
        String lockKey = keyFactoryPrefix();
        return lock.lock(lockKey, 2, TimeUnit.SECONDS);
    }
    
    private Closeable lockTask(String taskName) throws TimeoutException, InterruptedException {
        String lockKey = keyTask(taskName);
        return lock.lock(lockKey, 2, TimeUnit.SECONDS);
    }
    
    private void initDataSource(ScheduleConfig config) throws SQLException {
        dataSource = new DruidDataSource();
        dataSource.setUrl(config.getAddress());
        dataSource.setUsername(config.getUsername());
        dataSource.setPassword(config.getPassword());
        dataSource.setInitialSize(1);
        dataSource.setMinIdle(1);
        dataSource.setMaxActive(5);
        dataSource.setMaxWait(30000);
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(300000);
        dataSource.setValidationQuery("select 'x'");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        dataSource.setKeepAlive(true);
        dataSource.setPoolPreparedStatements(false);
        dataSource.setConnectionInitSqls(Arrays.asList("set names 'utf8'"));
        dataSource.init();
    }
    
    private void initSqlSessionFactory(ScheduleConfig config) throws Exception {
        initDataSource(config);
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new Resource[] {
            new ClassPathResource("com/yoloho/enhanced/data/dao/xml/enhanced-dao-generic.xml")
        });
        sqlSessionFactory = factoryBean.getObject();
    }
    
    private <T, PK extends Serializable> EnhancedDao<T, PK> createDao(
            Class<T> cls, String tableName) {
        try {
            if (StringUtils.isEmpty(tableName)) {
                tableName = "schedule_info";
            }
            EnhancedDaoImpl<T, PK> dao = new EnhancedDaoImpl<>(cls.getName(), tableName);
            dao.setSqlSessionFactory(sqlSessionFactory);
            return dao;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    private void initPersistent(ScheduleConfig config) throws Exception {
        if (StringUtils.isEmpty(config.getAddress()) 
                || !config.getAddress().toLowerCase().startsWith("jdbc:")) {
            throw new RuntimeException("Connection string must begin with \"jdbc:\"");
        }
        
        // init jdbc
        initSqlSessionFactory(config);
        URI uri = new URI(config.getAddress().substring(5));
        Map<String, String> params = HttpClientUtil.splitKeyValuePairString(uri.getQuery());
        
        // Dao
        storageInfoDao = createDao(StorageInfo.class, params.get("sched_tbl"));
        if (storageInfoDao == null) {
            throw new RuntimeException("Create DAO object failed during tbschedule initializing.");
        }
        
        // lock
        prefixKey = config.getRootPath() + "::";
        lock = new DistributedLock<>(new JdbcLock(storageInfoDao), prefixKey + "lock", 5);
    }
    
    private String getVersion() {
        StorageInfo bean = storageInfoDao.get("key", key("version"));
        return bean == null ? null : bean.getValue();
    }
    
    private void updateVersion(String version) {
        StorageInfo bean = new StorageInfo();
        bean.setKey(key("version"));
        bean.setValue(version);
        storageInfoDao.replace(bean);
    }
    
    private void checkStorage() throws Exception {
        // set version
        String storeVersion = getVersion();
        if (StringUtils.isNotEmpty(storeVersion)) {
            if (!DataVersion.isCompatible(storeVersion)) {
                throw new Exception("Current Version " + DataVersion.getCurrentVersion()
                        + " don't be compatible with the version " + storeVersion + " in storage");
            }
            logger.info(
                    "Current Data Structure Version: " + DataVersion.getCurrentVersion() + " Data in Storage: " + storeVersion);
        } else {
            updateVersion(DataVersion.getCurrentVersion());
        }
    }

    @Override
    public boolean init(final ScheduleConfig config, final OnConnected onConnected) {
        synchronized (this) {
            if (initialized) {
                return true;
            }
            try {
                initPersistent(config);
                checkStorage();
            } catch (Exception e) {
                logger.error("Initializing JDBC storage structure failed", e);
                return false;
            }
            initialized = true;
            onConnected.connected(this);
        }
        logger.info("JDBC storage initialized");
        return true;
    }
    
    @Override
    public void shutdown() {
        synchronized (this) {
            dataSource.close();
        }
    }
    
    private <T> T getObjectFromStorage(String key, Class<T> cls) {
        StorageInfo info = storageInfoDao.get("key", key);
        if (info == null) {
            return null;
        }
        try {
            return JSON.parseObject(info.getValue(), cls);
        } catch (Exception e) {
            return null;
        }
    }
    
    private <T> List<T> getObjectsFromStorage(String prefix, Class<T> cls) {
        List<StorageInfo> infoList = storageInfoDao.find(new DynamicQueryFilter()
                .startsWith("key", prefix)
                .limit(1000)
                .getQueryData());
        if (infoList.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(infoList.size());
        for (StorageInfo info : infoList) {
            try {
                result.add(JSON.parseObject(info.getValue(), cls));
            } catch (Exception e) {
                logger.warn("Deserialize object error: {}", info.getKey());
            }
        }
        return result;
    }
    
    private <T> void putObjectToStorage(String key, T obj) {
        StorageInfo info = storageInfoDao.get("key", key);
        if (info == null) {
            info = new StorageInfo();
            info.setKey(key);
        }
        info.setValue(JSON.toJSONString(obj));
        if (info.getId() > 0) {
            storageInfoDao.update(info);
        } else {
            storageInfoDao.insert(info);
        }
    }
    
    private boolean removeObjectFromStorage(String key) {
        return storageInfoDao.remove(new DynamicQueryFilter()
                .equalPair("key", key)
                .getQueryData()) > 0;
    }
    
    private String key(String key) {
        return prefixKey + key;
    }
    
    private String keyFactoryPrefix() {
        return key("factory/");
    }
    
    private String keyFactory(String name) {
        return keyFactoryPrefix() + name;
    }
    
    private String keyStrategy(String name) {
        return keyStrategyPrefix() + name;
    }
    
    private String keyStrategyPrefix() {
        return key("strategy/");
    }
    
    private String keyTask(String name) {
        return keyTaskPrefix() + name;
    }
    
    private String keyTaskPrefix() {
        return key("task/");
    }
    
    private String keyRunningEntriesBase(String taskName) {
        return key("taskitems/") + taskName + "/";
    }
    
    private String keyTaskItemsBase(String taskName, String ownSign) {
        return keyRunningEntriesBase(taskName)
                + ScheduleUtil.runningEntryFromTaskName(taskName, ownSign) + "/";
    }
    
    private String keyStrategyRuntimePrefix(String strategyName) {
        return key("runtime/" + strategyName + "/");
    }
    
    private String keyStrategyRuntime(String strategyName, String uuid) {
        return keyStrategyRuntimePrefix(strategyName) + uuid;
    }
    
    private String keyServerVersion(String taskName, String ownSign) {
        return keyTaskItemsBase(taskName, ownSign) + "version";
    }
    
    private String keyServersPrefix(String taskName, String ownSign) {
        return keyTaskItemsBase(taskName, ownSign) + "servers/";
    }
    
    private String keyServer(String taskName, String ownSign, String serverUUID) {
        return keyServersPrefix(taskName, ownSign) + serverUUID;
    }
    
    private String keyTaskItemsInitializer(String taskName, String ownSign) {
        return keyTaskItemsBase(taskName, ownSign) + "initializer";
    }
    
    private String keyTaskItemPrefix(String taskName, String ownSign) {
        return keyTaskItemsBase(taskName, ownSign) + "items/";
    }
    
    private String keyTaskItem(String taskName, String ownSign, String itemId) {
        return keyTaskItemPrefix(taskName, ownSign) + itemId;
    }
    
    @Override
    public String getName() {
        return "jdbc";
    }
    
    @Override
    public long getSequenceNumber() throws Exception {
        String key = key("global-sequence");
        Map<String, UpdateEntry> data = new HashMap<>(1);
        long nano = System.nanoTime();
        while (true) {
            StorageInfo bean = storageInfoDao.get("key", key);
            if (bean == null) {
                // try to insert and retry
                bean = new StorageInfo();
                bean.setKey(key);
                bean.setValue("1|" + nano);
                storageInfoDao.insert(bean, true);
                continue;
            }
            long seq = 0;
            String oldValue = bean.getValue();
            if (StringUtils.contains(oldValue, '|')) {
                seq = NumberUtils.toLong(oldValue.substring(0, oldValue.indexOf('|')));
            }
            seq ++;
            String newValue = seq + "|" + nano;
            data.put("value", new UpdateEntry(newValue));
            if (storageInfoDao.update(data, new DynamicQueryFilter()
                    .equalPair("key", key)
                    .equalPair("value", oldValue)
                    .getQueryData()) > 0) {
                // succ
                return seq;
            }
        }
    }
    
    /**
     * Fetch the global time based on zookeeper server
     * 
     * @return
     */
    @Override
    public long getGlobalTime() {
        FastDateFormat fastDateFormat = FastDateFormat.getInstance("yyyyMMddHHmmss.SSS");
        List<Map<String, Object>> list = storageInfoDao.find("1 as id, 'version' as `key`, current_timestamp(3) + 0 as value, 1 as `expire`", new DynamicQueryFilter()
                .getQueryData());
        if (list.isEmpty()) {
            return System.currentTimeMillis();
        }
        try {
            return fastDateFormat.parse(String.valueOf(list.get(0).get("value"))).getTime();
        } catch (ParseException e) {
            logger.warn("Time convertion failed: {}", list.get(0).get("value"));
        }
        return System.currentTimeMillis();
    }
    
    @Override
    public boolean test() {
        return initialized && getGlobalTime() > 0;
    }
    
    @Override
    public String dump() throws Exception {
        StringBuilder builder = new StringBuilder();
        DynamicQueryFilter filter = new DynamicQueryFilter()
                .startsWith("key", key(""))
                .orderBy("key", false)
                .limit(30);
        String lastKey = null;
        StorageInfo info;
        while (true) {
            if (StringUtils.isNotEmpty(lastKey)) {
                filter.greaterThan("key", lastKey);
            }
            List<StorageInfo> list = storageInfoDao.find(filter.getQueryData());
            if (list.isEmpty()) {
                break;
            }
            lastKey = list.get(list.size() - 1).getKey();
            for (int i = 0; i < list.size(); i++) {
                info = list.get(i);
                if (builder.length() > 0) builder.append("\n");
                builder.append(info.getKey()).append(": ").append(info.getValue());
            }
        }
        return builder.toString();
    }
    
    @Override
    public void createTask(Task task) throws Exception {
        String key = keyTask(task.getName());
        try (Closeable c = lock.lock(key, 2, TimeUnit.SECONDS)) {
            if (getTask(task.getName()) != null) {
                throw new Exception("Task [" + task.getName() + "] has already existed.");
            }
            putObjectToStorage(key, task);
        }
    }
    
    @Override
    public Task getTask(String taskName) throws Exception {
        String key = keyTask(taskName);
        return getObjectFromStorage(key, Task.class);
    }

    @Override
    public void updateTask(Task task) throws Exception {
        String key = keyTask(task.getName());
        try (Closeable c = lock.lock(key, 2, TimeUnit.SECONDS)) {
            if (getTask(task.getName()) == null) {
                throw new Exception("Task [" + task.getName() + "] doesn't exist.");
            }
            putObjectToStorage(key, task);
        }
    }
    
    @Override
    public boolean removeTask(String taskName) throws Exception {
        String key = keyTask(taskName);
        try (Closeable c = lock.lock(key, 2, TimeUnit.SECONDS)) {
            return removeObjectFromStorage(key);
        }
    }
    
    private List<String> getNames(String prefix) throws Exception {
        List<StorageInfo> list = storageInfoDao.find(new DynamicQueryFilter()
                .startsWith("key", prefix)
                .limit(1000)
                .getQueryData());
        List<String> result = new ArrayList<>(list.size());
        for (StorageInfo info : list) {
            String key = info.getKey();
            if (key.length() <= prefix.length()) {
                continue;
            }
            result.add(key.substring(prefix.length(), key.length()));
        }
        return result;
    }
    
    @Override
    public List<String> getTaskNames() throws Exception {
        return getNames(keyTaskPrefix());
    }
    
    @Override
    public void removeRunningEntry(String taskName, String ownSign) throws Exception {
        try (Closeable c = lockTask(taskName)) {
            storageInfoDao.remove(new DynamicQueryFilter()
                    .startsWith("key", keyTaskItemsBase(taskName, ownSign))
                    .getQueryData());
        }
    }
    
    @Override
    public void emptyTaskItems(String taskName, String ownSign) throws Exception {
        String baseKey = keyTaskItemPrefix(taskName, ownSign);
        try (Closeable c = lockTask(taskName)) {
            storageInfoDao.remove(new DynamicQueryFilter()
                    .startsWith("key", baseKey)
                    .getQueryData());
            storageInfoDao.remove(new DynamicQueryFilter()
                    .equalPair("key", keyTaskItemsInitializer(taskName, ownSign))
                    .getQueryData());
        }
    }
    
    @Override
    public InitialResult getInitialRunningInfoResult(String taskName, String ownSign) throws Exception {
        return getObjectFromStorage(keyTaskItemsInitializer(taskName, ownSign), InitialResult.class);
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
            putObjectToStorage(keyTaskItemsInitializer(taskName, ownSign), result);
        }
    }
    
    @Override
    public void initTaskItems(String taskName, String ownSign) throws Exception {
        try (Closeable c = lockTask(taskName)) {
            Task task = getTask(taskName);
            if (task != null) {
                // init task items
                TaskItem[] items = task.getTaskItemList();
                for (TaskItem define : items) {
                    // init other properties
                    TaskItemRuntime item = newTaskItemRuntime(taskName, ownSign, define.getTaskItemId());
                    item.setDealParameter(define.getParameter());
                    putObjectToStorage(keyTaskItem(taskName, ownSign, define.getTaskItemId()), item);
                }
            } else {
                logger.warn("Try to initial a non-existed task's item");
            }
        }
    }
    
    private String decodeRunningEntryName(String taskName, String path) {
        String postfix = "/initializer";
        if (StringUtils.isEmpty(path) || !path.endsWith(postfix)) {
            return null;
        }
        path = path.substring(0, path.length() - postfix.length());
        String prefix = keyRunningEntriesBase(taskName);
        if (!path.startsWith(prefix)) {
            return null;
        }
        return path.substring(prefix.length());
    }
    
    @Override
    public List<String> getRunningEntryList(String taskName) throws Exception {
        List<StorageInfo> infoList = storageInfoDao.find(new DynamicQueryFilter()
                .startsWith("key", keyRunningEntriesBase(taskName))
                .endsWith("key", "/initializer")
                .limit(1000)
                .getQueryData());
        if (infoList.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> names = new HashSet<>(2);
        for (StorageInfo info : infoList) {
            String entryName = decodeRunningEntryName(taskName, info.getKey());
            if (StringUtils.isEmpty(entryName)) {
                logger.warn("ignore illegal name: {} -> {}", taskName, info.getKey());
                continue;
            }
            names.add(entryName);
        }
        List<String> result = new ArrayList<>(names);
        if (!result.isEmpty()) {
            Collections.sort(result);
        }
        return result;
    }
    
    @Override
    public List<TaskItemRuntime> getTaskItems(String taskName, String ownSign) throws Exception {
        List<TaskItemRuntime> itemList = getObjectsFromStorage(keyTaskItemPrefix(taskName, ownSign), TaskItemRuntime.class);
        if (!itemList.isEmpty()) {
            Collections.sort(itemList, COMPARATOR_TASK_ITEM_RUNTIME);
        }
        return itemList;
    }
    
    @Override
    public int releaseTaskItemByOwner(String taskName, String ownSign, String ownerUuid) throws Exception {
        try (Closeable c = lockTask(taskName)) {
            List<TaskItemRuntime> taskItemList = getTaskItems(taskName, ownSign);
            int released = 0;
            if (taskItemList.isEmpty()) {
                return released;
            }
            for (TaskItemRuntime item : taskItemList) {
                if (StringUtils.isNotEmpty(item.getCurrentScheduleServer()) && StringUtils.isNotEmpty(item.getRequestScheduleServer())) {
                    if (StringUtils.equals(ownerUuid, item.getCurrentScheduleServer())) {
                        // release
                        item.setCurrentScheduleServer(item.getRequestScheduleServer());
                        item.setRequestScheduleServer("");
                        putObjectToStorage(keyTaskItem(taskName, ownSign, item.getTaskItem()), item);
                        released ++;
                    }
                }
            }
            return released;
        }
    }
    
    private TaskItemRuntime newTaskItemRuntime(String taskName, String ownSign, String taskItem) {
        TaskItemRuntime item = new TaskItemRuntime();
        item.setTaskName(taskName);
        item.setOwnSign(ownSign);
        item.setDealParameter("");
        item.setCurrentScheduleServer("");
        item.setRequestScheduleServer("");
        item.setTaskItem(taskItem);
        return item;
    }
    
    @Override
    public void updateTaskItemRequestServer(String taskName, String ownSign, String taskItem, String server) throws Exception {
        try (Closeable c = lockTask(taskName)) {
            String key = keyTaskItem(taskName, ownSign, taskItem);
            TaskItemRuntime item = getObjectFromStorage(key, TaskItemRuntime.class);
            if (item == null) {
                logger.warn("Lack of runtime of taskItem, initializing");
                // should not happen
                item = newTaskItemRuntime(taskName, ownSign, taskItem);
            }
            item.setRequestScheduleServer(server);
            putObjectToStorage(key, item);
        }
    }
    
    @Override
    public void updateTaskItemCurrentServer(String taskName, String ownSign, String taskItem, String server) throws Exception {
        try (Closeable c = lockTask(taskName)) {
            String key = keyTaskItem(taskName, ownSign, taskItem);
            TaskItemRuntime item = getObjectFromStorage(key, TaskItemRuntime.class);
            if (item == null) {
                logger.warn("Lack of runtime of taskItem, initializing");
                // should not happen
                item = newTaskItemRuntime(taskName, ownSign, taskItem);
            }
            item.setCurrentScheduleServer(server);
            putObjectToStorage(key, item);
        }
    }
    
    @Override
    public long getServerSchedulingVersion(String taskName, String ownSign) throws Exception {
        String key = keyServerVersion(taskName, ownSign);
        StorageInfo info = storageInfoDao.get("key", key);
        if (info == null) {
            return 0;
        }
        return info.getExpire();
    }
    
    @Override
    public void increaseServerSchedulingVersion(String taskName, String ownSign) throws Exception {
        String key = keyServerVersion(taskName, ownSign);
        int remain = 2;
        Map<String, UpdateEntry> data = new HashMap<>();
        data.put("expire", new UpdateEntry().increse());
        QueryData queryData = new DynamicQueryFilter()
                .equalPair("key", key)
                .getQueryData();
        while (remain > 0) {
            remain --;
            if (storageInfoDao.update(data, queryData) > 0) {
                break;
            }
            StorageInfo info = new StorageInfo();
            info.setKey(key);
            info.setValue("");
            info.setExpire(0);
            storageInfoDao.insert(info, true);
        }
    }
    
    @Override
    public List<String> getServerUuidList(String taskName, String ownSign) throws Exception {
        List<String> nameList = getNames(keyServersPrefix(taskName, ownSign));
        if (nameList.isEmpty()) {
            return Collections.emptyList();
        }
        Collections.sort(nameList, COMPARATOR_UUID);
        return nameList;
    }
    
    @Override
    public void removeServer(String taskName, String ownSign, String serverUuid) throws Exception {
        try (Closeable c = lockTask(taskName)) {
            String key = keyServer(taskName, ownSign, serverUuid);
            removeObjectFromStorage(key);
        }
    }
    
    @Override
    public boolean updateServer(ScheduleServer server) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotEmpty(server.getUuid()));
        try (Closeable c = lockTask(server.getTaskName())) {
            ScheduleServer oldServer = getServer(server.getTaskName(), server.getOwnSign(), server.getUuid());
            if (oldServer == null) {
                return false;
            }
            String key = keyServer(server.getTaskName(), server.getOwnSign(), server.getUuid());
            putObjectToStorage(key, server);;
            return true;
        }
    }
    
    @Override
    public void createServer(ScheduleServer server) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotEmpty(server.getUuid()));
        try (Closeable c = lockTask(server.getTaskName())) {
            ScheduleServer oldServer = getServer(server.getTaskName(), server.getOwnSign(), server.getUuid());
            if (oldServer != null) {
                throw new Exception(server.getUuid() + " duplicated");
            }
            String key = keyServer(server.getTaskName(), server.getOwnSign(), server.getUuid());
            Timestamp heartBeatTime = new Timestamp(getGlobalTime());
            server.setHeartBeatTime(heartBeatTime);
            putObjectToStorage(key, server);
        }
    }
    
    @Override
    public ScheduleServer getServer(String taskName, String ownSign, String serverUuid) throws Exception {
        String key = keyServer(taskName, ownSign, serverUuid);
        return getObjectFromStorage(key, ScheduleServer.class);
    }
    
    @Override
    public List<String> getStrategyNames() throws Exception {
        return getNames(keyStrategyPrefix());
    }
    
    @Override
    public Strategy getStrategy(String strategyName) throws Exception {
        String key = keyStrategy(strategyName);
        return getObjectFromStorage(key, Strategy.class);
    }
    
    @Override
    public void createStrategy(Strategy strategy) throws Exception {
        String key = keyStrategy(strategy.getName());
        try (Closeable c = lock.lock(key, 2, TimeUnit.SECONDS)) {
            if (getStrategy(strategy.getName()) != null) {
                throw new Exception("Srategy [" + strategy.getName() + "] has already existed.");
            }
            putObjectToStorage(key, strategy);
        }
    }
    
    @Override
    public void updateStrategy(Strategy strategy) throws Exception {
        String key = keyStrategy(strategy.getName());
        try (Closeable c = lock.lock(key, 2, TimeUnit.SECONDS)) {
            if (getStrategy(strategy.getName()) == null) {
                throw new Exception("Srategy [" + strategy.getName() + "] doesn't exist.");
            }
            putObjectToStorage(key, strategy);
        }
    }
    
    @Override
    public boolean removeStrategy(String strategyName) throws Exception {
        String key = keyStrategy(strategyName);
        try (Closeable c = lock.lock(key, 2, TimeUnit.SECONDS)) {
            Strategy strategy = getStrategy(strategyName);
            if (strategy == null) {
                return false;
            }
            List<StrategyRuntime> runtimes = getRuntimesOfStrategy(strategyName);
            // check if it has any server registered
            if (runtimes.size() > 0) {
                throw new RuntimeException("Can not remove running strategy");
            }
            removeObjectFromStorage(key);
            return true;
        }
    }
    
    @Override
    public void clearStrategiesOfFactory(String factoryUUID) throws Exception {
        List<String> names = getStrategyNames();
        if (names == null || names.size() == 0) {
            return;
        }
        for (String strategyName : names) {
            try (Closeable c = lockStrategyRuntime(strategyName)) {
                removeObjectFromStorage(keyStrategyRuntime(strategyName, factoryUUID));
            }
        }
    }
    
    @Override
    public boolean isFactoryAllowExecute(String factoryUUID) throws Exception {
        FactoryInfo info = getObjectFromStorage(keyFactory(factoryUUID), FactoryInfo.class);
        return info == null ? true : info.isAllow();
    }
    
    @Override
    public void setFactoryAllowExecute(String factoryUUID, boolean allow) throws Exception {
        String factoryKey = keyFactory(factoryUUID);
        try (Closeable c = lock.lock(factoryKey, 2, TimeUnit.SECONDS)) {
            FactoryInfo info = getObjectFromStorage(factoryKey, FactoryInfo.class);
            if (info == null) {
                info = new FactoryInfo();
                info.setHeartbeat(getGlobalTime());
            }
            info.setAllow(allow);
            putObjectToStorage(factoryKey, info);
        }
    }
    
    @Override
    public List<String> getFactoryUuidList() throws Exception {
        List<String> list = getNames(keyFactoryPrefix());
        Collections.sort(list, COMPARATOR_UUID);
        return list;
    }
    
    private void cleanExpiredFactory() throws Exception {
        List<String> factoryExpired = new ArrayList<>();
        String prefix = keyFactoryPrefix();
        List<StorageInfo> list = storageInfoDao.find(new DynamicQueryFilter()
                .startsWith("key", prefix)
                .orderBy("expire", false)
                .limit(20)
                .getQueryData());
        long now = getGlobalTime();
        for (StorageInfo info : list) {
            // 30 seconds ttl
            FactoryInfo factory = JSON.parseObject(info.getValue(), FactoryInfo.class);
            if (factory == null) {
                continue;
            }
            long diff = now - factory.getHeartbeat();
            if (diff > 30000) {
                // expired
                factoryExpired.add(info.getKey());
            }
        }
        if (factoryExpired.isEmpty()) {
            return;
        }
        List<String> strategyNameList = getStrategyNames();
        try (Closeable c = lockFactories()) {
            // clean
            for (String factoryKey : factoryExpired) {
                removeObjectFromStorage(factoryKey);
                String uuid = factoryKey.substring(prefix.length(), factoryKey.length());
                for (String strategyName : strategyNameList) {
                    removeObjectFromStorage(keyStrategyRuntime(strategyName, uuid));
                }
            }
        }
        logger.info("Clean {} expired factories", factoryExpired.size());
    }
    
    @Override
    public List<String> registerFactory(ScheduleFactory factory) throws Exception {
        List<String> unregistered = new ArrayList<>();
        String factoryKey = keyFactory(factory.getUuid());
        // heartbeat
        FactoryInfo info = null;
        try (Closeable c = lockFactories()) {
            info = getObjectFromStorage(factoryKey, FactoryInfo.class);
            if (info == null) {
                info = new FactoryInfo();
                info.setAllow(true);
            }
            info.setHeartbeat(getGlobalTime());
            putObjectToStorage(factoryKey, info);
        }
        cleanExpiredFactory();
        // register to strategy
        List<String> names = getStrategyNames();
        for (String strategyName : names) {
            try (Closeable c = lockStrategyRuntime(strategyName)) {
                Strategy strategy = getStrategy(strategyName);
                boolean isFind = false;
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
                        removeObjectFromStorage(keyStrategyRuntime(strategy.getName(), factory.getUuid()));
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
                removeObjectFromStorage(keyStrategyRuntime(strategyName, factory.getUuid()));
            }
        }
        try (Closeable c = lockFactories()) {
            removeObjectFromStorage(keyFactory(factory.getUuid()));
        }
    }
    
    @Override
    public List<StrategyRuntime> getRuntimesOfStrategy(String strategyName) throws Exception {
        String prefix = keyStrategyRuntimePrefix(strategyName);
        List<StorageInfo> list = storageInfoDao.find(new DynamicQueryFilter()
                .startsWith("key", prefix)
                .limit(1000)
                .getQueryData());
        Map<String, StrategyRuntime> map = new HashMap<>(list.size());
        for (StorageInfo info : list) {
            StrategyRuntime runtime = JSON.parseObject(info.getValue(), StrategyRuntime.class);
            map.put(runtime.getFactoryUuid(), runtime);
        }
        if (map.isEmpty()) {
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
        String key = keyStrategyRuntime(strategyName, factoryUUID);
        StrategyRuntime runtime = getObjectFromStorage(key, StrategyRuntime.class);
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
        String key = keyStrategyRuntime(runtime.getStrategyName(), runtime.getFactoryUuid());
        putObjectToStorage(key, runtime);
    }
}
