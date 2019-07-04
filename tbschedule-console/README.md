tbschedule-console
===

# Intro
Console panel for tbschedule. You can view/manage task/strategy/machines/thread groups on the panel.  
Panel can be run on any node which can access to the storage backend.  
It's more like a tool or client for tbschedule. It's not a necessary component and you can run it when you want to change the schedule or view the stats.

# Package
It can be packaged by simple command `mvn package`. Then you will get a file named `tbschedule-console-x.x.x-assembly.tar.gz` under the dictionary of `target`. Decompress it to any location and run `bin/startup`. The default listening port is `8080` and you can change it by add parameter `-Dserver.port=xxxx`.
