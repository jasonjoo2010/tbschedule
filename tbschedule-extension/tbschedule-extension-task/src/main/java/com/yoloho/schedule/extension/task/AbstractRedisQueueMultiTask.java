package com.yoloho.schedule.extension.task;

import java.util.Comparator;
import java.util.List;

import com.yoloho.schedule.interfaces.IScheduleTaskDealMulti;
import com.yoloho.schedule.types.TaskItem;

/**
 * 任务的抽象类，提供以redis为队列的简单抽象
 * 
 * @author jason<jason@dayima.com> @ Jan 4, 2019
 *
 */
public abstract class AbstractRedisQueueMultiTask<T> extends AbstractRedisQueueTask<T>
        implements IScheduleTaskDealMulti<T> {
    
    @Override
    public List<T> selectTasks(String taskParameter, String ownSign, int taskItemNum, List<TaskItem> taskItemList,
            int eachFetchDataNum) throws Exception {
        return getTaskList(eachFetchDataNum);
    }
    
    protected abstract boolean processItems(T[] items);
    
    @Override
    public boolean execute(T[] items, String ownSign) throws Exception {
        return processItems(items);
    }
    
    @Override
    public Comparator<T> getComparator() {
        return null;
    }
}
