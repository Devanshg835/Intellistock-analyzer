package com.intellistock.service;

import com.intellistock.config.AnalysisCache;
import com.intellistock.dto.AnalyzeResponse;
import com.intellistock.model.Stock;
import com.intellistock.service.client.*;
import com.intellistock.service.intelligence.*;
import com.intellistock.service.ai.GeminiSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnalysisServiceTest {

    @Mock
    private StockService stockService;
    @Mock
    private FmpClient fmpClient;
    @Mock
    private FinnhubClient finnhubClient;
    @Mock
    private TwelveDataClient twelveDataClient;
    @Mock
    private AlphaVantageClient alphaVantageClient;
    @Mock
    private NewsClient newsClient;

    @Spy
    private AnalysisCache analysisCache = new AnalysisCache();
    @Spy
    private FinancialScoreService financialScoreService = new FinancialScoreService();
    @Spy
    private TechnicalScoreService technicalScoreService = new TechnicalScoreService();
    @Spy
    private NewsSentimentService newsSentimentService = new NewsSentimentService();
    @Spy
    private RiskScoreService riskScoreService = new RiskScoreService();
    @Spy
    private ConfidenceService confidenceService = new ConfidenceService();
    @Spy
    private RecommendationEngine recommendationEngine = new RecommendationEngine();
    @Spy
    private GeminiSummaryService geminiSummaryService = new GeminiSummaryService(new org.springframework.web.client.RestTemplate(), "");

    @InjectMocks
    private AnalysisService analysisService;

    @BeforeEach
    void setUp() {
        analysisCache.clear();
    }

    @Test
    void testAnalyzeStock_AllApisFailed_FallbackToLocalDB() {
        // Arrange
        String symbol = "INFY";
        Stock localStock = Stock.builder()
                .symbol("INFY")
                .companyName("Infosys Ltd DB")
                .sector("Tech")
                .currentPrice(20.0)
                .marketCap(90.0)
                .peRatio(25.0)
                .roe(30.0)
                .debtToEquity(0.1)
                .build();

        when(stockService.getStockBySymbol(symbol)).thenReturn(Optional.of(localStock));
        when(finnhubClient.getQuote(symbol)).thenReturn(Optional.empty());
        when(twelveDataClient.getQuote(symbol)).thenReturn(Optional.empty());
        when(alphaVantageClient.getPrice(symbol)).thenReturn(Optional.empty());
        when(fmpClient.getProfile(symbol)).thenReturn(Optional.empty());
        when(fmpClient.getMetrics(symbol)).thenReturn(Optional.empty());
        when(newsClient.getNews(symbol)).thenReturn(Optional.empty());
        when(alphaVantageClient.getSma10(symbol)).thenReturn(Optional.empty());

        // Act
        AnalyzeResponse response = analysisService.analyzeStock(symbol);

        // Assert
        assertNotNull(response);
        assertEquals("INFY", response.getSymbol());
        assertEquals("Infosys Ltd DB", response.getCompanyName());
        assertEquals(20.0, response.getCurrentPrice());
        assertEquals(90.0, response.getMarketCap());
        assertEquals(25.0, response.getPeRatio());
        assertEquals(30.0, response.getRoe());
        assertEquals(0.1, response.getDebtToEquity());
        assertEquals("Tech", response.getSector());
        assertEquals("Neutral", response.getNewsSentiment());
        assertTrue(response.getConfidenceScore() < 100);

        // Verify caching happened
        assertNotNull(analysisCache.get(symbol));
    }

    @Test
    void testAnalyzeStock_ApisSucceed_VerifyCalculations() {
        // Arrange
        String symbol = "AAPL";
        
        FinnhubClient.FinnhubQuote quote = new FinnhubClient.FinnhubQuote();
        quote.setC(150.0);
        
        FmpClient.FmpProfile profile = new FmpClient.FmpProfile();
        profile.setCompanyName("Apple Inc.");
        profile.setSector("Technology");
        profile.setMktCap(2000000000000.0); // $2000B

        FmpClient.FmpMetrics metrics = new FmpClient.FmpMetrics();
        metrics.setPeRatioTTM(30.0);
        metrics.setRoeTTM(0.4); // 40%
        metrics.setDebtToEquityTTM(1.2);

        NewsClient.NewsArticle article = new NewsClient.NewsArticle();
        article.setTitle("Apple growth numbers beat record high expectations");
        
        when(finnhubClient.getQuote(symbol)).thenReturn(Optional.of(quote));
        when(fmpClient.getProfile(symbol)).thenReturn(Optional.of(profile));
        when(fmpClient.getMetrics(symbol)).thenReturn(Optional.of(metrics));
        when(newsClient.getNews(symbol)).thenReturn(Optional.of(Collections.singletonList(article)));
        // price (150) > sma10 (145) by > 2% -> Strong Bullish
        when(alphaVantageClient.getSma10(symbol)).thenReturn(Optional.of(145.0)); 

        // Act
        AnalyzeResponse response = analysisService.analyzeStock(symbol);

        // Assert
        assertNotNull(response);
        assertEquals("AAPL", response.getSymbol());
        assertEquals("Apple Inc.", response.getCompanyName());
        assertEquals(150.0, response.getCurrentPrice());
        assertEquals(2000.0, response.getMarketCap());
        assertEquals(30.0, response.getPeRatio());
        assertEquals(40.0, response.getRoe());
        assertEquals(1.2, response.getDebtToEquity());
        assertEquals("Technology", response.getSector());
        assertEquals("Strong Bullish", response.getTechnicalTrend());
        assertEquals("Very Positive", response.getNewsSentiment());
        assertTrue(response.getConfidenceScore() >= 90);
    }
}
