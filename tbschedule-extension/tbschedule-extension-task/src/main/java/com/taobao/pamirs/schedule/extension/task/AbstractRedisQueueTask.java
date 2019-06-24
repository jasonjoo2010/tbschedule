package com.taobao.pamirs.schedule.extension.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.yoloho.enhanced.common.annotation.NonNull;
import com.yoloho.enhanced.data.cache.lock.DistributedLock;
import com.yoloho.enhanced.data.cache.redis.api.RedisService;

/**
 * 任务的抽象类，提供以redis为队列的简单抽象<br>
 * 这里因为需要适配两个redisService(一个是uic自己写的redisService接口类)，所以要多一点处理逻辑<br>
 * 另外为了使用便利些，暂时适配的是redisService而不是redisTemplate<br>
 * 另外为了实现时防止业务方癔想出来的一些坑，这里对于模板方法的调用都是直接调用而没有缓存其值，所以需要尽量保持模板方法的高效
 * 
 * @author jason
 *
 */
public abstract class AbstractRedisQueueTask<T> {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    /**
     * 上次获取重试数据的毫秒时间戳
     */
    private long lastRetry = 0;
    private volatile DistributedLock<String> lock = null;
    
    /**
     * 绑定的redis
     * 
     * @return
     */
    protected abstract RedisService getRedisService();
    
    /**
     * 队列名
     * 
     * @return
     */
    protected abstract String getQueueName();
    
    @PostConstruct
    public void init() {
        Preconditions.checkNotNull(getRedisService(), "getRedisService() not implemented a none null");
    }
    
    /**
     * 重试队列名，默认是主队列后加尾巴"_retry"
     * 
     * @return
     */
    protected String getRetryQueueName() {
        return getQueueName() + "_retry";
    }
    
    /**
     * 将redis里每个元素做类型解析
     * 
     * @param item
     * @return null for fail or invalid
     */
    protected abstract T convertType(String item);
    
    /**
     * 将元素转换回redis里的单条，比如转换成json
     * 
     * @param item
     * @return
     */
    protected abstract String convertTypeRevert(T item);
    
    /**
     * 是否启用retry功能，不需要的可以覆盖本方法禁用
     * 
     * @return
     */
    protected boolean enableRetry() {
        return true;
    }
    
    /**
     * 稍后重试某item<br>
     * 注意，如果需要设置最大重试次数，这个目前需要具体逻辑自行实现(因为无法判定唯一识别方式)
     * 
     * @param item
     */
    protected void retry(T item) {
        String val = convertTypeRevert(item);
        if (StringUtils.isEmpty(val)) {
            return;
        }
        getRedisService().inQueue(getRetryQueueName(), val);
        logger.info("任务数据回重试队列：{}", val);
    }
    
    private void initLock() {
        if (lock != null) {
            return;
        }
        synchronized (this) {
            if (lock != null) {
                return;
            }
            lock = new DistributedLock<>(getRedisService(), "AbstractRedisQueueTask", 60);
        }
    }
    
    /**
     * @param queueName
     * @param count
     * @return
     */
    @NonNull
    private List<String> getDataList(String queueName, int count) {
        initLock();
        String lockKey = getQueueName();
        List<String> result = new ArrayList<>(10);
        try {
            lock.lock(lockKey, 1, TimeUnit.SECONDS);
            try {
                List<String> list = getRedisService().outQueueRange(queueName, 0, count);
                if (list != null) {
                    getRedisService().outQueueTrim(queueName, list.size(), -1);
                    result.addAll(list);
                }
            } finally {
                lock.unlock(lockKey);
            }
        } catch (TimeoutException | InterruptedException e) {
            logger.info("遇到并发未获得锁，跳过本次取数步骤");
        }
        return result;
    }
    
    protected List<T> getTaskList(int capacity) {
        List<String> list = getDataList(getQueueName(), capacity);
        if (enableRetry()) {
            long now = System.currentTimeMillis();
            // 这里为了避免外部服务或逻辑出问题时，非常频繁地取重试队列再塞回去的性能问题，所以规定了一个每10秒取一次的限制
            // 带来的另外一个问题，就是这样的机制并不针对特别大量的需要重试的场景，要注意
            if (now - lastRetry > 10000) {
                lastRetry = now;
                List<String> retryList = getDataList(getRetryQueueName(), capacity);
                if (retryList.size() > 0) {
                    list.addAll(retryList);
                }
            }
        }
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(list.size());
        for (String str : list) {
            if (StringUtils.isEmpty(str)) {
                continue;
            }
            try {
                T item = convertType(str);
                if (item != null) {
                    result.add(item);
                }
            } catch (Exception e) {
                logger.warn("类型转换失败", e);
            }
        }
        logger.info("获取到了 {} 条待处理数据", result.size());
        return result;
    }
}
