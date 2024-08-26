package com.riotConsumer.riot.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum ServerBaseUrlEnum {

    BR_1("br1", false),
    KR("kr", false),
    EUN1("eun1", false),
    EUW("euw1", false),
    AMERICAS("americas", true),
    ASIA("asia", true);

    private static final Map<String, ServerBaseUrlEnum> CONTINENT_BY_REGION_CODE = new HashMap<>();

    static {
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

    public static String toUrl(ServerBaseUrlEnum key) {
        return "https://{0}.api.riotgames.com/lol".replace("{0}", key.getCode());
    }

    public static ServerBaseUrlEnum continentFromRegion(String code) {
        return CONTINENT_BY_REGION_CODE.get(code);
    }

    public static List<String> getAllServerUrls() {
        String baseUrl = "https://{0}.api.riotgames.com/lol";

        return Arrays.stream(ServerBaseUrlEnum.values())
            .filter(serverBaseUrl -> !serverBaseUrl.isContinent())
            .map(serverUrl -> baseUrl.replace("{0}", serverUrl.getCode()))
            .collect(Collectors.toList());
    }
}
