# Enterprise E-Commerce Microservices Platform

Production-ready microservice architecture built with **Spring Boot 3**, **Spring Cloud**, **Netflix Eureka**, **Spring Cloud API Gateway**, and an enterprise observability stack featuring **ELK (Elasticsearch, Logstash, Kibana)**, **Prometheus**, **Grafana**, and **Zipkin**.

---

## 🏛 Architecture Overview

```
                                      [ Clients / Web / Mobile ]
                                                  │
                                                  ▼
                                     ┌─────────────────────────┐
                                     │  Spring Cloud Gateway   │  (Port: 1020)
                                     │  (RateLimit/Circuit/MDC)│
                                     └────────────┬────────────┘
                                                  │
                 ┌────────────────────────────────┼──────────────────────────────┐
                 ▼                                ▼                              ▼
    ┌─────────────────────────┐      ┌─────────────────────────┐    ┌─────────────────────────┐
    │    Customer Service     │      │     Product Service     │    │      Order Service      │
    │      (Port: 8081)       │      │      (Port: 8082)       │    │   (Orchestrator: 8083)  │
    └────────────┬────────────┘      └────────────┬────────────┘    └────────────┬────────────┘
                 │                                │                              │ (Feign + Fallback)
                 │                                │                              ▼
                 │                                │                 ┌─────────────────────────┐
                 │                                └────────────────►│     Payment Service     │
                 │                                                  │      (Port: 8084)       │
                 └──────────────────────────────────────────────────┴────────────┬────────────┘
                                                                                 │
     ════════════════════════════════════════════════════════════════════════════╪════════════════
     SERVICE DISCOVERY, METRICS & CENTRALIZED LOGGING INFRASTRUCTURE:            │
                                                                                 ▼
     ┌──────────────────┐    ┌──────────────────┐    ┌────────────────────────────────────────┐
     │  Eureka Registry │    │    Prometheus    │    │                 ELK Stack              │
     │   (Port: 8761)   │    │   (Port: 9090)   │    │ ┌────────────┐ ┌──────────────┐ ┌────┐ │
     └──────────────────┘    └────────┬─────────┘    │ │  Logstash  │►│Elasticsearch │►│Kib.│ │
                                      ▼              │ │(Port: 5000)│ │ (Port: 9200) │ │5601│ │
                             ┌──────────────────┐    │ └────────────┘ └──────────────┘ └────┘ │
                             │     Grafana      │    └────────────────────────────────────────┘
                             │   (Port: 3000)   │                    ┌────────────────────────┐
                             └──────────────────┘                    │  Zipkin (Port: 9411)   │
                                                                     └────────────────────────┘
```

---

## 📦 Services & Ports

| Service | Port | Description | Health / Metrics |
| :--- | :---: | :--- | :--- |
| **Eureka Server** | `8761` | Service Registry & Discovery | `/actuator/prometheus` |
| **API Gateway** | `8080` | Reactive Entrypoint, Routing, CORS, Trace Injection | `/actuator/gateway/routes` |
| **Customer Service** | `8081` | Profiles, Account Status, Wallet Balances | `/actuator/prometheus` |
| **Product Service** | `8082` | Product Catalog, Stock Reservation, Inventory | `/actuator/prometheus` |
| **Order Service** | `8083` | Order Orchestration, Saga Compensation | `/actuator/prometheus` |
| **Payment Service** | `8084` | Payment Processing, Wallet Debit, Refunds | `/actuator/prometheus` |
| **Elasticsearch** | `9200` | Search & Analytics Log Storage | `/_cluster/health` |
| **Logstash** | `5000` | TCP JSON Log ingestion pipeline | `localhost:5000` |
| **Kibana** | `5601` | Log Visualization Dashboard | `http://localhost:5601` |
| **Prometheus** | `9090` | Time-Series Metrics Scraper | `http://localhost:9090` |
| **Grafana** | `3000` | Metrics Dashboards (User: `admin` / `admin`) | `http://localhost:3000` |
| **Zipkin** | `9411` | Distributed Tracing UI | `http://localhost:9411` |
| **PostgreSQL** | `5432` | Production Relational Database | `localhost:5432` |

---

## 🔍 Observability & Centralized Logging Highlights

### 1. ELK Stack Architecture
- **Logback Configuration**: Each microservice uses `logstash-logback-encoder` to stream structured JSON logs over non-blocking TCP sockets directly to Logstash on `logstash:5000`.
- **MDC Correlation IDs**: Every request received by the API Gateway is stamped with a unique `X-Correlation-ID` header. If absent, one is generated via `UUID.randomUUID()`.
- **Feign Propagation**: The `FeignCorrelationInterceptor` ensures the `X-Correlation-ID` is passed downstream across all HTTP hops.
- **Logstash Pipeline**: `monitoring/elk/logstash/pipeline/logstash.conf` ingests JSON lines, normalizes timestamps, and outputs to daily Elasticsearch indices `microservices-logs-%{+YYYY.MM.dd}`.
- **Kibana**: Query logs by `correlationId`, `traceId`, `spanId`, `service`, `level`, or HTTP URI.

