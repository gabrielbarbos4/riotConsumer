package com.riotConsumer.riot.data.matchDetails;

import org.springframework.stereotype.Service;

@Service
public class MatchDetailService {

    private final MatchDetailRepository repository;

    public MatchDetailService(MatchDetailRepository repository) {
        this.repository = repository;
    }

    public MatchDetail save(MatchDetail matchDetail) {
        return repository.save(matchDetail);
    }
}
