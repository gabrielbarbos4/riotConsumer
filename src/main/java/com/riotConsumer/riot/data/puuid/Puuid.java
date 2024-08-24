package com.riotConsumer.riot.data.puuid;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@AllArgsConstructor
@Document(collection = "puuids")
public class Puuid {
    @Id
    private String puuid;
    private String region;
}
