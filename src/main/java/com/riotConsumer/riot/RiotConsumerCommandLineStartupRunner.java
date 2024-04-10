package com.riotConsumer.riot;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RiotConsumerCommandLineStartupRunner implements CommandLineRunner {

    private final RiotConsumerService riotConsumerService;

    public RiotConsumerCommandLineStartupRunner(RiotConsumerService riotConsumerService) {
        this.riotConsumerService = riotConsumerService;
    }

    @Override
    public void run(String... args) throws Exception {
        riotConsumerService.runConsumer();
    }
}
