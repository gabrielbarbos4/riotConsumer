package com.riotConsumer.riot.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ServerBaseUrlEnum {

    BR_1("br1"),
    KR("kr"),
    EUN1("eun1"),
    EUW("euw1");

    private final String code;

    ServerBaseUrlEnum(String code) {
        this.code = code;
    }

    public static String fromUrl(String url) {
        final String URL_START = "https://";

        if(!url.contains(URL_START)) {
            throw new RuntimeException("Invalid String Url");
        }

        return url.substring(URL_START.length(), url.indexOf(".api.riotgames.com/lol")).toUpperCase();
    }

    public static List<String> getAllServerUrls() {
        String baseUrl = "https://{0}.api.riotgames.com/lol";

        return Arrays.stream(ServerBaseUrlEnum.values())
            .map(serverUrl -> baseUrl.replace("{0}", serverUrl.getCode()))
            .collect(Collectors.toList());
    }

    public String getCode() {
        return code;
    }
}
