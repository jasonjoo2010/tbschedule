package com.taobao.pamirs.schedule.console.vo;

import org.apache.commons.lang3.StringUtils;

import com.taobao.pamirs.schedule.strategy.ScheduleStrategy;
import com.taobao.pamirs.schedule.strategy.ScheduleStrategy.Kind;
import com.yoloho.enhanced.common.util.BeansUtil;
import com.yoloho.enhanced.common.util.JoinerSplitters;

public class StrategyVo {
    private String name;
    private boolean running;
    private Kind kind;
    private String taskName;
    private String parameter;
    private int assignNum;
    private int numOfSingleServer;
    private String ip;
    
    public StrategyVo(ScheduleStrategy strategy) {
        BeansUtil.copyBean(strategy, this);
        this.name = strategy.getStrategyName();
        this.parameter = strategy.getTaskParameter();
        this.running = StringUtils.equalsIgnoreCase(ScheduleStrategy.STS_RESUME, strategy.getSts());
        this.ip = JoinerSplitters.getJoiner(",").join(strategy.getIPList());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void setRunning(boolean running) {
        this.running = running;
    }

    public Kind getKind() {
        return kind;
    }
    
    public void setKind(Kind kind) {
        this.kind = kind;
    }
    
    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }
    
    public int getNumOfSingleServer() {
        return numOfSingleServer;
    }
    
    public void setNumOfSingleServer(int numOfSingleServer) {
        this.numOfSingleServer = numOfSingleServer;
    }
    
    public int getAssignNum() {
        return assignNum;
    }
    
    public void setAssignNum(int assignNum) {
        this.assignNum = assignNum;
    }
    
    public String getIp() {
        return ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }
    
    
}
