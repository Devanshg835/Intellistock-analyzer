package com.intellistock.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.intellistock.dto.AnalysisData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class GeminiSummaryService {

    private static final Logger log = LoggerFactory.getLogger(GeminiSummaryService.class);

    private final RestTemplate restTemplate;
    private final String apiKey;

    @Autowired
    public GeminiSummaryService(RestTemplate restTemplate, @Value("${api.gemini.key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey.trim();
    }

    public String generateSummary(AnalysisData data, int overallScore, int financialScore, int technicalScore, int newsScore, int riskScore, String recommendation) {
        if (apiKey.isEmpty()) {
            log.warn("Gemini API key is not configured. Falling back to local summary generator.");
            return generateLocalFallbackSummary(data, overallScore, riskScore, recommendation);
        }

        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=%s", apiKey);
        String prompt = buildPrompt(data, overallScore, financialScore, technicalScore, newsScore, riskScore, recommendation);
        log.info("Invoking Gemini API for symbol: {}", data.getSymbol());

        try {
            GeminiRequest requestBody = new GeminiRequest(prompt);
            GeminiResponse response = restTemplate.postForObject(url, requestBody, GeminiResponse.class);

            if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                GeminiResponse.Candidate candidate = response.getCandidates().get(0);
                if (candidate.getContent() != null && candidate.getContent().getParts() != null && !candidate.getContent().getParts().isEmpty()) {
                    String resultText = candidate.getContent().getParts().get(0).getText();
                    if (resultText != null && !resultText.trim().isEmpty()) {
                        return resultText.trim();
                    }
                }
            }
            log.warn("Gemini returned empty candidate content. Falling back to local summary.");
        } catch (Exception e) {
            log.error("Error calling Gemini API for symbol: {}. Error: {}", data.getSymbol(), e.getMessage());
        }

        return generateLocalFallbackSummary(data, overallScore, riskScore, recommendation);
    }

    private String buildPrompt(AnalysisData data, int overall, int financial, int technical, int news, int risk, String recommendation) {
        return String.format(
            "You are a financial analyst at IntelliStock. Provide a concise, human-readable paragraph analysis of the following stock data. " +
            "Limit the output to a maximum of 4 sentences and keep it under 100 words. " +
            "Your output must cover: 1) trend summary, 2) risk note, 3) valuation note, and 4) final suggestion. Do not include markdown headers or list formatting.\n" +
            "Stock Details:\n" +
            "- Symbol: %s\n" +
            "- Company Name: %s\n" +
            "- Current Price: $%s\n" +
            "- Sector: %s\n" +
            "- Market Capitalization: $%sB\n" +
            "- P/E Ratio: %s\n" +
            "- ROE: %s%%\n" +
            "- Debt-to-Equity: %s\n" +
            "- Technical Trend: %s\n" +
            "- Risk Profile Rating: %s/100\n" +
            "- Calculated Scores (out of 100): Overall Score: %s, Fundamentals Score: %s, Technical Score: %s, News Sentiment Score: %s\n" +
            "- Target Recommendation: %s\n",
            data.getSymbol(),
            data.getCompanyName() != null ? data.getCompanyName() : data.getSymbol(),
            data.getCurrentPrice() != null ? data.getCurrentPrice() : "N/A",
            data.getSector() != null ? data.getSector() : "N/A",
            data.getMarketCap() != null ? data.getMarketCap() : "N/A",
            data.getPeRatio() != null ? data.getPeRatio() : "N/A",
            data.getRoe() != null ? data.getRoe() : "N/A",
            data.getDebtToEquity() != null ? data.getDebtToEquity() : "N/A",
            data.getTechnicalTrend() != null ? data.getTechnicalTrend() : "N/A",
            risk,
            overall, financial, technical, news,
            recommendation
        );
    }

    private String generateLocalFallbackSummary(AnalysisData data, int overall, int risk, String recommendation) {
        StringBuilder sb = new StringBuilder();
        sb.append(data.getCompanyName() != null ? data.getCompanyName() : data.getSymbol())
          .append(" (").append(data.getSymbol()).append(") ")
          .append("presents a ").append(recommendation).append(" opportunity with an overall score of ")
          .append(overall).append("/100. ");

        if (data.getCurrentPrice() != null) {
            sb.append("At $").append(data.getCurrentPrice());
            if (data.getPeRatio() != null) {
                sb.append(" and a P/E of ").append(String.format("%.1f", data.getPeRatio()));
            }
            sb.append(", stock valuations appear ");
            if (data.getPeRatio() != null && data.getPeRatio() > 35) {
                sb.append("elevated. ");
            } else {
                sb.append("fairly valued. ");
            }
        }

        sb.append("Technical signal trends bullish/bearish indicate a ").append(data.getTechnicalTrend().toLowerCase()).append(" trend. ");
        
        if (risk >= 65) {
            sb.append("Cautious approaches are advised due to a higher risk profile (").append(risk).append("/100).");
        } else {
            sb.append("Overall risk remains within standard low-to-moderate tolerances.");
        }

        return sb.toString();
    }

    // --- JSON Mapping POJOs for Gemini Rest Request ---

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiRequest {
        private List<Content> contents = new ArrayList<>();
        private GenerationConfig generationConfig = new GenerationConfig();

        public GeminiRequest(String promptText) {
            Content content = new Content();
            Part part = new Part();
            part.setText(promptText);
            content.getParts().add(part);
            this.contents.add(content);
        }
    }

    @Data
    public static class Content {
        private List<Part> parts = new ArrayList<>();
    }

    @Data
    public static class Part {
        private String text;
    }

    @Data
    public static class GenerationConfig {
        private Double temperature = 0.2;
        private Integer maxOutputTokens = 200;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiResponse {
        private List<Candidate> candidates;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Candidate {
            private Content content;
        }
    }
}
