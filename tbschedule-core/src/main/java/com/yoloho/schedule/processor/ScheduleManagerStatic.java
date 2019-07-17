package com.yoloho.schedule.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yoloho.schedule.ScheduleManagerFactory;
import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.types.TaskItemRuntime;
import com.yoloho.schedule.types.ScheduleServer;
import com.yoloho.schedule.types.TaskItem;
import com.yoloho.schedule.util.ScheduleUtil;

public class ScheduleManagerStatic extends AbstractScheduleManager {
	private static transient Logger logger = LoggerFactory.getLogger(ScheduleManagerStatic.class);
    /**
     * 总的任务数量
     */
    protected int taskItemCount = 0;

    protected long lastFetchVersion = -1;
    /**
     * 最近一起重新装载调度任务的时间。
     * 当前实际  - 上此装载时间  > intervalReloadTaskItemList，则向配置中心请求最新的任务分配情况
     */
    private long lastReloadTaskItemListTime = 0;
    private final Object NeedReloadTaskItemLock = new Object();

    public ScheduleManagerStatic(ScheduleManagerFactory factory, String taskName, String ownSign,
            IStorage storage) throws Exception {
        super(factory, taskName, ownSign, storage);
    }

    public void initialRunningInfo() throws Exception {
        cleanExpiredServer(getTask().getJudgeDeadInterval());
        if (isLeader()) {
            // 是第一次启动，先清所有的垃圾数据
            this.storage.initTaskRunningInfo(this.currentServer().getTaskName(),
                    this.currentServer().getOwnSign());
            this.storage.initTaskItemsRunningInfo(this.currentServer().getTaskName(),
                    this.currentServer().getOwnSign(), this.currentServer().getUuid());
        }
    }

    public void initial() throws Exception {
        new Thread(this.currentServer().getRunningEntry() + "-" + this.currentSerialNumber + "-StartProcess") {
            @SuppressWarnings("static-access")
            public void run() {
                try {
                    logger.info("Fetching task items for {}", currentServer().getUuid());
                    while (isRuntimeInfoInitial == false) {
                        if (isStopSchedule == true) {
                            logger.debug("Stop scheduling due to stop flag: {}", currentServer().getUuid());
                            return;
                        }
                        //logger.info("isRuntimeInfoInitial={}", isRuntimeInfoInitial);
                        try {
                            initialRunningInfo();
                            isRuntimeInfoInitial = storage.isInitialRunningInfoSuccess(
                                    currentServer().getTaskName(), currentServer().getOwnSign());
                        } catch (Throwable e) {
                            // ignore exceptions and retry
                            logger.error(e.getMessage(), e);
                        }
                        if (isRuntimeInfoInitial == false) {
                            sleep(1000);
                        }
                    }
                    int count = 0;
                    lastReloadTaskItemListTime = storage.getGlobalTime();
                    while (getCurrentScheduleTaskItemListNow().size() <= 0) {
                        if (isStopSchedule == true) {
                            logger.debug("Stop scheduling due to stop flag: {}", currentServer().getUuid());
                            return;
                        }
                        //logger.info("Try to fetch any task item: {}", count) ;
                        Thread.currentThread().sleep(1000);
                        count = count + 1;
                    }
                    String tmpStr = "TaskItemDefine:";
                    for (int i = 0; i < currentTaskItemList.size(); i++) {
                        if (i > 0) {
                            tmpStr = tmpStr + ",";
                        }
                        tmpStr = tmpStr + currentTaskItemList.get(i);
                    }
                    logger.info("Got task item(s), begin to schedule {} of {}", tmpStr, currentServer().getUuid());

                    // 任务总量
                    taskItemCount = storage.getRunningTaskItems(currentServer().getTaskName(),
                            currentServer().getOwnSign()).size();
                    // 只有在已经获取到任务处理队列后才开始启动任务处理器
                    computerStart();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    String str = e.getMessage();
                    if (str.length() > 300) {
                        str = str.substring(0, 300);
                    }
                    startErrorInfo = "StartProcess error with " + str;
                }
            }
        }.start();
    }
	
