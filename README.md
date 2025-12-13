High-Frequency Trading (HFT) Engine

A low-latency, event-driven trading engine capable of processing limit orders with sub-millisecond internal latency. The system utilizes a hybrid persistence architecture, matching orders in memory via Redis while ensuring durable audit trails in MySQL.

System Architecture

The system follows a decoupled microservices-style architecture pattern.

graph TD
    subgraph Clients
        User[Trader / Python Bot]
        Dash[Web Dashboard]
    end

    subgraph "Ingestion Layer"
        API[REST API Controller]
        Kafka{Apache Kafka}
    end

    subgraph "Core Engine"
        Consumer[Kafka Consumer]
        Matcher[Matching Engine]
    end

    subgraph "Persistence & State"
        Redis[(Redis Order Book)]
        MySQL[(MySQL Trade History)]
    end

    User -->|POST /orders| API
    API -->|Produce Message| Kafka
    Kafka -->|Consume Message| Consumer
    Consumer --> Matcher
    
    Matcher <-->|Query/Update ZSET O(log n)| Redis
    Matcher -->|Async Write| MySQL
    
    Matcher -->|Publish Match| Dash
    
    style Redis fill:#ff5555,stroke:#333,stroke-width:2px
    style Kafka fill:#222,stroke:#fff,stroke-width:2px,color:#fff
    style MySQL fill:#00758f,stroke:#333,stroke-width:2px,color:#fff


Key Features

Sub-Millisecond Matching: Leverages Redis Sorted Sets (ZSET) using Skip Lists for $O(\log N)$ time complexity matching algorithms.

Event-Driven Ingestion: Decouples API ingestion from processing using Apache Kafka, allowing the system to absorb traffic spikes without locking.

Real-Time Visualization: Full-duplex WebSocket feed broadcasting executed trades to a live frontend dashboard with <15ms tick-to-trade latency.

Hybrid Persistence: "Hot" data (Order Book) lives in Redis for speed; "Cold" data (Trade History) lives in MySQL for transactional durability.

Dockerized Infrastructure: Complete environment (Zookeeper, Kafka, Redis, MySQL) orchestrated via Docker Compose.

Tech Stack

Component

Technology

Purpose

Language

Java 17

Core application logic

Framework

Spring Boot 3

Dependency injection, Web, WebSocket support

Messaging

Apache Kafka

Asynchronous order ingestion and buffering

Engine

Redis

In-memory Limit Order Book (Price-Time Priority)

Database

MySQL

Persistent storage for executed trades

Frontend

HTML5 / JS / STOMP

Real-time dashboard using SockJS

⚙️ Setup & Installation

Prerequisites

Docker Desktop & Docker Compose

Java JDK 17+

Python 3 (for traffic simulation)

1. Start Infrastructure

Spin up the containers (Kafka, Zookeeper, Redis, MySQL).

docker-compose up -d


2. Start the Trading Engine

Run the Spring Boot application.

./mvnw spring-boot:run


The engine will start on port 8080 and connect to the Docker containers.

3. Access the Dashboard

Open your browser to the local dashboard to view the real-time feed.
 http://localhost:8080

🚦 Usage & Testing

Simulate High-Frequency Traffic

Use the included Python script to flood the engine with random BUY/SELL orders.

Install dependencies:

pip3 install requests


Run the Market Maker:

python3 src/main/resources/static/market_maker.py


You will see the dashboard light up with live trades, and the "Latency" column will calculate the end-to-end processing time (Tick-to-Trade).

Manual API Usage

You can also place manual orders via cURL:

curl -X POST http://localhost:8080/api/orders \
-H "Content-Type: application/json" \
-d '{"userId": "Whale_1", "type": "BUY", "price": 50000, "quantity": 5.0}'


Future Roadmap

Symbol Sharding: Implement Kafka partitioning strategy to scale horizontally (e.g., Partition 0 for BTC, Partition 1 for ETH).

Balance Service: Integrate a separate centralized balance reservation service with pessimistic locking to prevent double-spending.

Order Types: Support Market Orders, Stop-Loss, and Time-in-Force policies (IOC/FOK).
