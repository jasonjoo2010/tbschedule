package com.yoloho.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodically check the storage status and request factory to refresh if needed(After some failures)
 * 
 * @author jason
 *
 */
class FactoryChecker extends java.util.TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(FactoryChecker.class.getSimpleName());
    ScheduleManagerFactory factory;
    int count = 0;

    public FactoryChecker(ScheduleManagerFactory factory) {
        this.factory = factory;
    }

    public void run() {
        try {
            if (this.factory.getStorage().test() == false) {
                if (count > 5) {
                    //log.error("Storage status failed for several times, try to restart......");
                    //this.factory.restart();
                    // Maybe there was no need to perform restarting
                    logger.error("Storage status failed for several times");
                } else {
                    count ++;
                }
            } else {
                // reset the timer once successfully
                // refresh and refresh to make sure everything up to date
                count = 0;
                this.factory.refresh();
            }
        } catch (Throwable ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            factory.setLastCheck(System.currentTimeMillis());
        }
    }
}