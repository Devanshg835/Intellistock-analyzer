package com.intellistock.service.intelligence;

import org.springframework.stereotype.Service;

@Service
public class RecommendationEngine {

    public int calculateOverallScore(int financial, int technical, int news, int risk) {
        double weighted = (financial * 0.4) + (technical * 0.3) + (news * 0.2) + ((100 - risk) * 0.1);
        return (int) Math.round(weighted);
    }

    public String determineRecommendation(int overallScore, int riskScore) {
        if (overallScore >= 75 && riskScore <= 60) {
            return "BUY";
        } else if (overallScore < 45 && riskScore >= 50) {
            return "SELL";
        } else if (overallScore < 50 && riskScore >= 70) {
            return "WATCH";
        } else {
            return "HOLD";
        }
    }

    public String generateReason(int overallScore, int riskScore, int financial, int technical, int news) {
        StringBuilder reason = new StringBuilder();

        if (overallScore >= 75 && riskScore <= 60) {
            reason.append("Strong financial health (").append(financial).append("/100) ")
                  .append("and supportive market signals (Technical: ").append(technical).append(", News: ").append(news).append(") ")
                  .append("outweigh the low-to-moderate risk profile (").append(riskScore).append("/100), suggesting a favorable investment setup.");
        } else if (overallScore < 45 && riskScore >= 50) {
            reason.append("Weak fundamental health (").append(financial).append("/100) ")
                  .append("and unfavorable sentiment trends (News: ").append(news).append(") ")
                  .append("coupled with elevated leverage risk (").append(riskScore).append("/100) suggest significant downside pressure.");
        } else if (overallScore < 50 && riskScore >= 70) {
            reason.append("Extreme risk parameters (").append(riskScore).append("/100) ")
                  .append("and stagnant technical support indicators require cautionary monitoring. Placed on watchlist.");
        } else {
            // Mixed signals
            reason.append("The stock displays a mixed setup: ");
            if (financial >= 70 && technical <= 40) {
                reason.append("robust fundamentals (").append(financial).append("/100) are offset by bearish technical momentum (").append(technical).append("/100), warranting a patient watch.");
            } else if (financial <= 40 && technical >= 70) {
                reason.append("strong technical momentum (").append(technical).append("/100) is unsupported by weak balance sheet health (").append(financial).append("/100).");
            } else {
                reason.append("current valuations (Score: ").append(overallScore).append("/100) and risk factors (").append(riskScore).append("/100) appear balanced in the current consolidation zone.");
            }
        }

        return reason.toString();
    }
}
