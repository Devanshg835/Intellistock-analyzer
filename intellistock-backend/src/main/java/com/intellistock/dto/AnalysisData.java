package com.intellistock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisData {
    private String symbol;
    private String companyName;
    private Double currentPrice;
    private Double marketCap;
    private Double peRatio;
    private Double roe;
    private Double debtToEquity;
    private String sector;
    
    @Builder.Default
    private List<String> newsHeadlines = new ArrayList<>();
    
    private String technicalTrend;
    
    // Tracking API source information
    private String priceSource;
    private String fundamentalSource;
    private String newsSource;
    private String indicatorSource;

    private boolean hasRealPrice;
    private boolean hasRealFundamentals;
    private boolean hasRealNews;
    private boolean hasRealIndicators;
}
