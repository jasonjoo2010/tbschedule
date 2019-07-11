package com.yoloho.schedule.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.taobao.pamirs.schedule.TaskItemDefine;

public class ScheduleTaskUtil {
    private static final Pattern PATTERN_TASK_ITEM = Pattern.compile("^(.+):\\{([^}]*)\\}$");
    
    /**
     * only parse taskItem and dealParameter properties
     * 
     * @param item
     * @return
     */
    public static TaskItemDefine parseItem(String item) {
        Matcher m = PATTERN_TASK_ITEM.matcher(item);
        TaskItemDefine taskItem = new TaskItemDefine();
        if (m.find()) {
            taskItem.setTaskItemId(m.group(1));
            taskItem.setParameter(m.group(2));
        } else {
            taskItem.setTaskItemId(item);
        }
        return taskItem;
    }
    
    /**
     * only parse taskItem and dealParameter properties
     * 
     * @param items
     * @return
     */
    public static TaskItemDefine[] parseItems(String[] items) {
        TaskItemDefine[] taskItems = new TaskItemDefine[items.length];
        for (int i = 0; i < taskItems.length; i++) {
            taskItems[i] = parseItem(items[i]);
        }
        return taskItems;
    }
}
