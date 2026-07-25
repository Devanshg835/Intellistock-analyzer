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
public class FinnhubClient {

    private static final Logger log = LoggerFactory.getLogger(FinnhubClient.class);

    private final RestTemplate restTemplate;
    private final String apiKey;

    @Autowired
    public FinnhubClient(RestTemplate restTemplate, @Value("${api.finnhub.key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey.trim();
    }

    public Optional<FinnhubQuote> getQuote(String symbol) {
        if (apiKey.isEmpty()) {
            log.warn("Finnhub API key is not configured. Skipping quote fetch for: {}", symbol);
            return Optional.empty();
        }

        String url = String.format("https://finnhub.io/api/v1/quote?symbol=%s&token=%s", symbol, apiKey);
        log.info("Fetching Finnhub quote for: {}", symbol);

        try {
            FinnhubQuote response = restTemplate.getForObject(url, FinnhubQuote.class);
            if (response != null && response.getC() != null && response.getC() > 0) {
                return Optional.of(response);
            }
            log.warn("Finnhub quote returned empty or zero price for: {}", symbol);
        } catch (Exception e) {
            log.error("Error calling Finnhub Quote API for: {}. Message: {}", symbol, e.getMessage());
        }
        return Optional.empty();
    }

    @Data
    public static class FinnhubQuote {
        private Double c;  // Current price
        private Double d;  // Change
        private Double dp; // Percent change
        private Double h;  // High price of the day
        private Double l;  // Low price of the day
        private Double o;  // Open price of the day
        private Double pc; // Previous close price
    }
}
