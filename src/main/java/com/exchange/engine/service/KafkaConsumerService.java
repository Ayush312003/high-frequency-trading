package com.exchange.engine.service;

import com.exchange.engine.dto.OrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final OrderMatchingService orderMatchingService;

    @KafkaListener(topics = "orders.in", groupId = "trading-engine-group")
    public void listen(OrderRequest orderRequest) {
        log.info("Received order from Kafka: {}", orderRequest);

        try {
            // Trigger the matching engine
            // Note: match logic includes saving to DB.
            // Since this is running in the KafkaListener thread, it is async relative to the HTTP user.
            orderMatchingService.processOrder(orderRequest);

        } catch (Exception e) {
            log.error("Error processing order: {}", orderRequest, e);
            // In production: Send to Dead Letter Queue (DLQ)
        }
    }
}