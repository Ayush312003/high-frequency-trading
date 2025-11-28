package com.exchange.engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents an active order in the Order Book.
 * This object is serialized to JSON and stored in Redis.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    private String id;
    private String userId;
    private OrderType type;
    private BigDecimal price;
    private BigDecimal quantity;
    private long timestamp;
}