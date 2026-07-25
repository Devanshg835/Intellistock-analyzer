package com.intellistock.service.client;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
            log.warn("NewsAPI key is not configured. Skipping news fetch for: {}", symbol);
            return Optional.empty();
        }

        // Querying for symbol name + stock keyword to focus search results
        String url = String.format("https://newsapi.org/v2/everything?q=%s+stock&pageSize=5&apiKey=%s", symbol, apiKey);
        log.info("Fetching news for symbol: {}", symbol);

        try {
            NewsResponse response = restTemplate.getForObject(url, NewsResponse.class);
            if (response != null && "ok".equalsIgnoreCase(response.getStatus()) && response.getArticles() != null) {
                return Optional.of(response.getArticles());
            }
            log.warn("NewsAPI returned non-ok status or empty articles list for: {}", symbol);
        } catch (Exception e) {
            log.error("Error calling News API for: {}. Message: {}", symbol, e.getMessage());
        }
        return Optional.empty();
    }

    @Data
    public static class NewsResponse {
        private String status;
        private List<NewsArticle> articles;
    }

    @Data
    public static class NewsArticle {
        private String title;
        private String description;
        private NewsSource source;
    }

    @Data
    public static class NewsSource {
        private String name;
    }
}
