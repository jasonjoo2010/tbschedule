package com.yoloho.schedule.util;

public class LockObject {
	private int threadCount = 0;
	private Object waitOnObject = new Object();

	public LockObject() {
	}

	public void waitCurrentThread() throws Exception {
		synchronized (waitOnObject) {
			// System.out.println(Thread.currentThread().getName() + ":" +
			// "休眠当前线程");
			this.waitOnObject.wait();
		}
	}

	public void notifyOtherThread() throws Exception {
		synchronized (waitOnObject) {
			// System.out.println(Thread.currentThread().getName() + ":" +
			// "唤醒所有等待线程");
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
	 * 降低线程数量，如果是最后一个线程，则不能休眠
	 * 
	 * @return boolean
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

	public int count() {
		synchronized (this) {
			return threadCount;
		}
	}
}
