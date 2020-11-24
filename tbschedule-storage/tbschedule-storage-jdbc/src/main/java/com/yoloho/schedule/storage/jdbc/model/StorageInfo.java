package com.yoloho.schedule.storage.jdbc.model;

import com.yoloho.enhanced.data.dao.api.PrimaryKey;

/**
 * Store the information or other K/V pairs for storage.
 * 
 * @author jason
 *
 */
public class StorageInfo {
    @PrimaryKey(autoIncrement = true)
    private long id;
    private String key;
    private String value;
    private long expire;
    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    public long getExpire() {
        return expire;
    }
    
    public void setExpire(long expire) {
        this.expire = expire;
    }

}
