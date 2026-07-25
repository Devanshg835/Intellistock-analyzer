package com.intellistock.service.intelligence;

import com.intellistock.dto.AnalysisData;
import org.springframework.stereotype.Service;

@Service
public class ConfidenceService {

    public int calculateScore(AnalysisData data) {
        if (data == null) return 40;

        int score = 100;

        if (!data.isHasRealPrice()) {
            score -= 15;
        }
        if (!data.isHasRealFundamentals()) {
            score -= 25;
        }
        if (!data.isHasRealNews()) {
            score -= 10;
        }
        if (!data.isHasRealIndicators()) {
            score -= 10;
        }

        // Subtract for stale local database falls
        if ("Local H2 Database".equalsIgnoreCase(data.getPriceSource())) {
            score -= 5;
        }
        if ("Local H2 Database".equalsIgnoreCase(data.getFundamentalSource())) {
            score -= 5;
        }

        return Math.max(40, score); // minimum score limit is 40
    }
}
