package com.riotConsumer.riot;

import com.riotConsumer.riot.enums.ServerBaseUrlEnum;
import com.riotConsumer.riot.response.queue.QueueResponse;
import com.riotConsumer.riot.response.queue.QueueSummonerResponse;
import com.riotConsumer.riot.response.summoner.GetSummonerWithEncryptedIdResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class RiotConsumerServiceImpl implements RiotConsumerService {

    private final RestTemplate restTemplate;
    private final Logger logger = LogManager.getLogger(RiotConsumerServiceImpl.class);
    private String API_KEY_QUERY_PARAMETER;
    private String BASE_URL;
    private int rate_limit_counter = 0;

    public RiotConsumerServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Async
    public void runConsumer(String apiKey) {
        API_KEY_QUERY_PARAMETER = "?api_key=".concat(apiKey);

        ServerBaseUrlEnum.getAllServerUrls().forEach(url -> {
            BASE_URL = url;

            QueueResponse queueResponse = getQueue();
            List<String> puuids = getPuuids(queueResponse);
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
                rate_limit_counter = 0;
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }
}
