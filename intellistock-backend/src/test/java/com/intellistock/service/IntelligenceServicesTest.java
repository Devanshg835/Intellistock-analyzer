package com.intellistock.service;

import com.intellistock.dto.AnalysisData;
import com.intellistock.service.ai.GeminiSummaryService;
import com.intellistock.service.intelligence.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import com.intellistock.repository.WatchlistRepository;
import com.intellistock.service.WatchlistService;
import com.intellistock.service.client.*;
import java.util.Optional;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class IntelligenceServicesTest {

    private final FinancialScoreService financialScoreService = new FinancialScoreService();
    private final TechnicalScoreService technicalScoreService = new TechnicalScoreService();
    private final NewsSentimentService newsSentimentService = new NewsSentimentService();
    private final RiskScoreService riskScoreService = new RiskScoreService();
    private final ConfidenceService confidenceService = new ConfidenceService();
    private final RecommendationEngine recommendationEngine = new RecommendationEngine();

    @Test
    void testFinancialScoreCalculation() {
        // High quality balance sheet
        int scoreHigh = financialScoreService.calculateScore(12.0, 22.0, 0.2);
        assertTrue(scoreHigh >= 90);

        // Stretched debt and high multiples
        int scoreLow = financialScoreService.calculateScore(60.0, 3.0, 2.5);
        assertTrue(scoreLow <= 25);
    }

    @Test
    void testTechnicalScoreMapping() {
        assertEquals(95, technicalScoreService.calculateScore("Strong Bullish"));
        assertEquals(75, technicalScoreService.calculateScore("Bullish"));
        assertEquals(50, technicalScoreService.calculateScore("Neutral"));
        assertEquals(25, technicalScoreService.calculateScore("Bearish"));
        assertEquals(10, technicalScoreService.calculateScore("Strong Bearish"));
    }

    @Test
    void testNewsSentimentScoreMapping() {
        assertEquals(95, newsSentimentService.calculateScore("Very Positive"));
        assertEquals(75, newsSentimentService.calculateScore("Positive"));
        assertEquals(50, newsSentimentService.calculateScore("Neutral"));
        assertEquals(25, newsSentimentService.calculateScore("Negative"));
        assertEquals(10, newsSentimentService.calculateScore("Very Negative"));
    }

    @Test
    void testRiskScoreAndLevel() {
        // High debt, high PE -> High Risk score
        int riskScoreHigh = riskScoreService.calculateScore(2.5, 45.0);
        assertEquals("High", riskScoreService.determineRiskLevel(riskScoreHigh));

        // Low debt, low PE -> Low Risk score
        int riskScoreLow = riskScoreService.calculateScore(0.15, 10.0);
        assertEquals("Low", riskScoreService.determineRiskLevel(riskScoreLow));
    }

    @Test
    void testConfidenceServiceScore() {
        AnalysisData realData = AnalysisData.builder()
                .hasRealPrice(true)
                .hasRealFundamentals(true)
                .hasRealNews(true)
                .hasRealIndicators(true)
                .build();
        assertEquals(100, confidenceService.calculateScore(realData));

        AnalysisData fallbackData = AnalysisData.builder()
                .hasRealPrice(false)
                .hasRealFundamentals(false)
                .hasRealNews(false)
                .hasRealIndicators(false)
                .build();
        assertEquals(40, confidenceService.calculateScore(fallbackData)); // Minimum score boundary
    }

    @Test
    void testRecommendationThresholds() {
        // BUY: High overall, low risk
        assertEquals("BUY", recommendationEngine.determineRecommendation(80, 20));

        // SELL: Low overall, moderate risk
        assertEquals("SELL", recommendationEngine.determineRecommendation(40, 55));

        // WATCH: Moderate overall, high risk
        assertEquals("WATCH", recommendationEngine.determineRecommendation(45, 75));

        // HOLD: Mixed score
        assertEquals("HOLD", recommendationEngine.determineRecommendation(60, 40));
    }

    @Test
    void testGeminiServiceFallbackPrompt() {
        // Instantiate GeminiSummaryService with empty api key to trigger local fallback immediately
        GeminiSummaryService summaryService = new GeminiSummaryService(new RestTemplate(), "");

        AnalysisData data = AnalysisData.builder()
                .symbol("TCS")
                .companyName("Tata Consultancy Services")
                .currentPrice(40.0)
                .marketCap(150.0)
                .peRatio(25.0)
                .roe(35.0)
                .debtToEquity(0.1)
                .technicalTrend("Bullish")
                .newsHeadlines(new ArrayList<>())
                .build();

        String summary = summaryService.generateSummary(data, 85, 95, 75, 50, 15, "BUY");

        assertNotNull(summary);
        assertTrue(summary.contains("Tata Consultancy Services"));
        assertTrue(summary.contains("BUY"));
        assertTrue(summary.contains("85/100"));
    }

    @Test
    void testWatchlistServiceLogic() {
        WatchlistRepository repo = org.mockito.Mockito.mock(WatchlistRepository.class);
        StockService stockService = org.mockito.Mockito.mock(StockService.class);
        
        WatchlistService service = new WatchlistService(repo, stockService);
        
        org.mockito.Mockito.when(repo.existsBySymbolIgnoreCase("AAPL")).thenReturn(true);
        assertTrue(service.isWatched("AAPL"));
        
        org.mockito.Mockito.when(repo.existsBySymbolIgnoreCase("MSFT")).thenReturn(false);
        assertFalse(service.isWatched("msft"));
    }

    @Test
    void testStockHistoryMockGeneration() {
        StockService stockService = org.mockito.Mockito.mock(StockService.class);
        FmpClient fmpClient = org.mockito.Mockito.mock(FmpClient.class);
        FinnhubClient finnhubClient = org.mockito.Mockito.mock(FinnhubClient.class);
        TwelveDataClient twelveDataClient = org.mockito.Mockito.mock(TwelveDataClient.class);
        AlphaVantageClient alphaVantageClient = org.mockito.Mockito.mock(AlphaVantageClient.class);
        NewsClient newsClient = org.mockito.Mockito.mock(NewsClient.class);
        com.intellistock.config.AnalysisCache analysisCache = org.mockito.Mockito.mock(com.intellistock.config.AnalysisCache.class);
        FinancialScoreService finScore = new FinancialScoreService();
        TechnicalScoreService techScore = new TechnicalScoreService();
        NewsSentimentService newsSentiment = new NewsSentimentService();
        RiskScoreService riskScore = new RiskScoreService();
        ConfidenceService conf = new ConfidenceService();
        RecommendationEngine engine = new RecommendationEngine();
        GeminiSummaryService gemini = new GeminiSummaryService(new RestTemplate(), "");

        AnalysisService analysisService = new AnalysisService(
            stockService, fmpClient, finnhubClient, twelveDataClient, alphaVantageClient, newsClient,
            analysisCache, finScore, techScore, newsSentiment, riskScore, conf, engine, gemini
        );

        org.mockito.Mockito.when(twelveDataClient.getTimeSeries("MSFT")).thenReturn(Optional.empty());

        com.intellistock.dto.StockHistoryResponse historyResponse = analysisService.getStockHistory("MSFT");

        assertNotNull(historyResponse);
        assertEquals("MSFT", historyResponse.getSymbol());
        assertFalse(historyResponse.getHistory().isEmpty());
        assertEquals(31, historyResponse.getHistory().size());
    }
}
