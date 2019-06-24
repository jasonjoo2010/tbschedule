package com.taobao.pamirs.schedule.console.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.taobao.pamirs.schedule.ConsoleManager;
import com.taobao.pamirs.schedule.console.vo.StrategyVo;
import com.taobao.pamirs.schedule.strategy.ScheduleStrategy;
import com.taobao.pamirs.schedule.strategy.ScheduleStrategyRunntime;
import com.yoloho.enhanced.common.support.MsgBean;
import com.yoloho.enhanced.common.util.JoinerSplitters;

@Controller
@RequestMapping("/strategy")
public class StrategyController {
    
    @RequestMapping("/index")
    public ModelAndView index() throws Exception {
        ModelAndView mav = new ModelAndView("strategy/index");
        List<ScheduleStrategy> scheduleStrategyList = ConsoleManager.getScheduleStrategyManager()
                .loadAllScheduleStrategy();
        mav.addObject("strategyList", scheduleStrategyList.stream()
                .map(g -> new StrategyVo(g))
                .collect(Collectors.toList()));
        return mav;
    }
    
    @RequestMapping("/runtime")
    public ModelAndView runtime(
            @RequestParam(required = false) String strategyName,
            @RequestParam(required = false) String uuid
            ) throws Exception {
        ModelAndView mav = new ModelAndView("strategy/runtime");
        List<ScheduleStrategyRunntime> runtimeList = new ArrayList<>();
        if (StringUtils.isNotEmpty(strategyName)) {
            runtimeList = ConsoleManager.getScheduleStrategyManager().loadAllScheduleStrategyRunntimeByTaskType(strategyName);
        } else if (StringUtils.isNotEmpty(uuid)) {
            runtimeList = ConsoleManager.getScheduleStrategyManager().loadAllScheduleStrategyRunntimeByUUID(uuid);
        }
        mav.addObject("runtimeList", runtimeList);
        return mav;
    }
    
    @RequestMapping("/edit")
    public ModelAndView edit(String strategyName) throws Exception {
        ModelAndView mav = new ModelAndView("strategy/edit");
        ScheduleStrategy strategy = null;
        if (!StringUtils.equalsIgnoreCase("-1", strategyName)) {
            strategy = ConsoleManager.getScheduleStrategyManager().loadStrategy(strategyName);
        }
        mav.addObject("isCreate", false);
        if (strategy == null) {
            strategy = new ScheduleStrategy();
            strategy.setStrategyName("");
            strategy.setKind(ScheduleStrategy.Kind.Schedule);
            strategy.setTaskName("");
            strategy.setTaskParameter("");
            strategy.setNumOfSingleServer(0);
            strategy.setAssignNum(2);
            mav.addObject("ips", "127.0.0.1");
            mav.addObject("isCreate", true);
        } else {
            mav.addObject("ips", JoinerSplitters.getJoiner(",").join(strategy.getIPList()));
        }
        mav.addObject("strategy", strategy);
        return mav;
    }
    
    @RequestMapping("/pause")
    @ResponseBody
    public Map<String, Object> pause(String strategyName) throws Exception {
        MsgBean msgBean = new MsgBean();
        ConsoleManager.getScheduleStrategyManager().pause(strategyName);
        return msgBean.returnMsg();
    }
    
    @RequestMapping("/resume")
    @ResponseBody
    public Map<String, Object> resume(String strategyName) throws Exception {
        MsgBean msgBean = new MsgBean();
        ConsoleManager.getScheduleStrategyManager().resume(strategyName);
        return msgBean.returnMsg();
    }
    
    @RequestMapping("/remove")
    @ResponseBody
    public Map<String, Object> remove(String strategyName) throws Exception {
        MsgBean msgBean = new MsgBean();
        ConsoleManager.getScheduleStrategyManager().deleteMachineStrategy(strategyName);
        return msgBean.returnMsg();
    }
    
    @RequestMapping({ "/save" })
    @ResponseBody
    public Map<String, Object> save(
            boolean isCreate,
            String strategyName,
            String kind,
            String taskName,
            String taskParameter,
            @RequestParam(required = false, defaultValue = "0") int numOfSingleServer,
            @RequestParam(required = false, defaultValue = "0") int assignNum,
            String ips
            ) throws Exception {
        MsgBean msgBean = new MsgBean();
        ScheduleStrategy strategy = new ScheduleStrategy();
        strategy.setStrategyName(strategyName);
        strategy.setKind(ScheduleStrategy.Kind.valueOf(kind));
        strategy.setTaskName(taskName);
        strategy.setTaskParameter(taskParameter);
        strategy.setNumOfSingleServer(numOfSingleServer);
        strategy.setAssignNum(assignNum);
        if (StringUtils.isBlank(ips)) {
            strategy.setIPList(new String[0]);
        } else {
            strategy.setIPList(JoinerSplitters.getSplitter(",").splitToList(ips).toArray(new String[] {}));
        }
        if (isCreate) {
            ConsoleManager.getScheduleStrategyManager().createScheduleStrategy(strategy);
        } else {
            ConsoleManager.getScheduleStrategyManager().updateScheduleStrategy(strategy);
        }
        return msgBean.returnMsg();
    }

}
