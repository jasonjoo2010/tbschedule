package com.yoloho.schedule.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ManagerHeartBeatTask extends java.util.TimerTask {
    private static transient Logger logger = LoggerFactory.getLogger(ManagerHeartBeatTask.class.getSimpleName());
    AbstractScheduleManager manager;

    public ManagerHeartBeatTask(AbstractScheduleManager aManager) {
        manager = aManager;
    }

    public void run() {
        try {
            int priority = Thread.currentThread().getPriority();
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            manager.refreshScheduleServerInfo();
            Thread.currentThread().setPriority(priority);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}