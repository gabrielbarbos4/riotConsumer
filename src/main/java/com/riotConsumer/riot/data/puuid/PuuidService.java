package com.riotConsumer.riot.data.puuid;

import com.riotConsumer.riot.enums.ServerBaseUrlEnum;
import org.springframework.stereotype.Service;

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

    public List<Puuid> saveAll(List<String> puuids, String url, String rankWhenSaved) {
        final Set<String> puuidsSet = new HashSet<>(puuids);
        final Set<String> existentPuuids = repository.findAllById(puuidsSet)
            .stream()
            .map(Puuid::getPuuid)
            .collect(Collectors.toSet());
        final List<Puuid> finalPuuids = puuids
            .stream()
            .filter(puuid -> !existentPuuids.contains(puuid))
            .map(puuid -> new Puuid(puuid, ServerBaseUrlEnum.codeFromUrl(url), rankWhenSaved))
            .toList();

        return repository.saveAll(finalPuuids);
    }

    public List<Puuid> getAll(String region) {
        return repository.findByRegion(region);
    }

    public void clearAll() {
        repository.deleteAll();
    }
}
