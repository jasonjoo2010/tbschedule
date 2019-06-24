package com.taobao.pamirs.schedule.console.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.taobao.pamirs.schedule.ConsoleManager;
import com.taobao.pamirs.schedule.strategy.ManagerFactoryInfo;
import com.yoloho.enhanced.common.support.MsgBean;

@Controller
@RequestMapping("/machine")
public class MachineController {
    
    @RequestMapping("/index")
    public ModelAndView index() throws Exception {
        ModelAndView mav = new ModelAndView("machine/index");
        List<ManagerFactoryInfo> list =  ConsoleManager.getScheduleStrategyManager().loadAllManagerFactoryInfo();
        mav.addObject("machines", list);
        return mav;
    }

    @RequestMapping("/start")
    @ResponseBody
    public Map<String, Object> start(String uuid) throws Exception {
        MsgBean msgBean = new MsgBean();
        ConsoleManager.getScheduleStrategyManager().updateManagerFactoryInfo(uuid, true);
        return msgBean.returnMsg();
    }
    
    @RequestMapping("/stop")
    @ResponseBody
    public Map<String, Object> stop(String uuid) throws Exception {
        MsgBean msgBean = new MsgBean();
        ConsoleManager.getScheduleStrategyManager().updateManagerFactoryInfo(uuid, false);
        return msgBean.returnMsg();
    }
    
}
