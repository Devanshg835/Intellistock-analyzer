package com.intellistock.service.intelligence;

import org.springframework.stereotype.Service;

@Service
public class FinancialScoreService {

    public int calculateScore(Double pe, Double roe, Double debtToEquity) {
        int score = 50; // base score

        if (roe != null) {
            if (roe > 20) score += 20;
            else if (roe > 15) score += 15;
            else if (roe > 10) score += 10;
            else if (roe < 5) score -= 10;
        }

        if (pe != null) {
            if (pe > 0 && pe < 15) score += 15;
            else if (pe >= 15 && pe < 30) score += 10;
            else if (pe >= 30 && pe < 50) score -= 5;
            else if (pe >= 50) score -= 15;
        }

        if (debtToEquity != null) {
            if (debtToEquity < 0.5) score += 15;
            else if (debtToEquity < 1.0) score += 10;
            else if (debtToEquity > 2.0) score -= 15;
        }

        return Math.max(0, Math.min(100, score));
    }
}
