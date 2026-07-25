package com.intellistock.controller;

import com.intellistock.dto.AnalyzeRequest;
import com.intellistock.dto.AnalyzeResponse;
import com.intellistock.service.AnalysisService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class StockController {

    private final AnalysisService analysisService;

    @Autowired
    public StockController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<AnalyzeResponse> analyzeStock(@Valid @RequestBody AnalyzeRequest request) {
        AnalyzeResponse response = analysisService.analyzeStock(request.getSymbol());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stocks/{symbol}/history")
    public ResponseEntity<com.intellistock.dto.StockHistoryResponse> getStockHistory(@PathVariable String symbol) {
        com.intellistock.dto.StockHistoryResponse response = analysisService.getStockHistory(symbol);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "IntelliStock Backend");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}
