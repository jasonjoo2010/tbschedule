package com.yoloho.schedule.storage.zk;

import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.interfaces.IStorage.OnConnected;
import com.yoloho.schedule.interfaces.IStorageTest;
import com.yoloho.schedule.storage.memory.MemoryStorage;
import com.yoloho.schedule.types.ScheduleConfig;

public class MemoryStorageTest extends IStorageTest {
    
    @Override
    protected IStorage getStorage() {
        MemoryStorage storage = new MemoryStorage();
        ScheduleConfig config = new ScheduleConfig();
        config.setStorage("memory");
        storage.init(config, new OnConnected() {
            
            @Override
            public void connected(IStorage storage) {
            }
        });
        return storage;
    }
    
}
