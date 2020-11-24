package com.yoloho.schedule.storage.zk;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryForever;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.yoloho.enhanced.common.util.JoinerSplitters;
import com.yoloho.enhanced.common.util.RandomUtil;
import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.interfaces.ScheduleFactory;
import com.yoloho.schedule.storage.zk.util.PathUtil;
import com.yoloho.schedule.types.InitialResult;
import com.yoloho.schedule.types.ScheduleConfig;
import com.yoloho.schedule.types.ScheduleServer;
import com.yoloho.schedule.types.Strategy;
import com.yoloho.schedule.types.StrategyRuntime;
import com.yoloho.schedule.types.Task;
import com.yoloho.schedule.types.TaskItem;
import com.yoloho.schedule.types.TaskItemRuntime;
import com.yoloho.schedule.util.DataVersion;

/**
 * Zookeeper backend
 * 
 * @author jason
 *
 */
public class ZookeeperStorage implements IStorage {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperStorage.class.getSimpleName());
    private CuratorFramework client = null;
    private boolean connected = false;
    private long timeDelta = 0;
    
    @Override
    public String getName() {
        return "zookeeper";
    }
    
    @Override
    public long getSequenceNumber() throws Exception {
        String path = "/generateSequence$";
        String newPath = client.create()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(path);
        client.delete().forPath(newPath);
        return NumberUtils.toLong(newPath.substring(newPath.lastIndexOf('$') + 1));
    }
    
    private void checkParent() throws Exception {
        CuratorFramework clientWithoutNamespace = client.usingNamespace(null);
        String fullPath = "/" + client.getNamespace();
        List<String> list = JoinerSplitters.getSplitter("/").splitToList(fullPath);
        StringBuilder builder = new StringBuilder();
        for (String segment : list) {
            if (StringUtils.isEmpty(segment))
                continue;
            builder.append('/').append(segment);
            if (builder.toString().equals(fullPath))
                break;
            String path = builder.toString();
            if (clientWithoutNamespace.checkExists().forPath(path) == null)
                throw new RuntimeException("Parent path can not be created: " + path);
            byte[] data = clientWithoutNamespace.getData().forPath(path);
            if (data != null) {
                String tmpVersion = new String(data);
                if (tmpVersion.indexOf("schedule-") >= 0) {
                    throw new Exception("[" + path
                            + "] is already a tbschedule instance's root directory, its any subdirectory cannot be any other's root directory");
                }
            }
        }
    }
    
    /**
     * Get value for specific node, ignore exception(return null)
     * 
     * @param path
     * @return null for error or not existed
     */
    private String getNode(String path) {
        try {
            if (exist(path)) {
                return new String(client.getData().forPath(path));
            }
        } catch (Exception e) {
        }
        return null;
    }
    
    private void createIfNotExist(String path) throws Exception {
        if (client.checkExists().forPath(path) == null) {
            // create root
            client.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.PERSISTENT)
                .forPath(path);
        }
    }
    
    /**
     * Create node and set its value
     * 
     * @param path
     * @param value
     * @return false when node already exists
     * @throws Exception
     */
    private boolean createNode(String path, String value) throws Exception {
        if (client.checkExists().forPath(path) == null) {
            // create root
            client.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.PERSISTENT)
                .forPath(path, value.getBytes());
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Update data for an existed path
     * 
     * @param path
     * @param value
     * @return
     * @throws Exception
     */
    private boolean replaceNode(String path, String value) throws Exception {
        if (value == null) {
            value = "";
        }
        if (client.checkExists().forPath(path) != null) {
            // create root
            return client.setData().forPath(path, value.getBytes()) != null;
        } else {
            createNode(path, value);
            return client.checkExists().forPath(path) != null;
        }
    }
    
    private void checkVersion() throws Exception {
        // set version
        String storeVersion = getNode("/");
        if (StringUtils.isNotEmpty(storeVersion)) {
            if (!DataVersion.isCompatible(storeVersion)) {
                throw new Exception("Current Version " + DataVersion.getCurrentVersion()
                        + " don't be compatible with the version " + storeVersion + " in storage");
            }
            logger.info(
                    "Current Data Structure Version: " + DataVersion.getCurrentVersion() + " Data in Storage: " + storeVersion);
        } else {
            client.setData().forPath("/", DataVersion.getCurrentVersion().getBytes());
        }
    }
    
    private boolean exist(String path) throws Exception {
        return client.checkExists().forPath(path) != null;
    }
    
    private void initial() throws Exception {
        int waitTime = 10000;
        while (!connected && waitTime > 0) {
            Thread.sleep(100);
            waitTime -= 100;
        }
        if (!connected) {
            throw new RuntimeException("Zookeeper cannot be connected.");
        }
        createIfNotExist("/");
        checkParent();
        checkVersion();
        
        // base task type
        createIfNotExist(PathUtil.taskBasePath());
        createIfNotExist(PathUtil.factoryBasePath());
        createIfNotExist(PathUtil.strategyBasePath());
    }
    
    /**
     * Fetch the global time based on zookeeper server
     * 
     * @return
     */
    @Override
    public long getGlobalTime() {
        return System.currentTimeMillis() + timeDelta;
    }
    
    @Override
    public boolean test() {
        try {
            return exist("/");
        } catch (Exception e) {
        }
        return false;
    }
    
    /**
     * The algorithm:
     * <pre>
     * /root -> /root   -> /root     -> /root     (sort) /root
     *          /root/a    /root/a      /root/a          /root/a
     *          /root/b    /root/b      /root/b          /root/a/c
     *                     /root/a/c    /root/a/c        /root/a/d
     *                     /root/a/d    /root/a/d        /root/b
     *                                  /root/b/e        /root/b/e
     */
    @Override
    public String dump() throws Exception {
        List<Pair<String, String>> pathList = new ArrayList<>(1);
        byte[] data = client.getData().forPath("/");
        pathList.add(Pair.of("/", data == null ? "" : new String(data)));
        int cur = 0;
        // rolling get & list & append to tail
        while (cur < pathList.size()) {
            try {
                String basePath = pathList.get(cur).getLeft();
                List<String> list = client.getChildren().forPath(basePath);
                if (!basePath.endsWith("/")) {
                    basePath += "/";
                }
                for (String child : list) {
                    try {
                        String p = basePath + child;
                        data = client.getData().forPath(p);
                        pathList.add(Pair.of(p, data == null ? "" : new String(data)));
                    } catch (NoNodeException e) {
                        // ignore
                        continue;
                    }
                }
            } catch (NoNodeException e) {
                // ignore
                continue;
            }
            cur ++;
        }
        // sort
        Collections.sort(pathList);
        // join into a string
        StringBuilder builder = new StringBuilder();
        for (Pair<String, String> pair : pathList) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(pair.getLeft())
                .append(": ")
                .append(pair.getRight());
        }
        return builder.toString();
    }
    
    /**
     * Try to fetch server's time and use it for global basement
     */
    private void checkServerTime() {
        timeDelta = 0;
        String path = "/" + RandomUtil.getRandomString(18);
        try {
            String newPath = client.create().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(path);
            long now = System.currentTimeMillis();
            Stat stat = client.checkExists().forPath(newPath);
            timeDelta = stat.getCtime() - now;
            if(Math.abs(timeDelta) > 5000){
                logger.error("ATTENTION, difference of system time between local and server is considerable: {} ms", Math.abs(timeDelta));
            }
            client.delete().forPath(newPath);
        } catch (Exception e) {
            logger.error("Check server time failed", e);
        }
    }
    
    @Override
    public boolean init(final ScheduleConfig config, final OnConnected onConnected) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(config.getAddress()), "Address should not be empty");
        Preconditions.checkArgument(StringUtils.isNotEmpty(config.getRootPath()), "rootPath should not be empty");
        synchronized (this) {
            if (client != null) {
                client.close();
            }
            final String authString = config.getUsername() + ":" + config.getPassword();
            ACLProvider aclProvider = new ACLProvider() {
                private List<ACL> acl = new ArrayList<>();
                {
                    try {
                        acl.add(new ACL(ZooDefs.Perms.ALL, new Id("digest", DigestAuthenticationProvider.generateDigest(authString))));
                        acl.add(new ACL(ZooDefs.Perms.READ, Ids.ANYONE_ID_UNSAFE));
                    } catch (Exception e) {
                        logger.error("Initial acl provider failed", e);
                    }
                }
                
                @Override
                public List<ACL> getDefaultAcl() {
                    return acl;
                }
                
                @Override
                public List<ACL> getAclForPath(String path) {
                    return acl;
                }
            };
            RetryPolicy retryPolicy = new RetryForever(10000);
            client = CuratorFrameworkFactory.builder()
                    .connectString(config.getAddress())
                    .namespace(StringUtils.strip(config.getRootPath(), "/"))
                    .retryPolicy(retryPolicy)
                    .aclProvider(aclProvider)
                    .authorization("digest", authString.getBytes())
                    .sessionTimeoutMs(60000)
                    .connectionTimeoutMs(10000)
                    .canBeReadOnly(false)
                    .build();
            final ZookeeperStorage instance = this;
            client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
                
                @Override
                public void stateChanged(CuratorFramework client, ConnectionState newState) {
                    switch (newState) {
                        case CONNECTED:
                        case RECONNECTED:
                            logger.info("Connection connected: {}", config.getAddress());
                            connected = true;
                            onConnected.connected(instance);
                            break;
                            
                        case LOST:
                            logger.warn("Connection lost: {}", config.getAddress());
                            break;
                            
                        case SUSPENDED:
                            logger.warn("Connection suspended: {}", config.getAddress());
                            break;

                        default:
                            logger.warn("Unhandled connection state received: {}", newState);
                            break;
                    }
                }
            });
            client.start();
            try {
                client.blockUntilConnected(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("Zookeeper connect failed");
                return false;
            }
            try {
                initial();
            } catch (Exception e) {
                logger.error("Initializing zookeeper storage structure failed", e);
                return false;
            }
            checkServerTime();
        }
        return true;
    }
    
    @Override
    public void shutdown() {
        synchronized (this) {
            if (client != null) {
                client.close();
                client = null;
            }
        }
    }
    
    @Override
    public void createTask(Task task) throws Exception {
        String zkPath = PathUtil.taskPath(task.getName());
        String json = JSON.toJSONString(task);
        if (!createNode(zkPath, json)) {
            throw new Exception("Task [" + task.getName() + "] has already existed.");
        }
    }

    @Override
    public void updateTask(Task task) throws Exception {
        String zkPath = PathUtil.taskPath(task.getName());
        String json = JSON.toJSONString(task);
        replaceNode(zkPath, json);
    }
    
    @Override
    public boolean removeTask(String taskName) throws Exception {
        String path = PathUtil.taskPath(taskName);
        if (exist(path)) {
            client.delete().deletingChildrenIfNeeded().forPath(path);
            return true;
        }
        return false;
    }
    
    @Override
    public void removeRunningEntry(String taskName, String ownSign) throws Exception {
        client.delete().deletingChildrenIfNeeded().forPath(PathUtil.runningEntryPath(taskName, ownSign));
    }
    
    @Override
    public Task getTask(String taskName) throws Exception {
        String path = PathUtil.taskPath(taskName);
        if (!exist(path)) {
            return null;
        }
        byte[] data = client.getData().forPath(path);
        if (data == null) {
            return null;
        }
        Task task = JSON.parseObject(new String(data), Task.class);
        return task;
    }
    
    @Override
    public List<String> getTaskNames() throws Exception {
        String path = PathUtil.taskBasePath();
        if (!exist(path)) {
            return Collections.emptyList();
        }
        List<String> list = client.getChildren().forPath(path);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        Collections.sort(list);
        return list;
    }
    
    @Override
    public void emptyTaskItems(String taskName, String ownSign) throws Exception {
        String taskItemPath = PathUtil.taskItemBasePath(taskName, ownSign);
        if (exist(taskItemPath)) {
            client.delete().deletingChildrenIfNeeded().forPath(taskItemPath);
        }
        createIfNotExist(taskItemPath);
    }
    
    @Override
    public InitialResult getInitialRunningInfoResult(String taskName, String ownSign) throws Exception {
        String taskItemPath = PathUtil.taskItemBasePath(taskName, ownSign);
        String json = getNode(taskItemPath);
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        try {
            return JSON.parseObject(json, InitialResult.class);
        } catch (Exception e) {
        }
        return null;
    }
    
    @Override
    public void updateTaskItemsInitialResult(String taskName, String ownSign, 
            String initializerUuid) throws Exception {
        String taskItemPath = PathUtil.taskItemBasePath(taskName, ownSign);
        InitialResult result = getInitialRunningInfoResult(taskName, ownSign);
        if (result == null) {
            result = new InitialResult();
        }
        result.setVersion(result.getVersion() + 1);
        result.setUpdateTime(getGlobalTime());
        result.setUuid(initializerUuid);
        replaceNode(taskItemPath, JSON.toJSONString(result));
    }
    
    @Override
    public void initTaskItems(String taskName, String ownSign) throws Exception {
        Task task = getTask(taskName);
        if (task != null) {
            // init task items
            TaskItem[] items = task.getTaskItemList();
            for (TaskItem define : items) {
                // init other properties
                String itemPath = PathUtil.taskItemPath(taskName, ownSign, define.getTaskItemId());
                createIfNotExist(itemPath);
                createIfNotExist(itemPath + "/cur_server");
                createIfNotExist(itemPath + "/req_server");
                replaceNode(itemPath + "/parameter", define.getParameter());
            }
        }
    }
    
    @Override
    public List<String> getRunningEntryList(String taskName) throws Exception {
        String path = PathUtil.taskPath(taskName);
        if (!exist(path)) {
            return Collections.emptyList();
        }
        List<String> list = client.getChildren().forPath(path);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        Collections.sort(list);
        return list;
    }
    
    @Override
    public List<TaskItemRuntime> getTaskItems(String taskName, String ownSign) throws Exception {
        String path = PathUtil.taskItemBasePath(taskName, ownSign);
        if (!exist(path)) {
            return Collections.emptyList();
        }
        List<String> names = client.getChildren().forPath(path);
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }
        List<TaskItemRuntime> result = new ArrayList<>(names.size());
        for (String name : names) {
            String taskItemPath = PathUtil.taskItemPath(taskName, ownSign, name);
            TaskItemRuntime item = new TaskItemRuntime();
            item.setTaskName(taskName);
            item.setOwnSign(ownSign);
            item.setCurrentScheduleServer(getNode(taskItemPath + "/cur_server"));
            item.setRequestScheduleServer(getNode(taskItemPath + "/req_server"));
            item.setDealParameter(getNode(taskItemPath + "/parameter"));
            item.setTaskItem(name);
            result.add(item);
        }
        if (!result.isEmpty()) {
            Collections.sort(result, COMPARATOR_TASK_ITEM_RUNTIME);
        }
        return result;
    }
    
    @Override
    public int releaseTaskItemByOwner(String taskName, String ownSign, String ownerUuid) throws Exception {
        List<TaskItemRuntime> items = getTaskItems(taskName, ownSign);
        int released = 0;
        for (TaskItemRuntime item : items) {
            if (StringUtils.isNotEmpty(item.getCurrentScheduleServer()) && StringUtils.isNotEmpty(item.getRequestScheduleServer())) {
                if (StringUtils.equals(item.getCurrentScheduleServer(), ownerUuid)) {
                    String path = PathUtil.taskItemPath(taskName, ownSign, item.getTaskItem());
                    client.setData().forPath(path + "/req_server", "".getBytes());
                    client.setData().forPath(path + "/cur_server", item.getRequestScheduleServer().getBytes());
                    released ++;
                }
            }
        }
        return released;
    }
    
    private void updateTaskItemServer(String taskName, String ownSign, String taskItem, String type, String server) {
        String path = PathUtil.taskItemPath(taskName, ownSign, taskItem) + "/" + type;
        try {
            if (!exist(path)) {
                createIfNotExist(path);
            }
            client.setData().forPath(path, server.getBytes());
        } catch (Exception e) {
            logger.error("Save server failed", e);
        }
    }
    
    @Override
    public void updateTaskItemRequestServer(String taskName, String ownSign, String taskItem, String server) throws Exception {
        updateTaskItemServer(taskName, ownSign, taskItem, "req_server", server);
    }
    
    @Override
    public void updateTaskItemCurrentServer(String taskName, String ownSign, String taskItem, String server) throws Exception {
        updateTaskItemServer(taskName, ownSign, taskItem, "cur_server", server);
    }
    
    @Override
    public long getServerSchedulingVersion(String taskName, String ownSign) throws Exception {
        String path = PathUtil.serverBasePath(taskName, ownSign);
        byte[] data = client.getData().forPath(path);
        if (data == null) {
            return 0;
        }
        return NumberUtils.toLong(new String(data));
    }
    
    @Override
    public void increaseServerSchedulingVersion(String taskName, String ownSign) throws Exception {
        String path = PathUtil.serverBasePath(taskName, ownSign);
        long version = getServerSchedulingVersion(taskName, ownSign);
        client.setData().forPath(path, String.valueOf(version + 1).getBytes());
    }
    
    @Override
    public List<String> getServerUuidList(String taskName, String ownSign) throws Exception {
        String path = PathUtil.serverBasePath(taskName, ownSign);
        if (!exist(path)) {
            return Collections.emptyList();
        }
        List<String> list = client.getChildren().forPath(path);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        Collections.sort(list, COMPARATOR_UUID);
        return list;
    }
    
    @Override
    public void removeServer(String taskName, String ownSign, String serverUuid) throws Exception {
        String path = PathUtil.serverPath(taskName, ownSign, serverUuid);
        if (exist(path)) {
            client.delete().forPath(path);
        }
    }
    
    @Override
    public boolean updateServer(ScheduleServer server) throws Exception {
        String taskName = server.getTaskName();
        String ownSign = server.getOwnSign();
        String path = PathUtil.serverPath(taskName, ownSign, server.getUuid());
        if (!exist(path)) {
            return false;
        }
        client.setData().forPath(path, JSON.toJSONString(server).getBytes());
        return true;
    }
    
    @Override
    public void createServer(ScheduleServer server) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotEmpty(server.getUuid()));
        String taskName = server.getTaskName();
        String ownSign = server.getOwnSign();
        String path = PathUtil.serverPath(taskName, ownSign, server.getUuid());
        if (exist(path)) {
            throw new Exception(server.getUuid() + " duplicated");
        }
        Timestamp heartBeatTime = new Timestamp(getGlobalTime());
        server.setHeartBeatTime(heartBeatTime);
        createNode(path, JSON.toJSONString(server));
    }
    
    @Override
    public ScheduleServer getServer(String taskName, String ownSign, String serverUuid) throws Exception {
        String path = PathUtil.serverPath(taskName, ownSign, serverUuid);
        if (!exist(path)) {
            return null;
        }
        String json = new String(client.getData().forPath(path));
        if (StringUtils.isEmpty(json))
            return null;
        try {
            return JSON.parseObject(json, ScheduleServer.class);
        } catch (Exception e) {
            logger.warn("Get server info error", e);
        }
        return null;
    }
    
    @Override
    public List<String> getStrategyNames() throws Exception {
        String path = PathUtil.strategyBasePath();
        if (!exist(path)) {
            return Collections.emptyList();
        }
        List<String> list = client.getChildren().forPath(path);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        Collections.sort(list);
        return list;
    }
    
    @Override
    public Strategy getStrategy(String strategyName) throws Exception {
        String path = PathUtil.strategyPath(strategyName);
        if (!exist(path)) {
            return null;
        }
        byte[] data = client.getData().forPath(path);
        if (data == null) {
            return null;
        }
        Strategy strategy = JSON.parseObject(new String(data), Strategy.class);
        return strategy;
    }
    
    @Override
    public void createStrategy(Strategy strategy) throws Exception {
        String path = PathUtil.strategyPath(strategy.getName());
        String json = JSON.toJSONString(strategy);
        if (!createNode(path, json)) {
            throw new Exception("Srategy [" + strategy.getName() + "] has already existed.");
        }
    }
    
    @Override
    public void updateStrategy(Strategy strategy) throws Exception {
        String path = PathUtil.strategyPath(strategy.getName());
        String json = JSON.toJSONString(strategy);
        replaceNode(path, json);
    }
    
    @Override
    public boolean removeStrategy(String strategyName) throws Exception {
        String path = PathUtil.strategyPath(strategyName);
        // check serverlist
        if (exist(path)) {
            List<String> list = client.getChildren().forPath(path);
            if (list != null && list.size() > 0) {
                throw new RuntimeException("Can not remove running strategy");
            }
            client.delete().deletingChildrenIfNeeded().forPath(path);
            return true;
        }
        return false;
    }
    
    @Override
    public void clearStrategiesOfFactory(String factoryUUID) throws Exception {
        List<String> strategyNames = getStrategyNames();
        for (String strategyName : strategyNames) {
            String path = PathUtil.factoryForStrategyPath(strategyName, factoryUUID);
            if (exist(path)) {
                client.delete().forPath(path);
            }
        }
    }
    
    @Override
    public boolean isFactoryAllowExecute(String factoryUUID) throws Exception {
        String path = PathUtil.factoryPath(factoryUUID);
        if (!exist(path)) {
            throw new RuntimeException("Factory doesn't exist");
        }
        return !StringUtils.equalsIgnoreCase(getNode(path), "false");
    }
    
    @Override
    public void setFactoryAllowExecute(String factoryUUID, boolean allow) throws Exception {
        String path = PathUtil.factoryPath(factoryUUID);
        if (!exist(path)) {
            throw new RuntimeException("Factory doesn't exist");
        }
        replaceNode(path, String.valueOf(allow));
    }
    
    @Override
    public List<String> getFactoryUuidList() throws Exception {
        String path = PathUtil.factoryBasePath();
        if (!exist(path)) {
            return Collections.emptyList();
        }
        List<String> list = client.getChildren().forPath(path);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        Collections.sort(list, COMPARATOR_UUID);
        return list;
    }
    
    @Override
    public List<String> registerFactory(ScheduleFactory factory) throws Exception {
        // register to factory list
        String path = PathUtil.factoryPath(factory.getUuid());
        if (!exist(path)) {
            client.create().withMode(CreateMode.EPHEMERAL).forPath(path);
        }
        // register to strategy
        List<String> unregistered = new ArrayList<>();
        List<String> names = getStrategyNames();
        for (String strategyName : names) {
            Strategy strategy = getStrategy(strategyName);
            boolean isFind = false;
            String zkPath = PathUtil.factoryForStrategyPath(strategyName, factory.getUuid());
            if (Strategy.STS_PAUSE.equalsIgnoreCase(strategy.getSts()) == false
                    && strategy.getIPList() != null) {
                for (String ip : strategy.getIPList()) {
                    if (ip.equals("127.0.0.1") || ip.equalsIgnoreCase("localhost") || ip.equals(factory.getIp())
                            || ip.equalsIgnoreCase(factory.getHostName())) {
                        // Can be scheduled in this factory
                        if (!exist(zkPath)) {
                            StrategyRuntime runtime = new StrategyRuntime();
                            runtime.setStrategyName(strategyName);
                            runtime.setFactoryUuid(factory.getUuid());
                            runtime.setRequestNum(0);
                            client.create().withMode(CreateMode.EPHEMERAL).forPath(zkPath, JSON.toJSONString(runtime).getBytes());
                        }
                        isFind = true;
                        break;
                    }
                }
            }
            if (isFind == false) {// 清除原来注册的Factory
                if (exist(zkPath)) {
                    client.delete().forPath(zkPath);
                    unregistered.add(strategyName);
                }
            }
        }
        return unregistered;
    }
    
    @Override
    public void unregisterFactory(ScheduleFactory factory) throws Exception {
        // nothing need to be done
    }
    
    @Override
    public List<StrategyRuntime> getRuntimesOfStrategy(String strategyName) throws Exception {
        String path = PathUtil.strategyPath(strategyName);
        List<String> list = client.getChildren().forPath(path);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        Collections.sort(list, COMPARATOR_UUID);
        List<StrategyRuntime> result = new ArrayList<>(list.size());
        for (String factoryUUID : list) {
            StrategyRuntime runtime = getStrategyRuntime(strategyName, factoryUUID);
            if (runtime != null) {
                result.add(runtime);
            }
        }
        return result;
    }
    
    @Override
    public StrategyRuntime getStrategyRuntime(String strategyName, String factoryUUID) throws Exception {
        String path = PathUtil.factoryForStrategyPath(strategyName, factoryUUID);
        if (!exist(path)) {
            return null;
        }
        String json = getNode(path);
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        try {
            StrategyRuntime runtime = JSON.parseObject(json, StrategyRuntime.class);
            Preconditions.checkArgument(StringUtils.isNotEmpty(runtime.getFactoryUuid()), "Factory UUID of runtime is empty");
            Preconditions.checkArgument(StringUtils.isNotEmpty(runtime.getStrategyName()), "Strategy name of runtime is empty");
            return runtime;
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public void updateRuntimeOfStrategy(StrategyRuntime runtime) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotEmpty(runtime.getFactoryUuid()), "Factory UUID of runtime is empty");
        Preconditions.checkArgument(StringUtils.isNotEmpty(runtime.getStrategyName()), "Strategy name of runtime is empty");
        String path = PathUtil.factoryForStrategyPath(runtime.getStrategyName(), runtime.getFactoryUuid());
        replaceNode(path, JSON.toJSONString(runtime));
    }
}
