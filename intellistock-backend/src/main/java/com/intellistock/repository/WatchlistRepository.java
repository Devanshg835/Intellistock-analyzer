package com.intellistock.repository;

import com.intellistock.model.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {
    Optional<WatchlistItem> findBySymbolIgnoreCase(String symbol);
    void deleteBySymbolIgnoreCase(String symbol);
    boolean existsBySymbolIgnoreCase(String symbol);
}
