package com.intellistock.service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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

        String url = String.format("https://financialmodelingprep.com/stable/profile?symbol=%s&apikey=%s", symbol, apiKey);
        log.info("Fetching FMP company profile (stable) for: {}", symbol);

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

        String ratiosUrl = String.format("https://financialmodelingprep.com/stable/ratios?symbol=%s&apikey=%s", symbol, apiKey);
        String keyMetricsUrl = String.format("https://financialmodelingprep.com/stable/key-metrics?symbol=%s&apikey=%s", symbol, apiKey);
        log.info("Fetching FMP ratios/key-metrics (stable) for: {}", symbol);

        FmpMetrics combined = new FmpMetrics();
        boolean gotAny = false;

        try {
            FmpRatios[] ratiosResponse = restTemplate.getForObject(ratiosUrl, FmpRatios[].class);
            if (ratiosResponse != null && ratiosResponse.length > 0) {
                FmpRatios r = ratiosResponse[0];
                combined.setPeRatioTTM(r.getPriceToEarningsRatio());
                combined.setDebtToEquityTTM(r.getDebtToEquityRatio());
                gotAny = true;
            }
        } catch (Exception e) {
            log.error("Error calling FMP Ratios API for: {}. Message: {}", symbol, e.getMessage());
        }

        try {
            FmpKeyMetrics[] kmResponse = restTemplate.getForObject(keyMetricsUrl, FmpKeyMetrics[].class);
            if (kmResponse != null && kmResponse.length > 0) {
                FmpKeyMetrics km = kmResponse[0];
                if (km.getReturnOnEquity() != null) {
                    combined.setRoeTTM(km.getReturnOnEquity() * 100.0);
                }
                gotAny = true;
            }
        } catch (Exception e) {
            log.error("Error calling FMP Key-Metrics API for: {}. Message: {}", symbol, e.getMessage());
        }

        if (gotAny) {
            return Optional.of(combined);
        }
        log.warn("FMP metrics returned empty response for symbol: {}", symbol);
        return Optional.empty();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FmpProfile {
        private String companyName;
        private Double price;

        @JsonProperty("marketCap")
        private Double mktCap;

        private String sector;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FmpRatios {
        private Double priceToEarningsRatio;
        private Double debtToEquityRatio;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FmpKeyMetrics {
        private Double returnOnEquity;
    }

    @Data
    public static class FmpMetrics {
        private Double peRatioTTM;
        private Double roeTTM;
        private Double debtToEquityTTM;
    }
}