# Smart Parking Management System (SPMS)

## Project Purpose

The Smart Parking Management System (SPMS) is a **backend-only, microservice-based** application that lets drivers discover available parking spaces in real time, reserve them, and pay for parking digitally. It exposes its functionality exclusively through **REST APIs**; there is no frontend/UI.

The system is built with **Java**, **Spring Boot**, and **Spring Cloud** (Maven build), following **clean architecture** and production-quality coding practices. It is intended as a CSE coursework project demonstrating a realistic cloud-native microservice architecture.

## Microservice Architecture Overview

SPMS is decomposed into seven Spring Boot services, each independently deployable and communicating over HTTP:

| Module | Port (planned) | Responsibility |
| --- | --- | --- |
| `eureka-server` | 8761 | Netflix Eureka **service registry** — all services register here for service discovery |
| `config-server` | 8888 | Spring Cloud Config **centralized configuration** for all services |
| `api-gateway` | 8080 | Spring Cloud Gateway — single REST **entry point**, routes requests to the correct service |
| `user-service` | 8081 | User registration, authentication, and user profile management |
| `vehicle-service` | 8082 | Vehicle registration and management (per user) |
| `parking-service` | 8083 | Parking space/spot availability, reservations, and parking sessions |
| `payment-service` | 8084 | Payment processing and billing for parking sessions |

Typical request flow: `Client -> api-gateway (8080) -> [user|vehicle|parking|payment]-service -> database`. Services locate each other via the **Eureka registry**, and configuration is centralized via the **Config Server** (all services pull their config from `config-server` at startup).

## Technology Stack

- **Java 17** (JDK 26 installed on the dev machine, source/target pinned to 17)
- **Spring Boot 4.1.0**
- **Spring Cloud 2025.1.2** (Oakwood — compatible with Spring Boot 4.1.0)
  - Netflix Eureka Server, Config Server, Gateway, OpenFeign, LoadBalancer
- **Maven 3.9.16** (via the project Maven wrapper `mvnw`/`mvnw.cmd`)
- **REST APIs** (no frontend/UI)

## Project Structure

```
smart-parking-management-system/
├── eureka-server/        # Service registry (implemented — Phase 1)
├── config-server/        # Centralized config (implemented — Phase 2)
├── api-gateway/          # API gateway (implemented — Phase 3)
├── user-service/         # (placeholder — later phase)
├── vehicle-service/      # (placeholder — later phase)
├── parking-service/      # (placeholder — later phase)
├── payment-service/      # (placeholder — later phase)
├── docs/                 # Design/architecture documentation
├── postman_collection.json  # Postman collection (grows with each phase)
├── pom.xml               # Parent aggregator (packaging=pom)
└── README.md
```

The root `pom.xml` is a Maven **parent aggregator**: it manages dependency versions (Spring Cloud BOM) and lists every module in its `<modules>` section. Each microservice lives in its own Maven module.

## Current Implementation Status

| Component | Status |
| --- | --- |
| Eureka Server (`eureka-server`) | **Implemented (Phase 1)** |
| Config Server (`config-server`) | **Implemented (Phase 2)** |
| API Gateway (`api-gateway`) | **Implemented (Phase 3)** |
| User Service (`user-service`) | Placeholder module only |
| Vehicle Service (`vehicle-service`) | Placeholder module only |
| Parking Service (`parking-service`) | Placeholder module only |
| Payment Service (`payment-service`) | Placeholder module only |

Placeholder modules contain only a `pom.xml` (they build as empty JARs) and will be implemented in subsequent phases.

## Building the Project

All services build together from the repository root:

```bash
# Windows
mvnw.cmd clean package

# macOS / Linux
./mvnw clean package
```

## Running the Eureka Server

From the repository root:

```bash
# Windows
mvnw.cmd -pl eureka-server spring-boot:run

# macOS / Linux
./mvnw -pl eureka-server spring-boot:run
```

Or run the built artifact directly:

```bash
java -jar eureka-server/target/eureka-server-1.0.0-SNAPSHOT.jar
```

The Eureka Server is configured as a **standalone** registry: it does not register itself and does not fetch a registry from any peer (`eureka.client.register-with-eureka=false`, `eureka.client.fetch-registry=false`).

## Eureka Dashboard

Once the Eureka Server is running, open the dashboard in a browser:

```
http://localhost:8761
```

The dashboard shows the Eureka application registry. As services come online (including the Config Server), they will appear here as application instances.

## Config Server

### Purpose

The **Config Server** centralizes all configuration for the SPMS microservices. Instead of hardcoding settings in each service, every microservice pulls its configuration from the Config Server at startup. This keeps ports, application names, the Eureka server URL, database details, and common Spring settings in one place.

### Port

The Config Server listens on **port 8888** (`http://localhost:8888`).

### Configuration Repository

The Config Server runs with the **`native` profile**, so it serves configuration from local files on the classpath — no GitHub repository is required, which keeps the coursework fully runnable offline. The configuration files live in:

```
config-server/src/main/resources/config/
├── application.yml        # Shared config for all services (Eureka URL, common Spring settings)
├── api-gateway.yml        # Port 8080
├── user-service.yml       # Port 8081, PostgreSQL placeholders
├── vehicle-service.yml    # Port 8082, PostgreSQL placeholders
├── parking-service.yml    # Port 8083, PostgreSQL placeholders
└── payment-service.yml    # Port 8084, PostgreSQL placeholders
```

Sensitive values are **not hardcoded** — they are resolved from environment variables with local-development defaults:

