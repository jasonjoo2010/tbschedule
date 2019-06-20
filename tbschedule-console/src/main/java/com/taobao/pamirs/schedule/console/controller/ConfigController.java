package com.taobao.pamirs.schedule.console.controller;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.base.Preconditions;
import com.taobao.pamirs.schedule.ConsoleManager;
import com.taobao.pamirs.schedule.zk.ZKManager;
import com.yoloho.common.support.MsgBean;

@Controller
@RequestMapping("/config")
public class ConfigController {
    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class.getSimpleName());
    
    @RequestMapping({ "/modify" })
    public ModelAndView index() throws Exception {
        ModelAndView mav = new ModelAndView("config/modify");
        mav.addObject("isInitial", ConsoleManager.isInitial());
        mav.addObject("errorMessage", ConsoleManager.getScheduleManagerFactory().getErrorMessage());
        try {
            Properties  p = ConsoleManager.loadConfig();
            mav.addObject("address", p.getProperty(ZKManager.keys.zkConnectString.toString()));
            mav.addObject("timeout", p.getProperty(ZKManager.keys.zkSessionTimeout.toString()));
            mav.addObject("rootPath", p.getProperty(ZKManager.keys.rootPath.toString()));
            mav.addObject("userName", p.getProperty(ZKManager.keys.userName.toString()));
            mav.addObject("password", p.getProperty(ZKManager.keys.password.toString()));
        } catch (IOException e) {
            logger.warn("Loading configuration failed", e);
        }
        return mav;
    }

    @RequestMapping({ "/save" })
    @ResponseBody
    public Map<String, Object> save(
            String address, 
            int timeout, 
            String rootPath, 
            String userName,
            String password) {
        Preconditions.checkArgument(timeout >= 5000, "Session timeout should be less than 5000 ms");
        MsgBean msgBean = new MsgBean();
        Properties p = new Properties();
        p.setProperty(ZKManager.keys.zkConnectString.toString(), address);
        p.setProperty(ZKManager.keys.rootPath.toString(), rootPath);
        p.setProperty(ZKManager.keys.userName.toString(), userName);
        p.setProperty(ZKManager.keys.password.toString(), password);
        p.setProperty(ZKManager.keys.zkSessionTimeout.toString(), String.valueOf(timeout));
        try {
            ConsoleManager.saveConfigInfo(p);
            int waitCount = 5;
            while (waitCount >= 0) {
                if (ConsoleManager.getScheduleManagerFactory().getZkManager().checkZookeeperState()) {
                    break;
                }
                Thread.sleep(1000);
                waitCount --;
            }
            if (!ConsoleManager.getScheduleManagerFactory().getZkManager().checkZookeeperState()) {
                return msgBean.failure("Cannot establish to the server").returnMsg();
            }
        } catch (Exception e) {
            logger.warn("Save new configuration error", e);
            return msgBean.failure(e.getMessage()).returnMsg();
        }
        return msgBean.returnMsg();
    }
}
