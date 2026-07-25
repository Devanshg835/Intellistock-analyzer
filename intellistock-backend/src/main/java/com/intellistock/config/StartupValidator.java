package com.intellistock.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StartupValidator {

    private static final Logger log = LoggerFactory.getLogger(StartupValidator.class);

    @Value("${api.gemini.key:}")
    private String geminiKey;

    @Value("${api.finnhub.key:}")
    private String finnhubKey;

    @Value("${api.fmp.key:}")
    private String fmpKey;

    @Value("${api.news.key:}")
    private String newsKey;

    @Value("${api.alphavantage.key:}")
    private String alphaVantageKey;

    @Value("${api.twelvedata.key:}")
    private String twelveDataKey;

    @PostConstruct
    public void validate() {
        List<String> missingKeys = new ArrayList<>();
        if (geminiKey.trim().isEmpty()) missingKeys.add("GEMINI_API_KEY");
        if (finnhubKey.trim().isEmpty()) missingKeys.add("FINNHUB_API_KEY");
        if (fmpKey.trim().isEmpty()) missingKeys.add("FMP_API_KEY");
        if (newsKey.trim().isEmpty()) missingKeys.add("NEWS_API_KEY");
        if (alphaVantageKey.trim().isEmpty()) missingKeys.add("ALPHA_VANTAGE_API_KEY");
        if (twelveDataKey.trim().isEmpty()) missingKeys.add("TWELVE_DATA_API_KEY");

        log.info("=================================================================");
        log.info("                 INTELLISTOCK STARTUP DIAGNOSTICS                ");
        log.info("=================================================================");
        if (missingKeys.isEmpty()) {
            log.info("   All core external API integration keys are configured!");
        } else {
            log.warn("   The following optional API keys are missing from environment:");
            for (String key : missingKeys) {
                log.warn("   - {}", key);
            }
            log.warn("   Fallback mock engine and cached H2 DB values will be used.");
        }
        log.info("=================================================================");
    }
}
