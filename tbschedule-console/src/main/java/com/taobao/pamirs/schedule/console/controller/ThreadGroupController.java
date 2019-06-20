package com.taobao.pamirs.schedule.console.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.taobao.pamirs.schedule.ConsoleManager;
import com.taobao.pamirs.schedule.taskmanager.ScheduleServer;
import com.taobao.pamirs.schedule.taskmanager.ScheduleTaskType;

@Controller
@RequestMapping("/threadgroup")
public class ThreadGroupController {
    
    @RequestMapping("/index")
    public ModelAndView index(
            @RequestParam(required = false) String uuid,
            @RequestParam(required = false) String task,
            @RequestParam(required = false) String ownSign,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String orderStr
            ) throws Exception {
        ModelAndView mav = new ModelAndView("threadgroup/index");
        List<ScheduleServer> list = null;
        if (StringUtils.isNotEmpty(uuid)) {
            list = ConsoleManager.getScheduleDataManager()
                    .selectScheduleServerByManagerFactoryUUID(uuid);
        } else {
            list = ConsoleManager.getScheduleDataManager()
                    .selectScheduleServer(task, ownSign, ip, orderStr);
        }
        Map<String, ScheduleTaskType> taskMap = new HashMap<>();
        for (ScheduleServer item : list) {
            if (taskMap.containsKey(item.getBaseTaskType())) {
                continue;
            }
            ScheduleTaskType base = ConsoleManager.getScheduleDataManager().loadTaskTypeBaseInfo(item.getBaseTaskType());
            if (base == null) {
                continue;
            }
            taskMap.put(item.getBaseTaskType(), base);
        }
        mav.addObject("now", System.currentTimeMillis());
        mav.addObject("taskMap", taskMap);
        mav.addObject("task", task);
        mav.addObject("ownSign", ownSign);
        mav.addObject("ip", ip);
        mav.addObject("orderStr", orderStr);
        mav.addObject("canDoFilter", StringUtils.isEmpty(uuid));
        mav.addObject("groups", list);
        return mav;
    }
}
