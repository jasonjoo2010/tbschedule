package com.yoloho.schedule.demo.task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.comparator.Comparators;

import com.yoloho.schedule.interfaces.IScheduleTaskDealSingle;
import com.yoloho.schedule.memory.annotation.Strategy;
import com.yoloho.schedule.memory.annotation.Task;
import com.yoloho.schedule.types.TaskItem;

import io.netty.util.internal.ThreadLocalRandom;

@Strategy(
    name = "demoTaskStrategy",
    taskName = "task1",
    count = "1"
)
@Task(
    name = "task1",
    beanName = "taskConsumer",
    fetchDataNumber = "200",
    sleepTimeNoData = "20",
    threadCount = "2",
    factory = "com.yoloho.schedule.ScheduleManagerFactory"
)
@Component("taskConsumer")
public class TaskConsumer implements IScheduleTaskDealSingle<String> {
    private static final Logger logger = LoggerFactory.getLogger(TaskConsumer.class.getSimpleName());
    @Autowired
    private SingletonProducer producer;

    @Override
    public List<String> selectTasks(String taskParameter, String ownSign, int taskItemNum, List<TaskItem> taskItemList,
            int eachFetchDataNum) throws Exception {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < eachFetchDataNum; i++) {
            String item = producer.poll();
            if (item == null) {
                break;
            }
            result.add(item);
        }
        logger.info("Got {} tasks", result.size());
        return result;
    }

    @Override
    public Comparator<String> getComparator() {
        return Comparators.comparable();
    }

    @Override
    public boolean execute(String task, String ownSign) throws Exception {
        // simulate processing, cost 250ms in average
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(500));
            logger.info("done with {}", task);
        } catch (InterruptedException e) {
        }
        return true;
    }

}
