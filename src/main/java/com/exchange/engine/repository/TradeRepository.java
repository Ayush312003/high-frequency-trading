package com.exchange.engine.repository;

import com.exchange.engine.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    // Custom query methods can be added here if needed for reporting
}