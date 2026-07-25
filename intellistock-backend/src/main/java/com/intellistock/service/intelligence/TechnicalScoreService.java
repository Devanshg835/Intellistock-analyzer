package com.intellistock.service.intelligence;

import org.springframework.stereotype.Service;

@Service
public class TechnicalScoreService {

    public int calculateScore(String trend) {
        if (trend == null) return 50;

        switch (trend.trim()) {
            case "Strong Bullish":
                return 95;
            case "Bullish":
                return 75;
            case "Bearish":
                return 25;
            case "Strong Bearish":
                return 10;
            case "Neutral":
            default:
                return 50;
        }
    }
}
