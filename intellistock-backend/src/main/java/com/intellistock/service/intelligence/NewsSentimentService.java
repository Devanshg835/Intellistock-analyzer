package com.intellistock.service.intelligence;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NewsSentimentService {

    public String analyzeSentiment(List<String> headlines) {
        if (headlines == null || headlines.isEmpty()) {
            return "Neutral";
        }

        int score = 0;
        String[] positiveWords = {"positive", "buy", "growth", "record", "rise", "profit", "gain", "beat", "upgrade", "bullish", "strong", "higher"};
        String[] negativeWords = {"negative", "sell", "fall", "decline", "loss", "drop", "miss", "downgrade", "bearish", "caution", "lower", "weak"};

        for (String headline : headlines) {
            String lower = headline.toLowerCase();
            for (String pos : positiveWords) {
                if (lower.contains(pos)) score++;
            }
            for (String neg : negativeWords) {
                if (lower.contains(neg)) score--;
            }
        }

        if (score >= 2) return "Very Positive";
        else if (score > 0) return "Positive";
        else if (score < -1) return "Very Negative";
        else if (score < 0) return "Negative";
        return "Neutral";
    }

    public int calculateScore(String sentiment) {
        if (sentiment == null) return 50;

        switch (sentiment.trim()) {
            case "Very Positive":
                return 95;
            case "Positive":
                return 75;
            case "Very Negative":
                return 10;
            case "Negative":
                return 25;
            case "Neutral":
            default:
                return 50;
        }
    }
}
