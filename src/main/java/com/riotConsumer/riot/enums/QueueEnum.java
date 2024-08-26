package com.riotConsumer.riot.enums;

import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Getter
public enum QueueEnum {

    CHALLENGER("challengerleagues"),
    GRANDMASTER("grandmasterleagues"),
    MASTER("masterleagues");

    private final String name;
    private final String url;

    QueueEnum(String name) {
        this.name = name;
        this.url = "/" + name + "/by-queue/RANKED_SOLO_5x5";
    }

    public static List<String> urls() {
        return Stream.of(QueueEnum.values()).map(QueueEnum::getUrl).toList();
    }

    public static List<QueueEnum> urlsExcluding(QueueEnum excludedQueue) {
        return Stream.of(QueueEnum.values())
            .filter(value -> !Objects.equals(value, excludedQueue))
            .toList();
    }
}
