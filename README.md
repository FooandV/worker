# 📌 Order Worker Service

> 📢 This project is now fully documented in English.  
> For the original version in Spanish, switch to the [`main`](https://github.com/FooandV/worker/tree/main) branch.

Reactive microservice built with Spring WebFlux, Kafka, Redis, and MongoDB for real-time order processing and enrichment using external Go APIs.

## 📑 Table of Contents
- [Project Overview](#-project-overview)
- [Key Features](#-key-features)
- [Tech Stack](#-tech-stack)
- [Setup](#-setup)
- [Run the Project](#-run-the-project)
- [Sample Payload](#-sample-payload)
- [Testing](#-testing)
- [Scalability and Optimization](#-scalability-and-optimization)
  
## 🚀 Project Overview

This project processes orders received through Kafka by enriching them with customer and product data via Go APIs. Orders are validated and stored in MongoDB. It also includes resilience mechanisms like retries and circuit breakers using Resilience4j.

## ✅ Key Features

- Reactive Kafka consumer with Spring WebFlux
- External data enrichment using Go APIs
- Redis for caching and distributed locks
- MongoDB for order persistence
- Retry and circuit breaker mechanisms with Resilience4j

## 🛠️ Tech Stack

- **Java 21**, **Spring Boot 3.x**, **Spring WebFlux**
- **Apache Kafka** – event streaming
- **Redis** – caching + locks
- **MongoDB** – order storage
- **Go** – mock APIs for customer and product enrichment
- **Resilience4j**, **Reactor Retry**
- **JUnit 5**, **Mockito**
- **Docker** (planned)

## ⚙️ Setup

### Requirements

- Java 17+
- Maven
- Docker (Mongo, Redis, Kafka)
- Go

### Configuration

Update `application.properties`:
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.data.mongodb.uri=mongodb://localhost:27017/pedidosDB
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

## ▶️ Run the Project

1. Start Kafka, Redis, MongoDB, and Go APIs
2. Build and run the app:
```bash
mvn spring-boot:run
```
3. Start Go APIs (from `/go-api`):
```bash
go run main.go
```

APIs:
- `GET /product`
- `GET /customer`

## 📃 Sample Payload

```json
{
  "orderId": "order-100",
  "customerId": "Freyder-111",
  "products": [
    {
      "productId": "product-100",
      "name": "Iphone",
      "price": 2000
    }
  ]
}
```

## 🧪 Testing

Run tests with:
```bash
mvn test
```

Covered scenarios:
- Successful order processing
- Inactive client
- Product not found
- Redis lock handling

## 📈 Scalability and Optimization

- **MongoDB indexes**: created on `orderId` and `customerId`
- **Redis caching**: reduces external API calls
- **Retry & Circuit Breaker**: for API resilience

