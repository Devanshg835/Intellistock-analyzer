package com.intellistock.service;

import com.intellistock.config.AnalysisCache;
import com.intellistock.dto.AnalyzeResponse;
import com.intellistock.dto.AnalysisData;
import com.intellistock.dto.HistoryDataPoint;
import com.intellistock.dto.StockHistoryResponse;
import com.intellistock.model.Stock;
import com.intellistock.service.client.*;
import com.intellistock.service.intelligence.*;
import com.intellistock.service.ai.GeminiSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final StockService stockService;
    private final FmpClient fmpClient;
    private final FinnhubClient finnhubClient;
    private final TwelveDataClient twelveDataClient;
    private final YahooFinanceClient yahooFinanceClient;
    private final AlphaVantageClient alphaVantageClient;
    private final NewsClient newsClient;
    private final AnalysisCache analysisCache;

    private final FinancialScoreService financialScoreService;
    private final TechnicalScoreService technicalScoreService;
    private final NewsSentimentService newsSentimentService;
    private final RiskScoreService riskScoreService;
    private final ConfidenceService confidenceService;
    private final RecommendationEngine recommendationEngine;
    private final GeminiSummaryService geminiSummaryService;

    @Autowired
    public AnalysisService(StockService stockService, FmpClient fmpClient,
                           FinnhubClient finnhubClient, TwelveDataClient twelveDataClient,YahooFinanceClient yahooFinanceClient,
                           AlphaVantageClient alphaVantageClient, NewsClient newsClient,
                           AnalysisCache analysisCache, FinancialScoreService financialScoreService,
                           TechnicalScoreService technicalScoreService, NewsSentimentService newsSentimentService,
                           RiskScoreService riskScoreService, ConfidenceService confidenceService,
                           RecommendationEngine recommendationEngine, GeminiSummaryService geminiSummaryService) {
        this.stockService = stockService;
        this.fmpClient = fmpClient;
        this.finnhubClient = finnhubClient;
        this.twelveDataClient = twelveDataClient;
        this.yahooFinanceClient = yahooFinanceClient;
        this.alphaVantageClient = alphaVantageClient;
        this.newsClient = newsClient;
        this.analysisCache = analysisCache;
        this.financialScoreService = financialScoreService;
        this.technicalScoreService = technicalScoreService;
        this.newsSentimentService = newsSentimentService;
        this.riskScoreService = riskScoreService;
        this.confidenceService = confidenceService;
        this.recommendationEngine = recommendationEngine;
        this.geminiSummaryService = geminiSummaryService;
    }

    public AnalyzeResponse analyzeStock(String symbol) {
        String cleanSymbol = symbol.trim().toUpperCase();

        AnalyzeResponse cachedResponse = analysisCache.get(cleanSymbol);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        log.info("Cache miss. Firing API calls for symbol: {}", cleanSymbol);

        AnalysisData data = fetchUnifiedData(cleanSymbol);
        AnalyzeResponse response = processAnalysis(data);
        analysisCache.put(cleanSymbol, response);

        return response;
    }

    private AnalysisData fetchUnifiedData(String symbol) {
        AnalysisData.AnalysisDataBuilder builder = AnalysisData.builder().symbol(symbol);
        Optional<Stock> localStockOpt = stockService.getStockBySymbol(symbol);

      // --- Price Fallback Tree ---
        log.debug("Fetching price data for symbol: {}", symbol);
        String nseSymbol = symbol + ":NSE";

        Optional<Double> yahooPriceOpt = yahooFinanceClient.getNsePrice(symbol);
        if (yahooPriceOpt.isPresent()) {
            builder.currentPrice(yahooPriceOpt.get())
                   .priceSource("Yahoo Finance (NSE)")
                   .hasRealPrice(true);
        } else {
            Optional<TwelveDataClient.TwelveDataQuote> twelveDataQuoteOpt = twelveDataClient.getQuote(nseSymbol);
            if (twelveDataQuoteOpt.isPresent() && twelveDataQuoteOpt.get().getPriceAsDouble() != null) {
                builder.currentPrice(twelveDataQuoteOpt.get().getPriceAsDouble())
                       .priceSource("Twelve Data API (NSE)")
                       .hasRealPrice(true);
            } else {
                Optional<FinnhubClient.FinnhubQuote> finnhubQuoteOpt = finnhubClient.getQuote(symbol);
                if (finnhubQuoteOpt.isPresent() && finnhubQuoteOpt.get().getC() != null && finnhubQuoteOpt.get().getC() > 0) {
                    builder.currentPrice(finnhubQuoteOpt.get().getC())
                           .priceSource("Finnhub API")
                           .hasRealPrice(true);
                } else {
                    Optional<Double> avPriceOpt = alphaVantageClient.getPrice(symbol);
                    if (avPriceOpt.isPresent()) {
                        builder.currentPrice(avPriceOpt.get())
                               .priceSource("Alpha Vantage API")
                               .hasRealPrice(true);
                    } else if (localStockOpt.isPresent()) {
                        builder.currentPrice(localStockOpt.get().getCurrentPrice())
                               .priceSource("Local H2 Database")
                               .hasRealPrice(false);
                    } else {
                        builder.currentPrice(generateMockPrice(symbol))
                               .priceSource("Dynamic Fallback Engine")
                               .hasRealPrice(false);
                    }
                }
            }
        }

        AnalysisData temp = builder.build();

        // --- Fundamentals Fallback Tree ---
        log.debug("Fetching fundamentals data for symbol: {}", symbol);
        Optional<FmpClient.FmpProfile> fmpProfileOpt = fmpClient.getProfile(symbol);
        Optional<FmpClient.FmpMetrics> fmpMetricsOpt = fmpClient.getMetrics(symbol);

        if (fmpProfileOpt.isPresent() || fmpMetricsOpt.isPresent()) {
            builder.fundamentalSource("Financial Modeling Prep API")
                   .hasRealFundamentals(true);

            if (fmpProfileOpt.isPresent()) {
                FmpClient.FmpProfile profile = fmpProfileOpt.get();
                builder.companyName(profile.getCompanyName())
                       .sector(profile.getSector());

                if (profile.getMktCap() != null) {
                    builder.marketCap(profile.getMktCap() / 1_000_000_000.0);
                }
            }

            if (fmpMetricsOpt.isPresent()) {
                FmpClient.FmpMetrics metrics = fmpMetricsOpt.get();
                builder.peRatio(metrics.getPeRatioTTM());
                builder.debtToEquity(metrics.getDebtToEquityTTM());

                if (metrics.getRoeTTM() != null) {
                    double roeVal = metrics.getRoeTTM();
                    if (Math.abs(roeVal) <= 1.0) {
                        roeVal = roeVal * 100.0;
                    }
                    builder.roe(roeVal);
                }
            }
        } else if (localStockOpt.isPresent()) {
            Stock dbStock = localStockOpt.get();
            builder.companyName(dbStock.getCompanyName())
                   .sector(dbStock.getSector())
                   .marketCap(dbStock.getMarketCap())
                   .peRatio(dbStock.getPeRatio())
                   .roe(dbStock.getRoe())
                   .debtToEquity(dbStock.getDebtToEquity())
                   .fundamentalSource("Local H2 Database")
                   .hasRealFundamentals(false);
        } else {
            Stock mockStock = generateMockStockRecord(symbol);
            builder.companyName(mockStock.getCompanyName())
                   .sector(mockStock.getSector())
                   .marketCap(mockStock.getMarketCap())
                   .peRatio(mockStock.getPeRatio())
                   .roe(mockStock.getRoe())
                   .debtToEquity(mockStock.getDebtToEquity())
                   .fundamentalSource("Dynamic Fallback Engine")
                   .hasRealFundamentals(false);
        }

        // --- News Headlines Fallback ---
        log.debug("Fetching news data for symbol: {}", symbol);
        Optional<List<NewsClient.NewsArticle>> newsArticlesOpt = newsClient.getNews(symbol);
        if (newsArticlesOpt.isPresent() && !newsArticlesOpt.get().isEmpty()) {
            List<String> headlines = newsArticlesOpt.get().stream()
                    .map(NewsClient.NewsArticle::getTitle)
                    .collect(Collectors.toList());
            builder.newsHeadlines(headlines)
                   .newsSource("NewsAPI")
                   .hasRealNews(true);
        } else {
            builder.newsHeadlines(new ArrayList<>())
                   .newsSource("No News Available")
                   .hasRealNews(false);
        }

        // --- Technical indicators (SMA) Fallback ---
        log.debug("Fetching technical indicators for symbol: {}", symbol);
        Optional<Double> smaOpt = alphaVantageClient.getSma10(symbol);
        if (smaOpt.isPresent() && temp.getCurrentPrice() != null) {
            double currentPrice = temp.getCurrentPrice();
            double sma10 = smaOpt.get();
            String trend = evaluateTechnicalTrend(currentPrice, sma10);
            builder.technicalTrend(trend)
                   .indicatorSource("Alpha Vantage SMA(10)")
                   .hasRealIndicators(true);
        } else {
            builder.technicalTrend(determineTechnicalTrendFallback(symbol))
                   .indicatorSource("Deterministic Analysis Engine")
                   .hasRealIndicators(false);
        }

        return builder.build();
    }

    private AnalyzeResponse processAnalysis(AnalysisData data) {
        String newsSentiment = newsSentimentService.analyzeSentiment(data.getNewsHeadlines());
        int newsScore = newsSentimentService.calculateScore(newsSentiment);

        int technicalScore = technicalScoreService.calculateScore(data.getTechnicalTrend());

        int financialScore = financialScoreService.calculateScore(data.getPeRatio(), data.getRoe(), data.getDebtToEquity());

        int riskScore = riskScoreService.calculateScore(data.getDebtToEquity(), data.getPeRatio());
        String riskLevel = riskScoreService.determineRiskLevel(riskScore);

        int confidenceScore = confidenceService.calculateScore(data);

        int overallScore = recommendationEngine.calculateOverallScore(financialScore, technicalScore, newsScore, riskScore);
        String recommendation = recommendationEngine.determineRecommendation(overallScore, riskScore);
        String recommendationReason = recommendationEngine.generateReason(overallScore, riskScore, financialScore, technicalScore, newsScore);

        String aiSummary = geminiSummaryService.generateSummary(data, overallScore, financialScore, technicalScore, newsScore, riskScore, recommendation);

        List<String> sources = new ArrayList<>();
        sources.add(data.getPriceSource());
        sources.add(data.getFundamentalSource());
        if (data.isHasRealNews()) {
            sources.add(data.getNewsSource());
        }
        sources.add(data.getIndicatorSource());

        String lastUpdated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return AnalyzeResponse.builder()
                .symbol(data.getSymbol())
                .companyName(data.getCompanyName())
                .currentPrice(data.getCurrentPrice())
                .marketCap(data.getMarketCap())
                .peRatio(data.getPeRatio())
                .roe(data.getRoe())
                .debtToEquity(data.getDebtToEquity())
                .technicalTrend(data.getTechnicalTrend())
                .financialScore(financialScore)
                .riskLevel(riskLevel)
                .newsSentiment(newsSentiment)
                .confidenceScore(confidenceScore)
                .sources(sources)
                .summary(aiSummary)
                .sector(data.getSector())
                .recommendation(recommendation)
                .recommendationReason(recommendationReason)
                .aiSummary(aiSummary)
                .overallScore(overallScore)
                .technicalScore(technicalScore)
                .newsScore(newsScore)
                .riskScore(riskScore)
                .dataSources(sources)
                .lastUpdated(lastUpdated)
                .build();
    }

    private String evaluateTechnicalTrend(double currentPrice, double sma10) {
        double diffPct = ((currentPrice - sma10) / sma10) * 100.0;
        if (diffPct > 2.0) {
            return "Strong Bullish";
        } else if (diffPct > 0.0) {
            return "Bullish";
        } else if (diffPct < -2.0) {
            return "Strong Bearish";
        } else {
            return "Bearish";
        }
    }

    private String determineTechnicalTrendFallback(String symbol) {
        int hash = Math.abs(symbol.hashCode());
        String[] trends = {"Strong Bullish", "Bullish", "Neutral", "Bearish", "Strong Bearish"};
        return trends[hash % trends.length];
    }

    private double generateMockPrice(String symbol) {
        int hash = Math.abs(symbol.hashCode());
        double base = 50.0 + (hash % 450);
        return Math.round(base * 100.0) / 100.0;
    }

    private Stock generateMockStockRecord(String symbol) {
        int hash = Math.abs(symbol.hashCode());
        double marketCap = 5.0 + (hash % 995);
        double peRatio = 12.0 + (hash % 45);
        double roe = 5.0 + (hash % 30);
        double debtToEquity = 0.1 + ((hash % 200) / 100.0);

        return Stock.builder()
                .symbol(symbol)
                .companyName(symbol + " Corporation (Mock)")
                .sector("Technology")
                .marketCap(Math.round(marketCap * 100.0) / 100.0)
                .peRatio(Math.round(peRatio * 100.0) / 100.0)
                .roe(Math.round(roe * 100.0) / 100.0)
                .debtToEquity(Math.round(debtToEquity * 100.0) / 100.0)
                .build();
    }

    public StockHistoryResponse getStockHistory(String symbol) {
        String cleanSymbol = symbol.trim().toUpperCase();

        Optional<TwelveDataClient.TwelveDataTimeSeries> twelveDataSeries = twelveDataClient.getTimeSeries(cleanSymbol + ":NSE");
        if (twelveDataSeries.isPresent()) {
            List<HistoryDataPoint> history = twelveDataSeries.get().getValues().stream()
                    .map(v -> new HistoryDataPoint(v.getDatetime(), v.getCloseAsDouble()))
                    .filter(pt -> pt.getPrice() != null && pt.getDate() != null)
                    .collect(Collectors.toList());
            if (!history.isEmpty()) {
                java.util.Collections.reverse(history);
                return new StockHistoryResponse(cleanSymbol, history);
            }
        }

        log.info("API history fetch failed or unavailable. Generating mock random walk history for symbol: {}", cleanSymbol);
        return generateMockHistory(cleanSymbol);
    }

    private StockHistoryResponse generateMockHistory(String symbol) {
        List<HistoryDataPoint> history = new ArrayList<>();
        double currentPrice = generateMockPrice(symbol);

        Optional<Stock> localStockOpt = stockService.getStockBySymbol(symbol);
        if (localStockOpt.isPresent()) {
            currentPrice = localStockOpt.get().getCurrentPrice();
        }

        java.time.LocalDate date = java.time.LocalDate.now();
        int points = 30;
        double price = currentPrice;

        java.util.Random rand = new java.util.Random(symbol.hashCode());
        for (int i = 0; i < points; i++) {
            date = date.minusDays(1);
            while (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                date = date.minusDays(1);
            }

            double changePct = -0.03 + (rand.nextDouble() * 0.06);
            price = price / (1.0 + changePct);

            history.add(new HistoryDataPoint(date.toString(), Math.round(price * 100.0) / 100.0));
        }

        java.util.Collections.reverse(history);
        history.add(new HistoryDataPoint(java.time.LocalDate.now().toString(), currentPrice));

        return new StockHistoryResponse(symbol, history);
    }
}