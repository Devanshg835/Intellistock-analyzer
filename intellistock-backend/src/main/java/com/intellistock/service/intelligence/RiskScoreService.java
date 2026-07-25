package com.intellistock.service.intelligence;

import org.springframework.stereotype.Service;

@Service
public class RiskScoreService {

    public int calculateScore(Double debtToEquity, Double pe) {
        if (debtToEquity == null || pe == null) return 50; // default medium risk

        double rawRisk = (debtToEquity * 35) + (pe * 0.6);
        
        // Normalize to 0-100 range
        int score = (int) Math.round(rawRisk);
        return Math.max(5, Math.min(95, score)); // keep bounds between 5 and 95
    }

    public String determineRiskLevel(int riskScore) {
        if (riskScore > 65) {
            return "High";
        } else if (riskScore > 35) {
            return "Medium";
        } else {
            return "Low";
        }
    }
}
