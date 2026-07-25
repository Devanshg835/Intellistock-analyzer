package com.intellistock.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellistock.dto.AnalyzeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class AnalysisCache {

    private static final Logger log = LoggerFactory.getLogger(AnalysisCache.class);
    
    // 5 minutes Time-to-Live (TTL)
    private static final long TTL_MS = 5 * 60 * 1000;
    private static final String REDIS_PREFIX = "intellistock:analysis:";

    private final Map<String, CacheEntry> memoryCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final StringRedisTemplate redisTemplate;
    private boolean isRedisAvailable = false;

    public AnalysisCache() {
        this.redisTemplate = null;
    }

    @Autowired
    public AnalysisCache(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        if (redisTemplate != null) {
            try {
                // Ping Redis to test connection availability
                redisTemplate.getConnectionFactory().getConnection().ping();
                isRedisAvailable = true;
                log.info("Successfully connected to Redis. Caching will use Redis backend.");
            } catch (Exception e) {
                log.warn("Redis is configured, but connection test failed: {}. Falling back to in-memory caching.", e.getMessage());
                isRedisAvailable = false;
            }
        } else {
            log.info("Redis client not configured. Caching will use in-memory backend.");
        }
    }

    private static class CacheEntry {
        final AnalyzeResponse response;
        final long timestamp;

        CacheEntry(AnalyzeResponse response, long timestamp) {
            this.response = response;
            this.timestamp = timestamp;
        }
    }

    public void put(String symbol, AnalyzeResponse response) {
        if (symbol == null || response == null) return;
        String cleanSymbol = symbol.trim().toUpperCase();
        
        // Mark as cached before saving it
        response.setCached(true);

        if (isRedisAvailable) {
            try {
                String redisKey = REDIS_PREFIX + cleanSymbol;
                String json = objectMapper.writeValueAsString(response);
                redisTemplate.opsForValue().set(redisKey, json, TTL_MS, TimeUnit.MILLISECONDS);
                log.info("Analysis cached in Redis for symbol: {} with TTL of 5 minutes", cleanSymbol);
                return;
            } catch (Exception e) {
                log.error("Failed to write to Redis for symbol: {}. Falling back to memory: {}", cleanSymbol, e.getMessage());
            }
        }

        memoryCache.put(cleanSymbol, new CacheEntry(response, System.currentTimeMillis()));
        log.info("Analysis cached in-memory for symbol: {} with TTL of 5 minutes", cleanSymbol);
    }

    public AnalyzeResponse get(String symbol) {
        if (symbol == null) return null;
        String cleanSymbol = symbol.trim().toUpperCase();

        if (isRedisAvailable) {
            try {
                String redisKey = REDIS_PREFIX + cleanSymbol;
                String json = redisTemplate.opsForValue().get(redisKey);
                if (json != null) {
                    AnalyzeResponse response = objectMapper.readValue(json, AnalyzeResponse.class);
                    response.setCached(true);
                    log.info("Redis cache hit for symbol: {}", cleanSymbol);
                    return response;
                }
                log.debug("Redis cache miss for symbol: {}", cleanSymbol);
            } catch (Exception e) {
                log.error("Failed to read from Redis for symbol: {}. Falling back to memory check: {}", cleanSymbol, e.getMessage());
            }
        }

        CacheEntry entry = memoryCache.get(cleanSymbol);
        if (entry == null) {
            log.debug("Memory cache miss for symbol: {}", cleanSymbol);
            return null;
        }

        long age = System.currentTimeMillis() - entry.timestamp;
        if (age > TTL_MS) {
            log.info("Memory cache expired for symbol: {} (age: {}ms)", cleanSymbol, age);
            memoryCache.remove(cleanSymbol);
            return null;
        }

        log.info("Memory cache hit for symbol: {} (age: {}ms)", cleanSymbol, age);
        
        entry.response.setCached(true);
        return entry.response;
    }

    public void clear() {
        if (isRedisAvailable) {
            try {
                java.util.Set<String> keys = redisTemplate.keys(REDIS_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
                log.info("Redis stock analysis cache cleared");
            } catch (Exception e) {
                log.error("Failed to clear Redis cache: {}", e.getMessage());
            }
        }
        memoryCache.clear();
        log.info("In-memory stock analysis cache cleared");
    }
}
