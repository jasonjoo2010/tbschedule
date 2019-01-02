package com.taobao.pamirs.schedule.zk;

/**
 * 注意，这里的版本定义的是数据格式版本，不能轻易修改<br>
 * 当产生了数据结构的变化时，方可修改，并且原则上高版本可操作低版本，而低版本遇到高版本将无法初始化
 */
public class Version {
    protected final static String VERSION = "tbschedule-3.2.12";

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
