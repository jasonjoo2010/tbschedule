package com.taobao.pamirs.schedule.taskmanager;

import java.util.List;

import com.taobao.pamirs.schedule.strategy.ScheduleStrategy;
import com.taobao.pamirs.schedule.strategy.ScheduleStrategyRuntime;
import com.taobao.pamirs.schedule.strategy.TBScheduleManagerFactory;

/**
 * @author jason
 *
 */
public interface IStorage {
    
    long getGlobalTime();
    
    /**
     * Test the storage
     * 
     * @return true for successful
     */
    boolean test();

    boolean init(ScheduleConfig config);

    void shutdown();

    void createTask(ScheduleTaskType task) throws Exception;

    void updateTask(ScheduleTaskType task) throws Exception;
    
    boolean removeTask(String taskName) throws Exception;
    
    /**
     * Clean all running entry data for task specified
     * 
     * @param taskName
     * @throws Exception
     */
    void cleanTaskRunningInfo(String taskName) throws Exception;
    
    /**
     * Clean specified running entry of task
     * 
     * @param taskName
     * @param ownSign
     * @throws Exception
     */
    void cleanTaskRunningInfo(String taskName, String ownSign) throws Exception;
    
    /**
     * @param taskName
     * @param runningEntry Include ownSign in it, taskName -> runningEntry
     */
    void initTaskRunningInfo(String taskName, String ownSign);

    ScheduleTaskType getTask(String taskName) throws Exception;

    void initTaskItemsRunningInfo(String taskName, String ownSign, String uuid) throws Exception;

    /**
     * Get running entry list for task (sorted)
     * 
     * @param taskName
     * @return
     * @throws Exception
     */
    List<String> getRunningEntryList(String taskName) throws Exception;

    /**
     * Get all task name list (sorted)
     * 
     * @return
     * @throws Exception
     */
    List<String> getTaskNames() throws Exception;

    List<ScheduleTaskItem> getRunningTaskItems(String taskName, String ownSign) throws Exception;

    void updateTaskItemRequestServer(String taskName, String ownSign, String taskItem, String server) throws Exception;

    void updateTaskItemCurrentServer(String taskName, String ownSign, String taskItem, String server) throws Exception;

    void setInitialRunningInfoSuccess(String taskName, String ownSign, String uuid) throws Exception;
    
    InitialResult getInitialRunningInfoResult(String taskName, String ownSign) throws Exception;

    boolean isInitialRunningInfoSuccess(String taskName, String ownSign) throws Exception;

    /**
     * Get server's uuid list (sorted)
     * 
     * @param taskName
     * @param ownSign
     * @return
     * @throws Exception
     */
    List<String> getServerUuidList(String taskName, String ownSign) throws Exception;
    
    /**
     * Get server's list (Must be valid or alive)
     * 
     * @param taskName
     * @param ownSign
     * @return
     * @throws Exception
     */
    List<ScheduleServer> getServerList(String taskName, String ownSign) throws Exception;

    /**
     * Unregister the server
     * 
     * @param taskName
     * @param ownSign
     * @param uuid
     * @throws Exception
     */
    void unregisterServer(String taskName, String ownSign, String uuid) throws Exception;

    /**
     * Register a server to storage
     * 
     * @param server
     * @throws Exception
     */
    void registerServer(ScheduleServer server) throws Exception;

    /**
     * Do a heartbeat, heartbeatTime and version will be updated if successful
     * 
     * @param server
     * @return
     * @throws Exception
     */
    boolean heartbeatServer(ScheduleServer server) throws Exception;

    /**
     * Get the current reload sign to decide whether should reload
     * 
     * @param taskName
     * @param ownSign
     * @return
     * @throws Exception
     */
    long getAllServerReload(String taskName, String ownSign) throws Exception;
    
    /**
     * Set the reload sign
     * 
     * @param taskName
     * @param ownSign
     * @throws Exception
     */
    void requestAllServerReload(String taskName, String ownSign) throws Exception;

    /**
     * Move request server to current.
     * Called by previous server.
     * 
     * @param taskName
     * @param ownSign
     * @param uuid
     * @return
     * @throws Exception
     */
    int releaseTaskItemByServer(String taskName, String ownSign, String uuid) throws Exception;

    /**
     * Dump the inner data as string.
     * Different from {@link #export()}, it dump all the structure including runtime data and can be non-structural
     * 
     * @return
     */
    String dump();

    void createStrategy(ScheduleStrategy strategy) throws Exception;

    void updateStrategy(ScheduleStrategy strategy) throws Exception;

    ScheduleStrategy getStrategy(String strategyName) throws Exception;
    
    /**
     * Remove a strategy which is stopped
     * 
     * @param strategyName
     * @return false for not existed strategy
     * @throws Exception
     */
    boolean removeStrategy(String strategyName) throws Exception;

    List<String> getStrategyNames() throws Exception;

    /**
     * Unregister a factory
     * 
     * @param factoryUUID
     * @throws Exception
     */
    void unregisterFactory(String factoryUUID) throws Exception;

    /**
     * Register a factory
     * 
     * @param factory
     * @return strategyName collection that need to be unregisterd
     * @throws Exception
     */
    List<String> registerFactory(TBScheduleManagerFactory factory) throws Exception;

    /**
     * @param strategyName
     * @param factoryUUID
     * @return
     * @throws Exception 
     */
    ScheduleStrategyRuntime getStrategyRuntime(String strategyName, String factoryUUID) throws Exception;

    /**
     * Get runtime list of specified strategy
     * 
     * @param strategyName
     * @return
     * @throws Exception
     */
    List<ScheduleStrategyRuntime> getStrategyRuntimes(String strategyName) throws Exception;

    /**
     * Update runtime information for a factory and strategy pair
     * 
     * @param runtime
     * @throws Exception
     */
    void updateStrategyRuntime(ScheduleStrategyRuntime runtime) throws Exception;

    /**
     * Whether the factory allows to run.
     * 
     * @param factoryUUID
     * @return
     * @throws Exception
     */
    boolean isFactoryAllowExecute(String factoryUUID) throws Exception;

    /**
     * Set whether the factory is allowed to run
     * 
     * @param factoryUUID
     * @param allow
     * @throws Exception
     */
    void setFactoryAllowExecute(String factoryUUID, boolean allow) throws Exception;

    List<String> getFactoryNames() throws Exception;

}