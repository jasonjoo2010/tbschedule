package com.taobao.pamirs.schedule.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.google.common.base.Preconditions;
import com.taobao.pamirs.schedule.taskmanager.ScheduleConfig;
import com.yoloho.schedule.interfaces.IScheduleTaskDeal;
import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.interfaces.IStrategyTask;
import com.yoloho.schedule.processor.TBScheduleManagerStatic;
import com.yoloho.schedule.types.Strategy;
import com.yoloho.schedule.types.StrategyKind;
import com.yoloho.schedule.util.ScheduleUtil;

/**
 * 调度服务器构造器
 * 
 * @author xuannan
 * 
 */
public class TBScheduleManagerFactory implements ApplicationContextAware {

	protected static transient Logger logger = LoggerFactory.getLogger(TBScheduleManagerFactory.class);
	
    private ScheduleConfig config;
	
	protected IStorage storage;

	/**
	 * Whether should really do schedule or just as an operator (console client)
	 */
	private boolean enableSchedule = true;
	private int timerInterval = 2000;
	/**
	 * ManagerFactoryTimerTask上次执行的时间戳。<br/>
	 * zk环境不稳定，可能导致所有task自循环丢失，调度停止。<br/>
	 * 外层应用，通过jmx暴露心跳时间，监控这个tbschedule最重要的大循环。<br/>
	 */
	public volatile long timerTaskHeartBeatTS = System.currentTimeMillis();
	
    private Map<String, List<IStrategyTask>> managerMap = new ConcurrentHashMap<String, List<IStrategyTask>>();
	
	private ApplicationContext			applicationcontext;	
	private String uuid;
	private String ip;
	private String hostName;

	private Timer timer;
	private ManagerFactoryTimerTask timerTask;
	protected Lock  lock = new ReentrantLock();
    
	volatile String  errorMessage = "No config Zookeeper connect infomation";
	private InitialThread initialThread;
	
    public TBScheduleManagerFactory() {
        this.ip = ScheduleUtil.getLocalIP();
        this.hostName = ScheduleUtil.getLocalHostName();
    }
    
    public String getErrorMessage() {
        return errorMessage;
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
        if (this.initialThread != null) {
            this.initialThread.stopThread();
        }
        this.lock.lock();
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
                this.storage = it.next();
                this.storage.init(getConfig());
            }
            
