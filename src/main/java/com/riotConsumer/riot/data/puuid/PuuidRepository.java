package com.riotConsumer.riot.data.puuid;


import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PuuidRepository extends MongoRepository<Puuid, String> {
    List<Puuid> findByRegion(String region);
}
