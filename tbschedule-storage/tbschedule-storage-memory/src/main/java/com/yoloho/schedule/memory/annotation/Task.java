package com.yoloho.schedule.memory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.yoloho.schedule.interfaces.IScheduleTaskDealMulti;

@Documented
@Inherited  
@Retention(RetentionPolicy.RUNTIME)  
@Target({ElementType.TYPE})
public @interface Task {
    /**
     * Which factory should bind to
     * 
     * @return
     */
    String factory() default "";
    
    /**
     * Task name
     * 
     * @return
     */
    String name();
    
    /**
     * If strategy is binded to a bean here's its bean name.
     * 
     * @return
     */
    String beanName() default "";
    
    /**
     * If binded to task it has no use;
     * 
     * @return
     */
    String parameter() default "";
    
    /**
     * How many threads executed jobs paralleled.
     * 
     * @return
     */
    String threadCount() default "1";
    
    /**
     * Millis to delay when no tasks selected
     * 
     * @return
     */
    String sleepTimeNoData() default "500";
    
    /**
     * Fix interval between selecting
     * 
     * @return
     */
    String sleepTimeInterval() default "0";
    
    /**
     * The "eachFetchDataNum" parameter's value when invoked "select"
     * 
     * @return
     */
    String fetchDataNumber() default "500";
    
    /**
     * How many jobs when invoking "execute()".<br />
     * When set to a value bigger than "1" you should make your bean implementing {@link IScheduleTaskDealMulti}
     */
    String executeNumber() default "1";
    
    /**
     * Cron begin.<br>
     * eg. "0 * * * * ?"<br>
     * <pre>
     *   0      *     *    *    *    ?
     * second minute hour day month week
     * </pre>
     * 
     * @return
     */
    String cronBegin() default "";
    
    /**
     * Cron end.<br>
     * When end of cron is empty but begin of cron is not,
     * execution will lasts until no data returned by select().
     * 
     * @return
     */
    String cronEnd() default "";
    
    /**
     * Task items
     * 
     * @return
     */
    String[] taskItems() default {"0"};
}
