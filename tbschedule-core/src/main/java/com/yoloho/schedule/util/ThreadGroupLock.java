package com.yoloho.schedule.util;

import com.yoloho.schedule.processor.TaskProcessorSleep;

/**
 * Lock utility for thread group, please refer to {@link TaskProcessorSleep}
 * <p>
 * TIPS: Not for performance sensitive scenario
 *
 */
public class ThreadGroupLock {
    private volatile int threadCount = 0;
    private volatile Object waitOnObject = new Object();

    public void waitSignal() throws Exception {
        synchronized (waitOnObject) {
            this.waitOnObject.wait();
        }
    }

    public void signalGroup() throws Exception {
        synchronized (waitOnObject) {
            this.waitOnObject.notifyAll();
        }
    }

    public void addThread() {
        synchronized (this) {
            threadCount = threadCount + 1;
        }
    }

    public void releaseThread() {
        synchronized (this) {
            threadCount = threadCount - 1;
        }
    }

    /**
     * If count == 1 then skip releasing and return false
     * 
     * @return boolean false for failed release (Can't release more)
     */
    public boolean releaseThreadButNotLast() {
        synchronized (this) {
            if (this.threadCount == 1) {
                return false;
            } else {
                threadCount = threadCount - 1;
                return true;
            }
        }
    }

    /**
     * Current acquired thread count
     * 
     * @return
     */
    public int count() {
        synchronized (this) {
            return threadCount;
        }
    }
}
