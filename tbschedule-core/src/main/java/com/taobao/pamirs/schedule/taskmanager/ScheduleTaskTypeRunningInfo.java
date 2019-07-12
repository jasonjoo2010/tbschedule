package com.taobao.pamirs.schedule.taskmanager;

import java.sql.Timestamp;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.yoloho.schedule.util.ScheduleUtil;

public class ScheduleTaskTypeRunningInfo {

    private long id;

    /**
     * 任务类型：原始任务类型+"-"+ownSign
     */
    private String taskType;

    /**
     * 原始任务类型
     */
    private String baseTaskType;

    /**
     * 环境
     */
    private String ownSign;

    /**
     * 最后一次任务分配的时间
     */
    private Timestamp lastAssignTime;

    /**
     * 最后一次执行任务分配的服务器
     */
    private String lastAssignUUID;

    private Timestamp gmtCreate;

    private Timestamp gmtModified;
    
    public ScheduleTaskTypeRunningInfo() {
    }
    
    public ScheduleTaskTypeRunningInfo(String taskName, String ownSign) {
        setBaseTaskType(taskName);
        setOwnSign(ownSign);
        setTaskType(ScheduleUtil.getTaskTypeByBaseAndOwnSign(taskName, ownSign));
    }
    
    public ScheduleTaskTypeRunningInfo(String runningEntry) {
        String taskName = ScheduleUtil.splitBaseTaskTypeFromTaskType(runningEntry);
        String ownSign = ScheduleUtil.splitOwnsignFromTaskType(runningEntry);
        setBaseTaskType(taskName);
        setOwnSign(ownSign);
        setTaskType(runningEntry);
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getOwnSign() {
        return ownSign;
    }

    public void setOwnSign(String ownSign) {
        this.ownSign = ownSign;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Timestamp getLastAssignTime() {
        return lastAssignTime;
    }

    public void setLastAssignTime(Timestamp lastAssignTime) {
        this.lastAssignTime = lastAssignTime;
    }

    public String getLastAssignUUID() {
        return lastAssignUUID;
    }

    public void setLastAssignUUID(String lastAssignUUID) {
        this.lastAssignUUID = lastAssignUUID;
    }

    public Timestamp getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Timestamp gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Timestamp getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Timestamp gmtModified) {
        this.gmtModified = gmtModified;
    }

    public String getBaseTaskType() {
        return baseTaskType;
    }

    public void setBaseTaskType(String baseTaskType) {
        this.baseTaskType = baseTaskType;
    }

}
