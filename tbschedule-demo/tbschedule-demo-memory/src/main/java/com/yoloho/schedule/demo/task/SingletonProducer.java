package com.yoloho.schedule.demo.task;

import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.RateLimiter;
import com.yoloho.enhanced.common.util.RandomUtil;
import com.yoloho.schedule.interfaces.IStrategyTask;
import com.yoloho.schedule.memory.annotation.Strategy;

@Strategy(
    name = "SingletonProducer",
    beanName = "singletonProducer",
    count = "1"
)
@Component("singletonProducer")
public class SingletonProducer implements IStrategyTask, Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SingletonProducer.class.getSimpleName());
    private static SingletonProducer instance = null;
    private LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(1000);
    private RateLimiter rateLimiter = RateLimiter.create(10);
    private boolean running = false;
    private boolean shutdown = false;
    private boolean stopped = false;
    
    public static SingletonProducer getInstance() {
        return instance;
    }
    
    @PostConstruct
    public void init() {
        instance = this;
        Thread thread = new Thread(this);
        thread.setDaemon(false);
        thread.start();
    }
    
    @PreDestroy
    public void deinit() {
        this.shutdown = true;
        int remainMillis = 10000;
        while (!stopped && remainMillis > 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
            remainMillis -= 10;
        }
    }
    
    public String poll() {
        return queue.poll();
    }
    
    @Override
    public void run() {
        while (!shutdown) {
            if (!running) {
                // pause
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }
                continue;
            }
            rateLimiter.acquire();
            try {
                queue.put(RandomUtil.getRandomString(8));
            } catch (InterruptedException e) {
            }
        }
        this.stopped = true;
    }

    @Override
    public void initialTaskParameter(String strategyName, String taskParameter) throws Exception {
        this.running = true;
        logger.info("Producer started");
    }

    @Override
    public void stop(String strategyName) throws Exception {
        this.running = false;
        logger.info("Producer paused");
    }

}
