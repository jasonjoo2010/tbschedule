package com.yoloho.schedule.interfaces;

public interface ITaskProcessor {
	 /**
	  * Whether it still has remained task to deal with
	  * 
	  * @return
	  */
	 boolean hasRemainedTask();
	 
	 /**
	  * Whether it's sleeping
	  * 
	  * @return
	  */
	 boolean isSleeping();
	 
	 /**
	  * Stop the scheduling
	  * 
	  * @throws Exception
	  */
	 void stopSchedule() throws Exception;
	 
	 /**
	  * 清除所有已经取到内存中的数据，在心跳线程失败的时候调用，避免数据重复
	  */
	 void clearAllHasFetchData();
}
