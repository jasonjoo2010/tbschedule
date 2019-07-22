package com.yoloho.schedule.types;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.alibaba.fastjson.annotation.JSONField;
import com.yoloho.schedule.interfaces.IScheduleTaskDealMulti;
import com.yoloho.schedule.util.TaskItemUtil;

/**
 * Task
 * 
 * @author xuannan
 * 
 * Restructured by 
 * @author jason
 *
 */
public class Task implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	public static final String TASKKIND_STATIC = "static";
    public static final String TASKKIND_DYNAMIC = "dynamic";
    
	private String name;
    /**
     * Heartbeat interval in millis
     */
    private long heartBeatRate = 5 * 1000;
    
    /**
     * Whether a server seems to be a zombie or is dead 
     * if there was no update after the specific amount of millis.<br>
     * It should be more than <b>2 times</b> of heartbeat interval.
     */
    private long judgeDeadInterval = 1*60*1000;//2分钟
    
    /**
     * Millis to delay when no tasks selected
     * 
     */
    private int sleepTimeNoData = 500;
    
    /**
     * Fix interval between selecting
     */
    private int sleepTimeInterval = 0;
    
    /**
     * The "eachFetchDataNum" parameter's value when invoked "select"
     */
    private int fetchDataNumber = 500;
    
    /**
     * How many jobs when invoking "execute()".<br />
     * When set to a value bigger than "1" you should make your bean implementing {@link IScheduleTaskDealMulti}
     */
    private int executeNumber = 1;
    
    /**
     * How many threads executed paralleled.
     */
    private int threadNumber = 5;
    
    /**
     * Processor type: SLEEP/NOTSLEEP.
     * 
     */
    private String processorType = "SLEEP";
    
    /**
     * Cron begin.<br>
     * eg. "0 * * * * ?"<br>
     * <pre>
     *   0      *     *    *    *    ?
     * second minute hour day month week
     * </pre>
     */
    private String permitRunStartTime;
    
    /**
     * Cron end.<br>
     * When end of cron is empty but begin of cron is not,
     * execution will lasts until no data returned by select().
     */
    private String permitRunEndTime;
    
    /**
     * Expiration of runtime information which not updated for X day(s).
     */
    private double expireOwnSignInterval = 1;
    
    /**
     * Bean name binded.
     */
    private String dealBeanName;
    
    /**
     * Parameter
     */
    private String taskParameter;
    
    private String taskKind = TASKKIND_STATIC;
    
    /**
     * Task items
     */
    private String[] taskItems;
    
    /**
     * Max number of task items one server(thread group) can process.
     */
    private int maxTaskItemsOfOneThreadGroup = 0;
    
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getHeartBeatRate() {
		return heartBeatRate;
	}
	public void setHeartBeatRate(long heartBeatRate) {
		this.heartBeatRate = heartBeatRate;
	}

	public long getJudgeDeadInterval() {
		return judgeDeadInterval;
	}

	public void setJudgeDeadInterval(long judgeDeadInterval) {
		this.judgeDeadInterval = judgeDeadInterval;
	}

	public int getFetchDataNumber() {
		return fetchDataNumber;
	}

	public void setFetchDataNumber(int fetchDataNumber) {
		this.fetchDataNumber = fetchDataNumber;
	}

	public int getExecuteNumber() {
		return executeNumber;
	}

	public void setExecuteNumber(int executeNumber) {
		this.executeNumber = executeNumber;
	}

	public int getSleepTimeNoData() {
		return sleepTimeNoData;
	}

	public void setSleepTimeNoData(int sleepTimeNoData) {
		this.sleepTimeNoData = sleepTimeNoData;
	}

	public int getSleepTimeInterval() {
		return sleepTimeInterval;
	}

	public void setSleepTimeInterval(int sleepTimeInterval) {
		this.sleepTimeInterval = sleepTimeInterval;
	}

	public int getThreadNumber() {
		return threadNumber;
	}

	public void setThreadNumber(int threadNumber) {
		this.threadNumber = threadNumber;
	}

	public String getPermitRunStartTime() {
		return permitRunStartTime;
	}

	public String getProcessorType() {
		return processorType;
	}

	public void setProcessorType(String processorType) {
		this.processorType = processorType;
    }

    public void setPermitRunStartTime(String permitRunStartTime) {
        this.permitRunStartTime = permitRunStartTime;
        if (this.permitRunStartTime != null && this.permitRunStartTime.trim().length() == 0) {
            this.permitRunStartTime = null;
        }
    }

    public String getPermitRunEndTime() {
        return permitRunEndTime;
    }

    public double getExpireOwnSignInterval() {
        return expireOwnSignInterval;
    }

    public void setExpireOwnSignInterval(double expireOwnSignInterval) {
        this.expireOwnSignInterval = expireOwnSignInterval;
    }

    public String getDealBeanName() {
        return dealBeanName;
    }

    public void setDealBeanName(String dealBeanName) {
        this.dealBeanName = dealBeanName;
    }

    public void setPermitRunEndTime(String permitRunEndTime) {
        this.permitRunEndTime = permitRunEndTime;
        if (this.permitRunEndTime != null && this.permitRunEndTime.trim().length() == 0) {
            this.permitRunEndTime = null;
        }

    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public void setTaskItems(String[] aTaskItems) {
        this.taskItems = aTaskItems;
    }

    public String[] getTaskItems() {
        return taskItems;
    }
    
    @JSONField(serialize = false)
    public TaskItem[] getTaskItemList() {
        return TaskItemUtil.parseItems(taskItems);
    }

    public void setTaskKind(String taskKind) {
        this.taskKind = taskKind;
    }

    public String getTaskKind() {
        return taskKind;
    }

    public void setTaskParameter(String taskParameter) {
        this.taskParameter = taskParameter;
    }

    public String getTaskParameter() {
        return taskParameter;
    }

    public int getMaxTaskItemsOfOneThreadGroup() {
        return maxTaskItemsOfOneThreadGroup;
    }

    public void setMaxTaskItemsOfOneThreadGroup(int maxTaskItemsOfOneThreadGroup) {
        this.maxTaskItemsOfOneThreadGroup = maxTaskItemsOfOneThreadGroup;
    }
	
}
