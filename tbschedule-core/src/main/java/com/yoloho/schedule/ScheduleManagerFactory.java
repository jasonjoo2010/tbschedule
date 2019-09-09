package com.yoloho.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.google.common.base.Preconditions;
import com.yoloho.schedule.interfaces.IScheduleTaskDeal;
import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.interfaces.IStrategyTask;
import com.yoloho.schedule.processor.ScheduleManagerStatic;
import com.yoloho.schedule.types.ScheduleConfig;
import com.yoloho.schedule.types.Strategy;
import com.yoloho.schedule.types.StrategyKind;
import com.yoloho.schedule.types.StrategyRuntime;
import com.yoloho.schedule.util.ScheduleUtil;

/**
 * The Global Schedule Factory
 * 
 * @author xuannan
 * 
 */
public class ScheduleManagerFactory implements ApplicationContextAware {
	protected static transient Logger logger = LoggerFactory.getLogger(ScheduleManagerFactory.class);
	
    private ScheduleConfig config;
	private IStorage storage;

	/**
	 * Whether should really do schedule or just as an operator (console client)
	 */
	private boolean enableSchedule = true;
	private boolean initializing = false;
	private int factoryHeartBeatInterval = 2000;
    private volatile long lastCheck = System.currentTimeMillis();

    private Map<String, List<IStrategyTask>> managerMap = new ConcurrentHashMap<>();

    private ApplicationContext applicationcontext;
    private String uuid;
    private String ip;
    private String hostName;

    private Timer factoryCheckerTimer;
    private FactoryChecker factoryChecker;
    private Lock innerLock = new ReentrantLock();

    private volatile String errorMessage = "Empty address in config";

