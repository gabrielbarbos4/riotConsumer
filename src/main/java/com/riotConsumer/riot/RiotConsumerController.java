package com.riotConsumer.riot;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/v1/consumer")
public class RiotConsumerController {

    final RiotConsumerService riotConsumerService;

    public RiotConsumerController(RiotConsumerService riotConsumerService) {
        this.riotConsumerService = riotConsumerService;
    }

    @GetMapping
    ResponseEntity<String> runConsumer(@RequestParam(name = "api_key") String apiKey) {
        riotConsumerService.runConsumer(apiKey);

        return ResponseEntity.accepted().body("Request received, processing.");
    }
}
