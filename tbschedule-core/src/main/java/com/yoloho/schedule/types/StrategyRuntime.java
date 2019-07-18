package com.yoloho.schedule.types;

public class StrategyRuntime {
    private String strategyName;
    private String factoryUuid;
    private String ip;
	private StrategyKind kind; 
	/**
	 * Schedule Name, Class Name or Bean Name
	 */
	private String taskName; 
	private String taskParameter;
	private int	requestNum;
	private int currentNum;
	
    public String getFactoryUuid() {
        return factoryUuid;
    }

    public void setFactoryUuid(String uuid) {
        this.factoryUuid = uuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public StrategyKind getKind() {
        return kind;
    }

    public void setKind(StrategyKind kind) {
        this.kind = kind;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskParameter() {
        return taskParameter;
    }

    public void setTaskParameter(String taskParameter) {
        this.taskParameter = taskParameter;
    }

    public int getRequestNum() {
        return requestNum;
    }

    public void setRequestNum(int requestNum) {
        this.requestNum = requestNum;
    }

    public int getCurrentNum() {
        return currentNum;
    }

    public void setCurrentNum(int currentNum) {
        this.currentNum = currentNum;
    }
    
	@Override
	public String toString() {
		return "ScheduleStrategyRunntime [strategyName=" + strategyName
				+ ", uuid=" + factoryUuid + ", ip=" + ip + ", kind=" + kind
				+ ", taskName=" + taskName + ", taskParameter=" + taskParameter
				+ ", requestNum=" + requestNum + ", currentNum=" + currentNum
				+ "]";
	}
}