### 2. Prometheus & Grafana Monitoring
- **Micrometer Registry**: All services export Prometheus metrics under `/actuator/prometheus`.
- **Prometheus Scraping**: `monitoring/prometheus/prometheus.yml` scrapes Actuator endpoints every 5 seconds.
- **Grafana Provisioning**: Automatically provisions the Prometheus datasource and a pre-built dashboard:
  - Service Health (UP/DOWN)
  - HTTP Server Request Throughput (req/sec)
  - HTTP 95th Percentile Response Latencies
  - JVM Heap Memory and CPU Utilization
  - Custom Domain Metrics:
    - `orders.created.count`
    - `orders.failed.count`
    - `payments.success.count`
    - `customers.created.count`
    - `products.created.count`

### 3. Distributed Tracing
- Integrated **Micrometer Tracing** with **Brave** automatically populates `traceId` and `spanId` in every log message:
  ```text
  2026-11-02 21:18:27.123 INFO  [order-service] [672658b1c4e976f2] [3ab81c01e5d9a912] [e283ab01-7fa8-4f51-b0ef-6df093e921d2] ...
  ```
- Send traces to Zipkin (`http://localhost:9411`) for deep timeline waterfall inspection.

---

## 🚀 Getting Started

### Prerequisites
- **JDK 25** or higher
- **Maven 3.8+**
- **Docker & Docker Compose**

### Quick Launch with Docker Compose

1. **Package the applications**:
   ```bash
   mvn clean package -DskipTests
   ```

2. **Start the complete stack**:
   ```bash
   docker compose up --build -d
   ```

   *Or run the automated helper:*
   ```powershell
   .\build-and-run.ps1
   ```

3. **Verify running containers**:
   ```bash
   docker compose ps
   ```

---

## 🧪 End-to-End API Flow Testing

Run the automated PowerShell verification script:
```powershell
.\test-api-flow.ps1
```

Or execute via `cURL`:

### 1. Create Customer
```bash
curl -X POST http://localhost:8080/api/v1/customers \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: test-trace-001" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+1-555-0100",
    "address": "456 Microservice Avenue",
    "initialBalance": 2000.00
  }'
```

### 2. Create Product
```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: test-trace-001" \
  -d '{
    "sku": "IPHONE-16-PRO",
    "name": "Apple iPhone 16 Pro 256GB",
    "description": "Titanium design, A18 Pro chip",
    "price": 999.00,
    "stockQuantity": 25,
    "category": "Smartphones"
  }'
```

### 3. Place Order (Orchestrated Transaction)
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: test-trace-001" \
  -d '{
    "customerId": 1,
    "items": [
      {
        "productId": 1,
        "quantity": 1
      }
    ],
    "paymentMethod": "WALLET"
  }'
```

### 4. Query Order Status
```bash
curl http://localhost:8080/api/v1/orders/1 \
  -H "X-Correlation-ID: test-trace-001"
```

---

## 📊 Dashboards & Interfaces

- **Eureka Registry**: [http://localhost:8761](http://localhost:8761)
- **API Gateway**: [http://localhost:1020](http://localhost:1020)
- **Customer Swagger**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **Product Swagger**: [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)
- **Order Swagger**: [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)
- **Payment Swagger**: [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html)
- **Grafana**: [http://localhost:3000](http://localhost:3000) (Credentials: `admin` / `admin`)
- **Prometheus**: [http://localhost:9090](http://localhost:9090)
- **Kibana (Logs)**: [http://localhost:5601](http://localhost:5601)
- **Zipkin (Tracing)**: [http://localhost:9411](http://localhost:9411)

---

## 🛡️ Production Standards Implemented
- **Layered Clean Architecture**: Strict separation of Controller, DTO, Service, Entity, Repository, and Exception handling layers.
- **Resilience**: Resilience4j Circuit Breaker with sliding windows and graceful fallback handlers on the API Gateway and OpenFeign clients.
- **Optimistic Locking**: JPA `@Version` on entities to prevent race conditions during concurrent stock reservations and wallet deductions.
- **Saga Compensation**: Order Service rolls back reserved product stock if downstream payment fails.
- **Zero-Config Local Profile**: Default H2 in-memory databases allow instant local execution without external database containers; Docker profile runs PostgreSQL.
- **Container Health Probes**: Dockerfiles and Compose configurations incorporate Docker `HEALTHCHECK` and Spring Boot Actuator probes (`/actuator/health`).
