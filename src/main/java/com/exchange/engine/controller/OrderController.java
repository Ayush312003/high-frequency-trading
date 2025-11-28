package com.exchange.engine.controller;

import com.exchange.engine.dto.OrderRequest;
import com.exchange.engine.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final KafkaProducerService kafkaProducerService;

    @PostMapping
    public ResponseEntity<String> placeOrder(@RequestBody OrderRequest orderRequest) {
        // Validate basic constraints (though Record constructor handles some)
        if (orderRequest.quantity() == null || orderRequest.price() == null) {
            return ResponseEntity.badRequest().body("Price and Quantity cannot be null");
        }

        // Publish to Kafka (Async ingestion)
        kafkaProducerService.sendOrder(orderRequest);

        return ResponseEntity.ok("Order received and queued for processing");
    }
}