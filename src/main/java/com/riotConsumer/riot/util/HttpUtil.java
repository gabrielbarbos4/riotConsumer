package com.riotConsumer.riot.util;

import java.util.Map;

public class HttpUtil {
    static public String applyQueryParameters(Map<String, String> queryMap, String baseUrl) {
        StringBuilder completeUrl = new StringBuilder(baseUrl).append("?");

        for (var entry : queryMap.entrySet()) {
            completeUrl
                .append(entry.getKey())
                .append("=")
                .append(entry.getValue())
                .append("&");
        }

        return completeUrl.substring(0, completeUrl.toString().length() - 1);
    }
}
