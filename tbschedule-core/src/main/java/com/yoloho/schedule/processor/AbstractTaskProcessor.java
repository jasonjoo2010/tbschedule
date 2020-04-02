package com.yoloho.schedule.processor;

import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yoloho.schedule.interfaces.IScheduleTaskDeal;
import com.yoloho.schedule.interfaces.IScheduleTaskDealMulti;
import com.yoloho.schedule.interfaces.IScheduleTaskDealSingle;
import com.yoloho.schedule.interfaces.ITaskProcessor;
import com.yoloho.schedule.types.StatisticsInfo;
import com.yoloho.schedule.types.Task;
import com.yoloho.schedule.types.TaskItem;

public abstract class AbstractTaskProcessor<T> implements ITaskProcessor, Runnable {
    private final static Logger logger = LoggerFactory.getLogger(AbstractTaskProcessor.class.getSimpleName());
    private List<Thread> threadList = new CopyOnWriteArrayList<Thread>();
    private AbstractScheduleManager manager;
    private boolean isSleeping = false;
    private boolean isStopSchedule = false;
    private boolean isStopped = false; // Whether really stopped
    private IScheduleTaskDeal<T> taskDealBean;
    private boolean isMulti = false;
    private Task task;
    private StatisticsInfo statisticsInfo;
    private final List<T> taskList = new CopyOnWriteArrayList<T>(); // XXX Consider to use Queue
    private Lock lockLoadData = new ReentrantLock();

    public AbstractTaskProcessor(AbstractScheduleManager manager, IScheduleTaskDeal<T> taskDealBean,
            StatisticsInfo statisticsInfo) throws Exception {
        this.manager = manager;
        this.task = manager.getTask();
        this.taskDealBean = taskDealBean;
        this.statisticsInfo = statisticsInfo;
        if (this.taskDealBean instanceof IScheduleTaskDealSingle<?>) {
            if (this.task.getExecuteNumber() > 1) {
                this.task.setExecuteNumber(1);
            }
            isMulti = false;
        } else {
            isMulti = true;
        }
        // check parameters
        if (this.task.getFetchDataNumber() < this.task.getThreadNumber() * 3) {
            logger.warn("Maybe performance is bad due to fetchnum < threadnum * 3");
        }
        // init threads
        for (int i = 0; i < this.task.getThreadNumber(); i++) {
            createThread(i);
        }
    }
    
    private void createThread(int index) {
        Thread thread = new Thread(this);
        threadList.add(thread);
        String threadName = this.manager.currentServer().getRunningEntry() + "-"
                + this.manager.getCurrentSerialNumber() + "-exe" + index;
        thread.setName(threadName);
        thread.start();
    }
    
    /**
     * @return true when unregistered
     */
    protected boolean releaseCurrentThread() {
        synchronized (this.threadList) {
            this.threadList.remove(Thread.currentThread());
            if (this.threadList.size() == 0) {
                try {
                    this.manager.unregisterScheduleServer();
                    return true;
                } catch (Exception e) {
                    logger.error("Unregister server faield", e);
                }
            }
            return false;
        }
    }
    
    @Override
    public boolean hasRemainedTask() {
        return this.taskList.size() > 0;
    }

    @Override
    public boolean isSleeping() {
        return this.isSleeping;
    }

    protected void sleep(long millis) {
        this.isSleeping = true;
        try {
            while (millis > 0 && !isStopSchedule) {
                if (millis >= 2000) {
                    Thread.sleep(2000);
                    millis -= 2000;
                } else {
                    Thread.sleep(millis);
                    millis = 0;
                }
            }
        } catch (InterruptedException e) {
        }
        this.isSleeping = false;
    }

    /**
     * 需要注意的是，调度服务器从配置中心注销的工作，必须在所有线程退出的情况下才能做
     * 
     * @throws Exception
     */
    @Override
    public void stopSchedule() throws Exception {
        // 设置停止调度的标志,调度线程发现这个标志，执行完当前任务后，就退出调度
        this.isStopSchedule = true;
        // 清除所有未处理任务,但已经进入处理队列的，需要处理完毕
        this.taskList.clear();
        int maxWait = 3000;
        while (maxWait > 0 && isStopped == false) {
            Thread.sleep(100);
            maxWait -= 100;
        }
        logger.info("Stop schedule {}", getTask().getName());
    }

    protected boolean isStopSchedule() {
        return isStopSchedule;
    }
    
    /**
     * Whether really stopped
     */
    protected void setStopped() {
        isStopped = true;
    }

