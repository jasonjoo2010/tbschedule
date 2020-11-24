package com.yoloho.schedule.storage.jdbc;

import java.util.HashMap;
import java.util.Map;

import com.yoloho.enhanced.data.cache.lock.DistributedLock.LockSupport;
import com.yoloho.enhanced.data.dao.api.EnhancedDao;
import com.yoloho.enhanced.data.dao.api.UpdateEntry;
import com.yoloho.enhanced.data.dao.api.filter.DynamicQueryFilter;
import com.yoloho.schedule.storage.jdbc.model.StorageInfo;

public class JdbcLock implements LockSupport {
    private EnhancedDao<StorageInfo, Long> lockDao;
    
    public JdbcLock(EnhancedDao<StorageInfo, Long> lockDao) {
        this.lockDao = lockDao;
    }

    @Override
    public void keep(String key, String value, int keepInSeconds) {
        Map<String, UpdateEntry> data = new HashMap<String, UpdateEntry>(1);
        data.put("expire", new UpdateEntry(String.valueOf(System.currentTimeMillis() / 1000 + keepInSeconds)));
        data.put("value", new UpdateEntry(value));
        lockDao.update(data, new DynamicQueryFilter()
                .equalPair("key", key)
                .getQueryData());
    }

    @Override
    public boolean exists(String key) {
        return lockDao.count("key", key) > 0;
    }

    @Override
    public String get(String key) {
        StorageInfo bean = lockDao.get("key", key);
        if (bean == null) return null;
        return bean.getValue();
    }

    @Override
    public boolean setIfAbsent(String key, String value, int expireInSeconds) {
        if (exists(key)) return false;
        StorageInfo bean = new StorageInfo();
        bean.setKey(key);
        bean.setValue(value);
        bean.setExpire(System.currentTimeMillis() / 1000 + expireInSeconds);
        return lockDao.insert(bean, true) > 0;
    }

    @Override
    public void delete(String key) {
        lockDao.remove(new DynamicQueryFilter()
                .equalPair("key", key)
                .getQueryData());
    }

}
