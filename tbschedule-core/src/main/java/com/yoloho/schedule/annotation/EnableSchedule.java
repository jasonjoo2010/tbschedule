package com.yoloho.schedule.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.yoloho.schedule.config.EnableScheduleConfiguration;

/**
 * Enable tbschedule support<br>
 * Support for placeholders
 * 
 * @author jason
 *
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(EnableScheduleConfiguration.class)
public @interface EnableSchedule {
    /**
     * Server address
     * @return
     */
    String address(); // required
    String timeout() default "60000"; // optional, string for placeholder support
    /**
     * Base path
     * @return
     */
    String rootPath(); // required
    /**
     * ACL information if required
     */
    String username();
    String password();
    
    /**
     * Storage backend, eg. zookeeper/redis/jdbc
     * 
     * @return
     */
    String storage() default "";
}