| Environment variable | Default | Used for |
| --- | --- | --- |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka` | Eureka registry URL |
| `DB_HOST` | `localhost` | Database host |
| `DB_PORT` | `5432` | Database port |
| `DB_NAME` | service-specific (e.g. `spms_user`) | Database name |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password (dev-only placeholder) |
| `JPA_DDL_AUTO` | `update` | Hibernate DDL mode |

### How to Run

```bash
# Windows
mvnw.cmd -pl config-server spring-boot:run

# macOS / Linux
./mvnw -pl config-server spring-boot:run
```

Or run the built artifact directly:

```bash
java -jar config-server/target/config-server-1.0.0-SNAPSHOT.jar
```

The Config Server registers itself with the Eureka registry (pointing at `EUREKA_SERVER_URL`) so that other services and the gateway can locate it by name. For the registration to succeed, start the **Eureka Server first**.

### Example URLs to Retrieve Configuration

The Config Server serves configuration at `/{application}/{profile}` (a label is optional: `/{application}/{profile}/{label}`):

| URL | Returns |
| --- | --- |
| `http://localhost:8888/user-service/default` | `user-service` config as JSON |
| `http://localhost:8888/vehicle-service/default` | `vehicle-service` config as JSON |
| `http://localhost:8888/parking-service/default` | `parking-service` config as JSON |
| `http://localhost:8888/payment-service/default` | `payment-service` config as JSON |
| `http://localhost:8888/api-gateway/default` | `api-gateway` config as JSON |
| `http://localhost:8888/application/default` | Shared config for all services |
| `http://localhost:8888/user-service-default.yml` | `user-service` config as YAML |

The response includes a `propertySources` array; the merged effective configuration for a client is the union of the shared `application` sources plus its own `{application}` source.

### How Clients Consume Configuration

A microservice consumes configuration from the Config Server by adding the `spring-cloud-starter-config` dependency and pointing at the server:

```yaml
# in the client's bootstrap/local config
spring:
  application:
    name: user-service
  config:
    import: configserver:http://localhost:8888
```

At startup the client fetches `application.{profile}` (shared) and `user-service.{profile}` (its own), then merges them with its local config. `EUREKA_SERVER_URL`, `DB_*`, and `JPA_DDL_AUTO` placeholders are resolved by the **Config Server process** from its environment before the values are served to clients, so no secrets travel in plain text in the repository.

## API Gateway

### Purpose

The **API Gateway** is the single entry point for all REST clients. It receives every external request, matches it against the configured routes, and forwards it to the correct backend microservice **by its Eureka service ID** — no backend hostnames or ports are hardcoded anywhere in the gateway.

### Port

The Gateway listens on **port 8080** (`http://localhost:8080`).

### Gateway Architecture

- Built on **Spring Cloud Gateway (WebFlux/Netty)**, reactive and non-blocking.
- Registers itself with the **Eureka registry** as `API-GATEWAY` so it is visible/discoverable in the service mesh.
- Fetches the service registry (`fetch-registry: true`) so it knows the live instances of each backend service.
- Resolves routes with the **`lb://`** (load-balanced) URI scheme — Spring Cloud LoadBalancer picks an available instance from Eureka for each request.
- Pulls **all of its own configuration** (port, Eureka, routes) from the **Config Server**; its local config only contains the bootstrap data needed to find the Config Server (`spring.config.import: configserver:...`).
- Uses explicit routes (the automatic discovery locator is disabled) so only the intended paths are exposed.
- Has a **global error handler** that returns a consistent JSON error body (`timestamp`, `status`, `error`, `path`, `message`) for any unhandled failure (e.g. 404 when no route matches, 503 when a service has no registered instances).

### Route Table

Each route strips the leading `/api` prefix (`StripPrefix=1`) before forwarding, so backend services expose domain paths such as `/users`, `/vehicles`, `/parking`, `/payments`.

| Gateway path | Target (Eureka service ID) | Forwarded path (after strip) |
| --- | --- | --- |
| `/api/users/**` | `lb://USER-SERVICE` | `/users/**` |
| `/api/vehicles/**` | `lb://VEHICLE-SERVICE` | `/vehicles/**` |
| `/api/parking/**` | `lb://PARKING-SERVICE` | `/parking/**` |
| `/api/payments/**` | `lb://PAYMENT-SERVICE` | `/payments/**` |

For example, `POST /api/users` matches the `user-service-route` and is forwarded to `USER-SERVICE` (as `POST /users`) — it will be served once User Service is implemented in a later phase.

### How to Run

Start the **Eureka Server** and **Config Server** first, then:

```bash
# Windows
mvnw.cmd -pl api-gateway spring-boot:run

# macOS / Linux
./mvnw -pl api-gateway spring-boot:run
```

Or run the built artifact directly:

```bash
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar
```

### Example Requests

With Eureka + Config Server + Gateway running (backend services not yet implemented), requests are routed correctly but the backend responds with a `503` JSON error because no instance is registered yet:

```
POST http://localhost:8080/api/users
GET  http://localhost:8080/api/users/{id}
GET  http://localhost:8080/api/vehicles/{id}
GET  http://localhost:8080/api/parking/spaces
POST http://localhost:8080/api/payments
```

An unmatched path (e.g. `GET /api/unknown`) returns `404` with the same JSON error shape.

## Current Architecture (Phase 3)

```
                        ┌─────────────────┐
                        │  eureka-server  │  Service registry (8761)
                        │   (Phase 1)     │
                        └────────┬────────┘
                          register / discover │
   ┌───────────────┐           │     ┌───────────────────┐
   │ config-server │───────────┘     │    api-gateway    │  (8080)
   │   (Phase 2)   │                 │     (Phase 3)     │
   │    (8888)     │                 └─────────┬─────────┘
   └───────┬───────┘                           │ routes via lb://
           │ serve config                      │ + Eureka service IDs
           ▼                                   ▼
   user-service / vehicle-service / parking-service / payment-service
                    (planned, later phases)
```
