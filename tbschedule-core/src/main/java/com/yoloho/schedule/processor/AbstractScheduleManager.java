package com.yoloho.schedule.processor;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yoloho.schedule.ScheduleManagerFactory;
import com.yoloho.schedule.interfaces.IScheduleTaskDeal;
import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.interfaces.IStrategyTask;
import com.yoloho.schedule.interfaces.ITaskProcessor;
import com.yoloho.schedule.types.InitialResult;
import com.yoloho.schedule.types.ScheduleServer;
import com.yoloho.schedule.types.StatisticsInfo;
import com.yoloho.schedule.types.Task;
import com.yoloho.schedule.types.TaskItem;
import com.yoloho.schedule.util.CronExpression;
import com.yoloho.schedule.util.ScheduleUtil;

/**
 * 1、任务调度分配器的目标：	让所有的任务不重复，不遗漏的被快速处理。
 * 2、一个Manager只管理一种任务类型的一组工作线程。
 * 3、在一个JVM里面可能存在多个处理相同任务类型的Manager，也可能存在处理不同任务类型的Manager。
 * 4、在不同的JVM里面可以存在处理相同任务的Manager 
 * 5、调度的Manager可以动态的随意增加和停止
 * 
 * 主要的职责：
 * 1、定时向集中的数据配置中心更新当前调度服务器的心跳状态
 * 2、向数据配置中心获取所有服务器的状态来重新计算任务的分配。这么做的目标是避免集中任务调度中心的单点问题。
 * 3、在每个批次数据处理完毕后，检查是否有其它处理服务器申请自己把持的任务队列，如果有，则释放给相关处理服务器。
 *  
 * 其它：
 * 	 如果当前服务器在处理当前任务的时候超时，需要清除当前队列，并释放已经把持的任务。并向控制主动中心报警。
 * 
 * @author xuannan
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractScheduleManager implements IStrategyTask {
	private static transient Logger logger = LoggerFactory.getLogger(AbstractScheduleManager.class);
	/**
	 * 用户标识不同线程的序号
	 */
    private static volatile int nextSerialNumber = 0;
 
	/**
	 * 当前线程组编号
	 */
    protected int currentSerialNumber = 0;
	private Task task;
	/**
	 * 当前调度服务的信息
	 */
	private ScheduleServer currentServer;
    private IScheduleTaskDeal taskDealBean;
    protected ITaskProcessor processor;
    StatisticsInfo statisticsInfo = new StatisticsInfo();
    
    boolean isPauseSchedule = true;
    String pauseMessage="";
    /**
     *  当前处理任务队列清单
     *  ArrayList实现不是同步的。因多线程操作修改该列表，会造成ConcurrentModificationException
     */
    protected List<TaskItem> currentTaskItemList = new CopyOnWriteArrayList<TaskItem>();
    protected boolean isNeedReloadTaskItem = true;
    
    /**
     * 向配置中心更新信息的定时器
     */
    private Timer heartBeatTimer;

    protected String startErrorInfo = null;
    
    protected boolean isStopSchedule = false;
    protected Lock registerLock = new ReentrantLock();
    
    /**
     * 运行期信息是否初始化成功
     */
    protected boolean isRuntimeInfoInitial = false;
    
    protected IStorage storage;
    
    ScheduleManagerFactory factory;
    
    /**
     * Whether current server is the leader or not
     * 
     * @return
     * @throws Exception
     */
    protected boolean isLeader() throws Exception {
        return ScheduleUtil.isLeader(
                currentServer.getUuid(), 
                storage.getServerUuidList(currentServer.getTaskName(), currentServer.getOwnSign()));
    }
    
    /**
     * 清除已经过期的OWN_SIGN的自动生成的数据
     * @param taskType 任务类型
     * @param expireDays 过期时间，以天为单位
     * @throws Exception
     */
    private void clearExpireTaskTypeRunningInfo(String taskName, double expireDays)
            throws Exception {
        List<String> list = this.storage.getRunningEntryList(taskName);
        long diff = (long) (expireDays * 24 * 3600 * 1000);
        long now = this.storage.getGlobalTime();
        for (String runningEntryName : list) {
            String ownSign = ScheduleUtil.ownsignFromRunningEntry(runningEntryName);
            List<ScheduleServer> serverList = this.storage.getServerList(taskName, ownSign);
            InitialResult result = this.storage.getInitialRunningInfoResult(taskName, ownSign);
            if (!serverList.isEmpty()) {
                continue;
            }
            if (result != null && now - result.getUpdateTime() < diff) {
                continue;
            }
            // expired
            this.storage.cleanTaskRunningInfo(taskName, ownSign);
        }
    }

    AbstractScheduleManager(ScheduleManagerFactory aFactory, String taskName, String ownSign, IStorage storage)
            throws Exception {
        this.factory = aFactory;
        this.storage = storage;
        this.currentSerialNumber = serialNumber();
        this.task = this.storage.getTask(taskName);
        logger.info("create TBScheduleManager for taskType:" + taskName);
		//清除已经过期1天的TASK,OWN_SIGN的组合。超过一天没有活动server的视为过期
        clearExpireTaskTypeRunningInfo(taskName, this.task.getExpireOwnSignInterval());

        Object dealBean = aFactory.getBean(this.task.getDealBeanName());
        if (dealBean == null) {
            throw new Exception("SpringBean " + this.task.getDealBeanName() + " 不存在");
        }
        if (dealBean instanceof IScheduleTaskDeal == false) {
            throw new Exception("SpringBean " + this.task.getDealBeanName() + " 没有实现 IScheduleTaskDeal接口");
        }
        this.taskDealBean = (IScheduleTaskDeal) dealBean;

    	if(this.task.getJudgeDeadInterval() < this.task.getHeartBeatRate() * 5){
    		throw new Exception("数据配置存在问题，死亡的时间间隔，至少要大于心跳线程的5倍。当前配置数据：JudgeDeadInterval = "
    				+ this.task.getJudgeDeadInterval() 
    				+ ",HeartBeatRate = " + this.task.getHeartBeatRate());
        }
        this.currentServer = ScheduleServer.createScheduleServer(getGlobalTime(), taskName, ownSign,
                this.task.getThreadNumber());
        this.currentServer.setManagerFactoryUUID(this.factory.getUuid());
        this.storage.registerServer(this.currentServer);
        this.heartBeatTimer = new Timer(
                this.currentServer.getRunningEntry() + "-" + this.currentSerialNumber + "-HeartBeat");
        this.heartBeatTimer.schedule(new HeartBeatTimerTask(this), new java.util.Date(System.currentTimeMillis() + 500),
                this.task.getHeartBeatRate());
        initial();
	}  
	/**
	 * 对象创建时需要做的初始化工作
	 * 
	 * @throws Exception
	 */
	public abstract void initial() throws Exception;
	public abstract void refreshScheduleServerInfo() throws Exception;
	public abstract void assignScheduleTask() throws Exception;
	public abstract List<TaskItem> getCurrentScheduleTaskItemList();
	public abstract int getTaskItemCount();
	
	@Override
    public void initialTaskParameter(String strategyName, String taskParameter) {
        logger.info("Initialize strategy(schedule): {}", strategyName);
    }

    private static synchronized int serialNumber() {
        return nextSerialNumber++;
    }

    protected int getCurrentSerialNumber() {
        return this.currentSerialNumber;
    }
	
	protected long getGlobalTime() {
	    try {
	        return this.storage.getGlobalTime();
	    } catch (Exception e) {
        }
	    return System.currentTimeMillis();
	}
	
	/**
	 * 清除内存中所有的已经取得的数据和任务队列,在心跳更新失败，或者发现注册中心的调度信息被删除
	 */
	protected void clearMemoInfo(){
		try {
			// 清除内存中所有的已经取得的数据和任务队列,在心态更新失败，或者发现注册中心的调度信息被删除
			this.currentTaskItemList.clear();
			if (this.processor != null) {
				this.processor.clearAllHasFetchData();
			}
		} finally {
			//设置内存里面的任务数据需要重新装载
			this.isNeedReloadTaskItem = true;
		}

	}
	
    public void rewriteScheduleInfo() throws Exception {
        registerLock.lock();
        try {
            if (this.isStopSchedule == true) {
                if (logger.isDebugEnabled()) {
                    logger.debug("外部命令终止调度,不在注册调度服务，避免遗留垃圾数据：" + currentServer.getUuid());
                }
                return;
            }
            // 先发送心跳信息
            if (startErrorInfo == null) {
                this.currentServer
                        .setDealInfoDesc(this.pauseMessage + ":" + this.statisticsInfo.getDealDescription());
            } else {
                this.currentServer.setDealInfoDesc(startErrorInfo);
            }
            if (!this.storage.heartbeatServer(this.currentServer)) {
                // 更新信息失败，清除内存数据后重新注册
                this.clearMemoInfo();
                this.storage.registerServer(this.currentServer);
            }
        } finally {
            registerLock.unlock();
        }
    }

	/**
	 * 开始的时候，计算第一次执行时间
	 * @throws Exception
	 */
    public void computerStart() throws Exception{
    	//只有当存在可执行队列后再开始启动队列
        boolean isRunNow = false;
        if (this.task.getPermitRunStartTime() == null) {
            isRunNow = true;
        } else {
            String tmpStr = this.task.getPermitRunStartTime();
            if (tmpStr.toLowerCase().startsWith("startrun:")) {
                isRunNow = true;
                tmpStr = tmpStr.substring("startrun:".length());
            }
            CronExpression cexpStart = new CronExpression(tmpStr);
            Date current = new Date();
            Date firstStartTime = cexpStart.getNextValidTimeAfter(current);
            this.heartBeatTimer.schedule(new PauseOrResumeScheduleTask(this, this.heartBeatTimer,
                    PauseOrResumeScheduleTask.TYPE_RESUME, tmpStr), firstStartTime);
            this.currentServer.setNextRunStartTime(ScheduleUtil.dataToString(firstStartTime));
            if (this.task.getPermitRunEndTime() == null || this.task.getPermitRunEndTime().equals("-1")) {
                this.currentServer.setNextRunEndTime("当不能获取到数据的时候pause");
            } else {
                try {
                    String tmpEndStr = this.task.getPermitRunEndTime();
                    CronExpression cexpEnd = new CronExpression(tmpEndStr);
                    Date firstEndTime = cexpEnd.getNextValidTimeAfter(firstStartTime);
                    Date nowEndTime = cexpEnd.getNextValidTimeAfter(current);
                    if (!nowEndTime.equals(firstEndTime) && current.before(nowEndTime)) {
                        isRunNow = true;
                        firstEndTime = nowEndTime;
                    }
                    this.heartBeatTimer.schedule(new PauseOrResumeScheduleTask(this, this.heartBeatTimer,
                            PauseOrResumeScheduleTask.TYPE_PAUSE, tmpEndStr), firstEndTime);
                    this.currentServer.setNextRunEndTime(ScheduleUtil.dataToString(firstEndTime));
                } catch (Exception e) {
                    logger.error("计算第一次执行时间出现异常:" + currentServer.getUuid(), e);
                    throw new Exception("计算第一次执行时间出现异常:" + currentServer.getUuid(), e);
                }
            }
        }
        if (isRunNow == true) {
            this.resume("开启服务立即启动");
        }
        this.rewriteScheduleInfo();
    }
	/**
	 * 当Process没有获取到数据的时候调用，决定是否暂时停止服务器
	 * @throws Exception
	 */
    protected boolean isContinueWhenNoData() throws Exception {
        if (this.currentTaskItemList.size() > 0 && this.task.getPermitRunStartTime() != null) {
            if (this.task.getPermitRunEndTime() == null
                    || this.task.getPermitRunEndTime().equals("-1")) {
                this.pause("No more data, pause");
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

	/**
	 * 超过运行的运行时间，暂时停止调度
	 * @throws Exception 
	 */
    protected void pause(String message) throws Exception {
        if (this.isPauseSchedule == false) {
            this.isPauseSchedule = true;
            this.pauseMessage = message;
            if (logger.isDebugEnabled()) {
                logger.debug("Pause: {}:{}", this.currentServer.getUuid(), this.statisticsInfo.getDealDescription());
            }
            if (this.processor != null) {
                this.processor.stopSchedule();
            }
            rewriteScheduleInfo();
        }
    }
	/**
	 * 处在了可执行的时间区间，恢复运行
	 * @throws Exception 
	 */
    protected void resume(String message) throws Exception {
        if (this.isPauseSchedule == true) {
            if (logger.isDebugEnabled()) {
                logger.debug("恢复调度:" + this.currentServer.getUuid());
            }
            this.isPauseSchedule = false;
            this.pauseMessage = message;
            if (this.taskDealBean != null) {
                if (this.task.getProcessorType() != null
                        && this.task.getProcessorType().equalsIgnoreCase("NOTSLEEP") == true) {
                    this.task.setProcessorType("NOTSLEEP");
                    this.processor = new TaskProcessorNotSleep(this, taskDealBean, this.statisticsInfo);
                } else {
                    this.processor = new TaskProcessorSleep(this, taskDealBean, this.statisticsInfo);
                    this.task.setProcessorType("SLEEP");
                }
            }
            rewriteScheduleInfo();
        }
    }

	/**
	 * 当服务器停止的时候，调用此方法清除所有未处理任务，清除服务器的注册信息。
	 * 也可能是控制中心发起的终止指令。
	 * 需要注意的是，这个方法必须在当前任务处理完毕后才能执行
	 * 
	 * @throws Exception 
	 */
    public void stop(String strategyName) throws Exception {
        logger.info("Stop server: {}", this.currentServer.getUuid());
        this.isPauseSchedule = false;
        if (this.processor != null) {
            this.processor.stopSchedule();
        } else {
            this.unregisterScheduleServer();
        }
    }
	
	/**
     * 只应该在Processor中调用
     * 
     * @throws Exception
     */
    protected void unregisterScheduleServer() throws Exception {
        registerLock.lock();
        try {
            if (this.processor != null) {
                this.processor = null;
            }
            if (this.isPauseSchedule == true) {
                // 是暂停调度，不注销Manager自己
                return;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("注销服务器 ：" + this.currentServer.getUuid());
            }
            this.isStopSchedule = true;
            // 取消心跳TIMER
            this.heartBeatTimer.cancel();
            // 从配置中心注销自己
            this.storage.unregisterServer(this.currentServer.getTaskName(),
                    this.currentServer.getOwnSign(), this.currentServer.getUuid());
        } finally {
            registerLock.unlock();
        }
    }

    protected Task getTask() {
        return task;
    }

    protected ScheduleServer currentServer() {
        return this.currentServer;
    }

}