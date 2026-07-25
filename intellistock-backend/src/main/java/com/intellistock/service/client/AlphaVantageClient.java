package com.intellistock.service.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Component
public class AlphaVantageClient {

    private static final Logger log = LoggerFactory.getLogger(AlphaVantageClient.class);

    private final RestTemplate restTemplate;
    private final String apiKey;

    @Autowired
    public AlphaVantageClient(RestTemplate restTemplate, @Value("${api.alphavantage.key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey.trim();
    }

    public Optional<Double> getPrice(String symbol) {
        if (apiKey.isEmpty()) {
            log.warn("Alpha Vantage API key is not configured. Skipping quote fetch for: {}", symbol);
            return Optional.empty();
        }

        String url = String.format("https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s", symbol, apiKey);
        log.info("Fetching Alpha Vantage quote for: {}", symbol);

        try {
            AlphaVantageQuoteResponse response = restTemplate.getForObject(url, AlphaVantageQuoteResponse.class);
            if (response != null && response.getQuote() != null && response.getQuote().getPrice() != null) {
                try {
                    double price = Double.parseDouble(response.getQuote().getPrice().trim());
                    if (price > 0) {
                        return Optional.of(price);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Alpha Vantage quote price format error: {}", response.getQuote().getPrice());
                }
            }
            log.warn("Alpha Vantage quote returned empty response or invalid price for: {}", symbol);
        } catch (Exception e) {
            log.error("Error calling Alpha Vantage Quote API for: {}. Message: {}", symbol, e.getMessage());
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public Optional<Double> getSma10(String symbol) {
        if (apiKey.isEmpty()) {
            log.warn("Alpha Vantage API key is not configured. Skipping SMA fetch for: {}", symbol);
            return Optional.empty();
        }

        String url = String.format("https://www.alphavantage.co/query?function=SMA&symbol=%s&interval=daily&time_period=10&series_type=close&apikey=%s", symbol, apiKey);
        log.info("Fetching Alpha Vantage SMA(10) for: {}", symbol);

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("Technical Analysis: SMA")) {
                Map<String, Map<String, String>> timeSeries = (Map<String, Map<String, String>>) response.get("Technical Analysis: SMA");
                if (timeSeries != null && !timeSeries.isEmpty()) {
                    // Extract the first date's SMA value (which represents the most recent data point)
                    String firstKey = timeSeries.keySet().iterator().next();
                    Map<String, String> dataPoint = timeSeries.get(firstKey);
                    if (dataPoint != null && dataPoint.containsKey("SMA")) {
                        double sma = Double.parseDouble(dataPoint.get("SMA").trim());
                        return Optional.of(sma);
                    }
                }
            }
            log.warn("Alpha Vantage SMA returned empty response or invalid SMA values for: {}", symbol);
        } catch (Exception e) {
            log.error("Error calling Alpha Vantage SMA API for: {}. Message: {}", symbol, e.getMessage());
        }
        return Optional.empty();
    }

    @Data
    public static class AlphaVantageQuoteResponse {
        @JsonProperty("Global Quote")
        private AlphaVantageQuote quote;
    }

    @Data
    public static class AlphaVantageQuote {
        @JsonProperty("01. symbol")
        private String symbol;
        @JsonProperty("05. price")
        private String price;
    }
}
