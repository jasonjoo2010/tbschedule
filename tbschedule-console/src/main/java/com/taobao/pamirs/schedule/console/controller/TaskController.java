package com.taobao.pamirs.schedule.console.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.taobao.pamirs.schedule.console.ConsoleManager;
import com.yoloho.enhanced.common.support.MsgBean;
import com.yoloho.enhanced.common.util.JoinerSplitters;
import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.types.RunningEntryRuntime;
import com.yoloho.schedule.types.ScheduleServer;
import com.yoloho.schedule.types.TaskItemRuntime;
import com.yoloho.schedule.types.Task;

@Controller
@RequestMapping("/task")
public class TaskController {
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class.getSimpleName());
    
    @RequestMapping("/index")
    public ModelAndView index(HttpServletResponse response) throws Exception {
        ModelAndView mav = new ModelAndView("task/index");
        if (ConsoleManager.isInitial() == false) {
            response.sendRedirect("/config/modify");
            return null;
        }
        IStorage storage = ConsoleManager.getStorage();
        List<Task> taskList = storage.getTaskNames().stream()
                .map(name -> {
                    try {
                        return storage.getTask(name);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
        mav.addObject("taskList", taskList);
        return mav;
    }
    
    @RequestMapping("/edit")
    public ModelAndView edit(String taskName) throws Exception {
        ModelAndView mav = new ModelAndView("task/edit");
        Task task = ConsoleManager.getStorage().getTask(taskName);
        mav.addObject("isCreate", false);
        if (task == null) {
            task = new Task();
            task.setName("");
            task.setDealBeanName("");
            mav.addObject("isCreate", true);
        }
        mav.addObject("task", task);
        return mav;
    }
    
    @RequestMapping("/runtime")
    public ModelAndView runtime(
            String taskName,
            @RequestParam(required = false) String ownSign
            ) throws Exception {
        ModelAndView mav = new ModelAndView("task/runtime");
        List<RunningEntryRuntime> infoList = ConsoleManager.getStorage().getRunningEntryList(taskName).stream()
                .map(entry -> new RunningEntryRuntime(entry))
                .collect(Collectors.toList());
        IStorage storage = ConsoleManager.getStorage();
        if (StringUtils.isNotEmpty(ownSign)) {
            // filter by ownSign
            infoList = infoList.stream()
                    .filter(i -> StringUtils.equalsIgnoreCase(ownSign, i.getOwnSign()))
                    .collect(Collectors.toList());
        }
        Map<String, Task> taskMap = new HashMap<>();
        Map<String, List<ScheduleServer>> strategyMap = new HashMap<>();
        Map<String, List<TaskItemRuntime>> itemMap = new HashMap<>();
        for (RunningEntryRuntime info : infoList) {
            strategyMap.put(info.getRunningEntry(), ConsoleManager.getStorage().getServerList(info.getTaskName(), info.getOwnSign()));
            itemMap.put(info.getRunningEntry(), storage.getRunningTaskItems(info.getTaskName(), info.getOwnSign()));
            if (!taskMap.containsKey(info.getTaskName())) {
                taskMap.put(info.getTaskName(), ConsoleManager.getStorage().getTask(info.getTaskName()));
            }
        }
        mav.addObject("infoList", infoList);
        mav.addObject("strategyMap", strategyMap);
        mav.addObject("itemMap", itemMap);
        mav.addObject("taskMap", taskMap);
        mav.addObject("taskName", taskName);
        return mav;
    }
    
    @RequestMapping("/clean")
    @ResponseBody
    public Map<String, Object> clean(String taskName) throws Exception {
        MsgBean msgBean = new MsgBean();
        IStorage storage = ConsoleManager.getStorage();
        storage.cleanTaskRunningInfo(taskName);
        msgBean.put("refresh", false);
        return msgBean.returnMsg();
    }
    
    @RequestMapping("/remove")
    @ResponseBody
    public Map<String, Object> remove(String taskName) throws Exception {
        MsgBean msgBean = new MsgBean();
        ConsoleManager.getStorage().removeTask(taskName);
        msgBean.put("refresh", true);
        return msgBean.returnMsg();
    }

    @RequestMapping("/save")
    @ResponseBody
    public Map<String, Object> save(
            String taskName, 
            String dealBean, 
            float heartBeatRate, 
            float judgeDeadInterval,
            int threadNumber,
            String processType,
            int fetchNumber,
            int executeNumber,
            float sleepTimeNoData,
            float sleepTimeInterval,
            String permitRunStartTime,
            String permitRunEndTime,
            int maxTaskItemsOfOneThreadGroup,
            String taskParameter,
            String taskItems,
            boolean isCreate) throws Exception {
        MsgBean msgBean = new MsgBean();
        Task taskType = new Task();
        taskType.setName(taskName);
        taskType.setDealBeanName(dealBean);
        taskType.setHeartBeatRate((long) (heartBeatRate * 1000));
        taskType.setJudgeDeadInterval((long) (judgeDeadInterval * 1000));
        taskType.setThreadNumber(threadNumber);
        taskType.setFetchDataNumber(fetchNumber);
        taskType.setExecuteNumber(executeNumber);
        taskType.setSleepTimeNoData((int) (sleepTimeNoData * 1000));
        taskType.setSleepTimeInterval((int) (sleepTimeInterval * 1000));
        taskType.setProcessorType(processType);
        //taskType.setExpireOwnSignInterval(request.getParameter("expireOwnSignInterval")==null?0: Integer.parseInt(request.getParameter("threadNumber")));
        taskType.setPermitRunStartTime(permitRunStartTime);
        taskType.setPermitRunEndTime(permitRunEndTime);
        taskType.setMaxTaskItemsOfOneThreadGroup(maxTaskItemsOfOneThreadGroup);        
        taskType.setTaskParameter(taskParameter);
        
        String itemDefines = taskItems;
        itemDefines = itemDefines.replace("\r", "");
        itemDefines = itemDefines.replace("\n", "");          
        taskType.setTaskItems(JoinerSplitters.getSplitter(",").splitToList(itemDefines).toArray(new String[0]));
        IStorage storage = ConsoleManager.getStorage();
        if (isCreate) {
            storage.createTask(taskType);
        } else {
            storage.updateTask(taskType);
        }
        return msgBean.returnMsg();
    }
    
}
