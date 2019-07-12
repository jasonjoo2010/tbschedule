package com.taobao.pamirs.schedule.console.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.taobao.pamirs.schedule.console.ConsoleManager;
import com.taobao.pamirs.schedule.console.vo.StrategyVo;
import com.taobao.pamirs.schedule.strategy.ScheduleStrategyRuntime;
import com.yoloho.enhanced.common.support.MsgBean;
import com.yoloho.enhanced.common.util.JoinerSplitters;
import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.types.Strategy;
import com.yoloho.schedule.types.StrategyKind;

@Controller
@RequestMapping("/strategy")
public class StrategyController {
    
    @RequestMapping("/index")
    public ModelAndView index(HttpServletResponse response) throws Exception {
        ModelAndView mav = new ModelAndView("strategy/index");
        if (ConsoleManager.isInitial() == false) {
            response.sendRedirect("/config/modify");
            return null;
        }
        IStorage storage = ConsoleManager.getStorage();
        List<Strategy> scheduleStrategyList = storage.getStrategyNames().stream()
                .map(name -> {
                    try {
                        return storage.getStrategy(name);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
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
        List<ScheduleStrategyRuntime> runtimeList = new ArrayList<>();
        IStorage storage = ConsoleManager.getStorage();
        if (StringUtils.isNotEmpty(strategyName)) {
            runtimeList = storage.getStrategyRuntimes(strategyName);
        } else if (StringUtils.isNotEmpty(uuid)) {
            runtimeList = storage.getStrategyNames().stream()
                    .map(name -> {
                        try {
                            return storage.getStrategyRuntime(name, uuid);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(item -> item != null)
                    .collect(Collectors.toList());
        }
        mav.addObject("runtimeList", runtimeList);
        return mav;
    }
    
    @RequestMapping("/edit")
    public ModelAndView edit(String strategyName) throws Exception {
        ModelAndView mav = new ModelAndView("strategy/edit");
        Strategy strategy = null;
        if (!StringUtils.equalsIgnoreCase("-1", strategyName)) {
            strategy = ConsoleManager.getStorage().getStrategy(strategyName);
        }
        mav.addObject("isCreate", false);
        if (strategy == null) {
            strategy = new Strategy();
            strategy.setName("");
            strategy.setKind(StrategyKind.Schedule);
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
    
    private void pauseResumeStrategy(String strategyName, String sts) throws Exception {
        IStorage storage = ConsoleManager.getStorage();
        Strategy strategy = storage.getStrategy(strategyName);
        strategy.setSts(sts);
        storage.updateStrategy(strategy);
    }
    
    @RequestMapping("/pause")
    @ResponseBody
    public Map<String, Object> pause(String strategyName) throws Exception {
        MsgBean msgBean = new MsgBean();
        pauseResumeStrategy(strategyName, Strategy.STS_PAUSE);
        return msgBean.returnMsg();
    }
    
    @RequestMapping("/resume")
    @ResponseBody
    public Map<String, Object> resume(String strategyName) throws Exception {
        MsgBean msgBean = new MsgBean();
        pauseResumeStrategy(strategyName, Strategy.STS_RESUME);
        return msgBean.returnMsg();
    }
    
    @RequestMapping("/remove")
    @ResponseBody
    public Map<String, Object> remove(String strategyName) throws Exception {
        MsgBean msgBean = new MsgBean();
        ConsoleManager.getStorage().removeStrategy(strategyName);
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
        Strategy strategy = new Strategy();
        strategy.setName(strategyName);
        strategy.setKind(StrategyKind.valueOf(kind));
        strategy.setTaskName(taskName);
        strategy.setTaskParameter(taskParameter);
        strategy.setNumOfSingleServer(numOfSingleServer);
        strategy.setAssignNum(assignNum);
        if (StringUtils.isBlank(ips)) {
            strategy.setIPList(new String[0]);
        } else {
            strategy.setIPList(JoinerSplitters.getSplitter(",").splitToList(ips).toArray(new String[] {}));
        }
        IStorage storage = ConsoleManager.getStorage();
        if (isCreate) {
            storage.createStrategy(strategy);
        } else {
            storage.updateStrategy(strategy);
        }
        return msgBean.returnMsg();
    }

}
