package com.riotConsumer.riot.data.puuid;

import com.riotConsumer.riot.enums.ServerBaseUrlEnum;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PuuidService {

    private final PuuidRepository repository;

    public PuuidService(PuuidRepository repository) {
        this.repository = repository;
    }

    public List<Puuid> saveAll(List<Puuid> puuids) {
        return repository.saveAll(puuids);
    }

    public List<Puuid> saveAll(List<String> puuids, String url) {
        return repository.saveAll(
            puuids.stream()
                .map(puuid -> new Puuid(puuid, ServerBaseUrlEnum.fromUrl(url)))
                .collect(Collectors.toList())
        );
    }

    public List<Puuid> getAll(String region) {
        return repository.findByRegion(region);
    }

    public void clearAll() {
        repository.deleteAll();
    }
}
