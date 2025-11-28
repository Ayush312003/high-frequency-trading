package com.exchange.engine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a matched trade executed by the engine.
 * This entity serves as the persistent audit log in MySQL.
 */
@Entity
@Table(name = "trades", indexes = {
        @Index(name = "idx_trade_timestamp", columnList = "executionTimestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String buyerId;

    @Column(nullable = false)
    private String sellerId;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false)
    private LocalDateTime executionTimestamp;

    @Transient
    private long latency;

    /**
     * Pre-persist lifecycle hook to ensure timestamp is set before saving.
     */
    @PrePersist
    protected void onCreate() {
        if (this.executionTimestamp == null) {
            this.executionTimestamp = LocalDateTime.now();
        }
    }
}