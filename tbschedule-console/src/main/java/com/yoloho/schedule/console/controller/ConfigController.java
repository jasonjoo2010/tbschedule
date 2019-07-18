package com.yoloho.schedule.console.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Iterator;
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

import com.alibaba.fastjson.JSON;
import com.google.common.io.CharStreams;
import com.yoloho.enhanced.common.support.MsgBean;
import com.yoloho.schedule.console.ConsoleManager;
import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.types.ScheduleConfig;
import com.yoloho.schedule.types.Strategy;
import com.yoloho.schedule.types.Task;

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
            ScheduleConfig config = ConsoleManager.loadConfig();
            mav.addObject("address", config.getAddress());
            mav.addObject("rootPath", config.getRootPath());
            mav.addObject("userName", config.getUsername());
            mav.addObject("password", config.getPassword());
            mav.addObject("storage", config.getStorage());
        } catch (IOException e) {
            logger.warn("Loading configuration failed", e);
        }
        return mav;
    }

    @RequestMapping("/save")
    @ResponseBody
    public Map<String, Object> save(
            String storage,
            String address, 
            String rootPath, 
            String userName,
            String password) {
        MsgBean msgBean = new MsgBean();
        ScheduleConfig config = new ScheduleConfig();
        config.setStorage(storage);
        config.setAddress(address);
        config.setRootPath(rootPath);
        config.setUsername(userName);
        config.setPassword(password);
        try {
            ConsoleManager.saveConfigInfo(config);
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
        ModelAndView mav = new ModelAndView("config/export");
        return mav;
    }
    
    @RequestMapping("/exportToJSON")
    @ResponseBody
    public Map<String, Object> exportData(boolean download, HttpServletResponse response) throws Exception {
        MsgBean msgBean = new MsgBean();
        try {
            IStorage storage = ConsoleManager.getStorage();
            // Download
            if (download) {
                // 导出进行保存
                response.setContentType("text/plain;charset=GBK");
                response.setHeader("Content-disposition",
                        "attachment; filename=config.txt");
                PrintWriter out_ = response.getWriter();
                List<String> taskNames = storage.getTaskNames();
                for (String taskName : taskNames) {
                    Task task = storage.getTask(taskName);
                    if (task != null) {
                        out_.write("TASK");
                        out_.write(JSON.toJSONString(task));
                        out_.write("\n");
                    }
                }
                List<String> strategyNames = storage.getStrategyNames();
                for (String strategyName : strategyNames) {
                    Strategy strategy = storage.getStrategy(strategyName);
                    if (strategy != null) {
                        out_.write("STRATEGY");
                        out_.write(JSON.toJSONString(strategy));
                        out_.write("\n");
                    }
                }
                out_.close();
                return null;
            } else {
                msgBean.put("taskList", storage.getTaskNames().stream()
                        .map(name -> {
                            try {
                                return storage.getTask(name);
                            } catch (Exception e) {
                            }
                            return null;
                        }).filter(n -> n != null)
                        .collect(Collectors.toList()));
                msgBean.put("strategyList", storage.getStrategyNames().stream()
                        .map(name -> {
                            try {
                                return storage.getStrategy(name);
                            } catch (Exception e) {
                            }
                            return null;
                        }).filter(n -> n != null)
                        .collect(Collectors.toList()));
            }
        } catch (Exception e) {
            logger.error("Export configuration erorr", e);
            return msgBean.failure(e.getMessage()).returnMsg();
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
        int taskCount = 0;
        int strategyCount = 0;
        int taskSuccess = 0;
        int strategySuccess = 0;
        try {
            IStorage storage = ConsoleManager.getStorage();
            List<String> lines = CharStreams.readLines(new StringReader(content));
            Iterator<String> it = lines.iterator();
            String line;
            while (it.hasNext()) {
                line = it.next();
                if (StringUtils.isEmpty(line)) {
                    logger.info("Ignore empty line");
                    continue;
                }
                if (line.startsWith("TASK{")) {
                    // Task
                    String json = line.substring("TASK".length());
                    try {
                        Task task = JSON.parseObject(json, Task.class);
                        if (task == null) {
                            continue;
                        }
                        taskCount ++;
                        if (StringUtils.isEmpty(task.getName())
                                || StringUtils.isEmpty(task.getDealBeanName())) {
                            continue;
                        }
                        if (storage.getTask(task.getName()) != null) {
                            // update
                            if (force) {
                                storage.updateTask(task);
                                taskSuccess ++;
                            }
                        } else {
                            // new
                            storage.createTask(task);
                            taskSuccess ++;
                        }
                        taskSuccess ++;
                    } catch (Exception e) {
                    }
                } else if (line.startsWith("STRATEGY{")) {
                    // Strategy
                    String json = line.substring("STRATEGY".length());
                    try {
                        Strategy strategy = JSON.parseObject(json, Strategy.class);
                        if (strategy == null) {
                            continue;
                        }
                        strategyCount ++;
                        if (StringUtils.isEmpty(strategy.getName())
                                || StringUtils.isEmpty(strategy.getTaskName())) {
                            continue;
                        }
                        if (storage.getStrategy(strategy.getName()) != null) {
                            // update
                            if (force) {
                                storage.updateStrategy(strategy);
                                strategySuccess ++;
                            }
                        } else {
                            // new
                            storage.createStrategy(strategy);
                            strategySuccess ++;
                        }
                    } catch (Exception e) {
                    }
                } else {
                    logger.warn("Ignore malformed line: {}", line);
                }
            }
        } catch (Exception e) {
            logger.error("Import configuration error", e);
            return msgBean.failure(e.getMessage()).returnMsg();
        }
        msgBean.put("taskCount", taskCount);
        msgBean.put("taskSuccessCount", taskSuccess);
        msgBean.put("strategyCount", strategyCount);
        msgBean.put("strategySuccessCount", strategySuccess);
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
        ModelAndView mav = new ModelAndView("config/dump");
        IStorage storage = ConsoleManager.getStorage();
        mav.addObject("data", storage.dump().replace("\n", "<br />"));
        return mav;
    }
}
