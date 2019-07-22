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
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ScheduleManagerFactory.class);
        builder.setLazyInit(false);
        if (StringUtils.isEmpty(name)) {
            name = ScheduleManagerFactory.class.getName();
        }
        ScheduleConfig config = new ScheduleConfig();
        config.setAddress(address);
        config.setRootPath(rootPath);
        config.setUsername(username);
        config.setPassword(password);
        builder.addPropertyValue("config", config);
        builder.setInitMethodName("init");
        builder.setDestroyMethodName("stopAll");
        registry.registerBeanDefinition(name, builder.getBeanDefinition());
    }
    
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[] {Configuration.class.getName()};
    }
}
