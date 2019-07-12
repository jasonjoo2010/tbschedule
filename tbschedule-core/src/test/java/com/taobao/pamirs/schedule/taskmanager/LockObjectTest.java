package com.taobao.pamirs.schedule.taskmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.yoloho.schedule.util.ThreadGroupLock;

public class LockObjectTest {
    private ThreadGroupLock lockObject = new ThreadGroupLock();
    private AtomicInteger atomicInteger = new AtomicInteger(0);
    
    @Test
    public void test() {
        int threadNum = 10;
        ForkJoinPool pool = new ForkJoinPool(threadNum);
        for (int i = 0; i < threadNum; i ++) {
            pool.execute(new Runnable() {
                
                @Override
                public void run() {
                    lockObject.addThread();
                    atomicInteger.incrementAndGet();
                    try {
                        lockObject.waitSignal();
                    } catch (Exception e) {
                        e.printStackTrace();
                        assertFalse(true);
                    }
                    atomicInteger.decrementAndGet();
                    lockObject.releaseThread();
                }
            });
        }
        int maxWaitCount = 100;
        while (atomicInteger.get() != threadNum && maxWaitCount-- > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        assertEquals(threadNum, atomicInteger.get());
        assertEquals(threadNum, lockObject.count());
        try {
            lockObject.signalGroup();
        } catch (Exception e1) {
            e1.printStackTrace();
            assertFalse(true);
        }
        maxWaitCount = 100;
        while (atomicInteger.get() != 0 && maxWaitCount-- > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        assertEquals(0, atomicInteger.get());
        assertEquals(0, lockObject.count());
    }
}
