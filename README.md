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
├── user-service/         # User management (implemented — Phase 4)
├── vehicle-service/      # Vehicle management (implemented — Phase 5)
├── parking-service/      # Parking spaces & reservations (implemented — Phase 6)
├── payment-service/      # Payments & receipts (implemented — Phase 7)
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
| User Service (`user-service`) | **Implemented (Phase 4)** |
| Vehicle Service (`vehicle-service`) | **Implemented (Phase 5)** |
| Parking Service (`parking-service`) | **Implemented (Phase 6)** |
| Payment Service (`payment-service`) | **Implemented (Phase 7)** |

All services are now implemented; no placeholder modules remain.

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

## Vehicle Service

### Purpose

The **Vehicle Service** manages vehicles owned by registered users: registration, updates, retrieval, listing per user, deletion, and **simulated parking entry/exit** tracking. It is exposed to clients through the API Gateway at `http://localhost:8080/api/vehicles/**`.

### Port

The Vehicle Service listens on **port 8082** (`http://localhost:8082`) when reached directly; clients should use the Gateway.

### Configuration

All configuration comes from the **Config Server** (`vehicle-service.yml`, served at `http://localhost:8888/vehicle-service/default`):

| Property | Source |
| --- | --- |
| Port `8082`, datasource URL, `DB_USERNAME`, `DB_PASSWORD` | `config-server/.../config/vehicle-service.yml` (env-var driven with defaults) |
| Eureka registration + `spring.jpa.hibernate.ddl-auto` | `config-server/.../config/application.yml` (shared) |

The local `application.yml` only points at the Config Server:
`spring.config.import: optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}`.

### How to Run

Start **Eureka Server**, **Config Server**, then:

```bash
# Windows
mvnw.cmd -pl vehicle-service spring-boot:run

# macOS / Linux
./mvnw -pl vehicle-service spring-boot:run
```

### REST API

All endpoints are reachable through the Gateway (`http://localhost:8080/api/vehicles/...`) and directly (`http://localhost:8082/vehicles/...`).

| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/vehicles` | Register a vehicle | `201` |
| `GET` | `/api/vehicles/{id}` | Retrieve a vehicle | `200` |
| `GET` | `/api/vehicles/user/{userId}` | List vehicles of a user | `200` |
| `PUT` | `/api/vehicles/{id}` | Update vehicle fields | `200` |
| `DELETE` | `/api/vehicles/{id}` | Delete a vehicle | `204` |
| `POST` | `/api/vehicles/{id}/entry` | Simulate vehicle entry | `200` |
| `POST` | `/api/vehicles/{id}/exit` | Simulate vehicle exit | `200` |

`vehicleType` is one of `CAR`, `MOTORCYCLE`, `TRUCK`, `BUS`, `VAN`. Status is `OUTSIDE` (default) or `INSIDE`. Vehicle numbers are normalized to uppercase and must be unique.

Error responses use the shared JSON shape (`timestamp`, `status`, `error`, `path`, `message`): `400` invalid body, `404` vehicle not found, `409` duplicate vehicle number or illegal entry/exit.

### Entry/Exit Flow

- **Entry** (`POST /api/vehicles/{id}/entry`): the vehicle must exist (else `404`) and must not already be `INSIDE` (else `409`). The status is set to `INSIDE` and `entryTime` is stored.
- **Exit** (`POST /api/vehicles/{id}/exit`): the vehicle must exist (else `404`) and must currently be `INSIDE` (else `409`). The status is set to `OUTSIDE` and `exitTime` is stored.

`entryTime`/`exitTime` are exposed in the vehicle response alongside the current `status`.

## Parking Service

### Purpose

The **Parking Service** lets parking owners manage parking spaces and lets drivers search, filter, and reserve them. It also simulates IoT/manual status updates on spaces. It is exposed through the API Gateway at `http://localhost:8080/api/parking/**`.

### Port

The Parking Service listens on **port 8083** (`http://localhost:8083/parking/...`) when reached directly; clients should use the Gateway.

### Configuration

