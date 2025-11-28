package com.exchange.engine.service;

import com.exchange.engine.dto.OrderRequest;
import com.exchange.engine.model.Order;
import com.exchange.engine.model.OrderType;
import com.exchange.engine.model.Trade;
import com.exchange.engine.repository.TradeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderMatchingService {

    private final StringRedisTemplate redisTemplate;
    private final TradeRepository tradeRepository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String BUY_ORDER_BOOK = "orderbook:buy";
    private static final String SELL_ORDER_BOOK = "orderbook:sell";
    private static final String ORDER_DATA = "orders:data";

    @Transactional
    public synchronized void processOrder(OrderRequest orderRequest) {

        long incomingTimestamp = orderRequest.timestamp() > 0
                ? orderRequest.timestamp()
                : System.currentTimeMillis();

        Order order = new Order(
                UUID.randomUUID().toString(),
                orderRequest.userId(),
                orderRequest.type(),
                orderRequest.price(),
                orderRequest.quantity(),
                incomingTimestamp
        );

        if (order.getType() == OrderType.BUY) {
            matchBuyOrder(order);
        } else {
            matchSellOrder(order);
        }
    }

    private void matchBuyOrder(Order buyOrder) {
        while (buyOrder.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            Set<String> bestSellOrders = redisTemplate.opsForZSet().range(SELL_ORDER_BOOK, 0, 0);

            if (bestSellOrders == null || bestSellOrders.isEmpty()) {
                break;
            }

            String sellOrderId = bestSellOrders.iterator().next();
            Order sellOrder = getOrderFromRedis(sellOrderId);

            if (sellOrder == null) {
                redisTemplate.opsForZSet().remove(SELL_ORDER_BOOK, sellOrderId);
                continue;
            }

            if (buyOrder.getPrice().compareTo(sellOrder.getPrice()) < 0) {
                break;
            }

            executeTrade(buyOrder, sellOrder);
        }

        if (buyOrder.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            addToOrderBook(buyOrder, BUY_ORDER_BOOK);
        }
    }

    private void matchSellOrder(Order sellOrder) {
        while (sellOrder.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            Set<String> bestBuyOrders = redisTemplate.opsForZSet().reverseRange(BUY_ORDER_BOOK, 0, 0);

            if (bestBuyOrders == null || bestBuyOrders.isEmpty()) {
                break;
            }

            String buyOrderId = bestBuyOrders.iterator().next();
            Order buyOrder = getOrderFromRedis(buyOrderId);

            if (buyOrder == null) {
                redisTemplate.opsForZSet().remove(BUY_ORDER_BOOK, buyOrderId);
                continue;
            }

            if (sellOrder.getPrice().compareTo(buyOrder.getPrice()) > 0) {
                break;
            }

            executeTrade(buyOrder, sellOrder);
        }

        if (sellOrder.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            addToOrderBook(sellOrder, SELL_ORDER_BOOK);
        }
    }

    private void executeTrade(Order buyOrder, Order sellOrder) {
        BigDecimal matchQty = buyOrder.getQuantity().min(sellOrder.getQuantity());
        BigDecimal tradePrice = sellOrder.getPrice();

        // LATENCY CALCULATION
        long now = System.currentTimeMillis();
        // The latency is strictly: "Time since the newest order arrived" (Taker Latency)
        long latency = now - Math.max(buyOrder.getTimestamp(), sellOrder.getTimestamp());

        log.info("Match! {} BTC @ ${} | Latency: {} ms", matchQty, tradePrice, latency);

        // 1. Save to MySQL (JPA + Transactional)
        try {
            Trade trade = Trade.builder()
                    .buyerId(buyOrder.getUserId())
                    .sellerId(sellOrder.getUserId())
                    .price(tradePrice)
                    .quantity(matchQty)
                    .latency(latency)
                    .build();

            tradeRepository.save(trade);
            // Flushing ensures ID is generated immediately for the WebSocket message
            tradeRepository.flush();
            log.info("Trade saved to MySQL. ID: {}", trade.getId());

            // 2. Broadcast to WebSocket (Safe Wrap)
            // We catch errors here so a socket failure doesn't rollback the valid DB trade
            try {
                messagingTemplate.convertAndSend("/topic/trades", trade);
            } catch (Exception e) {
                log.error("WebSocket broadcast failed", e);
            }

        } catch (Exception e) {
            log.error("CRITICAL: Failed to persist trade", e);
            throw e; // Triggers Rollback
        }

        // 3. Update Redis (Memory)
        try {
            buyOrder.setQuantity(buyOrder.getQuantity().subtract(matchQty));
            sellOrder.setQuantity(sellOrder.getQuantity().subtract(matchQty));

            if (sellOrder.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                removeOrderFromRedis(sellOrder.getId(), SELL_ORDER_BOOK);
            } else {
                updateOrderInRedis(sellOrder);
            }

            if (isOrderInRedis(buyOrder.getId())) {
                if (buyOrder.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                    removeOrderFromRedis(buyOrder.getId(), BUY_ORDER_BOOK);
                } else {
                    updateOrderInRedis(buyOrder);
                }
            }
        } catch (Exception e) {
            log.error("Redis update failed", e);
        }
    }

    private void addToOrderBook(Order order, String key) {
        try {
            String orderJson = objectMapper.writeValueAsString(order);
            redisTemplate.opsForHash().put(ORDER_DATA, order.getId(), orderJson);
            redisTemplate.opsForZSet().add(key, order.getId(), order.getPrice().doubleValue());
        } catch (JsonProcessingException e) {
            log.error("Error serializing order", e);
        }
    }

    private Order getOrderFromRedis(String orderId) {
        Object json = redisTemplate.opsForHash().get(ORDER_DATA, orderId);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json.toString(), Order.class);
        } catch (JsonProcessingException e) { return null; }
    }

    private void removeOrderFromRedis(String orderId, String key) {
        redisTemplate.opsForZSet().remove(key, orderId);
        redisTemplate.opsForHash().delete(ORDER_DATA, orderId);
    }

    private void updateOrderInRedis(Order order) {
        try {
            String orderJson = objectMapper.writeValueAsString(order);
            redisTemplate.opsForHash().put(ORDER_DATA, order.getId(), orderJson);
        } catch (JsonProcessingException e) {}
    }

    private boolean isOrderInRedis(String orderId) {
        return redisTemplate.opsForHash().hasKey(ORDER_DATA, orderId);
    }
}