    public ScheduleManagerFactory() {
        this.ip = ScheduleUtil.getLocalIP();
        this.hostName = ScheduleUtil.getLocalHostName();
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    protected void lock() {
        this.innerLock.lock();
    }
    
    protected void unlock() {
        this.innerLock.unlock();
    }
    
    protected void setLastCheck(long lastCheck) {
        this.lastCheck = lastCheck;
    }
    
    /**
     * Last triggered time of Checker
     * 
     * @return
     */
    public long getLastCheck() {
        return lastCheck;
    }
    
    /**
     * Whether enable scheduling
     * 
     * @return
     */
    public boolean isEnableSchedule() {
        return enableSchedule;
    }
    
    /**
     * Enable / disable scheduling
     * 
     * @param enableSchedule
     */
    public void setEnableSchedule(boolean enableSchedule) {
        this.enableSchedule = enableSchedule;
    }

    public void init() throws Exception {
        lock();
        try {
            if (this.storage != null) {
                this.storage.shutdown();
                this.storage = null;
            }
            {
                // SPI
                ServiceLoader<IStorage> loader = ServiceLoader.load(IStorage.class);
                Preconditions.checkNotNull(loader, "Fail to fetch providers of IStorage");
                Iterator<IStorage> it = loader.iterator();
                Preconditions.checkArgument(it.hasNext(), "Fail to fetch providers of IStorage");
                while (it.hasNext()) {
                    IStorage item = it.next();
                    if (StringUtils.isEmpty(config.getStorage())
                            || StringUtils.equalsIgnoreCase(config.getStorage(), item.getName())) {
                        this.storage = item;
                        break;
                    }
                }
                Preconditions.checkNotNull(this.storage,
                        "Cound not found the storage specified: " + config.getStorage());
                // async
                new Thread("Schedule-Factory-Init") {
                    public void run() {
                        storage.init(getConfig(), new IStorage.OnConnected() {
                            
                            @Override
                            public void connected(IStorage storage) {
                                initialData();
                            }
                        });
                    }
                }.start();
            }
            
            this.errorMessage = "Initializing ......";
        } finally {
            unlock();
        }
    }
	
	/**
	 * reinit only for console
	 * 
	 * @param config
	 * @throws Exception
	 */
	public void reInit(ScheduleConfig config) throws Exception{
        if (isEnableSchedule() || this.factoryCheckerTimer != null || this.managerMap.size() > 0) {
            throw new Exception("Can not reinit scheduling factory, maybe you should stop it and recreate it");
        }
        setConfig(config);
        init();
	}
	
    /**
     * @param p
     * @throws Exception
     * @deprecated in favor of
     * {@link #init(ScheduleConfig)}
     */
    public void init(Properties p) throws Exception {
        setConfig(new ScheduleConfig(p));
        init();
	}
    
    public void init(ScheduleConfig config) throws Exception {
        setConfig(config);
        init();
    }
    
    /**
     * Do initializing work when storage get ready
     * @throws Exception
     */
    protected void initialData() {
        if (!isEnableSchedule() || initializing) {
            this.errorMessage = null;
            return;
        }
        boolean needRestart = false;
        lock();
        this.initializing = true;
        try {
            // generate uuid if needed
            if (StringUtils.isEmpty(getUuid())) {
                setUuid(generateUUID());
            }
            Preconditions.checkArgument(StringUtils.isNotEmpty(getUuid()));
            // Register node to storage
            this.storage.registerFactory(this);
            if (factoryCheckerTimer == null) {
                factoryCheckerTimer = new Timer("TBScheduleManagerFactory-HeartBeat");
            }
            if (factoryChecker == null) {
                factoryChecker = new FactoryChecker(this);
                factoryCheckerTimer.schedule(factoryChecker, 2000, this.factoryHeartBeatInterval);
            }
            this.errorMessage = null;
            logger.info("Scheduling initialized");
        } catch (Exception e) {
            // error occurred, restart
            logger.warn("Exception occurred, try to restart", e);
            needRestart = true;
        } finally {
            unlock();
        }
        while (needRestart && isEnableSchedule()) {
            try {
                restart();
                needRestart = false;
            } catch (Exception e) {
                logger.warn("Try to restart failed", e);
                try {
                    Thread.sleep(1000);
                } catch (Exception e1) {
                }
            }
        }
        this.initializing = false;
    }

    /**
     * Create task by type
     * 
     * @param strategy
     * @return
     * @throws Exception
     */
    private IStrategyTask createStrategyTask(Strategy strategy) throws Exception {
        IStrategyTask result = null;
        try {
            if (StrategyKind.Schedule == strategy.getKind()) {
                // schedule a task
                String taskName = ScheduleUtil.taskNameFromRunningEntry(strategy.getTaskName());
                String ownSign = ScheduleUtil.ownsignFromRunningEntry(strategy.getTaskName());
                result = new ScheduleManagerStatic(this, taskName, ownSign);
            } else if (StrategyKind.Java == strategy.getKind()) {
                // new instance
                result = (IStrategyTask) Class.forName(strategy.getTaskName()).newInstance();
                result.initialTaskParameter(strategy.getName(), strategy.getTaskParameter());
            } else if (StrategyKind.Bean == strategy.getKind()) {
                // reference to a bean
                result = (IStrategyTask) this.getBean(strategy.getTaskName());
                result.initialTaskParameter(strategy.getName(), strategy.getTaskParameter());
            }
        } catch (Exception e) {
            logger.error("Create task error. Please make sure the name is correct: ", strategy.getName(), e);
        }
        return result;
    }

    protected void refresh() throws Exception {
        lock();
        if (!isEnableSchedule()) {
            return;
        }
        try {
            // 判断状态是否终止
            boolean isException = false;
            boolean allowRun = true;
            try {
                allowRun = this.storage.isFactoryAllowExecute(getUuid());
            } catch (Exception e) {
                isException = true;
                logger.error("Fetch factory configuration failed: factoryUUID={}", getUuid(), e);
            }
            if (isException == true) {
                try {
                    stopServer(null); // Stop all servers in factory
                    this.storage.clearStrategiesOfFactory(getUuid());
                } finally {
                    reRegisterFactory();
                }
            } else if (allowRun == false) {
                stopServer(null); // Stop all servers in factory
                this.storage.clearStrategiesOfFactory(getUuid());
            } else {
                reRegisterFactory();
            }
        } finally {
            unlock();
        }
    }
    
    /**
     * Generate factory id according the logical unique string
     * <p>
     * Generally a sequence number will be appended used to be sorted<br>
     * eg. str -> str000001
     * <p>
     * Logical unique string is a string identifier that should be unique in logic layer.<br>
     * Generator (By calling this function) should append it by a sequence number to:<br><br>
     * 1. Make it unique globally.<br>
     * 2. Make it sorted meaningfully.<br>
     */
    private String generateUUID() throws Exception {
        return String.format("%s$%s$%s$%010d", getIp(), getHostName(), 
                UUID.randomUUID().toString().replace("-", "").toUpperCase(),
                getStorage().getSequenceNumber());
    }

    private void reRegisterFactory() throws Exception {
        // generate uuid
        if (StringUtils.isEmpty(getUuid())) {
            setUuid(generateUUID());
        }
        Preconditions.checkArgument(StringUtils.isNotEmpty(getUuid()));
        List<String> stopList = this.storage.registerFactory(this);
        for (String strategyName : stopList) {
            // Stop the server which will not be needed any more
            stopServer(strategyName);
        }
        // update the distribution table if needed
        assignScheduleServer();
        // make the new distribution table take effect
        adjustServerCount();
    }
    
    public IStorage getStorage() {
        if (storage == null) {
            throw new RuntimeException("TBSchedule task factory has been stopped.");
        }
        return storage;
    }
    
    /**
     * Get strategy list held by this factory
     * 
     * @return
     * @throws Exception
     */
    private List<StrategyRuntime> getStrategyListInCurrentFactory() throws Exception {
        List<StrategyRuntime> result = new ArrayList<>();
        List<String> strategyNames = this.storage.getStrategyNames();
        for (String strategyName : strategyNames) {
            StrategyRuntime runtime = this.storage.getStrategyRuntime(strategyName, getUuid());
            if (runtime != null) {
                result.add(runtime);
            }
        }
        return result;
    }

	/**
	 * 根据策略重新分配调度任务的机器
	 * @throws Exception
	 */
    private void assignScheduleServer() throws Exception {
        for (StrategyRuntime run : getStrategyListInCurrentFactory()) {
            List<StrategyRuntime> factoryList = this.storage.getRuntimesOfStrategy(run.getStrategyName());
            if (factoryList.size() == 0 || this.isLeader(this.uuid, factoryList) == false) {
                // This node is not a leader in the factory group
                continue;
            }
            Strategy scheduleStrategy = this.storage.getStrategy(run.getStrategyName());

            int[] nums = ScheduleUtil.generateSequence(factoryList.size(), scheduleStrategy.getAssignNum(),
                    scheduleStrategy.getNumOfSingleServer());
            {
                /**
                 * 检查前后有效调度表是否有差异
                 * 注意: assignTaskNumber返回的调度策略均为前面的节点先填充
                 * 目的:
                 * 1. 做到在调用器启动数量相同时，不进行重启操作（老的调度机制）
                 * 2. 在N台server中启动M个线程组，初始策略为随机选取，老机制是每次都在有序UUID表中选取最先的那个
                 *   （由于按同样的顺序决定Leader，故总分Leader上，且所有策略均如此，有负载集中）
                 * 3. 在调度分布发生变化时，正常重新分布
                 */
                List<Integer> oldNums = new ArrayList<Integer>();
                for (int i = 0; i < factoryList.size(); i++) {
                    StrategyRuntime factory = factoryList.get(i);
                    oldNums.add(factory.getRequestNum());
                }
                Collections.sort(oldNums, Collections.reverseOrder());
                boolean tableChanged = false;
                for (int i = 0; i < nums.length; i++) {
                    if (nums[i] != oldNums.get(i)) {
                        tableChanged = true;
                        break;
                    }
                }
                if (!tableChanged) {
                    continue;
                }
                Collections.shuffle(factoryList);
            }
            for (int i = 0; i < factoryList.size(); i++) {
                StrategyRuntime factory = factoryList.get(i);
                // Update request num
                updateStrategyRuntimeRequestNum(run.getStrategyName(), factory.getFactoryUuid(), nums[i]);
            }
		}
	}
    
    /**
     * Update request num in strategy's runtime info
     * 
     * @param strategyName
     * @param factoryUUID
     * @param requestNum
     * @throws Exception
     */
    private void updateStrategyRuntimeRequestNum(String strategyName, String factoryUUID, int requestNum)
            throws Exception {
        StrategyRuntime runtime = this.storage.getStrategyRuntime(strategyName, factoryUUID);
        if(runtime !=null){
            runtime.setRequestNum(requestNum);
        } else {
            runtime = new StrategyRuntime();
            runtime.setStrategyName(strategyName);
            runtime.setFactoryUuid(factoryUUID);
            runtime.setRequestNum(requestNum);
        }
        this.storage.updateRuntimeOfStrategy(runtime);
    }
	
	public boolean isLeader(String uuid, List<StrategyRuntime> factoryList) {
		try {
			long no = Long.parseLong(uuid.substring(uuid.lastIndexOf("$") + 1));
			for (StrategyRuntime server : factoryList) {
				if (no > Long.parseLong(server.getFactoryUuid().substring(
						server.getFactoryUuid().lastIndexOf("$") + 1))) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			logger.error("判断Leader出错：uuif="+uuid, e);
			return true;
		}
	}	
	
	/**
	 * Adjust the servers' count of strategy managed by this factory(Node)
	 * 
	 * @throws Exception
	 */
	private void adjustServerCount() throws Exception{
        for (StrategyRuntime run : getStrategyListInCurrentFactory()) {
            List<IStrategyTask> list = this.managerMap.get(run.getStrategyName());
            if (list == null) {
                list = new ArrayList<IStrategyTask>();
                this.managerMap.put(run.getStrategyName(), list);
            }
            int reduced = 0;
            int increased = 0;
            // Over the limit
            while (list.size() > run.getRequestNum() && list.size() > 0) {
                IStrategyTask task = list.remove(list.size() - 1);
                try {
                    task.stop(run.getStrategyName());
                    reduced ++;
                } catch (Throwable e) {
                    logger.error("Stop server failed: strategyName={}", run.getStrategyName(), e);
                }
            }
            // Not enough
            Strategy strategy = this.storage.getStrategy(run.getStrategyName());
            while (list.size() < run.getRequestNum()) {
                IStrategyTask result = this.createStrategyTask(strategy);
                if (null == result) {
                    logger.error("Create strategy failed, strategy name={}", strategy.getName());
                }
                increased ++;
                list.add(result);
            }
            if (reduced + increased > 0) {
                logger.info("Adjust server for {}, increase={}, decrese={}", run.getStrategyName(), increased, reduced);
                run.setCurrentNum(list.size());
                getStorage().updateRuntimeOfStrategy(run);
            }
        }
	}
	
	/**
	 * 终止一类任务
	 * 
	 * @param strategyName
	 * @throws Exception
	 */
	public void stopServer(String strategyName) throws Exception {
        if (strategyName == null) {
            String[] nameList = (String[]) this.managerMap.keySet().toArray(new String[0]);
            for (String name : nameList) {
                for (IStrategyTask task : this.managerMap.get(name)) {
                    try {
                        task.stop(strategyName);
                    } catch (Throwable e) {
                        logger.error("Fail to stop strategy, name={}", strategyName, e);
                    }
                }
                this.managerMap.remove(name);
            }
            if (nameList.length > 0) {
                logger.info("Stop all strategies");
            }
        } else {
            List<IStrategyTask> list = this.managerMap.get(strategyName);
            if (list != null) {
                for (IStrategyTask task : list) {
                    try {
                        task.stop(strategyName);
                    } catch (Throwable e) {
                        logger.error("Fail to stop strategy, name={}", strategyName, e);
                    }
                }
                this.managerMap.remove(strategyName);
                logger.info("Stop strategy [{}]", strategyName);
            }
		}
	}
	
    public void shutdown() throws Exception {
        lock();
        try {
            setEnableSchedule(false);
            // wait restarting to be done
            int maxWait = 10000;
            while (this.initializing && maxWait > 0) {
                Thread.sleep(100);
                maxWait -= 100;
            }
            if (this.factoryCheckerTimer != null) {
                if (this.factoryChecker != null) {
                    this.factoryChecker.cancel();
                    this.factoryChecker = null;
                }
                this.factoryCheckerTimer.cancel();
                this.factoryCheckerTimer = null;
            }
            this.stopServer(null);
            if (this.storage != null) {
                logger.info("Prepare to shut down storage");
                IStorage s = this.storage;
                this.storage = null;
                try {
                    s.clearStrategiesOfFactory(getUuid());
                    s.unregisterFactory(this);
                    s.shutdown();
                } catch (Exception e) {
                    logger.error("Shut down storage failed", e);
                }
            }
            this.uuid = null;
            logger.info("Shutdown");
        } catch (Throwable e) {
            logger.error("Shutdown failed", e);
        } finally {
            unlock();
        }
    }
	/**
	 * Restart the world
	 * 
	 * @throws Exception
	 */
	protected void restart() throws Exception {
		try {
			if (this.factoryCheckerTimer != null) {
				if(this.factoryChecker != null){
					this.factoryChecker.cancel();
					this.factoryChecker = null;
				}
				this.factoryCheckerTimer.purge();
			}
			this.stopServer(null);
			this.uuid = null;
			this.init();
		} catch (Throwable e) {
			logger.error("Restart scheduler failed.", e);
		}
    }
	public String[] getScheduleTaskDealList() {
		return applicationcontext.getBeanNamesForType(IScheduleTaskDeal.class);
	}
    
	@Override
    public void setApplicationContext(ApplicationContext aApplicationcontext) throws BeansException {
        applicationcontext = aApplicationcontext;
    }

    public Object getBean(String beanName) {
        return applicationcontext.getBean(beanName);
    }

    public String getUuid() {
        return uuid;
    }

    public String getIp() {
        return ip;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHostName() {
        return hostName;
    }

    public void setTimerInterval(int timerInterval) {
        this.factoryHeartBeatInterval = timerInterval;
    }

    /**
     * @param zkConfig
     * @deprecated in favor of
     * {@link #setConfig(ScheduleConfig)}
     */
    public void setZkConfig(Map<String, String> zkConfig) {
        setConfig(new ScheduleConfig(zkConfig));
    }

    /**
     * @return
     * @deprecated in favor of
     * {@link #getConfig()}
     */
    public Map<String, String> getZkConfig() {
        Map<String, String> zkConfig = new HashMap<>(4);
        zkConfig.put("address", getConfig().getAddress());
        zkConfig.put("rootPath", getConfig().getRootPath());
        zkConfig.put("username", getConfig().getUsername());
        zkConfig.put("password", getConfig().getPassword());
        return zkConfig;
    }
    
    /**
     * Using raw map to load config key value pairs
     * 
     * @param configMap
     * @see {@link #setConfig(ScheduleConfig)}
     */
    public void setConfigMap(Map<String, String> configMap) {
        this.config = new ScheduleConfig(configMap);
    }
    
    /**
     * @param config
     */
    public void setConfig(ScheduleConfig config) {
        this.config = config;
    }
    
    /**
     * @return
     */
    public ScheduleConfig getConfig() {
        return config;
    }
    
}
