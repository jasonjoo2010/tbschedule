package com.yoloho.schedule.processor;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yoloho.schedule.interfaces.IScheduleTaskDeal;
import com.yoloho.schedule.types.StatisticsInfo;

/**
 * Single selector, multiple executors
 * 
 * <pre>
 * Thread0         Thread1         Thread2         Thread3
 * <b>select</b>          exe1(sleeping)  exe2(sleeping)  exe3(sleeping)
 * exe0            exe1            exe2            exe3
 * exe0            exe1            exe2            exe3
 * exe0            exe1            exe2            exe3(sleeping)
 * exe0            exe1(sleeping)  exe2            exe3(sleeping)
 * exe0            exe1(sleeping)  exe2(sleeping)  exe3(sleeping)
 * exe0(sleeping)  exe1(sleeping)  exe2(sleeping)  exe3(sleeping)
 * <b>select</b>          exe1(sleeping)  exe2(sleeping)  exe3(sleeping)
 * exe0            exe1            exe2            exe3
 * .....
 * </pre>
 * 
 * @author xuannan
 *
 * @param <T>
 */
public class TaskProcessorSleep<T> extends AbstractTaskProcessor<T> {
	private static transient Logger logger = LoggerFactory.getLogger(TaskProcessorSleep.class);
	private final AtomicInteger workingCount = new AtomicInteger(0);
	private boolean processorReady = false;
	/**
	 * 任务管理器
	 */
	protected AbstractScheduleManager scheduleManager;
	
	/**
	 * 创建一个调度处理器 
	 * @param aManager
	 * @param aTaskDealBean
	 * @param aStatisticsInfo
	 * @throws Exception
	 */
	public TaskProcessorSleep(AbstractScheduleManager aManager,
			IScheduleTaskDeal<T> aTaskDealBean,	StatisticsInfo aStatisticsInfo) throws Exception {
	    super(aManager, aTaskDealBean, aStatisticsInfo);
		this.scheduleManager = aManager;
		this.processorReady = true;
	}
	
	@Override
    public void run() {
        try {
            // Make threads held by group wait for all initializing works done
            while (!processorReady && !isStopSchedule()) {
                sleep(100);
            }
            while (true) {
                workingCount.incrementAndGet();
                Object executeTask;
                while (true) {
                    if (isStopSchedule()) {
                        workingCount.decrementAndGet();
                        workingCount.notifyAll();
                        if (releaseCurrentThread()) {
                            setStopped();
                        }
                        return;
                    }

                    executeTask = getNextTask();
                    if (executeTask == null) {
                        break;
                    }

                    executeTask(executeTask);
                }
                // task finished
                if (logger.isTraceEnabled()) {
                    logger.trace("{}: Thread count: {}", Thread.currentThread().getName(), this.workingCount.get());
                }
                if (workingCount.decrementAndGet() > 0) {
                    // stop to wait
                    if (logger.isTraceEnabled()) {
                        logger.trace("Turn into sleeping because it's not the last one");
                    }
                    workingCount.wait();
                } else {
                    int num = this.loadNewData();
                    if (num > 0) {
                        this.workingCount.notifyAll();
                    } else {
                        if (isStopSchedule() || this.scheduleManager.isContinueWhenNoData() == false) {
                            // Exit on no data or stopped flag
                            this.workingCount.notifyAll();
                        }
                    }
                }
            }
        } catch (Throwable e) {
            logger.error("Server thread exit abnormally", e);
        }
    }

}
