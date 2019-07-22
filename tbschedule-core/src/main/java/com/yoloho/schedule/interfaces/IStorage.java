package com.yoloho.schedule.interfaces;

import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.yoloho.schedule.ScheduleManagerFactory;
import com.yoloho.schedule.types.InitialResult;
import com.yoloho.schedule.types.ScheduleConfig;
import com.yoloho.schedule.types.ScheduleServer;
import com.yoloho.schedule.types.Strategy;
import com.yoloho.schedule.types.StrategyRuntime;
import com.yoloho.schedule.types.Task;
import com.yoloho.schedule.types.TaskItemRuntime;

/**
 * Storage interface
 * 
 * @author jason
 *
 */
public interface IStorage {
    //////////////////////////////////////////////////////////////////////////////////////////////
    // Comparators
    /**
     * Comparator for factory's name
     */
    public static final Comparator<String> COMPARATOR_UUID = new Comparator<String>() {
        public int compare(String u1, String u2) {
            return u1.substring(u1.lastIndexOf("$") + 1).compareTo(
                    u2.substring(u2.lastIndexOf("$") + 1));
        }
    };
    public static final Comparator<TaskItemRuntime> COMPARATOR_TASK_ITEM_RUNTIME = new Comparator<TaskItemRuntime>() {
        public int compare(TaskItemRuntime u1, TaskItemRuntime u2) {
            if (StringUtils.isNumeric(u1.getTaskItem()) && StringUtils.isNumeric(u2.getTaskItem())) {
                int iU1 = Integer.parseInt(u1.getTaskItem());
                int iU2 = Integer.parseInt(u2.getTaskItem());
                if (iU1 == iU2) {
                    return 0;
                } else if (iU1 > iU2) {
                    return 1;
                } else {
                    return -1;
                }
            } else {
                return u1.getTaskItem().compareTo(u2.getTaskItem());
            }
        }
    };
    
    /**
     * Invoked when storage becomes avaliable, whether initial or reconnect
     *
     */
    interface OnConnected {
        void connected(IStorage storage);
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////
    // Storage Related
    /**
     * The storage server side time, in millisecond timestamp.
     * <p>
     * This will help all the nodes use the same time avoiding the difference of local system's
     * <p>
     * NOTE: It should be efficient to call frequently and concurrently.
     * 
     * @return
     */
    long getGlobalTime();
    
    /**
     * Fetch a increasing, never duplicated, global (in distribution environment) sequence number
     * 
     * @return
     * @throws Exception When failed
     */
    long getSequenceNumber() throws Exception;
    
    /**
     * Storage name(Distinguish to other implementations)
     * 
     * @return
     */
    String getName();
    
    /**
     * Test the storage
     * 
     * @return true for successful
     */
    boolean test();

    /**
     * Initialize the storage, maybe it can be called multiple times to reconfigure it
     * 
     * @param config
     * @param onConnected
     * @return
     */
    boolean init(ScheduleConfig config, OnConnected onConnected);

    /**
     * Shutdown the storage
     */
    void shutdown();
    
    /**
     * Dump the inner data as string.
     * Different from {@link #export()}, it dump all the structure including runtime data and can be non-structural
     * 
     * @return
     * @throws Exception 
     */
    String dump() throws Exception;
    
    //////////////////////////////////////////////////////////////////////////////////////////////
    // Task Related
    /**
     * Create task
     * 
     * @param task
     * @throws Exception When failed
     */
    void createTask(Task task) throws Exception;

    /**
     * Update task
     * 
     * @param task
     * @throws Exception When failed
     */
    void updateTask(Task task) throws Exception;
    
    /**
     * Remove task
     * 
     * @param taskName
     * @return
     * @throws Exception When failed
     */
    boolean removeTask(String taskName) throws Exception;
    
    /**
     * Fetch a task's info by name
     * 
     * @param taskName
     * @return
     * @throws Exception When failed
     */
    Task getTask(String taskName) throws Exception;
    
    /**
     * Get all task name list (sorted)
     * 
     * @return
     * @throws Exception When failed
     */
    List<String> getTaskNames() throws Exception;
    
    //////////////////////////////////////////////////////////////////////////////////////////////
    // Strategy related
    /**
     * Create strategy
     * 
     * @param strategy
     * @throws Exception When failed
     */
    void createStrategy(Strategy strategy) throws Exception;

    /**
     * Update strategy
     * 
     * @param strategy
     * @throws Exception When failed
     */
    void updateStrategy(Strategy strategy) throws Exception;

    /**
     * Remove a strategy which is halted
     * 
     * @param strategyName
     * @return false for not existed strategy
     * @throws Exception When failed
     */
    boolean removeStrategy(String strategyName) throws Exception;
    
    /**
     * Fetch a strategy by name
     * 
     * @param strategyName
     * @return
     * @throws Exception When failed
     */
    Strategy getStrategy(String strategyName) throws Exception;

    /**
     * Fetch all strategies' name list(Sorted)
     * 
     * @return
     * @throws Exception When failed
     */
    List<String> getStrategyNames() throws Exception;
    
    //////////////////////////////////////////////////////////////////////////////////////////////
    // Factory (Machine/Node) Related
    /**
     * Register a factory (uuid must be set)
     * 
     * @param factory
     * @return The strategy name list which should not be scheduled on it
     *         anymore. They should be stopped (if running) and clear.
     * @throws Exception
     *             When failed
     */
    List<String> registerFactory(ScheduleManagerFactory factory) throws Exception;
    
    /**
     * Whether the factory is allowed to run.
     * 
     * @param factoryUUID
     * @return
     * @throws Exception When failed
     */
    boolean isFactoryAllowExecute(String factoryUUID) throws Exception;

