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
public class TwelveDataClient {

    private static final Logger log = LoggerFactory.getLogger(TwelveDataClient.class);

    private final RestTemplate restTemplate;
    private final String apiKey;

    @Autowired
    public TwelveDataClient(RestTemplate restTemplate, @Value("${api.twelvedata.key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey.trim();
    }

    public Optional<TwelveDataQuote> getQuote(String symbol) {
        if (apiKey.isEmpty()) {
            log.warn("Twelve Data API key is not configured. Skipping quote fetch for: {}", symbol);
            return Optional.empty();
        }

        String url = String.format("https://api.twelvedata.com/quote?symbol=%s&apikey=%s", symbol, apiKey);
        log.info("Fetching Twelve Data quote for: {}", symbol);

        try {
            TwelveDataQuote response = restTemplate.getForObject(url, TwelveDataQuote.class);
            if (response != null && response.getPrice() != null) {
                return Optional.of(response);
            }
            log.warn("Twelve Data quote returned empty response for: {}", symbol);
        } catch (Exception e) {
            log.error("Error calling Twelve Data Quote API for: {}. Message: {}", symbol, e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<TwelveDataTimeSeries> getTimeSeries(String symbol) {
        if (apiKey.isEmpty()) {
            log.warn("Twelve Data API key is not configured. Skipping history fetch for: {}", symbol);
            return Optional.empty();
        }

        String url = String.format("https://api.twelvedata.com/time_series?symbol=%s&interval=1day&outputsize=30&apikey=%s", symbol, apiKey);
        log.info("Fetching Twelve Data time series history for: {}", symbol);

        try {
            TwelveDataTimeSeries response = restTemplate.getForObject(url, TwelveDataTimeSeries.class);
            if (response != null && response.getValues() != null && !response.getValues().isEmpty()) {
                return Optional.of(response);
            }
            log.warn("Twelve Data time series returned empty for: {}", symbol);
        } catch (Exception e) {
            log.error("Error calling Twelve Data Time Series API for: {}. Message: {}", symbol, e.getMessage());
        }
        return Optional.empty();
    }

    @Data
    public static class TwelveDataTimeSeries {
        private java.util.List<TimeSeriesValue> values;
    }

    @Data
    public static class TimeSeriesValue {
        private String datetime;
        private String close;

        public Double getCloseAsDouble() {
            if (close == null || close.trim().isEmpty()) return null;
            try {
                return Double.parseDouble(close.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    @Data
    public static class TwelveDataQuote {
        private String symbol;
        private String name;
        private String price; // Price returned as string, e.g. "22.50000"
        private String sector;

        public Double getPriceAsDouble() {
            if (price == null || price.trim().isEmpty()) return null;
            try {
                return Double.parseDouble(price.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
