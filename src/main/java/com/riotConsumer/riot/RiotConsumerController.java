package com.riotConsumer.riot;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/run")
public class RiotConsumerController {

    final RiotConsumerService riotConsumerService;

    public RiotConsumerController(RiotConsumerService riotConsumerService) {
        this.riotConsumerService = riotConsumerService;
    }

    @GetMapping("/puuid")
    ResponseEntity<String> runPuuidConsumer(@RequestParam(name = "api_key") String apiKey) {
        riotConsumerService.runPuuidsConsumer(apiKey);

        return ResponseEntity.accepted().body("Request received, processing.");
    }

    @GetMapping("/match-id")
    ResponseEntity<String> runMatchIdConsumer(@RequestParam(name = "api_key") String apiKey) {
        riotConsumerService.runMatchIdConsumer(apiKey);

        return ResponseEntity.accepted().body("Request received, processing.");
    }

    @GetMapping("/match-detail")
    ResponseEntity<String> runMatchDetailConsumer(@RequestParam(name = "api_key") String apiKey) {
        riotConsumerService.runMatchDetailsConsumer(apiKey);

        return ResponseEntity.accepted().body("Request received, processing.");
    }
}
