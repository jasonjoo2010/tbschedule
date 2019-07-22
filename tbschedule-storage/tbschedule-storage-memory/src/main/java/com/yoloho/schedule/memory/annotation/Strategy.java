package com.yoloho.schedule.memory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Inherited  
@Retention(RetentionPolicy.RUNTIME)  
@Target({ElementType.TYPE})
public @interface Strategy {
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
     * If strategy is binded to a class here's its full class name.
     * <p>
     * Instance could be automatically created.
     * 
     * @return
     */
    String className() default "";
    
    /**
     * If strategy is binded to a task definition.
     * 
     * @return
     */
    String taskName() default "";
    
    /**
     * How many strategies should boot.
     * 
     * @return
     */
    String count() default "1";
    
    /**
     * If binded to task it has no use;
     * 
     * @return
     */
    String parameter() default "";
}
