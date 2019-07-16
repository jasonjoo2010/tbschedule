package com.taobao.pamirs.schedule.console;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yoloho.schedule.ScheduleManagerFactory;
import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.types.ScheduleConfig;

public class ConsoleManager {
    protected static transient Logger log = LoggerFactory.getLogger(ConsoleManager.class);

    public final static String defaultConfigFile = "pamirsScheduleConfig.properties";
    public final static String configFile = System.getProperty("user.dir") + File.separator + defaultConfigFile;
    static {
        synchronized(ConsoleManager.class) {
            try {
                initial();
            } catch (Exception e) {
                log.error("Initial schedule factory failed", e);
            }
        }
    }

    private static ScheduleManagerFactory scheduleManagerFactory;

    public static boolean isInitial() throws Exception {
        return scheduleManagerFactory != null;
    }

    public static boolean initial() throws Exception {
        if (isInitial()) {
            return true;
        }
        File file = new File(configFile);
        scheduleManagerFactory = new ScheduleManagerFactory();
        scheduleManagerFactory.setEnableSchedule(false);
        if (file.exists() == true) {
            // Console不启动调度能力
            Properties p = new Properties();
            FileReader reader = new FileReader(file);
            p.load(reader);
            reader.close();
            scheduleManagerFactory.init(p);
            log.info("Load configuration: {}", configFile);
            return true;
        }
        // fail back to resource file
        InputStream configStream = ConsoleManager.class.getResourceAsStream("/" + defaultConfigFile);
        if (configStream != null) {
            Properties p = new Properties();
            p.load(configStream);
            configStream.close();
            scheduleManagerFactory.init(p);
            log.info("Load configuration: {}", defaultConfigFile);
            return true;
        }
        return false;
    }

    public static ScheduleManagerFactory getScheduleManagerFactory() throws Exception {
        if (isInitial() == false) {
            initial();
        }
        return scheduleManagerFactory;
    }
    
    public static IStorage getStorage() throws Exception {
        return getScheduleManagerFactory().getStorage();
    }
    
    private static Properties getDefaultProperties(){
        Properties result = new Properties();
        result.setProperty("address", "localhost:2181");
        result.setProperty("rootPath", "/taobao-pamirs-schedule/huijin");
        result.setProperty("username", "ScheduleAdmin");
        result.setProperty("password", "password");
        
        return result;
    }

    public static ScheduleConfig loadConfig() throws IOException {
        File file = new File(configFile);
        Properties properties;
        if (file.exists() == false) {
            properties = getDefaultProperties();
        } else {
            properties = new Properties();
            FileReader reader = new FileReader(file);
            properties.load(reader);
            reader.close();
        }
        return new ScheduleConfig(properties);
    }

    public static void saveConfigInfo(ScheduleConfig config) throws Exception {
        try {
            FileWriter writer = new FileWriter(configFile);
            Properties result = new Properties();
            result.setProperty("address", config.getAddress());
            result.setProperty("rootPath", config.getRootPath());
            result.setProperty("username", config.getUsername());
            result.setProperty("password", config.getPassword());
            result.store(writer, "");
            writer.close();
        } catch (Exception ex) {
            throw new Exception("不能写入配置信息到文件：" + configFile, ex);
        }
        if (scheduleManagerFactory == null) {
            initial();
        } else {
            scheduleManagerFactory.reInit(config);
        }
    }

}
