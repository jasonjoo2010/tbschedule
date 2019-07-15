package com.yoloho.schedule.types;

import java.sql.Timestamp;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.yoloho.schedule.util.ScheduleUtil;

public class RunningEntryRuntime {

    private long id;

    /**
     * taskName$ownSign
     */
    private String runningEntry;
    private String taskName;
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
    
    public RunningEntryRuntime() {
    }
    
    public RunningEntryRuntime(String taskName, String ownSign) {
        setTaskName(taskName);
        setOwnSign(ownSign);
        setRunningEntry(ScheduleUtil.runningEntryFromTaskName(taskName, ownSign));
    }
    
    public RunningEntryRuntime(String runningEntry) {
        String taskName = ScheduleUtil.taskNameFromRunningEntry(runningEntry);
        String ownSign = ScheduleUtil.ownsignFromRunningEntry(runningEntry);
        setTaskName(taskName);
        setOwnSign(ownSign);
        setRunningEntry(runningEntry);
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

    public String getRunningEntry() {
        return runningEntry;
    }

    public void setRunningEntry(String runningEntry) {
        this.runningEntry = runningEntry;
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

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

}
