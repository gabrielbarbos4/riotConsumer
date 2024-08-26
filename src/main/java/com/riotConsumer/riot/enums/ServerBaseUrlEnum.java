package com.riotConsumer.riot.enums;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Getter
public enum ServerBaseUrlEnum {

    BR_1("br1", false),
    KR("kr", false),
    EUN1("eun1", false),
    EUW("euw1", false),
    AMERICAS("americas", true),
    ASIA("asia", true);

    private static final Map<String, ServerBaseUrlEnum> CONTINENT_BY_REGION_CODE = new HashMap<>();
    private static final Map<String, String> MOUNTED_URL_BY_REGION_CODE = new HashMap<>();

    static {
        for (ServerBaseUrlEnum serverBaseUrlEnum : values()) {
            MOUNTED_URL_BY_REGION_CODE.put(
                serverBaseUrlEnum.code,
                "https://{0}.api.riotgames.com/lol".replace("{0}", serverBaseUrlEnum.getCode())
            );
        }

        CONTINENT_BY_REGION_CODE.put(ServerBaseUrlEnum.BR_1.code, ServerBaseUrlEnum.AMERICAS);
        CONTINENT_BY_REGION_CODE.put(ServerBaseUrlEnum.EUN1.code, ServerBaseUrlEnum.AMERICAS);
        CONTINENT_BY_REGION_CODE.put(ServerBaseUrlEnum.EUW.code, ServerBaseUrlEnum.AMERICAS);
        CONTINENT_BY_REGION_CODE.put(ServerBaseUrlEnum.KR.code, ServerBaseUrlEnum.ASIA);
    }

    private final String code;
    private final boolean isContinent;

    ServerBaseUrlEnum(String code, boolean isContinent) {
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

    public static List<String> getAllServerUrls() {
        return new ArrayList<>(MOUNTED_URL_BY_REGION_CODE.values());
    }

    public static List<ServerBaseUrlEnum> continents() {
        return Stream.of(values()).filter(ServerBaseUrlEnum::isContinent).toList();
    }

    public static ServerBaseUrlEnum continentFromRegion(String code) {
        return CONTINENT_BY_REGION_CODE.get(code);
    }

    public static String mountedUrlFromCode(String code) {
        return MOUNTED_URL_BY_REGION_CODE.get(code);
    }
}
