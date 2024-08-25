package com.riotConsumer.riot.data.match;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private final MatchRepository repository;

    public MatchService(MatchRepository repository) {
        this.repository = repository;
    }

    public List<Match> saveAll(List<String> matchs, String region) {
        final Set<String> matchsSet = new HashSet<>(matchs);
        final Set<String> existentMatchs = repository.findAllById(matchsSet)
            .stream()
            .map(Match::getMatch)
            .collect(Collectors.toSet());
        final List<Match> finalMatchs = matchs
            .stream()
            .filter(match -> !existentMatchs.contains(match))
            .map(match -> new Match(match, region))
            .toList();

        return repository.saveAll(finalMatchs);
    }

    public List<Match> getAllByRegion(String region) {
        return repository.findByRegion(region);
    }
}