All configuration comes from the **Config Server** (`parking-service.yml`, served at `http://localhost:8888/parking-service/default`): port `8083`, PostgreSQL datasource with env-var credentials (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`), Eureka registration, and shared JPA settings.

The local `application.yml` only points at the Config Server:
`spring.config.import: optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}`.

### How to Run

Start **Eureka Server**, **Config Server**, then:

```bash
# Windows
mvnw.cmd -pl parking-service spring-boot:run

# macOS / Linux
./mvnw -pl parking-service spring-boot:run
```

### Parking Spaces

`ParkingSpace` fields: `id`, `ownerId`, `spaceNumber`, `location`, `city`, `zone`, `pricePerHour`, `status`, `createdAt`, `updatedAt`. New spaces start as `AVAILABLE`.

Status values: `AVAILABLE`, `RESERVED`, `OCCUPIED`, `MAINTENANCE`.

| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/parking/spaces` | Register a parking space | `201` |
| `GET` | `/api/parking/spaces` | Search spaces with optional filters | `200` |
| `GET` | `/api/parking/spaces/{id}` | Retrieve a space | `200` |
| `PUT` | `/api/parking/spaces/{id}` | Update space fields | `200` |
| `DELETE` | `/api/parking/spaces/{id}` | Delete a space | `204` |
| `PUT` | `/api/parking/spaces/{id}/status` | Manual/IoT status update | `200` |

**Search/filter** (all optional, combinable):

```
GET /api/parking/spaces?city=Colombo
GET /api/parking/spaces?zone=Zone-A
GET /api/parking/spaces?available=true
GET /api/parking/spaces?city=Colombo&available=true
```

- `city`, `zone`: case-insensitive exact match.
- `available=true` → only `AVAILABLE` spaces; `available=false` → only non-available spaces.

**Manual status update** (simulated IoT):

```json
PUT /api/parking/spaces/{id}/status
{ "status": "OCCUPIED" }
```

### Reservations

`Reservation` fields: `id`, `userId`, `vehicleId`, `parkingSpaceId`, `startTime`, `endTime`, `status`, `createdAt`.

Status values: `PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`. New reservations are created as `CONFIRMED`.

| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/parking/reservations` | Reserve an available space | `201` |
| `GET` | `/api/parking/reservations/{id}` | Retrieve a reservation | `200` |
| `GET` | `/api/parking/reservations/user/{userId}` | List a user's reservations | `200` |
| `POST` | `/api/parking/reservations/{id}/cancel` | Cancel a reservation | `200` |
| `POST` | `/api/parking/reservations/{id}/release` | Complete/release a reservation | `200` |

**Reservation flow** (`POST /api/parking/reservations`):

1. Parking space must exist — else `404`.
2. Parking space must be `AVAILABLE` — else `409` (prevents double booking).
3. `userId` and `vehicleId` must be provided — else `400`.
4. `startTime` must be before `endTime` — else `400`.
5. Reservation is created as `CONFIRMED` and the space changes `AVAILABLE → RESERVED`.
6. **Concurrency safety**: the space row is read with a **pessimistic write lock** (`SELECT ... FOR UPDATE`) inside the reservation transaction, so two simultaneous reservation attempts on the same space serialize — exactly one succeeds, the other gets `409`.

**Cancel** → reservation `CANCELLED`, space back to `AVAILABLE`. **Release** → reservation `COMPLETED`, space back to `AVAILABLE`. Repeating cancel/release in the wrong state returns `409`.

Errors use the shared JSON shape (`timestamp`, `status`, `error`, `path`, `message`).

## Payment Service

### Purpose

The **Payment Service** handles billing for parking reservations. It processes payments against a **mock payment gateway** (no real Stripe/PayPal/Visa), validates card data, prevents duplicate payments for the same reservation, stores every payment transaction, generates digital receipts, and exposes payment status retrieval. It is exposed through the API Gateway at `http://localhost:8080/api/payments/**`.

### Port

The Payment Service listens on **port 8084** (`http://localhost:8084/payments/...`) when reached directly; clients should use the Gateway.

### Configuration

All configuration comes from the **Config Server** (`payment-service.yml`, served at `http://localhost:8888/payment-service/default`): port `8084`, PostgreSQL datasource with env-var credentials (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`), Eureka registration, and shared JPA settings.

The local `application.yml` only points at the Config Server:
`spring.config.import: optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}`.

### How to Run

Start **Eureka Server**, **Config Server**, then:

```bash
# Windows
mvnw.cmd -pl payment-service spring-boot:run

