# Messaging Core

Centralized Kafka messaging library for Spring Boot — retries, DLQ, metrics & trace-friendly logging.

---

## 🚀 Features
- **EventProducer / EventConsumer** abstraction — hide KafkaTemplate and @KafkaListener
- **Retry with backoff** (configurable, exponential, max attempts)
- **Dead Letter Queue (DLQ)** handling when retries are exhausted
- **Structured logging** (traceId via MDC)
- **Metrics** (Micrometer counters for retry success, scheduled, exhausted)
- **Extensible handlers** (plug in custom exhausted handlers)

---

## 📦 Getting Started

### Requirements
- Java 17+
- Spring Boot 3.x
- Kafka cluster (local or remote)

### Installation
Clone the repo:
```bash
git clone https://github.com/<your-username>/messaging-core.git
```

Build with Maven or Gradle:
```bash
./mvnw clean install
# or
./gradlew build
```

Add it to your Spring Boot service as a dependency (if published to Maven Central / GitHub Packages in future).

---

## 🛠️ Usage

### Producing an event
``` java
@Autowired
private EventProducer<OrderEvent> producer;

public void placeOrder(OrderEvent order) {
    producer.send("orders", order)
            .thenAccept(result -> log.info("Sent: {}", result));
}
```

### Consuming an event
``` java
@Component
public class OrderConsumer implements EventConsumer<OrderEvent> {

    @Override
    public void onEvent(OrderEvent event) {
        log.info("Received order: {}", event);
        // business logic
    }
}
```

---

## ⚙️ Configuration

Example `application.properties`:
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=orders-service
spring.kafka.producer.acks=all
spring.kafka.producer.retries=5
spring.kafka.producer.properties.enable.idempotence=true

# Retry config
messaging.producer.max-attempts=5
messaging.producer.initial-backoff-ms=100
messaging.producer.multiplier=2.0
messaging.producer.max-backoff-ms=5000
```
