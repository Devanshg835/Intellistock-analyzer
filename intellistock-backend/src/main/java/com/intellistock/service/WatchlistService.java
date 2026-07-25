package com.intellistock.service;

import com.intellistock.model.Stock;
import com.intellistock.model.WatchlistItem;
import com.intellistock.repository.WatchlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final StockService stockService;

    @Autowired
    public WatchlistService(WatchlistRepository watchlistRepository, StockService stockService) {
        this.watchlistRepository = watchlistRepository;
        this.stockService = stockService;
    }

    public List<WatchlistItem> getWatchlist() {
        return watchlistRepository.findAll();
    }

    @Transactional
    public WatchlistItem addToWatchlist(String symbol) {
        String cleanSymbol = symbol.trim().toUpperCase();
        
        Optional<WatchlistItem> existing = watchlistRepository.findBySymbolIgnoreCase(cleanSymbol);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Resolve company name from local stocks db if available
        String companyName = cleanSymbol + " Corp";
        Optional<Stock> stockOpt = stockService.getStockBySymbol(cleanSymbol);
        if (stockOpt.isPresent()) {
            companyName = stockOpt.get().getCompanyName();
        }

        WatchlistItem item = WatchlistItem.builder()
                .symbol(cleanSymbol)
                .companyName(companyName)
                .addedAt(LocalDateTime.now())
                .build();

        return watchlistRepository.save(item);
    }

    @Transactional
    public void removeFromWatchlist(String symbol) {
        watchlistRepository.deleteBySymbolIgnoreCase(symbol.trim().toUpperCase());
    }

    public boolean isWatched(String symbol) {
        return watchlistRepository.existsBySymbolIgnoreCase(symbol.trim().toUpperCase());
    }
}
