# EnableSchedule
初始化`tbschedule`任务管理器

```java
@EnableSchedule(
        addr = "${schedule.addr}",
        rootPath = "${schedule.rootPath}",
        user = "${schedule.user}",
        pass = "${schedule.pass}"
        )
```