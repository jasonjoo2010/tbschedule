package com.yoloho.schedule.config;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import com.yoloho.schedule.ScheduleManagerFactory;
import com.yoloho.schedule.annotation.EnableSchedule;
import com.yoloho.schedule.types.ScheduleConfig;

/**
 * Annotation support of tbschedule
 * 
 * @author jason
 *
 */
public class EnableScheduleConfiguration implements DeferredImportSelector {
    /**
     * Injector of the initializing beans
     * 
     */
    public static class Configuration implements ImportBeanDefinitionRegistrar {

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                BeanDefinitionRegistry registry) {
            Map<String, Object> map = importingClassMetadata.getAnnotationAttributes(EnableSchedule.class.getName());
            String name = (String)map.get("name");
            String address = (String)map.get("address");
            String rootPath = (String)map.get("rootPath");
            String username = (String)map.get("username");
            String password = (String)map.get("password");
            injectInitializerBean(registry, name, address, rootPath, username, password);
        }
    }
    
    public static void injectInitializerBean(
            BeanDefinitionRegistry registry,
            String name,
            String address, 
            String rootPath, 
            String username, 
            String password) {
        if (StringUtils.isEmpty(name)) {
            name = ScheduleManagerFactory.class.getName();
        }
        // config
        {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ScheduleConfig.class);
            builder.addPropertyValue("address", address);
            builder.addPropertyValue("rootPath", rootPath);
            builder.addPropertyValue("username", username);
            builder.addPropertyValue("password", password);
            builder.setLazyInit(false);
            registry.registerBeanDefinition(name + "Config", builder.getBeanDefinition());
        }
        // factory
        {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ScheduleManagerFactory.class);
            builder.setLazyInit(false);
            builder.addPropertyReference("config", name + "Config");
            builder.setInitMethodName("init");
            builder.setDestroyMethodName("shutdown");
            registry.registerBeanDefinition(name, builder.getBeanDefinition());
        }
    }
    
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[] {Configuration.class.getName()};
    }
}
