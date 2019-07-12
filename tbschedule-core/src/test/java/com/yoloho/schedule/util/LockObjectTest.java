package com.yoloho.schedule.util;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class LockObjectTest {
    @Test
    public void test() {
        ThreadGroupLock lock = new ThreadGroupLock();
        assertEquals(0, lock.count());
        lock.addThread();
        lock.addThread();
        lock.addThread();
        assertEquals(3, lock.count());
        lock.releaseThread();
        assertEquals(2, lock.count());
        lock.releaseThreadButNotLast();
        lock.releaseThreadButNotLast();
        lock.releaseThreadButNotLast();
        lock.releaseThreadButNotLast();
        lock.releaseThreadButNotLast();
        lock.releaseThreadButNotLast();
        assertEquals(1, lock.count());
        lock.releaseThread();
        assertEquals(0, lock.count());
    }
    
    @Test
    public void signal() throws InterruptedException {
        final ThreadGroupLock lock = new ThreadGroupLock();
        final AtomicInteger result = new AtomicInteger(0);
        new Thread() {
            public void run() {
                try {
                    result.set(1);
                    lock.waitSignal();
                    result.set(2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
        Thread.sleep(100);
        assertEquals(1, result.get());
        new Thread() {
            public void run() {
                try {
                    lock.signalGroup();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
        Thread.sleep(100);
        assertEquals(2, result.get());
    }
}
