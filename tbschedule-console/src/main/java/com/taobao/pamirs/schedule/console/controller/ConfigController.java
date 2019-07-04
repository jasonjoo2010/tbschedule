package com.taobao.pamirs.schedule.console.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import com.taobao.pamirs.schedule.ConsoleManager;
import com.taobao.pamirs.schedule.zk.ZKManager;
import com.yoloho.enhanced.common.support.MsgBean;

@Controller
@RequestMapping("/config")
public class ConfigController {
    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class.getSimpleName());
    
    @RequestMapping("/modify")
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

    @RequestMapping("/save")
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
                if (ConsoleManager.getScheduleManagerFactory().getZkManager().checkZookeeperState() 
                        && ConsoleManager.isInitial()) {
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
    
    @RequestMapping("/export")
    public ModelAndView export(HttpServletResponse response) throws Exception {
        if (ConsoleManager.isInitial() == false) {
            response.sendRedirect("/config/modify");
            return null;
        }
        String rootPath = ConsoleManager.getScheduleStrategyManager().getRootPath();
        ModelAndView mav = new ModelAndView("config/export");
        mav.addObject("rootPath", rootPath);
        return mav;
    }
    
    @RequestMapping("/exportToJSON")
    @ResponseBody
    public Map<String, Object> exportData(boolean download, HttpServletResponse response) throws Exception {
        MsgBean msgBean = new MsgBean();
        String rootPath = ConsoleManager.getScheduleStrategyManager().getRootPath();
        StringWriter confWriter = new StringWriter();
        try {
            StringBuffer buffer = null;
            if (rootPath != null && rootPath.length() > 0) {
                buffer = ConsoleManager.getScheduleStrategyManager()
                        .exportConfig(rootPath, confWriter);
            } else {
                return msgBean.failure("No rootPath configured").returnMsg();
            }
            // Download
            if (download) {
                // 导出进行保存
                if (buffer != null) {
                    response.setContentType("text/plain;charset=GBK");
                    response.setHeader("Content-disposition",
                            "attachment; filename=config.txt");
                    PrintWriter out_ = response.getWriter();
                    out_.print(buffer.toString());
                    out_.close();
                }
                return null;
            } else {
                msgBean.put("configData", confWriter.toString());
            }
        } catch (Exception e) {
            logger.error("Export configuration erorr", e);
            return msgBean.failure(e.getMessage()).returnMsg();
        } finally {
            confWriter.close();
        }
        return msgBean.returnMsg();
    }
    
    @RequestMapping("/import")
    public ModelAndView importConfig(HttpServletResponse response) throws Exception {
        if (ConsoleManager.isInitial() == false) {
            response.sendRedirect("/config/modify");
            return null;
        }
        ModelAndView mav = new ModelAndView("config/import");
        return mav;
    }
    
    @RequestMapping("/importSave")
    @ResponseBody
    public Map<String, Object> importSave(String content, boolean force) {
        MsgBean msgBean = new MsgBean();
        if (StringUtils.isEmpty(content)) {
            return msgBean.failure("Config content is empty").returnMsg();
        }
        StringWriter writer = new StringWriter();
        try {
            List<String> lines = CharStreams.readLines(new StringReader(content));
            Iterator<String> it = lines.iterator();
            String line;
            while (it.hasNext()) {
                line = it.next();
                if (StringUtils.isEmpty(line)) {
                    logger.info("Ignore empty line");
                    continue;
                }
                if (line.contains("strategy")
                        || line.contains("baseTaskType")) {
                    ConsoleManager.getScheduleStrategyManager()
                            .importConfig(line, writer, force);
                } else {
                    logger.warn("Unrecognized configuration: {}", line);
                }
            }
        } catch (Exception e) {
            logger.error("Import configuration error", e);
            return msgBean.failure(e.getMessage()).returnMsg();
        }
        return msgBean.returnMsg();
    }
    
    @RequestMapping("/dump")
    public ModelAndView dump(
            @RequestParam(required = false) String path,
            HttpServletResponse response) throws Exception {
        if (ConsoleManager.isInitial() == false) {
            response.sendRedirect("/config/modify");
            return null;
        }
        if (StringUtils.isEmpty(path)) {
            path = ConsoleManager.getScheduleStrategyManager().getRootPath();
        }
        ModelAndView mav = new ModelAndView("config/dump");
        StringWriter writer = new StringWriter();
        try {
            ConsoleManager.getScheduleStrategyManager().printTree(path, writer, "<br/>");
            mav.addObject("data", writer.getBuffer().toString());
        } finally {
            writer.close();
        }
        return mav;
    }
}
