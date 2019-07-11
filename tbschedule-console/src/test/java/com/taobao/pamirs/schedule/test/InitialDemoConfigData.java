package com.taobao.pamirs.schedule.test;

import javax.annotation.Resource;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.taobao.pamirs.schedule.strategy.ScheduleStrategy;
import com.taobao.pamirs.schedule.strategy.TBScheduleManagerFactory;
import com.taobao.pamirs.schedule.taskmanager.ScheduleTaskType;


public class InitialDemoConfigData extends AbstractTest {
	protected static transient Logger log = LoggerFactory
			.getLogger(InitialDemoConfigData.class);
	@Resource
	TBScheduleManagerFactory scheduleManagerFactory;

	public void setScheduleManagerFactory(
			TBScheduleManagerFactory tbScheduleManagerFactory) {
		this.scheduleManagerFactory = tbScheduleManagerFactory;
	}

	@Test
	public void initialConfigData() throws Exception {
		String baseTaskTypeName = "DemoTask";
		scheduleManagerFactory.stopServer(null);
		Thread.sleep(1000);
		try {
			this.scheduleManagerFactory.getStorage().removeTask(baseTaskTypeName);
		} catch (Exception e) {

		}
		// 创建任务调度DemoTask的基本信息
		ScheduleTaskType task = new ScheduleTaskType();
		task.setBaseTaskType(baseTaskTypeName);
		task.setDealBeanName("demoTaskBean");
		task.setHeartBeatRate(2000);
		task.setJudgeDeadInterval(10000);
		task.setTaskParameter("AREA=杭州,YEAR>30");
		task.setTaskItems(ScheduleTaskType.splitTaskItem(
				"0:{TYPE=A,KIND=1},1:{TYPE=A,KIND=2},2:{TYPE=A,KIND=3},3:{TYPE=A,KIND=4}," +
				"4:{TYPE=A,KIND=5},5:{TYPE=A,KIND=6},6:{TYPE=A,KIND=7},7:{TYPE=A,KIND=8}," +
				"8:{TYPE=A,KIND=9},9:{TYPE=A,KIND=10}"));
        this.scheduleManagerFactory.getStorage().createTask(task);
		log.info("创建调度任务成功:" + task.toString());

		// 创建任务DemoTask的调度策略
		String taskName = baseTaskTypeName + "$TEST";
		String strategyName = baseTaskTypeName + "-Strategy";
		try {
			this.scheduleManagerFactory.getStorage().removeStrategy(strategyName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ScheduleStrategy strategy = new ScheduleStrategy();
		strategy.setStrategyName(strategyName);
		strategy.setKind(ScheduleStrategy.Kind.Schedule);
		strategy.setTaskName(taskName);
		strategy.setTaskParameter("中国");
		
		strategy.setNumOfSingleServer(1);
		strategy.setAssignNum(10);
		strategy.setIPList("127.0.0.1".split(","));
        this.scheduleManagerFactory.getStorage().createStrategy(strategy);
		log.info("创建调度策略成功:" + strategy.toString());

	}
}
