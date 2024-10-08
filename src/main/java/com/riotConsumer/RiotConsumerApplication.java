package com.riotConsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RiotConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiotConsumerApplication.class, args);
    }
}
