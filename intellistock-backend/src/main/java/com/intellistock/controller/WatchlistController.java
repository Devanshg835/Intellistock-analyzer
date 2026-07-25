package com.intellistock.controller;

import com.intellistock.model.WatchlistItem;
import com.intellistock.service.WatchlistService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:5173}")
public class WatchlistController {

    private final WatchlistService watchlistService;

    @Autowired
    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public ResponseEntity<List<WatchlistItem>> getWatchlist() {
        return ResponseEntity.ok(watchlistService.getWatchlist());
    }

    @PostMapping("/add")
    public ResponseEntity<WatchlistItem> addToWatchlist(@RequestBody WatchlistAddRequest request) {
        if (request == null || request.getSymbol() == null || request.getSymbol().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        WatchlistItem item = watchlistService.addToWatchlist(request.getSymbol());
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/remove/{symbol}")
    public ResponseEntity<Void> removeFromWatchlist(@PathVariable String symbol) {
        watchlistService.removeFromWatchlist(symbol);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check/{symbol}")
    public ResponseEntity<Boolean> checkIsWatched(@PathVariable String symbol) {
        return ResponseEntity.ok(watchlistService.isWatched(symbol));
    }

    @Data
    public static class WatchlistAddRequest {
        private String symbol;
    }
}
