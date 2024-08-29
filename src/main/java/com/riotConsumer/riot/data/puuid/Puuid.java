package com.riotConsumer.riot.data.puuid;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Document(collection = "puuids")
public class Puuid {
    @Id
    private String puuid;
    private String region;
    private String eloWhenSaved;
    private String summonerId;
    private List<PuuidVerifiedEpoch> verifiedEpochList;

    public void addVerified(PuuidVerifiedEpoch epoch) {
        verifiedEpochList.add(epoch);
    }

    public boolean isVerified(long startTime, long endtime) {
        return verifiedEpochList.stream()
            .parallel()
            .anyMatch(puuid -> puuid.getStartTime() == startTime && puuid.getEndTime() == endtime);
    }
}
