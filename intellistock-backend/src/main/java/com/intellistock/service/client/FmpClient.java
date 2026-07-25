package com.intellistock.service.client;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
public class FmpClient {

    private static final Logger log = LoggerFactory.getLogger(FmpClient.class);

    private final RestTemplate restTemplate;
    private final String apiKey;

    @Autowired
    public FmpClient(RestTemplate restTemplate, @Value("${api.fmp.key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey.trim();
    }

    public Optional<FmpProfile> getProfile(String symbol) {
        if (apiKey.isEmpty()) {
            log.warn("FMP API key is not configured. Skipping profile fetch for: {}", symbol);
            return Optional.empty();
        }

        String url = String.format("https://financialmodelingprep.com/api/v3/profile/%s?apikey=%s", symbol, apiKey);
        log.info("Fetching FMP company profile for: {}", symbol);

        try {
            FmpProfile[] response = restTemplate.getForObject(url, FmpProfile[].class);
            if (response != null && response.length > 0) {
                return Optional.of(response[0]);
            }
            log.warn("FMP profile returned empty response for symbol: {}", symbol);
        } catch (Exception e) {
            log.error("Error calling FMP Profile API for: {}. Message: {}", symbol, e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<FmpMetrics> getMetrics(String symbol) {
        if (apiKey.isEmpty()) {
            log.warn("FMP API key is not configured. Skipping metrics fetch for: {}", symbol);
            return Optional.empty();
        }

        String url = String.format("https://financialmodelingprep.com/api/v3/key-metrics-ttm/%s?apikey=%s", symbol, apiKey);
        log.info("Fetching FMP company metrics for: {}", symbol);

        try {
            FmpMetrics[] response = restTemplate.getForObject(url, FmpMetrics[].class);
            if (response != null && response.length > 0) {
                return Optional.of(response[0]);
            }
            log.warn("FMP metrics returned empty response for symbol: {}", symbol);
        } catch (Exception e) {
            log.error("Error calling FMP Metrics API for: {}. Message: {}", symbol, e.getMessage());
        }
        return Optional.empty();
    }

    @Data
    public static class FmpProfile {
        private String companyName;
        private Double price;
        private Double mktCap; // absolute Market Cap
        private String sector;
    }

    @Data
    public static class FmpMetrics {
        private Double peRatioTTM;
        private Double roeTTM;
        private Double debtToEquityTTM;
    }
}
