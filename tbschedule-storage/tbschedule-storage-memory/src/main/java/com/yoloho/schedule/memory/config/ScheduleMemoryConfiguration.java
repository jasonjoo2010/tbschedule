package com.yoloho.schedule.memory.config;

import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import com.google.common.base.Preconditions;
import com.yoloho.schedule.ScheduleManagerFactory;
import com.yoloho.schedule.memory.annotation.EnableScheduleMemory;
import com.yoloho.schedule.memory.annotation.Strategy;
import com.yoloho.schedule.memory.annotation.Task;
import com.yoloho.schedule.storage.memory.MemoryStorage;
import com.yoloho.schedule.types.StrategyKind;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;

/**
 * Annotation support of tbschedule
 * 
 * @author jason
 *
 */
public class ScheduleMemoryConfiguration implements DeferredImportSelector {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleMemoryConfiguration.class.getSimpleName());
    private static final AtomicLong beanId = new AtomicLong();
    
    /**
     * Actual initializer
     * 
     */
    public static class ScheduleMemoryConfigInit implements ApplicationContextAware {
        private List<String> strategyList;
        private List<String> taskList;
        
        public ScheduleMemoryConfigInit(List<String> strategyList, List<String> taskList) {
            this.strategyList = strategyList;
            this.taskList = taskList;
        }
        
        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            for (String taskClass : taskList) {
                try {
                    Class<?> clz = Class.forName(taskClass);
                    Task anno = clz.getAnnotation(Task.class);
                    com.yoloho.schedule.types.Task task = new com.yoloho.schedule.types.Task();
                    task.setName(anno.name());
                    task.setDealBeanName(anno.beanName());
                    task.setTaskParameter(anno.parameter());
                    task.setThreadNumber(NumberUtils.toInt(anno.threadCount()));
                    task.setSleepTimeNoData(NumberUtils.toInt(anno.sleepTimeNoData()));
                    task.setSleepTimeInterval(NumberUtils.toInt(anno.sleepTimeInterval()));
                    task.setFetchDataNumber(NumberUtils.toInt(anno.fetchDataNumber()));
                    task.setExecuteNumber(NumberUtils.toInt(anno.executeNumber()));
                    task.setPermitRunStartTime(anno.cronBegin());
                    task.setPermitRunEndTime(anno.cronEnd());
                    task.setTaskItems(anno.taskItems());
                    task.setHeartBeatRate(5000);
                    task.setJudgeDeadInterval(60000);
                    // fetch factory
                    String factoryName = anno.factory();
                    ScheduleManagerFactory factory = null;
                    if (StringUtils.isEmpty(factoryName)) {
                        factory = applicationContext.getBean(ScheduleManagerFactory.class);
                    } else {
                        factory = (ScheduleManagerFactory) applicationContext.getBean(factoryName);
                    }
                    if (factory == null) {
                        logger.warn("Ignore task {}: Cound not locate the schedule factory.", taskClass);
                        continue;
                    }
                    Preconditions.checkArgument(MemoryStorage.class.isAssignableFrom(factory.getStorage().getClass()),
                            "Specific factory's storage is not memory");
                    factory.getStorage().createTask(task);
                    logger.info("Create task: {}", task.getName());
                } catch (Exception e) {
                    logger.warn("Ignore task: {}", taskClass, e);
                }
            }
            for (String strategyClass : strategyList) {
                try {
                    Class<?> clz = Class.forName(strategyClass);
                    Strategy anno = clz.getAnnotation(Strategy.class);
                    com.yoloho.schedule.types.Strategy strategy = new com.yoloho.schedule.types.Strategy();
                    strategy.setName(anno.name());
                    strategy.setAssignNum(NumberUtils.toInt(anno.count()));
                    strategy.setIPList(new String[] {"127.0.0.1"});
                    strategy.setTaskParameter(anno.parameter());
                    strategy.setNumOfSingleServer(0);
                    strategy.setSts(com.yoloho.schedule.types.Strategy.STS_RESUME);
                    // fetch factory
                    String factoryName = anno.factory();
                    ScheduleManagerFactory factory = null;
                    if (StringUtils.isEmpty(factoryName)) {
                        factory = applicationContext.getBean(ScheduleManagerFactory.class);
                    } else {
                        factory = (ScheduleManagerFactory) applicationContext.getBean(factoryName);
                    }
                    if (factory == null) {
                        logger.warn("Ignore strategy {}: Cound not locate the schedule factory.", strategyClass);
                        continue;
                    }
                    if (StringUtils.isNotEmpty(anno.beanName())) {
                        strategy.setKind(StrategyKind.Bean);
                        strategy.setTaskName(anno.beanName());
                    } else if (StringUtils.isNotEmpty(anno.className())) {
                        strategy.setKind(StrategyKind.Java);
                        strategy.setTaskName(anno.className());
                    } else if (StringUtils.isNotEmpty(anno.taskName())) {
                        strategy.setKind(StrategyKind.Schedule);
                        strategy.setTaskName(anno.taskName());
                    } else {
                        logger.warn("Ignore strategy {}: None of beanName/className/taskName is set.", strategyClass);
                        continue;
                    }
                    Preconditions.checkArgument(MemoryStorage.class.isAssignableFrom(factory.getStorage().getClass()),
                            "Specific factory's storage is not memory");
                    factory.getStorage().createStrategy(strategy);
                    logger.info("Create strategy: {}", strategy.getName());
                } catch (Exception e) {
                    logger.warn("Ignore strategy: {}", strategyClass, e);
                }
            }
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
            Map<String, Object> map = importingClassMetadata.getAnnotationAttributes(EnableScheduleMemory.class.getName());
            String[] basePathArr = (String[])map.get("basePath");
            Preconditions.checkNotNull(basePathArr, "Base path array for scanning should not be empty");
            Preconditions.checkArgument(basePathArr.length > 0, "Base path array for scanning should not be empty");
            ScanResult scanResult = new FastClasspathScanner(basePathArr)
                    .ignoreFieldVisibility()
                    .setAnnotationVisibility(RetentionPolicy.RUNTIME)
                    .enableFieldAnnotationIndexing()
                    .scan();
            List<String> strategyClassList = scanResult.getNamesOfClassesWithAnnotation(Strategy.class);
            List<String> taskClassList = scanResult.getNamesOfClassesWithAnnotation(Task.class);
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ScheduleMemoryConfigInit.class);
            builder.addConstructorArgValue(strategyClassList);
            builder.addConstructorArgValue(taskClassList);
            builder.setLazyInit(false);
            registry.registerBeanDefinition(ScheduleMemoryConfigInit.class.getName() + "#" + beanId.getAndIncrement(),
                    builder.getBeanDefinition());
        }
    }
    
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[] {Configuration.class.getName()};
    }
}
