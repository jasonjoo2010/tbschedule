package com.yoloho.schedule.demo.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yoloho.schedule.interfaces.IStrategyTask;

import io.netty.util.internal.ThreadLocalRandom;

public class InstanceConsumer implements IStrategyTask, Runnable {
    private static final Logger logger = LoggerFactory.getLogger(InstanceConsumer.class.getSimpleName());
    private boolean shutdown = false;
    private boolean stopped = false;

    @Override
    public void initialTaskParameter(String strategyName, String taskParameter) throws Exception {
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
        logger.info("Consumer {}:{} started", strategyName, Thread.currentThread().getName());
    }
    
    @Override
    public void run() {
        String item = null;
        while (!shutdown) {
            item = SingletonProducer.getInstance().poll();
            if (item == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                continue;
            }
            // simulate processing, cost 250ms in average
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(500));
                logger.info("done with {}", item);
            } catch (InterruptedException e) {
            }
        }
        this.stopped = true;
    }

    @Override
    public void stop(String strategyName) throws Exception {
        this.shutdown = true;
        int remainTime = 10000;
        while (!stopped && remainTime > 0) {
            Thread.sleep(20);
            remainTime -= 20;
        }
        logger.info("Consumer {}:{} stopped", strategyName, Thread.currentThread().getName());
    }

}
