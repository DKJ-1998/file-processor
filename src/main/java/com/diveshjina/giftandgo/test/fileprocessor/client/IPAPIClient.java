package com.diveshjina.giftandgo.test.fileprocessor.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.diveshjina.giftandgo.test.fileprocessor.dto.IpDetailsDto;

@FeignClient("ip-api")
public interface IPAPIClient {

    @GetMapping("/json/{query}")
    IpDetailsDto getIpDetails(@PathVariable String query);
}
