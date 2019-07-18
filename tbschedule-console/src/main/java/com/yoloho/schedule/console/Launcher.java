package com.yoloho.schedule.console;

import java.net.UnknownHostException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import com.yoloho.enhanced.spring.annotation.InitDefaults;

@InitDefaults(
        projectName = "tbschedule-console"
    )
@SpringBootApplication
@ComponentScan(basePackages = "com.yoloho.schedule.console")
@Import({Web.class})
public class Launcher {
    public static void main(String[] args) throws UnknownHostException {
        SpringApplication.run(Launcher.class, args);
        try {
            ConsoleManager.initial();
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
        }
    }
}
