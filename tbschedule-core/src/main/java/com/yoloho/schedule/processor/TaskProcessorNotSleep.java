package com.yoloho.schedule.processor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.yoloho.schedule.interfaces.IScheduleTaskDeal;
import com.yoloho.schedule.types.StatisticsInfo;


/**
 * Continuous processing
 * 
 * <pre>
 * Thread0         Thread1         Thread2         Thread3
 * <b>select</b>          exe1(sleeping)  exe2(sleeping)  exe3(sleeping)
 * exe0            exe1            exe2            exe3
 * exe0            exe1            exe2            exe3
 * exe0            exe1            exe2            <b>select</b>
 * exe0            exe1            exe2            exe3
 * exe0            exe1            exe2            exe3
 * exe0            exe1            <b>select</b>          exe3
 * exe0            exe1            exe2            exe3
 * exe0            exe1            exe2            exe3
 * .....
 * </pre>
 * 
 * Who get nothing more who request to load new data
 * 
 * @author xuannan
 * @param <T>
 * 
 * resturctured by
 * @author jason
 */
class TaskProcessorNotSleep<T> extends AbstractTaskProcessor<T> {
	
	/**
	 * 任务比较器
	 */
    private Comparator<T> taskComparator;

	/**
	 * 正在处理中的任务队列
	 */
	private List<Object> runningTaskList = new CopyOnWriteArrayList<Object>(); 
	/**
	 * 在重新取数据，可能会重复的数据。在重新去数据前，从runningTaskList拷贝得来
	 */
	private List<T> maybeRepeatTaskList = new CopyOnWriteArrayList<T>();

	private Lock repeatableLock = new ReentrantLock();

    private boolean processorReady = false;
	
	/**
	 * 创建一个调度处理器
	 * @param manager
	 * @param dealBean
	 * @param statisticsInfo
	 * @throws Exception
	 */
	public TaskProcessorNotSleep(AbstractScheduleManager manager,
			IScheduleTaskDeal<T> dealBean,StatisticsInfo statisticsInfo) throws Exception {
	    super(manager, dealBean, statisticsInfo);
		this.taskComparator = new TaskComparator<T>(dealBean.getComparator());
		this.processorReady = true;
	}

    private boolean isDealing(T aTask) {
        repeatableLock.lock();
        try {
            if (this.maybeRepeatTaskList.size() == 0) {
                return false;
            }
            T found = null;
            for (T item : this.maybeRepeatTaskList) {
                addCompareCount(1);
                if (this.taskComparator.compare(aTask, item) == 0) {
                    found = item;
                }
            }
            if (found != null) {
                this.maybeRepeatTaskList.remove(found);
                return true;
            }
            return false;
        } finally {
            repeatableLock.unlock();
        }
    }

	/**
	 * 获取单个任务，注意lock是必须，
	 * 否则在maybeRepeatTaskList的数据处理上会出现冲突
	 * @return
	 */
	@Override
    protected T getNextTaskSingle() {
        T result = null;
        while (true) {
            result = super.getNextTaskSingle();
            if (result == null) {
                return null;
            }
            if (!isDealing(result)) {
                return result;
            }
        }
    }
    
	@SuppressWarnings("unchecked")
	public T[] getNextTaskMulti() {
		int batchSize = getTask().getExecuteNumber();
		List<T> result = new ArrayList<T>(16);
		int num = 0;
		T tmpObject = null;
		while (num < batchSize
				&& ((tmpObject = this.getNextTaskSingle()) != null)) {
			result.add(tmpObject);
			num ++;
		}
		if (result.isEmpty()) {
			return null;
		} else {
			return (T[]) result.toArray();
		}
	}
	
	@Override
    public boolean hasRemainedTask(){
    	return super.hasRemainedTask() || this.runningTaskList.size() > 0;  
    }
    
	/**
	 * Copy the running tasks to repeatable task queue
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void beforeLoadNewData() {
	    super.beforeLoadNewData();
		repeatableLock.lock();
		try {
			this.maybeRepeatTaskList.clear();
			if (this.runningTaskList.size() == 0) {
				return;
			}
			Object[] tmpList = this.runningTaskList.toArray();
			for (int i = 0; i < tmpList.length; i++) {
			    if (isMulti()) {
			        T[] aTasks = (T[]) tmpList[i];
			        for (int j = 0; j < aTasks.length; j++) {
			            this.maybeRepeatTaskList.add(aTasks[j]);
			        }
			    } else {
			        this.maybeRepeatTaskList.add((T) tmpList[i]);
			    }
			}
		} finally {
			repeatableLock.unlock();
		}
	}
	
    public void run() {
        Object executeTask = null;
        // Make threads held by group wait for all initializing works done
        while (!processorReady && !isStopSchedule()) {
            sleep(100);
        }
        while (true) {
            try {
                if (isStopSchedule()) {
                    if (releaseCurrentThread()) {
                        setStopped();
                    }
                    return;
                }
                
                executeTask = getNextTask();
                if (executeTask == null) {
                    // load new data immediately
                    loadNewData();
                    continue;
                }
                
                try {
                    this.runningTaskList.add(executeTask);
                    executeTask(executeTask);
                } finally {
                    this.runningTaskList.remove(executeTask);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

}