# macOS / Linux
./mvnw -pl payment-service spring-boot:run
```

### Mock Payment Gateway

Payments are processed by an in-process **mock gateway** (`MockPaymentGateway`) — no external provider is contacted. The outcome is **deterministic** so the API is fully testable:

- `CASH` and `MOCK_WALLET` payments **always succeed**.
- `CARD` payments succeed unless the card number ends with `0002` (a documented mock "declined" card, e.g. `4000000000000002`), in which case the transaction is stored with status `FAILED`.
- Each transaction receives a generated `transactionId` (e.g. `TXN-4B4F9C2E...`).

### Card Validation

Before processing, card numbers are validated (**invalid card data → `400`**):

- Required when `paymentMethod` is `CARD` — missing/blank → `400`.
- 13–19 digits (spaces/hyphens allowed) and must pass the **Luhn checksum** — else `400`.
- The **full card number is never stored**; only a masked form (`************1111`) is returned in the create response.

### Payment Entity

`Payment` fields: `id`, `reservationId`, `userId`, `amount`, `paymentMethod`, `transactionId`, `status`, `paymentDate`, `createdAt`.

- `paymentMethod`: `CARD`, `CASH`, `MOCK_WALLET`.
- `status`: `PENDING`, `SUCCESS`, `FAILED`.

### REST API

All endpoints are reachable through the Gateway (`http://localhost:8080/api/payments/...`) and directly (`http://localhost:8084/payments/...`).

| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/payments` | Process a payment | `201` |
| `GET` | `/api/payments/{id}` | Retrieve a payment transaction | `200` |
| `GET` | `/api/payments/reservation/{reservationId}` | List payments for a reservation | `200` |
| `GET` | `/api/payments/user/{userId}` | List payments of a user | `200` |
| `GET` | `/api/payments/{id}/receipt` | Get the digital receipt for a successful payment | `200` |

### Processing a Payment

`POST /api/payments`:

```json
{
  "reservationId": 1,
  "userId": 1,
  "amount": 500,
  "paymentMethod": "CARD",
  "cardNumber": "4111 1111 1111 1111"
}
```

Flow:

1. Request body is validated — missing/invalid fields → `400`.
2. Card data is validated (required for `CARD`, Luhn-valid format) — else `400`.
3. The reservation is looked up in the **Parking Service** (`GET /parking/reservations/{id}`) — if it does not exist → `404`.
4. Duplicate prevention: if a **successful** payment already exists for the same `reservationId` → `409`. A failed attempt does **not** block a retry.
5. The mock gateway generates a `transactionId` and decides `SUCCESS`/`FAILED`.
6. The transaction is stored and returned as `201` (status `SUCCESS` or `FAILED`).

Example response:

```json
{
  "id": 1,
  "reservationId": 1,
  "userId": 1,
  "amount": 500,
  "paymentMethod": "CARD",
  "transactionId": "TXN-4B4F9C2E1A3D5F7E8A0B",
  "status": "SUCCESS",
  "paymentDate": "2026-08-14T10:30:04",
  "createdAt": "2026-08-14T10:30:04",
  "maskedCardNumber": "************1111"
}
```

### Digital Receipt

`GET /api/payments/{id}/receipt` returns the receipt **only for `SUCCESS` payments**; for a failed/pending payment → `404`. The receipt contains: `receiptId`, `transactionId`, `reservationId`, `userId`, `amount`, `paymentMethod`, `paymentStatus`, `paymentDate`.

### Error Handling

Errors use the shared JSON shape (`timestamp`, `status`, `error`, `path`, `message`):

| Condition | Status |
| --- | --- |
| Invalid/missing payment data or invalid card | `400` |
| Reservation not found (Parking Service lookup) | `404` |
| Payment / receipt not found | `404` |
| Duplicate successful payment for a reservation | `409` |

## Current Architecture (Phase 7)

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
                        (all implemented)
```

The Payment Service depends on the **Parking Service** only to verify that a reservation exists before charging it (`GET /parking/reservations/{id}`); the Parking Service base URL is configurable via `parking.service.base-url` (default `http://localhost:8083`).
