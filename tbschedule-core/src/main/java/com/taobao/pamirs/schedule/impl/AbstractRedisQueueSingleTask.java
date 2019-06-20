package com.taobao.pamirs.schedule.impl;

import java.util.Comparator;
import java.util.List;

import com.taobao.pamirs.schedule.IScheduleTaskDealSingle;
import com.taobao.pamirs.schedule.TaskItemDefine;

/**
 * 任务的抽象类，提供以redis为队列的简单抽象
 * 
 * @author jason<jason@dayima.com> @ Jan 4, 2019
 *
 */
public abstract class AbstractRedisQueueSingleTask<T> extends AbstractRedisQueueTask<T>
        implements IScheduleTaskDealSingle<T> {
    
    @Override
    public List<T> selectTasks(String taskParameter, String ownSign, int taskItemNum, List<TaskItemDefine> taskItemList,
            int eachFetchDataNum) throws Exception {
        return getTaskList(eachFetchDataNum);
    }
    
    protected abstract boolean processItem(T item);
    
    @Override
    public boolean execute(T item, String ownSign) throws Exception {
        return processItem(item);
    }
    
    @Override
    public Comparator<T> getComparator() {
        return null;
    }

}
