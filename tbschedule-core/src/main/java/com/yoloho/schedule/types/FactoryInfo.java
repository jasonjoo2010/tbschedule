package com.yoloho.schedule.types;

public class FactoryInfo {
	private String uuid;
	private boolean start;

	public void setStart(boolean start) {
		this.start = start;
	}

	public boolean isStart() {
		return start;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getUuid() {
		return uuid;
	}
}
