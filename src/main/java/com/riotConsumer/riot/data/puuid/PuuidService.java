package com.riotConsumer.riot.data.puuid;

import com.riotConsumer.riot.enums.ServerEnum;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PuuidService {

    private final PuuidRepository repository;

    public PuuidService(PuuidRepository repository) {
        this.repository = repository;
    }

    public void saveAll(List<PuuidIdSummonerIdDTO> puuids, String url, String rankWhenSaved) {
        final Set<PuuidIdSummonerIdDTO> puuidsSet = new HashSet<>(puuids);
        final Set<String> existentPuuids = repository.findAllById(puuidsSet.stream().map(PuuidIdSummonerIdDTO::getPuuid).toList())
            .stream()
            .map(Puuid::getPuuid)
            .collect(Collectors.toSet());
        final List<Puuid> finalPuuids = puuids
            .stream()
            .filter(puuid -> !existentPuuids.contains(puuid.getPuuid()))
            .map(puuid -> new Puuid(puuid.getPuuid(), ServerEnum.codeFromUrl(url), rankWhenSaved, puuid.getSummonerId(), new ArrayList<>()))
            .toList();

        repository.saveAll(finalPuuids);
    }

    public List<Puuid> getAll(String region) {
        return repository.findByRegion(region);
    }

    public void saveVerifiedEpoch(String id, Long startTime, Long endTime) {
        Puuid puuid1 = repository.findById(id).orElseThrow(() -> new RuntimeException("Puuid not found"));

        puuid1.addVerified(new PuuidVerifiedEpoch(startTime, endTime));

        repository.save(puuid1);
    }
}
