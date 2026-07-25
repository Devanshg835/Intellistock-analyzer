package com.intellistock.service;

import com.intellistock.model.Stock;
import com.intellistock.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class StockService {

    private final StockRepository stockRepository;

    @Autowired
    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public Optional<Stock> getStockBySymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return Optional.empty();
        }
        return stockRepository.findBySymbolIgnoreCase(symbol.trim());
    }

    public Stock saveStock(Stock stock) {
        return stockRepository.save(stock);
    }
}
