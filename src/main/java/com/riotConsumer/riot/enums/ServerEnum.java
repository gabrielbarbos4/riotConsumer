package com.riotConsumer.riot.enums;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Getter
public enum ServerEnum {

    BR_1("br1", false),
    KR("kr", false),
    EUN1("eun1", false),
    EUW("euw1", false),
    NA1("na1", false),
    AMERICAS("americas", true),
    ASIA("asia", true);

    private static final Map<String, ServerEnum> CONTINENT_BY_REGION_CODE = new HashMap<>();
    private static final Map<String, String> MOUNTED_URL_BY_CODE = new HashMap<>();
    private static final Map<String, String> MOUNTED_REGION_URL = new HashMap<>();

    static {
        for (ServerEnum serverEnum : values()) {
            MOUNTED_URL_BY_CODE.put(serverEnum.code, "https://{0}.api.riotgames.com/lol".replace("{0}", serverEnum.getCode()));

            if(!serverEnum.isContinent) {
                MOUNTED_REGION_URL.put(serverEnum.code, "https://{0}.api.riotgames.com/lol".replace("{0}", serverEnum.getCode()));
            }
        }

        CONTINENT_BY_REGION_CODE.put(ServerEnum.BR_1.code, ServerEnum.AMERICAS);
        CONTINENT_BY_REGION_CODE.put(ServerEnum.EUN1.code, ServerEnum.AMERICAS);
        CONTINENT_BY_REGION_CODE.put(ServerEnum.EUW.code, ServerEnum.AMERICAS);
        CONTINENT_BY_REGION_CODE.put(ServerEnum.NA1.code, ServerEnum.AMERICAS);
        CONTINENT_BY_REGION_CODE.put(ServerEnum.KR.code, ServerEnum.ASIA);
    }

    private final String code;
    private final boolean isContinent;

    ServerEnum(String code, boolean isContinent) {
        this.code = code;
        this.isContinent = isContinent;
    }

    public static String codeFromUrl(String url) {
        final String URL_START = "https://";

        if (!url.contains(URL_START)) {
            throw new RuntimeException("Invalid String Url");
        }

        return url.substring(URL_START.length(), url.indexOf(".api.riotgames.com/lol"));
    }

    public static List<ServerEnum> continents() {
        return Stream.of(values()).filter(ServerEnum::isContinent).toList();
    }

    public static List<String> mountedRegionUrls() {
        return new ArrayList<>(MOUNTED_REGION_URL.values());
    }

    public static ServerEnum continentFromRegion(String code) {
        return CONTINENT_BY_REGION_CODE.get(code);
    }

    public static String mountedUrlFromCode(String code) {
        return MOUNTED_URL_BY_CODE.get(code);
    }
}
