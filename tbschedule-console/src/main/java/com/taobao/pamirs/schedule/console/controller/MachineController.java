package com.taobao.pamirs.schedule.console.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.taobao.pamirs.schedule.console.ConsoleManager;
import com.taobao.pamirs.schedule.strategy.ManagerFactoryInfo;
import com.yoloho.enhanced.common.support.MsgBean;
import com.yoloho.schedule.interfaces.IStorage;

@Controller
@RequestMapping("/machine")
public class MachineController {
    
    @RequestMapping("/index")
    public ModelAndView index() throws Exception {
        ModelAndView mav = new ModelAndView("machine/index");
        IStorage storage = ConsoleManager.getStorage();
        List<ManagerFactoryInfo> list = storage.getFactoryNames().stream()
                .map(uuid -> {
                    ManagerFactoryInfo obj = new ManagerFactoryInfo();
                    obj.setUuid(uuid);
                    try {
                        obj.setStart(storage.isFactoryAllowExecute(uuid));
                    } catch (Exception e) {
                    }
                    return obj;
                }).collect(Collectors.toList());
        mav.addObject("machines", list);
        return mav;
    }

    @RequestMapping("/start")
    @ResponseBody
    public Map<String, Object> start(String uuid) throws Exception {
        MsgBean msgBean = new MsgBean();
        ConsoleManager.getStorage().setFactoryAllowExecute(uuid, true);
        return msgBean.returnMsg();
    }
    
    @RequestMapping("/stop")
    @ResponseBody
    public Map<String, Object> stop(String uuid) throws Exception {
        MsgBean msgBean = new MsgBean();
        ConsoleManager.getStorage().setFactoryAllowExecute(uuid, false);
        return msgBean.returnMsg();
    }
    
}
