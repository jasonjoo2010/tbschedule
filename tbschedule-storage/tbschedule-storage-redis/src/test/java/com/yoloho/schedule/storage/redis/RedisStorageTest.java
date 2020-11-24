package com.yoloho.schedule.storage.redis;

import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.interfaces.IStorage.OnConnected;
import com.yoloho.schedule.interfaces.IStorageTest;
import com.yoloho.schedule.types.ScheduleConfig;

public class RedisStorageTest extends IStorageTest {
    @Override
    protected IStorage getStorage() {
        RedisStorage storage = new RedisStorage();
        ScheduleConfig config = new ScheduleConfig();
        config.setAddress("127.0.0.1:6379");
        config.setRootPath("/test/schedule/demo");
        config.setUsername("");
        config.setPassword("");
        storage.init(config, new OnConnected() {
            
            @Override
            public void connected(IStorage storage) {
            }
        });
        return storage;
    }
    
}
