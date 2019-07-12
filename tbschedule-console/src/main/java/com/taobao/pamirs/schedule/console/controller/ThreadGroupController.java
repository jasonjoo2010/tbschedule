package com.taobao.pamirs.schedule.console.controller;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.taobao.pamirs.schedule.console.ConsoleManager;
import com.taobao.pamirs.schedule.taskmanager.ScheduleServer;
import com.yoloho.schedule.interfaces.IStorage;
import com.yoloho.schedule.types.Task;
import com.yoloho.schedule.util.ScheduleUtil;

@Controller
@RequestMapping("/threadgroup")
public class ThreadGroupController {
    private static class ScheduleServerComparator implements Comparator<ScheduleServer> {
        String[] orderFields;

        public ScheduleServerComparator(String aOrderStr) {
            if (StringUtils.isNotEmpty(aOrderStr)) {
                orderFields = aOrderStr.toUpperCase().split(",");
            } else {
                orderFields = "TASK_TYPE,OWN_SIGN,REGISTER_TIME,HEARTBEAT_TIME,IP".toUpperCase().split(",");
            }
        }

        public int compareObject(String o1, String o2) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 != null) {
                return o1.compareTo(o2);
            } else {
                return -1;
            }
        }

        public int compareObject(Timestamp o1, Timestamp o2) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 != null) {
                return o1.compareTo(o2);
            } else {
                return -1;
            }
        }

        public int compare(ScheduleServer o1, ScheduleServer o2) {
            int result = 0;
            for (String name : orderFields) {
                if (name.equals("TASK_TYPE")) {
                    result = compareObject(o1.getTaskType(), o2.getTaskType());
                    if (result != 0) {
                        return result;
                    }
                } else if (name.equals("OWN_SIGN")) {
                    result = compareObject(o1.getOwnSign(), o2.getOwnSign());
                    if (result != 0) {
                        return result;
                    }
                } else if (name.equals("REGISTER_TIME")) {
                    result = compareObject(o1.getRegisterTime(), o2.getRegisterTime());
                    if (result != 0) {
                        return result;
                    }
                } else if (name.equals("HEARTBEAT_TIME")) {
                    result = compareObject(o1.getHeartBeatTime(), o2.getHeartBeatTime());
                    if (result != 0) {
                        return result;
                    }
                } else if (name.equals("IP")) {
                    result = compareObject(o1.getIp(), o2.getIp());
                    if (result != 0) {
                        return result;
                    }
                } else if (name.equals("MANAGER_FACTORY")) {
                    result = compareObject(o1.getManagerFactoryUUID(), o2.getManagerFactoryUUID());
                    if (result != 0) {
                        return result;
                    }
                }
            }
            return result;
        }
    }
    
    private List<ScheduleServer> selectScheduleServer(String taskName,
            String ownSign, String ip, String orderStr) throws Exception {
        IStorage storage = ConsoleManager.getStorage();
        Set<Pair<String, String>> names = new HashSet<>();
        if (StringUtils.isNotEmpty(taskName) && StringUtils.isNotEmpty(ownSign)) {
            names.add(Pair.of(taskName, ownSign)) ;
        } else if(StringUtils.isNotEmpty(taskName) && StringUtils.isEmpty(ownSign)) {
            // not limit to one ownsign, thus, all runningEntries
            for (String runningEnty : storage.getRunningEntryList(taskName)) {
                names.add(Pair.of(ScheduleUtil.taskNameFromRunningEntry(runningEnty),
                        ScheduleUtil.ownsignFromRunningEntry(runningEnty)));
            }
        } else if (StringUtils.isEmpty(taskName)) {
            List<String> taskNameList = storage.getTaskNames();
            for (String name : taskNameList) {
                if (StringUtils.isNotEmpty(ownSign)) {
                    names.add(Pair.of(name, ownSign));
                } else {
                    for (String runningEnty : storage.getRunningEntryList(name)) {
                        names.add(Pair.of(ScheduleUtil.taskNameFromRunningEntry(runningEnty),
                                ScheduleUtil.ownsignFromRunningEntry(runningEnty)));
                    }
                }
            }
        }
        List<ScheduleServer> result = new ArrayList<ScheduleServer>();
        for (Pair<String, String> pair : names) {
            List<ScheduleServer> tempList = storage.getServerList(pair.getLeft(), pair.getRight());
            if (StringUtils.isEmpty(ip)) {
                result.addAll(tempList);
            } else {
                for (ScheduleServer server : tempList) {
                    if (ip.equals(server.getIp())) {
                        result.add(server);
                    }
                }
            }
        }
        Collections.sort(result, new ScheduleServerComparator(orderStr));
        // 排序
        return result;
    }
    
    private List<ScheduleServer> selectScheduleServerByManagerFactoryUUID(String factoryUUID) throws Exception {
        IStorage storage = ConsoleManager.getStorage();
        List<ScheduleServer> result = new ArrayList<ScheduleServer>();
        List<String> taskNameList = storage.getTaskNames();
        for (String taskName : taskNameList) {
            List<String> runningEntryList = storage.getRunningEntryList(taskName);
            for (String runningEntry : runningEntryList) {
                String ownSign = ScheduleUtil.ownsignFromRunningEntry(runningEntry);
                List<ScheduleServer> list = storage.getServerList(taskName, ownSign);
                Iterator<ScheduleServer> it = list.iterator();
                while (it.hasNext()) {
                    ScheduleServer server = it.next();
                    if (server.getManagerFactoryUUID().equals(factoryUUID)) {
                        result.add(server);
                    }
                }
            }
        }
        Collections.sort(result, new Comparator<ScheduleServer>() {
            public int compare(ScheduleServer u1, ScheduleServer u2) {
                int result = u1.getTaskType().compareTo(u2.getTaskType());
                if (result == 0) {
                    String s1 = u1.getUuid();
                    String s2 = u2.getUuid();
                    result = s1.substring(s1.lastIndexOf("$") + 1).compareTo(s2.substring(s2.lastIndexOf("$") + 1));
                }
                return result;
            }
        });
        return result;
    }
    
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
            list = selectScheduleServerByManagerFactoryUUID(uuid);
        } else {
            list = selectScheduleServer(task, ownSign, ip, orderStr);
        }
        Map<String, Task> taskMap = new HashMap<>();
        for (ScheduleServer item : list) {
            if (taskMap.containsKey(item.getBaseTaskType())) {
                continue;
            }
            Task base = ConsoleManager.getStorage().getTask(item.getBaseTaskType());
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
