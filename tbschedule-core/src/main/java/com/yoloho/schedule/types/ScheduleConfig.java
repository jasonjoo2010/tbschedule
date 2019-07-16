package com.yoloho.schedule.types;

import java.util.Map;
import java.util.Properties;

/**
 * Configuration for tbschedule
 * 
 * @author jason
 *
 */
public class ScheduleConfig {
    private String address;
    private String rootPath;
    private String username;
    private String password;
    private String storage;
    private Map<String, String> extra;
    
    public ScheduleConfig() {
    }
    
    public ScheduleConfig(Map<String, String> map) {
        setAddress(map.get("address"));
        setRootPath(map.get("rootPath"));
        setUsername(map.get("username"));
        setPassword(map.get("password"));
    }
    
    public ScheduleConfig(Properties p) {
        setAddress(p.getProperty("address"));
        setRootPath(p.getProperty("rootPath"));
        setUsername(p.getProperty("username"));
        setPassword(p.getProperty("password"));
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }
    
    public void setStorage(String storage) {
        this.storage = storage;
    }
    
    public String getStorage() {
        return storage;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Map<String, String> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, String> extra) {
        this.extra = extra;
    }
    
}
