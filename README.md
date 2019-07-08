tbschedule
===
A simple non-centralizing scheduling framework based on `zookeeper`.

# Changelog

## 4.0.1
* Support packaging for tbschedule-console

## 4.0.0
* Regroup the modules
* Restructure console to support SpringBoot with enhanced library
* Add extension module
* Publish to maven centrel repository

## 3.3.4.0
* Separate console to submodule.
* Upgrade zookeeper to 3.4.8
* Restructure logging to slf4j
* Fix warnings and deprecates
* Fix UUID generating
* Add **load balancing**
* Other Bugs

# Load Balancing
In older original version the worker instances' distribution always includes the leader node. So if you have many jobs or some `single instance` jobs the leader will be the heaviest node. To solve this we introduce `dynamic schedule distribution`. A shuffling of server list will be done when rescheduling.

# Usage
## core

```
<groupId>com.yoloho.schedule</groupId>
<artifactId>tbschedule-core</artifactId>
<version>4.0.0</version>
```

## task

```
<groupId>com.yoloho.schedule</groupId>
<artifactId>tbschedule-extension-task</artifactId>
<version>4.0.0</version>
```