package com.intellistock.config;

import com.intellistock.model.Stock;
import com.intellistock.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final StockRepository stockRepository;

    @Autowired
    public DataInitializer(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (stockRepository.count() == 0) {
            List<Stock> seedStocks = Arrays.asList(
                    Stock.builder()
                            .symbol("INFY")
                            .companyName("Infosys Limited")
                            .sector("Technology")
                            .currentPrice(22.50)
                            .marketCap(93.4) // $93.4 Billion
                            .peRatio(25.4)
                            .roe(29.2) // 29.2%
                            .debtToEquity(0.12)
                            .build(),
                    Stock.builder()
                            .symbol("TCS")
                            .companyName("Tata Consultancy Services")
                            .sector("Technology")
                            .currentPrice(48.20)
                            .marketCap(175.8) // $175.8 Billion
                            .peRatio(28.1)
                            .roe(38.6) // 38.6%
                            .debtToEquity(0.08)
                            .build(),
                    Stock.builder()
                            .symbol("AAPL")
                            .companyName("Apple Inc.")
                            .sector("Technology")
                            .currentPrice(185.30)
                            .marketCap(2890.0) // $2.89 Trillion
                            .peRatio(30.5)
                            .roe(160.0) // 160%
                            .debtToEquity(1.45)
                            .build(),
                    Stock.builder()
                            .symbol("MSFT")
                            .companyName("Microsoft Corporation")
                            .sector("Technology")
                            .currentPrice(420.50)
                            .marketCap(3120.0) // $3.12 Trillion
                            .peRatio(36.2)
                            .roe(38.5) // 38.5%
                            .debtToEquity(0.28)
                            .build(),
                    Stock.builder()
                            .symbol("TSLA")
                            .companyName("Tesla Inc.")
                            .sector("Automotive")
                            .currentPrice(175.40)
                            .marketCap(550.0) // $550 Billion
                            .peRatio(52.8)
                            .roe(11.2) // 11.2%
                            .debtToEquity(0.06)
                            .build()
            );

            stockRepository.saveAll(seedStocks);
            System.out.println(">>> Seeded 5 mock stocks into H2 Database: INFY, TCS, AAPL, MSFT, TSLA");
        }
    }
}
