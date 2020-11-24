package com.yoloho.schedule.storage.jdbc;

class FactoryInfo {
    private boolean allow = true;
    private long heartbeat = 0;

    public boolean isAllow() {
        return allow;
    }

    public void setAllow(boolean allow) {
        this.allow = allow;
    }

    public long getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(long heartbeat) {
        this.heartbeat = heartbeat;
    }
}
