package com.yoloho.schedule.test;

import javax.annotation.Resource;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yoloho.enhanced.common.util.JoinerSplitters;
import com.yoloho.schedule.ScheduleManagerFactory;
import com.yoloho.schedule.types.Strategy;
import com.yoloho.schedule.types.StrategyKind;
import com.yoloho.schedule.types.Task;


public class InitialDemoConfigData extends AbstractTest {
	protected static transient Logger log = LoggerFactory
			.getLogger(InitialDemoConfigData.class);
	@Resource
	ScheduleManagerFactory scheduleManagerFactory;

	public void setScheduleManagerFactory(
			ScheduleManagerFactory tbScheduleManagerFactory) {
		this.scheduleManagerFactory = tbScheduleManagerFactory;
	}

	@Test
	public void initialConfigData() throws Exception {
		String taskName = "DemoTask";
		scheduleManagerFactory.stopServer(null);
		Thread.sleep(1000);
		try {
			this.scheduleManagerFactory.getStorage().removeTask(taskName);
		} catch (Exception e) {

		}
		// 创建任务调度DemoTask的基本信息
		Task task = new Task();
		task.setName(taskName);
		task.setDealBeanName("demoTaskBean");
		task.setHeartBeatRate(2000);
		task.setJudgeDeadInterval(10000);
		task.setTaskParameter("AREA=杭州,YEAR>30");
		task.setTaskItems(JoinerSplitters.getSplitter(",").splitToList(
				"0:{TYPE=A,KIND=1},1:{TYPE=A,KIND=2},2:{TYPE=A,KIND=3},3:{TYPE=A,KIND=4}," +
				"4:{TYPE=A,KIND=5},5:{TYPE=A,KIND=6},6:{TYPE=A,KIND=7},7:{TYPE=A,KIND=8}," +
				"8:{TYPE=A,KIND=9},9:{TYPE=A,KIND=10}").toArray(new String[0]));
        this.scheduleManagerFactory.getStorage().createTask(task);
		log.info("创建调度任务成功:" + task.toString());

		// 创建任务DemoTask的调度策略
		String bindTaskName = taskName + "$TEST";
		String strategyName = taskName + "-Strategy";
		try {
			this.scheduleManagerFactory.getStorage().removeStrategy(strategyName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Strategy strategy = new Strategy();
		strategy.setName(strategyName);
		strategy.setKind(StrategyKind.Schedule);
		strategy.setTaskName(bindTaskName);
		strategy.setTaskParameter("中国");
		
		strategy.setNumOfSingleServer(1);
		strategy.setAssignNum(10);
		strategy.setIPList("127.0.0.1".split(","));
        this.scheduleManagerFactory.getStorage().createStrategy(strategy);
		log.info("创建调度策略成功:" + strategy.toString());

	}
}
