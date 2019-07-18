package com.yoloho.schedule.test;


import javax.annotation.Resource;

import org.junit.Test;

import com.yoloho.schedule.ScheduleManagerFactory;

/**
 * 调度测试
 * @author xuannan
 *
 */
public class StartDemoSchedule extends AbstractTest {
	@Resource
	ScheduleManagerFactory scheduleManagerFactory;
	
    public void setScheduleManagerFactory(
			ScheduleManagerFactory tbScheduleManagerFactory) {
		this.scheduleManagerFactory = tbScheduleManagerFactory;
	}
	@Test    
	public void testRunData() throws Exception {
		Thread.sleep(100000000000000L);
	}
}