    @Override
    public void clearAllHasFetchData() {
        this.taskList.clear();
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void executeTask(Object executeTask) {
        long startTime = 0;
        try {
            startTime = this.manager.getGlobalTime();
            if (isMulti) {
                if (((IScheduleTaskDealMulti) this.taskDealBean).execute((Object[]) executeTask,
                        this.manager.currentServer().getOwnSign()) == true) {
                    addSuccessNum(((Object[]) executeTask).length,
                            this.manager.getGlobalTime() - startTime);
                } else {
                    addFailNum(((Object[]) executeTask).length,
                            this.manager.getGlobalTime() - startTime);
                }
            } else {
                if (((IScheduleTaskDealSingle) this.taskDealBean).execute(executeTask,
                        this.manager.currentServer().getOwnSign()) == true) {
                    addSuccessNum(1, this.manager.getGlobalTime() - startTime);
                } else {
                    addFailNum(1, this.manager.getGlobalTime() - startTime);
                }
            }
        } catch (Exception ex) {
            if (isMulti) {
                addFailNum(((Object[]) executeTask).length, this.manager.getGlobalTime() - startTime);
            } else {
                addFailNum(1, this.manager.getGlobalTime() - startTime);
            }
            logger.warn("Task: {} executed failed", executeTask, ex);
        }
    }
    
    protected Object getNextTask() {
        if (isMulti()) {
            return getNextTaskMulti();
        } else {
            return getNextTaskSingle();
        }
    }
    
    protected T getNextTaskSingle() {
        synchronized (this.taskList) {
            if (this.taskList.size() > 0)
                return this.taskList.remove(0); // 按正序处理
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    protected T[] getNextTaskMulti() {
        synchronized (this.taskList) {
            if (this.taskList.size() == 0) {
                return null;
            }
            int size = this.task.getExecuteNumber();
            if (size > this.taskList.size()) {
                size = this.taskList.size();
            }

            T[] result = null;
            if (size > 0) {
                result = (T[]) Array.newInstance(this.taskList.get(0).getClass(), size);
                for (int i = 0; i < size; i++) {
                    result[i] = this.taskList.remove(0); // 按正序处理
                }
            }
            return result;
        }
    }
    
    protected void beforeLoadNewData() {}
    
    protected int loadNewData() {
        lockLoadData.lock();
        try {
            if (this.taskList.size() > 0 || isStopSchedule()) {
                return this.taskList.size();
            }
            if (this.task.getSleepTimeInterval() > 0) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Fix Sleep: {}", this.task.getSleepTimeInterval());
                }
                sleep(this.task.getSleepTimeInterval());
                if (logger.isTraceEnabled()) {
                    logger.trace("Resume from sleeping");
                }
            }
            beforeLoadNewData();
            List<TaskItem> taskItemList = this.manager.getCurrentScheduleTaskItemList();
            // 根据队列信息查询需要调度的数据，然后增加到任务列表中
            if (taskItemList.size() > 0) {
                List<T> tmpList = this.taskDealBean.selectTasks(
                        this.task.getTaskParameter(),
                        this.manager.currentServer().getOwnSign(),
                        this.manager.getTaskItemCount(), new ArrayList<>(taskItemList),
                        this.task.getFetchDataNumber());
                this.manager.currentServer().setLastFetchDataTime(new Timestamp(this.manager.getGlobalTime()));
                if (tmpList != null) {
                    this.taskList.addAll(tmpList);
                }
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("No task item");
                }
            }
            addFetchNum(taskList.size());
            // Whether should sleep on no data
            if (taskList.size() <= 0) {
                try {
                    if (!isStopSchedule() && this.manager.isContinueWhenNoData()) {
                        // if continue when no data, add a interval if needed
                        if (this.task.getSleepTimeNoData() > 0) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("No task returned by select(), sleep {}", this.task.getSleepTimeNoData());
                            }
                            sleep(this.task.getSleepTimeNoData());
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error occurred", e);
                }
            }
            return taskList.size();
        } catch (Exception e) {
            logger.error("Get tasks error.", e);
            return 0;
        } finally {
            lockLoadData.unlock();
        }
    }
    
    protected boolean isMulti() {
        return isMulti;
    }
    
    protected Task getTask() {
        return task;
    }
    
    protected void addCompareCount(int num) {
        this.statisticsInfo.addOtherCompareCount(num);
    }
    
    private void addFetchNum(long num) {
        this.statisticsInfo.addFetchDataCount(1);
        this.statisticsInfo.addFetchDataNum(num);
    }

    private void addSuccessNum(long num, long spendTime) {
        this.statisticsInfo.addDealDataSucess(num);
        this.statisticsInfo.addDealSpendTime(spendTime);
    }

    private void addFailNum(long num, long spendTime) {
        this.statisticsInfo.addDealDataFail(num);
        this.statisticsInfo.addDealSpendTime(spendTime);
    }
    
}
