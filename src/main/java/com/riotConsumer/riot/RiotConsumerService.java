package com.riotConsumer.riot;

import com.riotConsumer.riot.data.match.Match;
import com.riotConsumer.riot.data.match.MatchService;
import com.riotConsumer.riot.data.puuid.Puuid;
import com.riotConsumer.riot.data.puuid.PuuidService;
import com.riotConsumer.riot.enums.ServerBaseUrlEnum;
import com.riotConsumer.riot.response.queue.QueueResponse;
import com.riotConsumer.riot.response.queue.QueueSummonerResponse;
import com.riotConsumer.riot.response.summoner.GetSummonerWithEncryptedIdResponse;
import com.riotConsumer.riot.util.HttpUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class RiotConsumerService {

    private final RestTemplate restTemplate;
    private final PuuidService puuidService;
    private final MatchService matchService;
    private final Logger logger = LogManager.getLogger(RiotConsumerService.class);

    private final int TIME_STAMP_PAST_LIMIT = 14;
    private int rate_limit_counter = 0;

    private String API_KEY_QUERY_PARAMETER;
    private String API_KEY;
    private String BASE_URL;

    public RiotConsumerService(RestTemplate restTemplate, PuuidService puuidService, MatchService matchService) {
        this.restTemplate = restTemplate;
        this.puuidService = puuidService;
        this.matchService = matchService;
    }

    @Async
    public void runConsumer(String apiKey) {
        API_KEY = apiKey;
        API_KEY_QUERY_PARAMETER = "?api_key=" + apiKey;

        puuidService.clearAll();

        ServerBaseUrlEnum.getAllServerUrls()
            .stream()
            .filter(e -> Objects.equals(e, "https://br1.api.riotgames.com/lol"))
            .forEach(url -> {
                BASE_URL = url;

                final QueueResponse queueResponse = getQueue();
                final List<String> apiPuuids = getPuuids(queueResponse);
                final List<Puuid> savedPuuids = puuidService.saveAll(apiPuuids, url);
//                final List<Puuid> savedPuuids = puuidService.getAll(ServerBaseUrlEnum.BR_1.getCode());
                final List<List<Match>> groupedMatches = new ArrayList<>();

                for (Puuid puuid : savedPuuids) {
                    final List<Match> savedMatches = getMatchIds(puuid.getPuuid());
                    groupedMatches.add(savedMatches);
                }
            });

        logger.info("Process finished.");
    }

    public List<Match> getMatchIds(String puuid) {
        logger.info("Start getting match ids");

        final String url = ServerBaseUrlEnum.toUrl(ServerBaseUrlEnum.AMERICAS) + "/match/v5/matches/by-puuid/" + puuid + "/ids";
        final List<Match> matches = new ArrayList<>();

        for (int i = 1; i <= TIME_STAMP_PAST_LIMIT; i++) {
            sleepRateLimit();

            final LocalDate timeNow = LocalDate.now().minusDays(i);
            final Instant endTime = timeNow.atStartOfDay(ZoneId.systemDefault()).toInstant();
            final Instant startTime = timeNow.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            final Map<String, String> queryParameters = Map.ofEntries(
                Map.entry("startTime", String.valueOf(startTime.getEpochSecond())),
                Map.entry("endTime", String.valueOf(endTime.getEpochSecond())),
                Map.entry("api_key", API_KEY),
                Map.entry("count", "100")
            );
            final String urlWithParameters = HttpUtil.applyQueryParameters(queryParameters, url);
            final ResponseEntity<String[]> response = this.restTemplate.getForEntity(urlWithParameters, String[].class);
            final List<String> match_ids = Arrays.asList(Objects.requireNonNull(response.getBody()));
            final List<Match> savedList = matchService.saveAll(match_ids, ServerBaseUrlEnum.fromUrl(BASE_URL));

            matches.addAll(savedList);

            rate_limit_counter++;
        }

        logger.info("Success to get match ids for puuid: " + puuid);
        return matches;
    }

    public QueueResponse getQueue() {
        sleepRateLimit();
        final String url = BASE_URL + "/league/v4/challengerleagues/by-queue/RANKED_SOLO_5x5" + API_KEY_QUERY_PARAMETER;
        final QueueResponse queueResponse = this.restTemplate.getForObject(url, QueueResponse.class);

        rate_limit_counter++;

        if (queueResponse == null || queueResponse.getEntries().isEmpty()) {
            logger.error("Empty Queue.");
            return null;
        }

        logger.info(String.format("[%s] %s %s", ServerBaseUrlEnum.fromUrl(BASE_URL), "Challenger league size:", queueResponse.getEntries().size()));
        return queueResponse;
    }

    public List<String> getPuuids(QueueResponse queueResponse) {
        List<String> puuidList = new ArrayList<>();

        for (QueueSummonerResponse entry : queueResponse.getEntries()) {
            sleepRateLimit();

            final String url = BASE_URL + "/summoner/v4/summoners/" + entry.getSummonerId() + API_KEY_QUERY_PARAMETER;
            final GetSummonerWithEncryptedIdResponse summonerWithEncryptedIdResponse = this.restTemplate.getForObject(url, GetSummonerWithEncryptedIdResponse.class);

            puuidList.add(summonerWithEncryptedIdResponse.getPuuid());

            rate_limit_counter++;
        }

        logger.info("Acquired all Puuids");
        return puuidList;
    }

    private void sleepRateLimit() {
        if (rate_limit_counter == 100) {
            try {
                logger.warn("Rate limit reached");
                Thread.sleep(120000);
                logger.warn("Rate limit reseted");
                rate_limit_counter = 0;
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }
}
