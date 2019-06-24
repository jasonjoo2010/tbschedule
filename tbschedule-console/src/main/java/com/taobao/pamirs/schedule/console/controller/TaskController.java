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

import com.taobao.pamirs.schedule.ConsoleManager;
import com.taobao.pamirs.schedule.taskmanager.ScheduleServer;
import com.taobao.pamirs.schedule.taskmanager.ScheduleTaskItem;
import com.taobao.pamirs.schedule.taskmanager.ScheduleTaskType;
import com.taobao.pamirs.schedule.taskmanager.ScheduleTaskTypeRunningInfo;
import com.yoloho.enhanced.common.support.MsgBean;

@Controller
@RequestMapping("/task")
public class TaskController {
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class.getSimpleName());
    
    @RequestMapping("/index")
    public ModelAndView index(HttpServletResponse response) throws Exception {
        ModelAndView mav = new ModelAndView("task/index");
        if (ConsoleManager.isInitial() == false) {
            response.sendRedirect("config");
            return null;
        }
        List<ScheduleTaskType> taskList = ConsoleManager.getScheduleDataManager().getAllTaskTypeBaseInfo();
        mav.addObject("taskList", taskList);
        return mav;
    }
    
    @RequestMapping("/edit")
    public ModelAndView edit(String taskName) throws Exception {
        ModelAndView mav = new ModelAndView("task/edit");
        ScheduleTaskType task = ConsoleManager.getScheduleDataManager().loadTaskTypeBaseInfo(taskName);
        mav.addObject("isCreate", false);
        if (task == null) {
            task = new ScheduleTaskType();
            task.setBaseTaskType("");
            task.setDealBeanName("");
            task.setSts(ScheduleTaskType.STS_RESUME);
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
        List<ScheduleTaskTypeRunningInfo> infoList = ConsoleManager.getScheduleDataManager().getAllTaskTypeRunningInfo(taskName);
        if (StringUtils.isNotEmpty(ownSign)) {
            // filter by ownSign
            infoList = infoList.stream()
                    .filter(i -> StringUtils.equalsIgnoreCase(ownSign, i.getOwnSign()))
                    .collect(Collectors.toList());
        }
        Map<String, ScheduleTaskType> taskMap = new HashMap<>();
        Map<String, List<ScheduleServer>> strategyMap = new HashMap<>();
        Map<String, List<ScheduleTaskItem>> itemMap = new HashMap<>();
        for (ScheduleTaskTypeRunningInfo info : infoList) {
            strategyMap.put(info.getTaskType(), ConsoleManager.getScheduleDataManager().selectAllValidScheduleServer(info.getTaskType()));
            itemMap.put(info.getTaskType(), ConsoleManager.getScheduleDataManager().loadAllTaskItem(info.getTaskType()));
            if (!taskMap.containsKey(info.getBaseTaskType())) {
                taskMap.put(info.getBaseTaskType(), ConsoleManager.getScheduleDataManager().loadTaskTypeBaseInfo(info.getBaseTaskType()));
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
        ConsoleManager.getScheduleDataManager().clearTaskType(taskName);
        msgBean.put("refresh", false);
        return msgBean.returnMsg();
    }
    
    @RequestMapping("/remove")
    @ResponseBody
    public Map<String, Object> remove(String taskName) throws Exception {
        MsgBean msgBean = new MsgBean();
        ConsoleManager.getScheduleDataManager().deleteTaskType(taskName);
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
            String sts,
            boolean isCreate) throws Exception {
        MsgBean msgBean = new MsgBean();
        ScheduleTaskType taskType = new ScheduleTaskType();
        taskType.setBaseTaskType(taskName);
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
        taskType.setTaskItems(ScheduleTaskType.splitTaskItem(itemDefines));
        taskType.setSts(sts);
        if (isCreate) {
            ConsoleManager.getScheduleDataManager().createBaseTaskType(taskType);
        } else {
            ConsoleManager.getScheduleDataManager().updateBaseTaskType(taskType);
        }
        return msgBean.returnMsg();
    }
    
}
