package com.yoloho.schedule.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HeartBeatTimerTask extends java.util.TimerTask {
    private static transient Logger log = LoggerFactory.getLogger(HeartBeatTimerTask.class);
    AbstractScheduleManager manager;

    public HeartBeatTimerTask(AbstractScheduleManager aManager) {
        manager = aManager;
    }

    public void run() {
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            manager.refreshScheduleServerInfo();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}