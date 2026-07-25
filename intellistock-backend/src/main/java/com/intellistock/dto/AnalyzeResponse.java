package com.intellistock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeResponse {
    private String symbol;
    private String companyName;
    private Double currentPrice;
    private Double marketCap;
    private Double peRatio;
    private Double roe;
    private Double debtToEquity;
    private String technicalTrend;
    private Integer financialScore;
    private String riskLevel;
    private String newsSentiment;
    private Integer confidenceScore;
    private List<String> sources;
    private String summary;
    private String sector;

    // Phase 3 Score and Recommendation fields
    private String recommendation;
    private String recommendationReason;
    private String aiSummary;
    private Integer overallScore;
    private Integer technicalScore;
    private Integer newsScore;
    private Integer riskScore;
    private List<String> dataSources;
    private String lastUpdated;
    private boolean isCached;
}
