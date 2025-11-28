package com.exchange.engine.dto;

import com.exchange.engine.model.OrderType;
import java.math.BigDecimal;

/**
 * Immutable Data Transfer Object for incoming orders via Kafka.
 * Using Java 17 Records for concise, immutable data carriers.
 *
 * @param userId   The identifier of the user placing the order.
 * @param type     BUY or SELL.
 * @param price    The limit price for the order.
 * @param quantity The amount of the asset to trade.
 */
public record OrderRequest(
        String userId,
        OrderType type,
        BigDecimal price,
        BigDecimal quantity,
        long timestamp
) {
    // Optional: Compact Constructor for validation if needed in the future
    public OrderRequest {
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}