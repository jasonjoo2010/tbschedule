tbschedule
===
A simple non-centralizing scheduling framework based on `zookeeper`.

# Improvements

* Separate console to submodule.
* Upgrade zookeeper to 3.4.8
* Restructure logging to slf4j
* Fix warnings and deprecates
* Fix UUID generating
* Add **load balancing**
* Other Bugs

# Load Balancing
In official version the worker instances' distribution always includes the leader node. So if you have many jobs or some `single instance` jobs the leader will be the heaviest node. To solve this we introduce `dynamic schedule distribution`. A shuffling of server list will be done when rescheduling.

# Usage
pom.xml：  
```
<groupId>com.taobao.pamirs.schedule</groupId>
<artifactId>tbschedule</artifactId>
<version>3.3.3.2</version>
```