	/**
	 * Send heartbeat to center (storage)
	 * <p>
	 * If expired for this server it cannot do the heartbeat, instead,
	 * it should reregister as <b>a new server</b>
	 * 
	 * @throws Exception 
	 */
	@Override
    public void refreshScheduleServerInfo() throws Exception {
        try {
            rewriteScheduleInfo();
            // if uninitialized wait 2 seconds most
            int timeout = 2000;
            while (this.isRuntimeInfoInitial == false) {
                Thread.sleep(100);
                timeout -= 100;
                if (timeout <= 0) {
                    return;
                }
            }

            // try to reassign
            assignScheduleTask();

            // 判断是否需要重新加载任务队列，避免任务处理进程不必要的检查和等待
            boolean tmpBoolean = this.isNeedReLoadTaskItemList();
            if (tmpBoolean != this.isNeedReloadTaskItem) {
                // 只要不相同，就设置需要重新装载，因为在心跳异常的时候，做了清理队列的事情，恢复后需要重新装载。
                synchronized (NeedReloadTaskItemLock) {
                    this.isNeedReloadTaskItem = true;
                }
                rewriteScheduleInfo();
            }

            if (this.isPauseSchedule == true || this.processor != null && processor.isSleeping() == true) {
                // 如果服务已经暂停了，则需要重新定时更新 cur_server 和 req_server
                // 如果服务没有暂停，一定不能调用的
                this.getCurrentScheduleTaskItemListNow();
            }
        } catch (Throwable e) {
            // 清除内存中所有的已经取得的数据和任务队列,避免心跳线程失败时候导致的数据重复
            this.clearMemoInfo();
            if (e instanceof Exception) {
                throw (Exception) e;
            } else {
                throw new Exception(e.getMessage(), e);
            }
        }
    }

	/**
	 * 在leader重新分配任务，在每个server释放原来占有的任务项时，都会修改这个版本号
	 * @return
	 * @throws Exception
	 */
	private boolean isNeedReLoadTaskItemList() throws Exception{
        return this.lastFetchVersion < this.storage.getAllServerReload(this.currentServer().getTaskName(),
                this.currentServer().getOwnSign());
	}
	
	/**
     * 判断某个任务对应的线程组是否处于僵尸状态。 true 表示有线程组处于僵尸状态。需要告警。
     * 
     * @param runningEntry
     * @param serverList
     * @return
     * @throws Exception
     */
    private boolean isExistZombieServ(String runningEntry, List<ScheduleServer> serverList) throws Exception {
        boolean exist = false;
        for (ScheduleServer server : serverList) {
            if (this.storage.getGlobalTime()
                    - server.getHeartBeatTime().getTime() > getTask().getHeartBeatRate() * 40) {
                logger.error("Detect zombie server! server={}, type={}", server.getUuid(), runningEntry);
                exist = true;
            }
        }
        return exist;
    }
    
    /**
     * Release all the task items current server may operated which maybe held by invalid servers
     * 
     * @param taskType
     * @param serverList
     * @return
     * @throws Exception
     */
    private int clearTaskItemsHeldByInvalidServer() throws Exception {
        List<String> uuidList = this.storage.getServerUuidList(this.currentServer().getTaskName(),
                this.currentServer().getOwnSign());
        List<TaskItemRuntime> taskItemList = this.storage.getRunningTaskItems(currentServer().getTaskName(),
                currentServer().getOwnSign());
        int result = 0;
        for (TaskItemRuntime item : taskItemList) {
            if (StringUtils.isNotEmpty(item.getCurrentScheduleServer())) {
                if (!uuidList.contains(item.getCurrentScheduleServer())) {
                    // invalid server found
                    this.storage.updateTaskItemCurrentServer(currentServer().getTaskName(),
                            currentServer().getOwnSign(), item.getTaskItem(), "");
                    logger.info("Clear invalid server's task items: {} -> {}", item.getTaskItem(),
                            item.getCurrentScheduleServer());
                    result++;
                }
            } else {
                result = result + 1;
            }
        }
        return result;
    }
    
