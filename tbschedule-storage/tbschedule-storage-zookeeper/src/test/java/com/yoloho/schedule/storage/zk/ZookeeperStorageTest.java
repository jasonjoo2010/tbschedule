package com.yoloho.schedule.storage.zk;

import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.interfaces.IStorage.OnConnected;
import com.yoloho.schedule.interfaces.IStorageTest;
import com.yoloho.schedule.types.ScheduleConfig;

public class ZookeeperStorageTest extends IStorageTest {

    @Override
    protected IStorage getStorage() {
        IStorage storage = new ZookeeperStorage();
        ScheduleConfig config = new ScheduleConfig();
        config.setAddress("127.0.0.1:2181");
        config.setRootPath("/test/schedule/demo");
        config.setUsername("test");
        config.setPassword("123123");
        storage.init(config, new OnConnected() {
            
            @Override
            public void connected(IStorage storage) {
            }
        });
        return storage;
    }
    
}
