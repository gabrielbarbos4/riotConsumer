package com.riotConsumer.riot;

import feign.RequestLine;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;
import java.util.List;

@FeignClient(value = "riotClient")
public interface RiotClient {

    @RequestLine("GET")
    List<Object> getPosts(URI baseurl);
}
