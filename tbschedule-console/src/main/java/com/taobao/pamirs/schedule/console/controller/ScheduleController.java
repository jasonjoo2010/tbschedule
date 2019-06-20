package com.taobao.pamirs.schedule.console.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.taobao.pamirs.schedule.ConsoleManager;

@Controller
@RequestMapping("/schedule")
public class ScheduleController {
    @RequestMapping({"/", "/index"})
    public ModelAndView index(HttpServletRequest request) throws Exception {
        ModelAndView mav = new ModelAndView("schedule/index");
        ConsoleManager.initial();
        ConsoleManager.getScheduleDataManager();
        return mav;
    }
}
