package com.yoloho.schedule.types;

/**
 * 任务定义，提供关键信息给使用者
 * @author xuannan
 *
 */
public class TaskItem {
	/**
	 * 任务项ID
	 */
	private String taskItemId;
	/**
	 * 任务项自定义参数
	 */
	private String parameter;
	
	public TaskItem() {
    }
	
	public TaskItem(String taskItem, String parameter) {
	    setTaskItemId(taskItem);
	    setParameter(parameter);
    }
	
	public void setParameter(String parameter) {
		this.parameter = parameter;
	}
	public String getParameter() {
		return parameter;
	}
	public void setTaskItemId(String taskItemId) {
		this.taskItemId = taskItemId;
	}
	public String getTaskItemId() {
		return taskItemId;
	}
	@Override
	public String toString() {
		return "(t=" + taskItemId + ",p="
				+ parameter + ")";
	}
	

}
