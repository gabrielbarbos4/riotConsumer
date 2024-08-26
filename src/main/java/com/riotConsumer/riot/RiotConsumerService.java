package com.riotConsumer.riot;

import com.riotConsumer.riot.data.match.Match;
import com.riotConsumer.riot.data.match.MatchService;
import com.riotConsumer.riot.data.matchDetails.MatchDetail;
import com.riotConsumer.riot.data.matchDetails.MatchDetailService;
import com.riotConsumer.riot.data.puuid.Puuid;
import com.riotConsumer.riot.data.puuid.PuuidService;
import com.riotConsumer.riot.enums.QueueEnum;
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
    private final MatchDetailService matchDetailService;
    private final Logger logger = LogManager.getLogger(RiotConsumerService.class);

    private final int TIME_STAMP_PAST_LIMIT = 14;
    private int rate_limit_counter = 0;

    private String API_KEY_QUERY_PARAMETER;
    private String API_KEY;
    private String BASE_URL;

    public RiotConsumerService(RestTemplate restTemplate, PuuidService puuidService, MatchService matchService, MatchDetailService matchDetailService) {
        this.restTemplate = restTemplate;
        this.puuidService = puuidService;
        this.matchService = matchService;
        this.matchDetailService = matchDetailService;
    }

    @Async
    public void runConsumer(String apiKey) {
        API_KEY = apiKey;
        API_KEY_QUERY_PARAMETER = "?api_key=" + apiKey;

        ServerBaseUrlEnum.getAllServerUrls().forEach(url -> {
            BASE_URL = url;

            final QueueResponse queueResponse = getQueue(QueueEnum.CHALLENGER);
            final List<String> apiPuuids = getPuuids(queueResponse);
            final List<Puuid> savedPuuids = puuidService.saveAll(apiPuuids, url, QueueEnum.CHALLENGER.toString());

//            for (Puuid puuid : savedPuuids) {
//                getMatchIds(puuid.getPuuid(), url);
//            }
        });

        logger.info("Process finished.");
    }

    @Async
    public void runPuuidsConsumer(String apiKey) {
        API_KEY = apiKey;
        API_KEY_QUERY_PARAMETER = "?api_key=" + apiKey;

        ServerBaseUrlEnum.getAllServerUrls().forEach(url -> {
            BASE_URL = url;
            QueueEnum.urlsExcluding(QueueEnum.MASTER).forEach(queueEnum -> {
                final QueueResponse queueResponse = getQueue(queueEnum);
                final List<String> apiPuuids = getPuuids(queueResponse);

                puuidService.saveAll(apiPuuids, url, queueEnum.toString());
            });
        });
    }

    @Async
    public void runMatchIdConsumer(String apiKey) {
        API_KEY = apiKey;
        API_KEY_QUERY_PARAMETER = "?api_key=" + apiKey;

        for (ServerBaseUrlEnum serverBaseUrlEnum : ServerBaseUrlEnum.values()) {
            logger.info("Consuming match id");
            final List<Puuid> savedPuuids = puuidService.getAll(serverBaseUrlEnum.getCode());

            for (Puuid puuid : savedPuuids) {
                getMatchIds(puuid.getPuuid(), puuid.getRegion());
            }
        }
    }

    @Async
    public void runMatchDetailsConsumer(String apiKey) {
        API_KEY = apiKey;
        API_KEY_QUERY_PARAMETER = "?api_key=" + apiKey;

        for (ServerBaseUrlEnum serverBaseUrlEnum : ServerBaseUrlEnum.continents()) {
            logger.info("Consuming details from region: " + serverBaseUrlEnum.getCode());

            final String code = serverBaseUrlEnum.getCode();
            final List<Match> matchs = matchService.getAllByRegion(code);

            for (Match match : matchs) {
                getMatchDetails(match.getMatch(), ServerBaseUrlEnum.mountedUrlFromCode(code));
            }
        }
    }

    public void getMatchDetails(String matchId, String serverUrl) {
        sleepRateLimit();
        logger.info("Consuming details for match: " + matchId);

        final String url = serverUrl + "/match/v5/matches/" + matchId + API_KEY_QUERY_PARAMETER;
        final MatchDetail detail = this.restTemplate.getForObject(url, MatchDetail.class);

        if(detail != null) {
            detail.setId(matchId);
            matchDetailService.save(detail);
        }

        rate_limit_counter++;
    }

    public void getMatchIds(String puuid, String region) {
        logger.info("Start getting match ids");

        final ServerBaseUrlEnum continent = ServerBaseUrlEnum.continentFromRegion(region);
        final String baseUrl = ServerBaseUrlEnum.toUrl(continent);
        final String url = baseUrl + "/match/v5/matches/by-puuid/" + puuid + "/ids";

        for (int i = 1; i <= TIME_STAMP_PAST_LIMIT; i++) {
            sleepRateLimit();

            final LocalDate timeNow = LocalDate.now().minusDays(i);
            final Instant endTime = timeNow.atStartOfDay(ZoneId.systemDefault()).toInstant();
            final Instant startTime = timeNow.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            final Map<String, String> queryParameters = Map.ofEntries(
                Map.entry("startTime", String.valueOf(startTime.getEpochSecond())),
                Map.entry("endTime", String.valueOf(endTime.getEpochSecond())),
                Map.entry("api_key", API_KEY),
                Map.entry("type", "ranked"),
                Map.entry("count", "100")
            );
            final String urlWithParameters = HttpUtil.applyQueryParameters(queryParameters, url);
            final ResponseEntity<String[]> response = this.restTemplate.getForEntity(urlWithParameters, String[].class);
            final List<String> match_ids = Arrays.asList(Objects.requireNonNull(response.getBody()));

            matchService.saveAll(match_ids, ServerBaseUrlEnum.codeFromUrl(baseUrl));
            rate_limit_counter++;
        }

        logger.info("Success to get match ids for puuid: " + puuid);
    }

    public QueueResponse getQueue(QueueEnum queue) {
        logger.info("Consuming queues");
        sleepRateLimit();

        final String url = BASE_URL + "/league/v4" + queue.getUrl() + API_KEY_QUERY_PARAMETER;
        final QueueResponse queueResponse = this.restTemplate.getForObject(url, QueueResponse.class);

        rate_limit_counter++;

        if (queueResponse == null || queueResponse.getEntries().isEmpty()) {
            logger.error("Empty Queue.");
            return null;
        }

        logger.info(String.format("[%s] %s %s %s", ServerBaseUrlEnum.codeFromUrl(BASE_URL), queue.toString(), "league size:", queueResponse.getEntries().size()));
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