    private void assignTaskItem(int maxNumOfOneServer)
            throws Exception {
        List<String> serverList = this.storage.getServerUuidList(this.currentServer().getTaskName(),
                this.currentServer().getOwnSign());
        String taskName = currentServer().getTaskName();
        String ownSign = currentServer().getOwnSign();
        String currentUuid = currentServer().getUuid();
        //设置初始化成功标准，避免在leader转换的时候，新增的线程组初始化失败
        // flag success first
        this.storage.setInitialRunningInfoSuccess(taskName, ownSign, currentUuid);
        if (logger.isDebugEnabled()) {
            logger.debug("{}: Begin to distribute task", currentUuid);
        }
        if (serverList.size() <= 0) {
            // 在服务器动态调整的时候，可能出现服务器列表为空的清空
            return;
        }
        List<TaskItemRuntime> taskItemList = this.storage.getRunningTaskItems(taskName, ownSign);
        // 20150323 有些任务分片，业务方其实是用数字的字符串排序的。优先以数字进行排序，否则以字符串排序
        Collections.sort(taskItemList, new Comparator<TaskItemRuntime>() {
            public int compare(TaskItemRuntime u1, TaskItemRuntime u2) {
                if (StringUtils.isNumeric(u1.getTaskItem()) && StringUtils.isNumeric(u2.getTaskItem())) {
                    int iU1 = Integer.parseInt(u1.getTaskItem());
                    int iU2 = Integer.parseInt(u2.getTaskItem());
                    if (iU1 == iU2) {
                        return 0;
                    } else if (iU1 > iU2) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else {
                    return u1.getTaskItem().compareTo(u2.getTaskItem());
                }
            }
        });
        int unModifyCount = 0;
        int[] taskNums = ScheduleUtil.generateSequence(serverList.size(), taskItemList.size(), maxNumOfOneServer);
        int point = 0;
        int count = 0;
        String NO_SERVER_DEAL = "NO SERVER";
        for (int i = 0; i < taskItemList.size(); i++) {
            TaskItemRuntime item = taskItemList.get(i);
            if (point < serverList.size() && i >= count + taskNums[point]) {
                count = count + taskNums[point];
                point = point + 1;
            }
            String serverName = NO_SERVER_DEAL;
            if (point < serverList.size()) {
                serverName = serverList.get(point);
            }
            if (StringUtils.isEmpty(item.getCurrentScheduleServer()) || StringUtils.equals(item.getCurrentScheduleServer(), NO_SERVER_DEAL)) {
                this.storage.updateTaskItemCurrentServer(taskName, ownSign, item.getTaskItem(), serverName);
                this.storage.updateTaskItemRequestServer(taskName, ownSign, item.getTaskItem(), "");
            } else if (StringUtils.equals(item.getCurrentScheduleServer(), serverName) 
                    && StringUtils.isEmpty(item.getRequestScheduleServer())) {
                // Do nothing
                unModifyCount ++;
            } else {
                this.storage.updateTaskItemRequestServer(taskName, ownSign, item.getTaskItem(), serverName);
            }
        }

        if (unModifyCount < taskItemList.size()) { // reload configuration
            logger.info("Request all nodes to reload configuration on {}${}, currentUuid {}", taskName, ownSign, currentUuid);
            this.storage.requestAllServerReload(taskName, ownSign);
        }
        if (logger.isDebugEnabled()) {
            StringBuffer buffer = new StringBuffer();
            for (TaskItemRuntime taskItem : this.storage.getRunningTaskItems(taskName, ownSign)) {
                buffer.append("\n").append(taskItem.toString());
            }
            logger.debug(buffer.toString());
        }
    }
    
	/**
	 * 根据当前调度服务器的信息，重新计算分配所有的调度任务
	 * 任务的分配是需要加锁，避免数据分配错误。为了避免数据锁带来的负面作用，通过版本号来达到锁的目的
	 * 
	 * 1、获取任务状态的版本号
	 * 2、获取所有的服务器注册信息和任务队列信息
	 * 3、清除已经超过心跳周期的服务器注册信息
	 * 3、重新计算任务分配
	 * 4、更新任务状态的版本号【乐观锁】
	 * 5、根系任务队列的分配信息
	 * @throws Exception 
	 */
	public void assignScheduleTask() throws Exception {
	    cleanExpiredServer(getTask().getJudgeDeadInterval());
	    if (!isLeader()) {
	        if (logger.isDebugEnabled()) {
	            logger.debug("{}: It's not the Leader, skip", this.currentServer().getUuid());
	        }
	        return;
	    }
        clearTaskItemsHeldByInvalidServer();
        assignTaskItem(getTask().getMaxTaskItemsOfOneThreadGroup());
	}	
	/**
	 * 重新加载当前服务器的任务队列
	 * 1、释放当前服务器持有，但有其它服务器进行申请的任务队列
	 * 2、重新获取当前服务器的处理队列
	 * 
	 * 为了避免此操作的过度，阻塞真正的数据处理能力。系统设置一个重新装载的频率。例如1分钟
	 * 
	 * 特别注意：
	 *   此方法的调用必须是在当前所有任务都处理完毕后才能调用，否则是否任务队列后可能数据被重复处理
	 */
	
    public List<TaskItem> getCurrentScheduleTaskItemList() {
        try {
            if (this.isNeedReloadTaskItem == true) {
                // 特别注意：需要判断数据队列是否已经空了，否则可能在队列切换的时候导致数据重复处理
                // 主要是在线程不休眠就加载数据的时候一定需要这个判断
                if (this.processor != null) {
                    while (this.processor.hasRemainedTask()) {
                        Thread.sleep(50);
                    }
                }
                // 真正开始处理数据
                synchronized (NeedReloadTaskItemLock) {
                    this.getCurrentScheduleTaskItemListNow();
                    this.isNeedReloadTaskItem = false;
                }
            }
            this.lastReloadTaskItemListTime = this.storage.getGlobalTime();
            return this.currentTaskItemList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
	
	/**
	 * Get current schedule server's task items
	 * 
	 * @return
	 * @throws Exception
	 */
	private List<TaskItem> getTaskItemsShouldScheduled() throws Exception {
        List<TaskItemRuntime> taskItems = this.storage.getRunningTaskItems(currentServer().getTaskName(),
                currentServer().getOwnSign());
        List<TaskItem> result = new ArrayList<TaskItem>();
        for (TaskItemRuntime item : taskItems) {
            if (StringUtils.equals(currentServer().getUuid(), item.getCurrentScheduleServer())) {
                // current server
                result.add(new TaskItem(item.getTaskItem(), item.getDealParameter()));
            }
        }
        return result;
    }
	
	/**
	 * @throws Exception
	 */
	private void releaseTaskItemsIfNeeded() throws Exception {
        int released = this.storage.releaseTaskItemByServer(currentServer().getTaskName(),
                currentServer().getOwnSign(), currentServer().getUuid());
        if (released > 0) { // Request to reload
            this.storage.requestAllServerReload(currentServer().getTaskName(),
                    currentServer().getOwnSign());
        }
    }
	
	//由于上面在数据执行时有使用到synchronized ，但是心跳线程并没有对应加锁。
	//所以在此方法上加一下synchronized。20151015
	private synchronized List<TaskItem> getCurrentScheduleTaskItemListNow() throws Exception {
		//如果已经稳定了，理论上不需要加载去扫描所有的叶子结点
		//20151019 by kongxuan.zlj
        try {
            List<ScheduleServer> serverList = this.storage.getServerList(this.currentServer().getTaskName(),
                    this.currentServer().getOwnSign());
            // server下面的机器节点的运行时环境是否在刷新，如果
            isExistZombieServ(this.currentServer().getRunningEntry(), serverList);
        } catch (Exception e) {
            logger.error("zombie serverList exists", e);
        }
//		
		
		//获取最新的版本号
        this.lastFetchVersion = this.storage.getAllServerReload(this.currentServer().getTaskName(),
                this.currentServer().getOwnSign());
        logger.debug("this.currentServer().getTaskType()={},  need reload={}", this.currentServer().getRunningEntry(),
                isNeedReloadTaskItem);
		try{
			// Release task items if any
		    releaseTaskItemsIfNeeded();
			//重新查询当前服务器能够处理的队列
			//为了避免在休眠切换的过程中出现队列瞬间的不一致，先清除内存中的队列
			this.currentTaskItemList.clear();
            this.currentTaskItemList = getTaskItemsShouldScheduled();
			
			//如果超过10个心跳周期还没有获取到调度队列，则报警
			if(this.currentTaskItemList.size() ==0 && 
					storage.getGlobalTime() - this.lastReloadTaskItemListTime
                    > getTask().getHeartBeatRate() * 20) {
                StringBuffer buf = new StringBuffer();
                buf.append("调度服务器");
                buf.append(this.currentServer().getUuid());
                buf.append("[TASK_TYPE=");
                buf.append(this.currentServer().getRunningEntry());
                buf.append("]自启动以来，超过20个心跳周期，还 没有获取到分配的任务队列;");
                buf.append("  currentTaskItemList.size() =" + currentTaskItemList.size());
                buf.append(", scheduleCenter.getSystemTime()=" + storage.getGlobalTime());
                buf.append(", lastReloadTaskItemListTime=" + lastReloadTaskItemListTime);
                buf.append(", taskTypeInfo.getHeartBeatRate()=" + getTask().getHeartBeatRate() * 10);
                logger.warn(buf.toString());
			}

            if (this.currentTaskItemList.size() > 0) {
                // 更新时间戳
                this.lastReloadTaskItemListTime = storage.getGlobalTime();
            }

            return this.currentTaskItemList;
        } catch (Throwable e) {
            this.lastFetchVersion = -1; // 必须把把版本号设置小，避免任务加载失败
            if (e instanceof Exception) {
                throw (Exception) e;
            } else {
                throw new Exception(e);
            }
        }
	}
	
	public int getTaskItemCount(){
		 return this.taskItemCount;
	}
	
	private int cleanExpiredServer(long expirationInMillis) throws Exception {
	    String taskName = currentServer().getTaskName();
	    String ownSign = currentServer().getOwnSign();
        List<ScheduleServer> list = this.storage.getServerList(taskName, ownSign);
        long now = this.storage.getGlobalTime();
        int clean_cnt = 0;
        for (ScheduleServer server : list) {
            if (now - server.getHeartBeatTime().getTime() > expirationInMillis) {
                this.storage.unregisterServer(taskName, ownSign, server.getUuid());
                clean_cnt ++;
            }
        }
        return clean_cnt;
    }

}
