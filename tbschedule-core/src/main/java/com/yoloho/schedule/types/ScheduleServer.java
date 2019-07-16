package com.yoloho.schedule.types;

import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.lang3.time.FastDateFormat;

/**
 * 调度服务器信息定义
 * @author xuannan
 *
 */
public class ScheduleServer {
	private String uuid;
	private String taskName;
	private String ownSign;
	private String runningEntry;
	/**
	 * 机器IP地址
	 */
	private String ip;

	/**
	 * 机器名称
	 */
	private String hostName;

	/**
	 * 数据处理线程数量
	 */
	private int threadNum;
	/**
	 * 服务开始时间
	 */
	private Timestamp registerTime;
	/**
	 * 最后一次心跳通知时间
	 */
	private Timestamp heartBeatTime;
	/**
	 * 最后一次取数据时间
	 */
	private Timestamp lastFetchDataTime;
	/**
	 * 处理描述信息，例如读取的任务数量，处理成功的任务数量，处理失败的数量，处理耗时
	 * FetchDataCount=4430,FetcheDataNum=438570,DealDataSucess=438570,DealDataFail=0,DealSpendTime=651066
	 */
	private String dealInfoDesc;

	private String nextRunStartTime;
	private String nextRunEndTime;
	
	/**
	 * 配置中心的当前时间
	 */
	private Timestamp centerServerTime;

	/**
	 * 数据版本号
	 */
	private long version;
	
	private String managerFactoryUUID;

	public ScheduleServer() {

	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	/**
	 * @return
	 */
	public String getRunningEntry() {
		return runningEntry;
	}

	public void setRunningEntry(String runningEntry) {
		this.runningEntry = runningEntry;
	}

	public int getThreadNum() {
		return threadNum;
	}

	public void setThreadNum(int threadNum) {
		this.threadNum = threadNum;
	}

	public Timestamp getRegisterTime() {
		return registerTime;
	}

	public void setRegisterTime(Timestamp registerTime) {
		this.registerTime = registerTime;
	}

	public Timestamp getHeartBeatTime() {
		return heartBeatTime;
	}

	public void setHeartBeatTime(Timestamp heartBeatTime) {
		this.heartBeatTime = heartBeatTime;
	}

	public Timestamp getLastFetchDataTime() {
		return lastFetchDataTime;
	}

	public void setLastFetchDataTime(Timestamp lastFetchDataTime) {
		this.lastFetchDataTime = lastFetchDataTime;
	}

	public String getDealInfoDesc() {
		return dealInfoDesc;
	}

	public void setDealInfoDesc(String dealInfoDesc) {
		this.dealInfoDesc = dealInfoDesc;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}


	public Timestamp getCenterServerTime() {
		return centerServerTime;
	}

	public void setCenterServerTime(Timestamp centerServerTime) {
		this.centerServerTime = centerServerTime;
	}

	public String getNextRunStartTime() {
		return nextRunStartTime;
	}

	public void setNextRunStartTime(String nextRunStartTime) {
		this.nextRunStartTime = nextRunStartTime;
	}

	public String getNextRunEndTime() {
		return nextRunEndTime;
	}

	public void setNextRunEndTime(String nextRunEndTime) {
		this.nextRunEndTime = nextRunEndTime;
	}
	
	public Timestamp getNextRunEnd() {
	    try {
	        Date date = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss").parse(nextRunEndTime);
	        if (date != null) {
	            return new Timestamp(date.getTime());
	        }
	    } catch (Exception e) {
        }
        return null;
    }
	
	public Timestamp getNextRunStart() {
	    try {
            Date date = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss").parse(nextRunStartTime);
            if (date != null) {
                return new Timestamp(date.getTime());
            }
        } catch (Exception e) {
        }
        return null;
    }
	
	public String getOwnSign() {
		return ownSign;
	}

	public void setOwnSign(String ownSign) {
		this.ownSign = ownSign;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public void setManagerFactoryUUID(String managerFactoryUUID) {
		this.managerFactoryUUID = managerFactoryUUID;
	}

	public String getManagerFactoryUUID() {
		return managerFactoryUUID;
	}

}
