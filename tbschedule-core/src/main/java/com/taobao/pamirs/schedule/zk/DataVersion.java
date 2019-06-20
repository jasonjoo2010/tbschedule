package com.taobao.pamirs.schedule.zk;

/**
 * Data persistent structure version
 */
public class DataVersion {
    private final static String VERSION = "tbschedule-3.2.12";

    public static String getVersion() {
        return VERSION;
    }

    public static boolean isCompatible(String dataVersion) {
        if (VERSION.compareTo(dataVersion) >= 0) {
            return true;
        } else {
            return false;
        }
    }
   
}
