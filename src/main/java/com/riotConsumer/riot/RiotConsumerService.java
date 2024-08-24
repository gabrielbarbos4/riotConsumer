package com.riotConsumer.riot;

import com.riotConsumer.riot.data.puuid.Puuid;
import com.riotConsumer.riot.data.puuid.PuuidService;
import com.riotConsumer.riot.enums.ServerBaseUrlEnum;
import com.riotConsumer.riot.response.queue.QueueResponse;
import com.riotConsumer.riot.response.queue.QueueSummonerResponse;
import com.riotConsumer.riot.response.summoner.GetSummonerWithEncryptedIdResponse;
import org.apache.catalina.users.SparseUserDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class RiotConsumerService {

    private final RestTemplate restTemplate;
    private final PuuidService puuidService;
    private final Logger logger = LogManager.getLogger(RiotConsumerService.class);

    private int rate_limit_counter = 0;

    private String API_KEY_QUERY_PARAMETER = "?api_key=";
    private String BASE_URL;

    public RiotConsumerService(RestTemplate restTemplate, PuuidService puuidService) {
        this.restTemplate = restTemplate;
        this.puuidService = puuidService;
    }

    @Async
    public void runConsumer(String apiKey) {
        API_KEY_QUERY_PARAMETER += apiKey;

        puuidService.clearAll();

        ServerBaseUrlEnum.getAllServerUrls()
            .stream()
            .filter(e -> Objects.equals(e, "https://br1.api.riotgames.com/lol"))
            .forEach(url -> {
                BASE_URL = url;

                final QueueResponse queueResponse = getQueue();
                final List<String> puuids = getPuuids(queueResponse);

                List<Puuid> list = puuidService.saveAll(puuids, url);
            });

        logger.info("Process finished.");
    }

    public QueueResponse getQueue() {
        String QUEUE_URL = "/league/v4/challengerleagues/by-queue/RANKED_SOLO_5x5";
        String url = BASE_URL + QUEUE_URL + API_KEY_QUERY_PARAMETER;
        QueueResponse queueResponse = this.restTemplate.getForObject(url, QueueResponse.class);

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

            GetSummonerWithEncryptedIdResponse summonerWithEncryptedIdResponse = this.getSummonerAccountDetails(entry.getSummonerId());
            rate_limit_counter++;

            puuidList.add(summonerWithEncryptedIdResponse.getPuuid());
        }

        logger.info("Acquired all Puuids.");
        return puuidList;
    }

    public GetSummonerWithEncryptedIdResponse getSummonerAccountDetails(String encryptedSummonerId) {
        String url = BASE_URL + "/summoner/v4/summoners/" + encryptedSummonerId + API_KEY_QUERY_PARAMETER;

        return this.restTemplate.getForObject(url, GetSummonerWithEncryptedIdResponse.class);
    }

    private void sleepRateLimit() {
        if (rate_limit_counter == 100) {
            try {
                logger.warn("Rate limit reached");
                Thread.sleep(120000);
                logger.warn("Rate limit restarted");
                rate_limit_counter = 0;
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }
}
