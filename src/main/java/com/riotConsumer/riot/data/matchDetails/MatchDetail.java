package com.riotConsumer.riot.data.matchDetails;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Document(collection = "match_detail")
public class MatchDetail {

    @Id
    private String id;
    private Map<String, Object> details;

    @JsonAnySetter
    public void add(String key, Object value) {
        if (null == details) {
            details = new HashMap<>();
        }
        details.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> get() {
        return details;
    }
}
