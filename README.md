tbschedule
===
A simple non-centralizing scheduling framework.

- [Usage](#usage)
- [Main features](#main-features)
	- [Multiple Storages Support](#multiple-storages-support)
		- [Local Memory Scheduling](#local-memory-scheduling)
	- [XML/Annotation Support](#xmlannotation-support)
		- [Manually(Programmatically)](#manuallyprogrammatically)
		- [XML](#xml)
		- [Annotation(SpringBoot)](#annotationspringBoot)
	- [dashboard](#dashboard)
		- [Intro](#intro)
		- [Run](#run)
- [Load Balancing](#load-balancing)
- [Changelog](#changelog)
	- [4.1.3](#413)
	- [4.1.2](#412)
	- [4.1.1](#411)
	- [4.1.0](#410)
	- [4.0.1](#401)
	- [4.0.0](#400)
	- [3.3.4.0](#3340)

# Usage
The necessary dependency:

```
<groupId>com.yoloho.schedule</groupId>
<artifactId>tbschedule-core</artifactId>
<version>4.1.3</version>
```

If you use zookeeper as storage:

```
<groupId>com.yoloho.schedule</groupId>
<artifactId>tbschedule-storage-zookeeper</artifactId>
<version>4.1.3</version>
```

If you want use extensions like extension of task:

```
<groupId>com.yoloho.schedule</groupId>
<artifactId>tbschedule-extension-task</artifactId>
<version>4.1.3</version>
```

See [tbschedule-demo](tbschedule-demo) for demo.

# Main features
* Multiple Storages(SPI)
* Flexible Initializing
* Dashboard to Operate
* Simple Start/Stop Strategy
* Complex Task
* Automatic Self Election
* Zombie Detection
* Cron Time on Begin/End Supported
* Sleep/Not Sleep Models
* Common Task Implementations

## Multiple Storages Support
You would have several choises on storage option.

* **memory**(For `local` scheduling)
* **zookeeper**(`curator-framework`)
* **redis**(`enhanced-cache` based on `jredis`)
* jdbc(`enhanced-data` base on `druid`)(**Developing**)

### Local Memory Scheduling
It's mainly driven by annotations:

Annotation | Function
--- | ---
EnableScheduleMemory | Set the scan package
Strategy | Define a strategy
Task | Define a task

See [tbschedule-demo-memory](tbschedule-demo/tbschedule-demo-memory) for details.

## XML/Annotation Support
### Manually(Programmatically)
The factory can be initialized by

```java
ScheduleManagerFactory factory = new ScheduleManagerFactory();
ScheduleConfig config = new ScheduleConfig();
config.setAddress(address);
config.setRootPath(rootPath);
config.setUsername(username);
config.setPassword(password);
factory.setConfig(config);
// Factory needs the context of spring
factory.setApplicationContext(applicationContext);
factory.init();
```

And better to shut it down when application is terminating:

```java
factory.shutdown();
```

### XML
```xml
<bean id="scheduleManagerFactory" class="com.yoloho.schedule.strategy.ScheduleManagerFactory"
		init-method="init">
	<property name="configMap">
      <map>
         <entry key="storage" value="zookeeper" />
         <entry key="address" value="192.168.123.106:2181" />
         <entry key="rootPath" value="/tbschedule/test" />
         <entry key="username" value="ScheduleAdmin" />
         <entry key="password" value="password" />
      </map>
  </property>	
</bean>
```

### Annotation(SpringBoot)
```java
@SpringBootApplication
@EnableSchedule(
    // Storage is zookeeper by default
    address = "192.168.123.106:2181",
    rootPath = "/test/demo/tmp",
    username = "test",
    password = "test"
)
public class Launcher {
    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(Launcher.class, args);
        while (true) {
            Thread.sleep(1000);
        }
    }
}
```

And examples please refer the demo modules:  
* tbschedule-demo-memory(**Developing**)
* tbschedule-demo-redis(**Developing**)
* tbschedule-demo-zookeeper
* tbschedule-demo-jdbc(**Developing**)

## Dashboard
### Intro
Dashboard(Console) is a tool to view/manage task/strategy/factory(machine)/configuration. It can be run on any host or local which can communicate to the storage of your target applications' scheduling. And scheduling doesn't rely on it so you can shut it down when you don't need it anymore.

### Run
There is already the assembly package on maven central repository with name of `tbschedule-console-<version>-assembly.tar.gz`.

If you want to package it by your own hands, run `mvn package -DskipTests` on the module directory `tbschedule/tbschedule-console`.

To start dashboard using:
```
bin/startup
```

If you want to start and block (eg. in container) using:
```
bin/start
```

The default listening port is `8080`. If you want to change it you can edit the shell script line:
```
SERVER_PORT=8080
```

# Load Balancing
In older original version the worker instances' distribution always includes the leader node. So if you have many jobs or some `single instance` jobs the leader will be the heaviest node. To solve this we introduce `dynamic schedule distribution` algorithm. A shuffling on scheduling servers will be done when rescheduling.

# Changelog
## 4.1.3
* Fix type error when using `IScheduleTaskDealMulti`
* Fix errors during shutting down

## 4.1.2
* Support placeholder config in `EnableSchedule`

## 4.1.1
* Introduce the flattern plugin
* Optimize the deploy files

## 4.1.0
* Restructure the storage related logics.
* Introduce redis/memory storage

## 4.0.1
* Support packaging for tbschedule-console

## 4.0.0
* Regroup the modules
* Restructure console to support SpringBoot with enhanced library
* Add extension module
* Publish to maven centrel repository
* Change the zookeeper client from `zookeeper` into `curator`

## 3.3.4.0
* Separate console to submodule.
* Upgrade zookeeper to 3.4.8
* Restructure logging to slf4j
* Fix warnings and deprecates
* Fix UUID generating
* Add **load balancing**
* Other Bugs


