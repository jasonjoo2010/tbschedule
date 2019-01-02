package com.taobao.pamirs.schedule.test;


import javax.annotation.Resource;

import org.junit.Test;

import com.taobao.pamirs.schedule.strategy.TBScheduleManagerFactory;

/**
 * 调度测试
 * @author xuannan
 *
 */
public class StartDemoSchedule extends AbstractTest {
	@Resource
	TBScheduleManagerFactory scheduleManagerFactory;
	
    public void setScheduleManagerFactory(
			TBScheduleManagerFactory tbScheduleManagerFactory) {
		this.scheduleManagerFactory = tbScheduleManagerFactory;
	}
	@Test    
	public void testRunData() throws Exception {
		Thread.sleep(100000000000000L);
	}
}
