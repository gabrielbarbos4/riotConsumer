package com.riotConsumer.riot;

import com.riotConsumer.riot.data.match.Match;
import com.riotConsumer.riot.data.match.MatchService;
import com.riotConsumer.riot.data.matchDetails.MatchDetail;
import com.riotConsumer.riot.data.matchDetails.MatchDetailService;
import com.riotConsumer.riot.data.puuid.Puuid;
import com.riotConsumer.riot.data.puuid.PuuidIdSummonerIdDTO;
import com.riotConsumer.riot.data.puuid.PuuidService;
import com.riotConsumer.riot.enums.QueueEnum;
import com.riotConsumer.riot.enums.ServerEnum;
import com.riotConsumer.riot.response.queue.QueueResponse;
import com.riotConsumer.riot.response.queue.QueueSummonerResponse;
import com.riotConsumer.riot.response.summoner.GetSummonerWithEncryptedIdResponse;
import com.riotConsumer.riot.util.HttpUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    private int rate_limit_counter = 0;

    private String API_KEY_QUERY_PARAMETER;
    private String API_KEY;

    public RiotConsumerService(RestTemplate restTemplate, PuuidService puuidService, MatchService matchService, MatchDetailService matchDetailService) {
        this.restTemplate = restTemplate;
        this.puuidService = puuidService;
        this.matchService = matchService;
        this.matchDetailService = matchDetailService;
    }

    @Async
    public void runPuuidsConsumer(String apiKey) {
        API_KEY = apiKey;
        API_KEY_QUERY_PARAMETER = "?api_key=" + apiKey;

        ServerEnum.mountedRegionUrls()
            .stream()
            .filter(url -> !ServerEnum.codeFromUrl(url).equals(ServerEnum.BR_1.getCode()))
            .forEach(url -> {
                QueueEnum.asStream().forEach(queueEnum -> {
                    final List<QueueSummonerResponse> queue = getQueue(queueEnum, url);
                    final List<PuuidIdSummonerIdDTO> apiPuuids = getPuuids(queue, url);

                    puuidService.saveAll(apiPuuids, url, queueEnum.toString());
                });
            });

        logger.info("Finished Puuid Consumer");
    }

    @Async
    public void runMatchIdConsumer(String apiKey) {
        API_KEY = apiKey;
        API_KEY_QUERY_PARAMETER = "?api_key=" + apiKey;

        for (ServerEnum serverEnum : ServerEnum.values()) {
            logger.info("Consuming match id");
            final List<Puuid> savedPuuids = puuidService.getAll(serverEnum.getCode());

            for (Puuid puuid : savedPuuids) {
                getMatchIds(puuid, puuid.getRegion());
            }
        }
    }

    @Async
    public void runMatchDetailsConsumer(String apiKey) {
        API_KEY = apiKey;
        API_KEY_QUERY_PARAMETER = "?api_key=" + apiKey;

        for (ServerEnum serverEnum : ServerEnum.continents()) {
            logger.info("Consuming details from region: " + serverEnum.getCode());

            final String code = serverEnum.getCode();
            final List<Match> matchs = matchService.getAllByRegionAndVerified(code, false);

            for (Match match : matchs) {
                getMatchDetails(match.getMatch(), ServerEnum.mountedUrlFromCode(code));
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
            matchService.setVerified(matchId);
            matchDetailService.save(detail);
        }

        rate_limit_counter++;
    }

    public void getMatchIds(Puuid puuid, String region) {
        logger.info("Start getting match ids");

        final ServerEnum continent = ServerEnum.continentFromRegion(region);
        final String baseUrl = ServerEnum.mountedUrlFromCode(continent.getCode());
        final String url = baseUrl + "/match/v5/matches/by-puuid/" + puuid.getPuuid() + "/ids";
        final int TIME_STAMP_PAST_LIMIT = 30;

        System.out.println(url);
        for (int i = 1; i <= TIME_STAMP_PAST_LIMIT; i++) {
            sleepRateLimit();

            final LocalDate timeNow = LocalDate.now().minusDays(i);
            final long endTime = timeNow.atStartOfDay(ZoneId.systemDefault()).toInstant().getEpochSecond();
            final long startTime = timeNow.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().getEpochSecond();

            if(!puuid.isVerified(startTime, endTime)) {
                final Map<String, String> queryParameters = Map.ofEntries(
                    Map.entry("startTime", String.valueOf(startTime)),
                    Map.entry("endTime", String.valueOf(endTime)),
                    Map.entry("api_key", API_KEY),
                    Map.entry("type", "ranked"),
                    Map.entry("count", "100")
                );
                final String urlWithParameters = HttpUtil.applyQueryParameters(queryParameters, url);
                final ResponseEntity<String[]> response = this.restTemplate.getForEntity(urlWithParameters, String[].class);
                final List<String> match_ids = Arrays.asList(Objects.requireNonNull(response.getBody()));

                puuidService.saveVerifiedEpoch(puuid.getPuuid(), startTime, endTime);
                matchService.saveAll(match_ids, ServerEnum.codeFromUrl(baseUrl));
                rate_limit_counter++;
            }
        }

        logger.info("Success to get match ids for puuid: " + puuid.getPuuid());
    }

    public List<QueueSummonerResponse> getQueue(QueueEnum queue, String baseUrl) {
        logger.info("Consuming queues");
        sleepRateLimit();

        final String url = baseUrl + "/league/v4" + queue.getUrl() + API_KEY_QUERY_PARAMETER;
        final QueueResponse queueResponse = this.restTemplate.getForObject(url, QueueResponse.class);

        rate_limit_counter++;

        if (queueResponse == null || queueResponse.getEntries().isEmpty()) {
            logger.error("Empty Queue.");
            return null;
        }

        final List<QueueSummonerResponse> filteredQueue = filterQueue(queueResponse, queue.getName());

        logger.info(String.format("[%s] %s league size: %s | after filter: %s", ServerEnum.codeFromUrl(baseUrl), queue, queueResponse.getEntries().size(), filteredQueue.size()));

        return filteredQueue;
    }

    private List<QueueSummonerResponse> filterQueue(QueueResponse queue, String queueName) {
        if(QueueEnum.MASTER.getName().equals(queueName)) {
            return queue.getEntries().stream().limit(1200).toList();
        }

        return queue.getEntries();
    }

    public List<PuuidIdSummonerIdDTO> getPuuids(List<QueueSummonerResponse> queueResponse, String baseUrl) {
        logger.info("Consuming Puuids");
        List<PuuidIdSummonerIdDTO> puuidList = new ArrayList<>();

        for (QueueSummonerResponse entry : queueResponse) {
            sleepRateLimit();

            final String url = baseUrl + "/summoner/v4/summoners/" + entry.getSummonerId() + API_KEY_QUERY_PARAMETER;
            final GetSummonerWithEncryptedIdResponse summonerWithEncryptedIdResponse = this.restTemplate.getForObject(url, GetSummonerWithEncryptedIdResponse.class);

            puuidList.add(new PuuidIdSummonerIdDTO(entry.getSummonerId(), summonerWithEncryptedIdResponse.getPuuid()));

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