    /**
     * Set whether the factory is allowed to run
     * 
     * @param factoryUUID
     * @param allow
     * @throws Exception When failed
     */
    void setFactoryAllowExecute(String factoryUUID, boolean allow) throws Exception;
    
    /**
     * Fetch the runtime information of the strategy in the factory
     * 
     * @param strategyName
     * @param factoryUUID
     * @return null for no runtime information
     * @throws Exception When failed
     */
    StrategyRuntime getStrategyRuntime(String strategyName, String factoryUUID) throws Exception;

    /**
     * Fetch all factories' uuid list (Sorted)
     * 
     * @return
     * @throws Exception When failed
     */
    List<String> getFactoryUuidList() throws Exception;
    
    /**
     * Clear all strategies' information registered in the factory specified.
     * 
     * @param factoryUUID
     * @throws Exception When failed
     */
    void clearStrategiesOfFactory(String factoryUUID) throws Exception;
    
    /**
     * Get running information of factories for specified strategy
     * 
     * @param strategyName
     * @return An empty list when no runtime, <b>never null</b>
     * @throws Exception When failed
     */
    List<StrategyRuntime> getRuntimesOfStrategy(String strategyName) throws Exception;

    /**
     * Update runtime information of factory for strategy
     * 
     * @param runtime
     * @throws Exception When failed
     */
    void updateRuntimeOfStrategy(StrategyRuntime runtime) throws Exception;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Schedule Server (Thread Group for Schedule Type Task) Related
    /**
     * Register a server to storage.<br>
     * Schedule server is a three-tuple object: ((task, ownsign), uuid).<br>
     * 
     * @param server
     *            The server to be registered.
     * @throws Exception When failed.
     */
    void createServer(ScheduleServer server) throws Exception;
    
    /**
     * Update a server to storage.
     * 
     * @param server
     *            The server to be registered.
     * @return true for success
     * @throws Exception When failed.
     */
    boolean updateServer(ScheduleServer server) throws Exception;
    
    /**
     * Unregister the server
     * 
     * @param taskName
     * @param ownSign
     * @param uuid Server's uuid
     * @throws Exception When failed.
     */
    void removeServer(String taskName, String ownSign, String serverUuid) throws Exception;
    
    /**
     * Get server's uuid list (sorted)
     * 
     * @param taskName
     * @param ownSign
     * @return
     * @throws Exception When failed.
     */
    List<String> getServerUuidList(String taskName, String ownSign) throws Exception;
    
    /**
     * Get server
     * 
     * @param taskName
     * @param ownSign
     * @return
     * @throws Exception When failed.
     */
    ScheduleServer getServer(String taskName, String ownSign, String serverUuid) throws Exception;

    /**
     * Get the current scheduling iteration version for specified running entry.<br>
     * It can be used to decide whether should reload the configuration.
     * 
     * @param taskName
     * @param ownSign
     * @return Scheduling version
     * @throws Exception When failed
     */
    long getServerSchedulingVersion(String taskName, String ownSign) throws Exception;
    
    /**
     * Increase the version to make leader reload configuration up-to-date.
     * 
     * @param taskName
     * @param ownSign
     * @throws Exception
     */
    void increaseServerSchedulingVersion(String taskName, String ownSign) throws Exception;
    
    //////////////////////////////////////////////////////////////////////////////////
    // Running Entry Related
    /**
     * Empty the running entry structure
     * 
     * @param taskName
     * @param ownSign
     * @throws Exception
     */
    void emptyTaskItems(String taskName, String ownSign) throws Exception;

    /**
     * Initial the running entry substructure
     * 
     * @param taskName
     * @param ownSign
     * @throws Exception
     */
    void initTaskItems(String taskName, String ownSign) throws Exception;
    
    /**
     * Set the standby server for the task item of running entry
     * 
     * @param taskName
     * @param ownSign
     * @param taskItem
     * @param server
     * @throws Exception
     */
    void updateTaskItemRequestServer(String taskName, String ownSign, String taskItem, String server) throws Exception;

    /**
     * Set the active server for the task item of running entry
     * 
     * @param taskName
     * @param ownSign
     * @param taskItem
     * @param server
     * @throws Exception
     */
    void updateTaskItemCurrentServer(String taskName, String ownSign, String taskItem, String server) throws Exception;
    
    /**
     * Get task items of running entry (sorted)
     * 
     * @param taskName
     * @param ownSign
     * @return
     * @throws Exception
     */
    List<TaskItemRuntime> getTaskItems(String taskName, String ownSign) throws Exception;
    
    /**
     * Remove specified running entry and all runtime informations related
     * 
     * @param taskName
     * @param ownSign
     * @throws Exception
     */
    void removeRunningEntry(String taskName, String ownSign) throws Exception;
    
    /**
     * Get running entry list for task (sorted)
     * 
     * @param taskName
     * @return
     * @throws Exception
     */
    List<String> getRunningEntryList(String taskName) throws Exception;
    
    /**
     * Flag a successful initializing for specific running entry
     * 
     * @param taskName
     * @param ownSign
     * @param initializerUuid The server who does initializing works
     * @throws Exception When failed unexpectly
     */
    void updateTaskItemsInitialResult(String taskName, String ownSign, String initializerUuid) throws Exception;
    
    /**
     * Get the initializing result of running entry
     * 
     * @param taskName
     * @param ownSign
     * @return
     * @throws Exception
     */
    InitialResult getInitialRunningInfoResult(String taskName, String ownSign) throws Exception;
    
    /**
     * Do releasing works if possible. (Check if there is a server request execusion)<br>
     * Called by current owner.
     * 
     * @param taskName
     * @param ownSign
     * @param ownerUuid
     * @return How many task items released.
     * @throws Exception
     */
    int releaseTaskItemByOwner(String taskName, String ownSign, String ownerUuid) throws Exception;

}