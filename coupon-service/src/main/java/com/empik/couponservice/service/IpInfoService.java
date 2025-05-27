package com.empik.couponservice.service;

import com.empik.couponservice.dto.IpInfoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class IpInfoService {

    @Value("${ip-api.scheme}")
    private String ipApiScheme;

    @Value("${ip-api.host}")
    private String ipApiHost;

    public String getCountryCodeByIp(String ip) {
        URI uri = UriComponentsBuilder
                .newInstance()
                .scheme(ipApiScheme)
                .host(ipApiHost)
                .path("/json/{ip}")
                .queryParam("fields", "status,countryCode,query")
                .build(ip);

        RestTemplate restTemplate = new RestTemplate();
        IpInfoDto ipInfoDto = restTemplate.getForObject(uri, IpInfoDto.class);

        if (ipInfoDto != null && ipInfoDto.getStatus().equals("success")) {
            return ipInfoDto.getCountryCode();
        } else {
            return null;
        }
    }
}