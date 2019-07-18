package com.yoloho.schedule.processor;

import java.util.Date;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yoloho.schedule.util.CronExpression;
import com.yoloho.schedule.util.ScheduleUtil;

class PauseOrResumeScheduleTask extends java.util.TimerTask {
	private static transient Logger log = LoggerFactory
			.getLogger(PauseOrResumeScheduleTask.class.getSimpleName());
    public static int TYPE_PAUSE = 1;
    public static int TYPE_RESUME = 2;
    AbstractScheduleManager manager;
    Timer timer;
    int type;
    String cronTabExpress;

    public PauseOrResumeScheduleTask(AbstractScheduleManager aManager, Timer aTimer, int aType, String aCronTabExpress) {
        this.manager = aManager;
        this.timer = aTimer;
        this.type = aType;
        this.cronTabExpress = aCronTabExpress;
    }

    public void run() {
        try {
            int priority = Thread.currentThread().getPriority();
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            cancel();
            Date current = new Date(System.currentTimeMillis());
            CronExpression cexp = new CronExpression(this.cronTabExpress);
            Date nextTime = cexp.getNextValidTimeAfter(current);
            if (this.type == TYPE_PAUSE) {
                manager.pause("到达终止时间,pause调度");
                this.manager.currentServer().setNextRunEndTime(ScheduleUtil.dataToString(nextTime));
            } else {
                manager.resume("到达开始时间,resume调度");
                this.manager.currentServer().setNextRunStartTime(ScheduleUtil.dataToString(nextTime));
            }
            // reset priority
            Thread.currentThread().setPriority(priority);
            this.timer.schedule(new PauseOrResumeScheduleTask(this.manager, this.timer, this.type, this.cronTabExpress),
                    nextTime);
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}