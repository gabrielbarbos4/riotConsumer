package com.riotConsumer.riot;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

@Service
public class RiotConsumerServiceImpl implements RiotConsumerService {

    private final RiotClient riotClient;

    public RiotConsumerServiceImpl(RiotClient riotClient) {
        this.riotClient = riotClient;
    }

    @Override
    public void runConsumer() {
        List<Object> players = getPlayer();
    }

    private List<Object> getPlayer() {
        return this.riotClient.getPosts(URI.create("https://jsonplaceholder.typicode.com/"));
    }
}
