package com.taobao.pamirs.schedule.console;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import com.yoloho.enhanced.spring.annotation.InitDefaults;

@InitDefaults(
        projectName = "tbschedule-console"
    )
@SpringBootApplication
@ComponentScan(basePackages = "com.taobao.pamirs.schedule.console")
@Import({Web.class})
public class Launcher {
    public static void main(String[] args) {
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