            this.errorMessage = "Initializing ......";
            initialThread = new InitialThread(this);
            initialThread.setName("TBScheduleManagerFactory-initialThread");
            initialThread.start();
        } finally {
            this.lock.unlock();
        }
    }
	
	public void reInit(ScheduleConfig config) throws Exception{
        if (isEnableSchedule() || this.timer != null || this.managerMap.size() > 0) {
            throw new Exception("Can not reinit due to running jobs");
        }
        setConfig(config);
        init();
	}
	
    public void init(Properties p) throws Exception {
        setConfig(new ScheduleConfig(p));
        init();
	}
    
    public void init(ScheduleConfig config) throws Exception {
        setConfig(config);
        init();
    }
    
    /**
     * 在Zk状态正常后回调数据初始化
     * @throws Exception
     */
    public void initialData() throws Exception {
        if (isEnableSchedule()) {
            // 注册调度管理器
            this.storage.registerFactory(this);
            if (timer == null) {
                timer = new Timer("TBScheduleManagerFactory-Timer");
            }
            if (timerTask == null) {
                timerTask = new ManagerFactoryTimerTask(this);
                timer.schedule(timerTask, 2000, this.timerInterval);
            }
        }
    }

	/**
	 * 创建调度服务器
	 * @param baseTaskType
	 * @param ownSign
	 * @return
	 * @throws Exception
	 */
    public IStrategyTask createStrategyTask(Strategy strategy) throws Exception {
        IStrategyTask result = null;
        try {
            if (StrategyKind.Schedule == strategy.getKind()) {
                String baseTaskType = ScheduleUtil.taskNameFromRunningEntry(strategy.getTaskName());
                String ownSign = ScheduleUtil.ownsignFromRunningEntry(strategy.getTaskName());
                result = new TBScheduleManagerStatic(this, baseTaskType, ownSign, storage);
            } else if (StrategyKind.Java == strategy.getKind()) {
                result = (IStrategyTask) Class.forName(strategy.getTaskName()).newInstance();
                result.initialTaskParameter(strategy.getName(), strategy.getTaskParameter());
            } else if (StrategyKind.Bean == strategy.getKind()) {
                result = (IStrategyTask) this.getBean(strategy.getTaskName());
                result.initialTaskParameter(strategy.getName(), strategy.getTaskParameter());
            }
        } catch (Exception e) {
            logger.error("strategy 获取对应的java or bean 出错,schedule并没有加载该任务,请确认" + strategy.getName(), e);
        }
        return result;
    }

    public void refresh() throws Exception {
        this.lock.lock();
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
                    stopServer(null); // 停止所有的调度任务
                    this.storage.unregisterFactory(getUuid());
                } finally {
                    reRegisterManagerFactory();
                }
            } else if (allowRun == false) {
                stopServer(null); // 停止所有的调度任务
                this.storage.unregisterFactory(getUuid());
            } else {
                reRegisterManagerFactory();
            }
        } finally {
            this.lock.unlock();
        }
    }

    public void reRegisterManagerFactory() throws Exception {
        // 重新分配调度器
        List<String> stopList = this.storage.registerFactory(this);
        for (String strategyName : stopList) {
            this.stopServer(strategyName);
        }
        this.assignScheduleServer();
        this.reRunScheduleServer();
    }
    
    public IStorage getStorage() {
        return storage;
    }
    
    private List<ScheduleStrategyRuntime> getStrategyRunntimeByUUID(String factoryUUID) throws Exception {
        List<ScheduleStrategyRuntime> result = new ArrayList<>();
        List<String> strategyNames = this.storage.getStrategyNames();
        for (String strategyName : strategyNames) {
            ScheduleStrategyRuntime runtime = this.storage.getStrategyRuntime(strategyName, factoryUUID);
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
    public void assignScheduleServer() throws Exception {
        for (ScheduleStrategyRuntime run : getStrategyRunntimeByUUID(getUuid())) {
            List<ScheduleStrategyRuntime> factoryList = this.storage.getStrategyRuntimes(run.getStrategyName());
            if (factoryList.size() == 0 || this.isLeader(this.uuid, factoryList) == false) {
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
                    ScheduleStrategyRuntime factory = factoryList.get(i);
                    oldNums.add(factory.getRequestNum());
                }
                Collections.sort(oldNums, Collections.reverseOrder());
                boolean needReschedule = false;
                for (int i = 0; i < nums.length; i++) {
                    if (nums[i] != oldNums.get(i)) {
                        needReschedule = true;
                        break;
                    }
                }
                /*System.out.println("need: " + needReschedule);
                System.out.println("old: " + oldNums.toString());
                System.out.print("new:");
                for (int i : nums) {
                    System.out.print(" " + i);
                }
                System.out.println();*/
                if (!needReschedule) {
                    continue;
                }
                Collections.shuffle(factoryList);
            }
            for (int i = 0; i < factoryList.size(); i++) {
                ScheduleStrategyRuntime factory = factoryList.get(i);
                // Update request num
                updateStrategyRuntimeRequestNum(run.getStrategyName(), factory.getUuid(), nums[i]);
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
        ScheduleStrategyRuntime runtime = this.storage.getStrategyRuntime(strategyName, factoryUUID);
        if(runtime !=null){
            runtime.setRequestNum(requestNum);
        } else {
            runtime = new ScheduleStrategyRuntime();
            runtime.setStrategyName(strategyName);
            runtime.setUuid(factoryUUID);
            runtime.setRequestNum(requestNum);
            runtime.setMessage("");
        }
        this.storage.updateStrategyRuntime(runtime);
    }
	
	public boolean isLeader(String uuid, List<ScheduleStrategyRuntime> factoryList) {
		try {
			long no = Long.parseLong(uuid.substring(uuid.lastIndexOf("$") + 1));
			for (ScheduleStrategyRuntime server : factoryList) {
				if (no > Long.parseLong(server.getUuid().substring(
						server.getUuid().lastIndexOf("$") + 1))) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			logger.error("判断Leader出错：uuif="+uuid, e);
			return true;
		}
	}	
	
	public void reRunScheduleServer() throws Exception{
		for (ScheduleStrategyRuntime run : getStrategyRunntimeByUUID(getUuid())) {
			List<IStrategyTask> list = this.managerMap.get(run.getStrategyName());
			if(list == null){
				list = new ArrayList<IStrategyTask>();
				this.managerMap.put(run.getStrategyName(),list);
			}
			while(list.size() > run.getRequestNum() && list.size() >0){
				IStrategyTask task  =  list.remove(list.size() - 1);
					try {
						task.stop(run.getStrategyName());
					} catch (Throwable e) {
						logger.error("注销任务错误：strategyName=" + run.getStrategyName(), e);
					}
				}
		   //不足，增加调度器
		   Strategy strategy = this.storage.getStrategy(run.getStrategyName());
		   while(list.size() < run.getRequestNum()){
			   IStrategyTask result = this.createStrategyTask(strategy);
			   if(null==result){
				   logger.error("strategy 对应的配置有问题。strategy name="+strategy.getName());
			   }
			   list.add(result);
		    }
		}
	}
	
	/**
	 * 终止一类任务
	 * 
	 * @param taskType
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
                        logger.error("注销任务错误：strategyName=" + strategyName, e);
                    }
                }
                this.managerMap.remove(name);
            }
        } else {
            List<IStrategyTask> list = this.managerMap.get(strategyName);
            if (list != null) {
                for (IStrategyTask task : list) {
                    try {
                        task.stop(strategyName);
                    } catch (Throwable e) {
                        logger.error("注销任务错误：strategyName=" + strategyName, e);
                    }
                }
                this.managerMap.remove(strategyName);
            }
		}
	}
	/**
	 * 停止所有调度资源(shut down)
	 */
    public void stopAll() throws Exception {
        try {
            lock.lock();
            setEnableSchedule(false);
            if (this.initialThread != null) {
                this.initialThread.stopThread();
            }
            if (this.timer != null) {
                if (this.timerTask != null) {
                    this.timerTask.cancel();
                    this.timerTask = null;
                }
                this.timer.cancel();
                this.timer = null;
            }
            this.stopServer(null);
            if (this.storage != null) {
                try {
                    this.storage.shutdown();
                } catch (Exception e) {
                    logger.error("Shut down storage failed", e);
                }
                this.storage = null;
            }
            this.uuid = null;
            logger.info("stopAll 停止服务成功！");
        } catch (Throwable e) {
            logger.error("stopAll 停止服务失败：" + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }
	/**
	 * 重启所有的服务
	 * @throws Exception
	 */
	public void reStart() throws Exception {
		try {
			if (this.timer != null) {
				if(this.timerTask != null){
					this.timerTask.cancel();
					this.timerTask = null;
				}
				this.timer.purge();
			}
			this.stopServer(null);
			this.uuid = null;
			this.init();
		} catch (Throwable e) {
			logger.error("重启服务失败：" + e.getMessage(), e);
		}
    }
	public String[] getScheduleTaskDealList() {
		return applicationcontext.getBeanNamesForType(IScheduleTaskDeal.class);
	}
    
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
        this.timerInterval = timerInterval;
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

class ManagerFactoryTimerTask extends java.util.TimerTask {
    private static transient Logger log = LoggerFactory.getLogger(ManagerFactoryTimerTask.class);
    TBScheduleManagerFactory factory;
    int count = 0;

    public ManagerFactoryTimerTask(TBScheduleManagerFactory aFactory) {
        this.factory = aFactory;
    }

    public void run() {
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            if (this.factory.storage.test() == false) {
                if (count > 5) {
                    log.error("Storage status failed for several times, try to restart......");
                    this.factory.reStart();
                } else {
                    count ++;
                }
            } else {
                // reset the timer once successfully
                // refresh and refresh to make sure everything up to date
                count = 0;
                this.factory.refresh();
            }
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            factory.timerTaskHeartBeatTS = System.currentTimeMillis();
        }
    }
}

class InitialThread extends Thread {
    private static transient Logger log = LoggerFactory.getLogger(InitialThread.class);
    TBScheduleManagerFactory factory;
    boolean isStop = false;

    public InitialThread(TBScheduleManagerFactory aFactory) {
        this.factory = aFactory;
    }

    public void stopThread() {
        this.isStop = true;
    }

    @Override
    public void run() {
        boolean needRestart = false;
        factory.lock.lock();
        try {
            int count = 0;
            final int interval = 20;
            while (factory.storage.test() == false) {
                count ++;
                if (count % 50 == 0) {
                    factory.errorMessage = "Storage still initializing ...... spendTime: " + (count * interval) + "ms";
                    log.error(factory.errorMessage);
                }
                Thread.sleep(interval);
                if (this.isStop == true) {
                    return;
                }
            }
            if (this.isStop == true) {
                return;
            }
            factory.initialData();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            /**
             * 这里一般意味着initialData()出错
             * check成功但初始化失败，说明连接又出问题了，这里继续重试重启状态
             */
            needRestart = true;
        } finally {
            factory.lock.unlock();
        }
        while (needRestart) {
            log.error("Initializing failed and preparing to restart.....");
            try {
                factory.reStart();
                needRestart = false;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                try {
                    sleep(20);
                } catch (InterruptedException e1) {
                }
            }
        }
    }

}