# Spring API Ecommerce

A demo e-commerce REST API built with Spring Boot, used to explore an event-driven
architecture: orders are placed and paid for over HTTP, but side effects (inventory
reservation, analytics, notifications) fan out asynchronously through Kafka and RabbitMQ.

## Tech stack

- Java 21, Spring Boot 3.2.5, Maven
- Spring Data JPA (H2 in dev, PostgreSQL in prod)
- Spring Kafka + Spring AMQP (RabbitMQ)
- Lombok, Bean Validation, springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, AssertJ

## Architecture

See [`ARCHITECTURE.md`](ARCHITECTURE.md) for the full deep-dive: the domain model
(ER diagram), both event-driven flows with sequence diagrams, the design decisions
behind them, and a couple of Lombok/JPA gotchas worth knowing before you touch the
entities. The summary below is enough to get oriented.

```
Buyer places order  ─────▶  OrderService writes an OutboxEvent
                            in the SAME transaction as the order
                                        │
                            OutboxPublisherService polls every 5s
                                        │
                                        ▼
                              Kafka topic: order.events
                                   /            \
                     inventory-service        analytics-service
                     (consumer group)          (consumer group)

Buyer pays for order ────▶  PaymentService confirms the order, then
                             publishes a Spring ApplicationEvent
                                        │
                             delivered AFTER_COMMIT only
                                        │
                                        ▼
                            RabbitMQ queue: notification.queue
                                        │
                                        ▼
                              NotificationListener
```

- **Kafka** is fed via a [transactional outbox](src/main/java/com/example/ecommerce/service/OutboxPublisherService.java):
  the order and its event are written in one DB transaction, so the event can never be
  lost even if Kafka is temporarily down. Two independent consumer groups
  (`inventory-service`, `analytics-service`) each get their own full copy of the event
  stream — that's Kafka's pub/sub fan-out.
- **RabbitMQ** is fed via an in-process `@TransactionalEventListener(phase = AFTER_COMMIT)`
  ([`NotificationPublisher`](src/main/java/com/example/ecommerce/service/NotificationPublisher.java)):
  lighter-weight than the outbox, appropriate for a best-effort side effect like an email,
  but a crash between commit and publish would lose the message (unlike the outbox).

## Prerequisites

- JDK 21
- Maven (or use the included wrapper if you add one — `mvn` is assumed on PATH)
- Docker Desktop (for Postgres/Kafka/RabbitMQ, or the full containerized stack)

## Running locally (H2, no Docker)

The `dev` profile (active by default) uses an in-memory H2 database, so you can run the
app directly:

```bash
mvn spring-boot:run
```

The Kafka/RabbitMQ producers and consumers will still try to connect (to
`localhost:9094` and `localhost:5672` by default) and log connection retries in the
background if no broker is reachable — the app itself starts fine either way. To exercise
the messaging flows locally, either start just the brokers:

```bash
docker-compose up -d kafka rabbitmq
```

or run the full containerized stack (below).

- App: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:demodb`, user `sa`, no password)

## Running the full stack (Docker Compose)

```bash
docker-compose up --build
```

This builds the app image and starts everything it depends on:

| Service    | Port(s)        | Notes                                             |
| ---------- | -------------- | -------------------------------------------------- |
| `api`      | 8080           | Spring Boot app, `prod` profile, builds from `Dockerfile` |
| `db`       | 5432           | PostgreSQL 16                                     |
| `kafka`    | 9094 (host)    | Single-node KRaft broker; containers use `kafka:9092` internally |
| `rabbitmq` | 5672, 15672    | AMQP + management UI (http://localhost:15672, guest/guest) |

Tear down with `docker-compose down` (add `-v` to also drop the Postgres volume).

## Standalone schema script

[`sql/schema.sql`](sql/schema.sql) creates all 16 tables (PostgreSQL DDL) matching the
current JPA entities exactly. The app itself doesn't run this automatically - Hibernate
still manages the schema at runtime via `ddl-auto: update`. Use this when you want to
stand up the schema by hand against a Postgres instance without booting the app first:

```bash
psql -h <host> -U <user> -d <database> -f sql/schema.sql
```

## Tests

```bash
mvn test
```

Controller tests use `@WebMvcTest`, repository tests use `@DataJpaTest`, service tests
are plain Mockito unit tests — none of them require Kafka/RabbitMQ/Postgres to be running.

## Building a jar

```bash
mvn -q -DskipTests package
```

## Project layout

```
controller/   @RestController - HTTP only, no business logic
service/      business logic, @Transactional lives here (incl. Kafka/RabbitMQ producers & consumers)
repository/   Spring Data interfaces
dto/          request/response records - entities never cross the controller boundary
model/        JPA entities
event/        Kafka/RabbitMQ event payload records
exception/    custom exceptions + @RestControllerAdvice handler
config/       beans, Kafka/RabbitMQ config, OpenAPI, CORS
```

See [`CLAUDE.md`](CLAUDE.md) for the fuller set of conventions this codebase follows.

## Configuration

Key environment variables (see [`application.yml`](src/main/resources/application.yml)
and [`docker-compose.yml`](docker-compose.yml) for full defaults):

| Variable                  | Default (dev)     | Used by                          |
| -------------------------- | ------------------ | --------------------------------- |
| `DATABASE_URL` / `_USER` / `_PASSWORD` | H2 in-memory | `prod` profile datasource        |
| `KAFKA_BOOTSTRAP_SERVERS`  | `localhost:9094`   | Spring Kafka producer/consumer   |
| `RABBITMQ_HOST` / `_PORT` / `_USERNAME` / `_PASSWORD` | `localhost` / `5672` / `guest` / `guest` | Spring AMQP |
