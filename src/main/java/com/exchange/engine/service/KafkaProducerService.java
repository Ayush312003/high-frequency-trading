package com.exchange.engine.service;

import com.exchange.engine.dto.OrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, OrderRequest> kafkaTemplate;
    private static final String TOPIC = "orders.in";

    public void sendOrder(OrderRequest order) {
        log.info("Ingesting order for User: {}", order.userId());

        // Use UserId as the key to ensure sequential processing per user if multiple partitions are used
        kafkaTemplate.send(TOPIC, order.userId(), order);
    }
}