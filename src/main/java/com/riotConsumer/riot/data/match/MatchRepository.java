package com.riotConsumer.riot.data.match;


import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MatchRepository extends MongoRepository<Match, String> {
    List<Match> findByRegion(String region);
}
