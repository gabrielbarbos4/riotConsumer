package com.riotConsumer.riot.response.queue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QueueResponse {
    private String tier;
    private String leagueId;
    private String queue;
    private String name;
    private List<QueueSummonerResponse> entries;
}
