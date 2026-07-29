package com.intellistock.service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Component
public class YahooFinanceClient {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceClient.class);

    private final RestTemplate restTemplate;

    public YahooFinanceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<Double> getNsePrice(String symbol) {
        String yahooSymbol = symbol + ".NS";
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + yahooSymbol;
        log.info("Fetching Yahoo Finance NSE quote for: {}", yahooSymbol);

        try {
            YahooChartResponse response = restTemplate.getForObject(url, YahooChartResponse.class);
            if (response != null && response.getChart() != null
                    && response.getChart().getResult() != null
                    && !response.getChart().getResult().isEmpty()) {
                YahooResult result = response.getChart().getResult().get(0);
                if (result.getMeta() != null && result.getMeta().getRegularMarketPrice() != null) {
                    return Optional.of(result.getMeta().getRegularMarketPrice());
                }
            }
            log.warn("Yahoo Finance returned empty/invalid response for: {}", yahooSymbol);
        } catch (Exception e) {
            log.error("Error calling Yahoo Finance API for: {}. Message: {}", yahooSymbol, e.getMessage());
        }
        return Optional.empty();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YahooChartResponse {
        private YahooChart chart;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YahooChart {
        private List<YahooResult> result;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YahooResult {
        private YahooMeta meta;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YahooMeta {
        private Double regularMarketPrice;
        private String currency;
        private String symbol;
    }
}