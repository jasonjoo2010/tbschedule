package com.yoloho.schedule.memory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.yoloho.schedule.memory.config.ScheduleMemoryConfiguration;

/**
 * Scan and initial configuration for tasks and strategies under memory storage
 * 
 * @author jason
 *
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ScheduleMemoryConfiguration.class)
public @interface EnableScheduleMemory {
    /**
     * Scanning package base path
     * 
     * @return
     */
    String[] basePath();
}
