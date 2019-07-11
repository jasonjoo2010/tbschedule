package com.yoloho.schedule.storage.zk.util;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.taobao.pamirs.schedule.ScheduleUtil;

public class PathUtil {
    public static String taskBasePath() {
        return "/baseTaskType";
    }
    
    public static String strategyBasePath() {
        return "/strategy";
    }
    
    public static String factoryBasePath() {
        return "/factory";
    }
    
    public static String taskPath(String taskName) {
        taskName = StringUtils.strip(taskName, "/");
        Preconditions.checkArgument(StringUtils.isNotEmpty(taskName), "Task name can not be empty.");
        Preconditions.checkArgument(StringUtils.containsNone(taskName, "/"), "Task name can not contain separator.");
        Preconditions.checkArgument(StringUtils.containsNone(taskName, "$"), "Task name can not contain special chars.");
        return taskBasePath() + "/" + taskName;
    }
    
    public static String strategyPath(String strategyName) {
        strategyName = StringUtils.strip(strategyName, "/");
        Preconditions.checkArgument(StringUtils.isNotEmpty(strategyName), "Strategy name can not be empty.");
        Preconditions.checkArgument(StringUtils.containsNone(strategyName, "/"), "Strategy name can not contain separator.");
        Preconditions.checkArgument(StringUtils.countMatches(strategyName, "$") <= 1, "Strategy name can not contain more than one '$'.");
        return strategyBasePath() + "/" + strategyName;
    }
    
    public static String factoryPath(String factoryUUID) {
        return factoryBasePath() + "/" + factoryUUID;
    }
    
    public static String factoryForStrategyPath(String strategyName, String factoryUUID) {
        return strategyPath(strategyName) + "/" + factoryUUID;
    }
    
    public static String runningEntryPath(String taskName, String ownSign) {
        ownSign = StringUtils.strip(ownSign, "/");
        String runningEntry = ScheduleUtil.getTaskTypeByBaseAndOwnSign(taskName, ownSign);
        return taskPath(taskName) + "/" + runningEntry;
    }
    
    public static String taskItemBasePath(String taskName, String ownSign) {
        return runningEntryPath(taskName, ownSign) + "/taskItem";
    }
    
    public static String taskItemPath(String taskName, String ownSign, String taskItem) {
        return taskItemBasePath(taskName, ownSign) + "/" + taskItem;
    }
    
    public static String serverBasePath(String taskName, String ownSign) {
        return runningEntryPath(taskName, ownSign) + "/server";
    }
    
    public static String serverPath(String taskName, String ownSign, String serverName) {
        return serverBasePath(taskName, ownSign) + "/" + serverName;
    }
}
