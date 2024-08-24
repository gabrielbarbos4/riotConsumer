package com.riotConsumer.riot;

import com.riotConsumer.riot.response.queue.QueueResponse;

public interface RiotConsumerService {
    void runConsumer(String apiKey);

    QueueResponse getQueue();
}
