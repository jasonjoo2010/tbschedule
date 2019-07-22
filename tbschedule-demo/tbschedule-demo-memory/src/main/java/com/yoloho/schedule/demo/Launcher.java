package com.yoloho.schedule.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.yoloho.schedule.annotation.EnableSchedule;
import com.yoloho.schedule.memory.annotation.EnableScheduleMemory;

@SpringBootApplication
@EnableSchedule(
    storage = "memory"
)
@EnableScheduleMemory(basePath = "com.yoloho.schedule.demo")
public class Launcher {
    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(Launcher.class, args);
        while (true) {
            Thread.sleep(1000);
        }
    }
}
