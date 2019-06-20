package com.taobao.pamirs.schedule.config;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import com.google.common.base.Preconditions;
import com.taobao.pamirs.schedule.annotation.EnableSchedule;
import com.taobao.pamirs.schedule.strategy.TBScheduleManagerFactory;

/**
 * Annotation support of tbschedule
 * 
 * @author jason
 *
 */
public class EnableScheduleConfiguration implements DeferredImportSelector {
    /**
     * Actual initializer
     * 
     */
    public static class ScheduleInit implements ApplicationContextAware {
        private TBScheduleManagerFactory factory;
        
        public ScheduleInit(String address, String rootPath, String username, String password, String timeout) {
            int zkSessionTimeout = NumberUtils.toInt(timeout);
            Preconditions.checkArgument(StringUtils.isNotEmpty(address), "Address should not be empty");
            Preconditions.checkArgument(StringUtils.isNotEmpty(rootPath), "rootPath should not be empty");
            Preconditions.checkArgument(zkSessionTimeout >= 2000, "timeout should not be less than 2000 milliseconds");
            Map<String, String> zkConfig = new HashMap<>(5);
            zkConfig.put("zkConnectString", address);
            zkConfig.put("rootPath", rootPath);
            zkConfig.put("zkSessionTimeout", String.valueOf(zkSessionTimeout));
            zkConfig.put("userName", username);
            zkConfig.put("password", password);
            factory = new TBScheduleManagerFactory();
            factory.setZkConfig(zkConfig);
        }
        
        @PostConstruct
        public void init() throws Exception {
            factory.init();
        }
        
        @PreDestroy
        public void destroy() throws Exception {
            factory.stopAll();
        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            factory.setApplicationContext(applicationContext);
        }
    }
    /**
     * Injector of the initializing beans
     * 
     */
    public static class Configuration implements ImportBeanDefinitionRegistrar {

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                BeanDefinitionRegistry registry) {
            Map<String, Object> map = importingClassMetadata.getAnnotationAttributes(EnableSchedule.class.getName());
            String address = (String)map.get("address");
            String rootPath = (String)map.get("rootPath");
            String username = (String)map.get("username");
            String password = (String)map.get("password");
            String timeout = (String)map.get("timeout");
            injectInitializerBean(registry, address, timeout, rootPath, username, password);
        }
    }
    
    public static void injectInitializerBean(
            BeanDefinitionRegistry registry,
            String address, 
            String timeout, 
            String rootPath, 
            String username, 
            String password) {
        // 插入中转类
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ScheduleInit.class);
        builder.addConstructorArgValue(address);
        builder.addConstructorArgValue(rootPath);
        builder.addConstructorArgValue(username);
        builder.addConstructorArgValue(password);
        builder.addConstructorArgValue(timeout);
        builder.setLazyInit(false);
        registry.registerBeanDefinition(ScheduleInit.class.getName(), builder.getBeanDefinition());
    }
    
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[] {Configuration.class.getName()};
    }
}
