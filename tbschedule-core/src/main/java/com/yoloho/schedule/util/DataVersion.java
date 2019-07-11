package com.yoloho.schedule.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Data persistent structure version
 */
public class DataVersion {
    private final static Pattern VERSION_PATTERN = Pattern.compile("^tbschedule-(\\d+)\\.(\\d+)\\.(\\d+)$");
    private final static int VERSION = 40100;
    private final static int VERSION_LOWEST = 40100;

    public static String getCurrentVersion() {
        int v1 = VERSION / 10000;
        int v2 = (VERSION % 10000) / 100;
        int v3 = VERSION % 100;
        return "tbschedule-" + v1 + "." + v2 + "." + v3;
    }
    
    private static int parseVersion(String version) {
        if (StringUtils.isEmpty(version)) {
            return VERSION_LOWEST;
        }
        Matcher m = VERSION_PATTERN.matcher(version);
        if (m.find()) {
            int v1 = NumberUtils.toInt(m.group(1));
            int v2 = NumberUtils.toInt(m.group(2));
            int v3 = NumberUtils.toInt(m.group(3));
            return v1 * 10000 + v2 * 100 + v3;
        } else {
            if (version.startsWith("tbschedule-")) {
                return 30112;
            } else {
                return VERSION_LOWEST;
            }
        }
    }

    public static boolean isCompatible(String dataVersion) {
        int dataVer = parseVersion(dataVersion);
        if (dataVer < VERSION_LOWEST) {
            return false;
        }
        if (VERSION >= dataVer) {
            return true;
        } else {
            return false;
        }
    }
   
}
