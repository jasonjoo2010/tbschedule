package com.yoloho.schedule.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.yoloho.schedule.annotation.EnableSchedule;

@SpringBootApplication
@EnableSchedule(
    address = "192.168.123.3:6381",
    rootPath = "/test/demo/tmp"
)
public class Launcher {
    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(Launcher.class, args);
        while (true) {
            Thread.sleep(1000);
        }
    }
}
