package com.intellistock.service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Component
public class NewsClient {

    private static final Logger log = LoggerFactory.getLogger(NewsClient.class);

    private final RestTemplate restTemplate;
    private final String apiKey;

    @Autowired
    public NewsClient(RestTemplate restTemplate, @Value("${api.news.key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey.trim();
    }

    public Optional<List<NewsArticle>> getNews(String symbol) {
        if (apiKey.isEmpty()) {
            log.warn("NewsData.io API key is not configured. Skipping news fetch for: {}", symbol);
            return Optional.empty();
        }

        String url = String.format("https://newsdata.io/api/1/news?apikey=%s&q=%s&language=en", apiKey, symbol);
        log.info("Fetching news (NewsData.io) for symbol: {}", symbol);

        try {
            NewsResponse response = restTemplate.getForObject(url, NewsResponse.class);
            if (response != null && "success".equalsIgnoreCase(response.getStatus()) && response.getResults() != null && !response.getResults().isEmpty()) {
                return Optional.of(response.getResults());
            }
            log.warn("NewsData.io returned non-success status or empty results for: {}", symbol);
        } catch (Exception e) {
            log.error("Error calling NewsData.io API for: {}. Message: {}", symbol, e.getMessage());
        }
        return Optional.empty();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewsResponse {
        private String status;
        private List<NewsArticle> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewsArticle {
        private String title;
        private String description;
        private String source_id;

        public NewsSource getSource() {
            NewsSource s = new NewsSource();
            s.setName(source_id);
            return s;
        }
    }

    @Data
    public static class NewsSource {
        private String name;
    }
}