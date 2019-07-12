package com.yoloho.schedule.types;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Strategy is the scheduling unit
 * 
 * @author jason
 *
 */
public class Strategy {
    public static String STS_PAUSE = "pause";
    public static String STS_RESUME = "resume";
    
    private String name;
    private String[] ipList;
    private int numOfSingleServer;
    
    /**
     * 指定需要执行调度的机器数量
     */
    private int assignNum;

    private StrategyKind kind;

    /**
     * Schedule Name,Class Name、Bean Name
     */
    private String taskName;

    private String taskParameter;

    /**
     * 服务状态: pause,resume
     */
    private String sts = STS_RESUME;


    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAssignNum() {
        return assignNum;
    }

    public void setAssignNum(int assignNum) {
        this.assignNum = assignNum;
    }

    public String[] getIPList() {
        return ipList;
    }

    public void setIPList(String[] iPList) {
        ipList = iPList;
    }

    public void setNumOfSingleServer(int numOfSingleServer) {
        this.numOfSingleServer = numOfSingleServer;
    }

    public int getNumOfSingleServer() {
        return numOfSingleServer;
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

    public String getSts() {
        return sts;
    }

    public void setSts(String sts) {
        this.sts = sts;
    }
}
