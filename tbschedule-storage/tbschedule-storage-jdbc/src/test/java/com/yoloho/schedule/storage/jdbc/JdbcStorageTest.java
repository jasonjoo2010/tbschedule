package com.yoloho.schedule.storage.jdbc;

import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.interfaces.IStorage.OnConnected;
import com.yoloho.schedule.interfaces.IStorageTest;
import com.yoloho.schedule.types.ScheduleConfig;

public class JdbcStorageTest extends IStorageTest {
    
    @Override
    protected IStorage getStorage() {
        JdbcStorage storage = new JdbcStorage();
        ScheduleConfig config = new ScheduleConfig();
        config.setAddress("jdbc:mysql://127.0.0.1:3306/test?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&tinyInt1isBit=false");
        config.setRootPath("/demo");
        config.setUsername("root");
        config.setPassword("");
        storage.init(config, new OnConnected() {
            
            @Override
            public void connected(IStorage storage) {
            }
        });
        return storage;
    }
}
