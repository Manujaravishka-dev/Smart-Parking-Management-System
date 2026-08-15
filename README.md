# Smart Parking Management System

A **microservice-based, backend-only** Smart Parking Management System (SPMS) built with **Java 17**, **Spring Boot 4.1.0**, and **Spring Cloud 2025.1.2**. The system lets drivers discover available parking spaces in real time, reserve them, and pay for parking digitally — exposed exclusively through **REST APIs** via a single **API Gateway** entry point.

## Project Overview

SPMS is decomposed into **seven independent Spring Boot services**, each deployable on its own, that collaborate over HTTP:

- **Eureka Server** — service registry for discovery.
- **Config Server** — centralized, environment-driven configuration.
- **API Gateway** — the single REST entry point (port 8080) that routes `/api/**` requests to the correct backend service.
- **User Service** — registration, authentication, and profile management.
- **Vehicle Service** — vehicle registration and entry/exit tracking.
- **Parking Service** — parking space management, search, reservations, and IoT-style status updates.
- **Payment Service** — billing, mock payment gateway integration, duplicate-payment prevention, and digital receipts.

Every request flows: `Client -> API Gateway (8080) -> backend service -> database`. Services discover each other via Eureka and pull their configuration from the Config Server, so no backend hostname or port is hardcoded in the gateway.

## Business Problem

Urban parking is inefficient for all parties involved:

- **Drivers** waste time and fuel circling blocks searching for an available spot, with no way to know availability or price in advance.
- **Parking owners** cannot easily track which of their spaces are free, reserved, or occupied at any moment, and have no simple way to collect payment for reservations.
- **Manual payment** for parking (cash/coin machines) is inconvenient, un-tracked, and prone to disputes.
- **No digital record** ties a driver, a vehicle, a reservation, and a payment together, so there is no reliable receipt or history.

## Proposed Solution

SPMS addresses the problem with a set of focused microservices:

1. **Users** register once and log in to use the system.
2. **Parking owners** register parking spaces with location (city/zone), pricing, and a live availability status.
3. **Drivers** search and filter spaces by city, zone, and availability, then **reserve** one for a time window.
4. **Vehicles** are registered per user, and entry/exit is tracked as drivers come and go.
5. **Payment** is processed against a mock gateway for the reservation, **duplicate charging is prevented**, and a **digital receipt** is generated for every successful payment.

The system is designed to be extended later with real IoT sensors, a real payment provider, JWT security, and container orchestration (see [Future Improvements](#future-improvements)).

## Architecture

```
                        ┌─────────────────┐
                        │  eureka-server  │  Service registry (8761)
                        └────────┬────────┘
                  register / discover │
   ┌───────────────┐           │     ┌───────────────────┐
   │ config-server │───────────┘     │    api-gateway    │  Single entry point (8080)
   │    (8888)     │                 └─────────┬─────────┘
   └───────┬───────┘                           │ routes /api/** via lb:// + Eureka
           │ serve config to all services      ▼
           ▼                      ┌──────────────────────────────┐
    ┌──────────────────┐            │ user-service   (8081)        │
    │ PostgreSQL       │            │ vehicle-service(8082)        │
    │ smart_parking_db │<───────────│ parking-service(8083)        │
    └──────────────────┘            │ payment-service(8084)        │
                                  └──────────────────────────────┘
```

- The **API Gateway** matches incoming paths (`/api/users/**`, `/api/vehicles/**`, `/api/parking/**`, `/api/payments/**`), strips the `/api` prefix, and forwards to the target service by its **Eureka service ID** using the load-balanced `lb://` URI scheme.
- The **Config Server** runs with the `native` profile and serves every service its configuration (ports, Eureka URL, datasource placeholders, JPA settings).
- The **Payment Service** depends on the **Parking Service** only to verify that a reservation exists before charging it (`GET /parking/reservations/{id}`), via a configurable base URL (`parking.service.base-url`, default `http://localhost:8083`).
- All error responses use one **shared JSON shape**: `{ "timestamp", "status", "error", "path", "message" }`.

## Technologies

| Technology | Version / Detail |
| --- | --- |
| Java | 17 (source/target pinned; JDK 26 installed on the dev machine) |
| Spring Boot | 4.1.0 |
| Spring Cloud | 2025.1.2 (Oakwood) |
| Spring Cloud Netflix | Eureka Server (service registry) |
| Spring Cloud Config | Config Server (native profile) |
| Spring Cloud Gateway | Reactive gateway (WebFlux / Netty) |
| Spring Data JPA | Persistence (Hibernate) |
| Validation | Bean Validation (Jakarta) |
| Spring Security | `spring-security-crypto` only (BCrypt password hashing) |
| Build | Maven 3.9.16 via the wrapper (`mvnw` / `mvnw.cmd`) |
| Database | PostgreSQL (placeholders; tests use in-memory H2) |
| Testing | JUnit 5, Mockito, `@SpringBootTest` + MockMvc |
| API Testing | Postman (`postman_collection.json`) |

## Microservices

### Eureka Server

Standalone Netflix Eureka **service registry**. All services (and the gateway) register here on startup; the gateway uses the registry to resolve routes. It does not register itself (`register-with-eureka: false`, `fetch-registry: false`). Dashboard available at `http://localhost:8761`.

### Config Server

Centralized configuration server (native profile). Serves every service's configuration from `config-server/src/main/resources/config/`. Sensitive values are **not hardcoded** — they are resolved from environment variables with local-development defaults:

| Environment variable | Default | Used for |
| --- | --- | --- |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka` | Eureka registry URL |
| `DB_HOST` | `localhost` | Database host |
| `DB_PORT` | `5432` | Database port |
| `DB_NAME` | `smart_parking_db` | Database name |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `1122` | Database password (dev-only placeholder) |
| `JPA_DDL_AUTO` | `update` | Hibernate DDL mode |
| `CONFIG_SERVER_URL` | `http://localhost:8888` | Config Server URL used by clients |

Example: `http://localhost:8888/user-service/default` returns the `user-service` configuration as JSON.

### API Gateway

Spring Cloud Gateway (WebFlux/Netty), the **single entry point** on port 8080. It registers with Eureka, fetches the registry, and routes via `lb://` (load-balanced) service IDs. Routes strip the leading `/api` prefix before forwarding:

| Gateway path | Target (Eureka service ID) | Forwarded path |
| --- | --- | --- |
| `/api/users/**` | `lb://USER-SERVICE` | `/users/**` |
| `/api/vehicles/**` | `lb://VEHICLE-SERVICE` | `/vehicles/**` |
| `/api/parking/**` | `lb://PARKING-SERVICE` | `/parking/**` |
| `/api/payments/**` | `lb://PAYMENT-SERVICE` | `/payments/**` |

A custom **global error handler** returns the shared JSON error shape for failures such as 404 (no route) and 503 (no registered instances).

### User Service

Manages users: **registration** (normalized email, BCrypt password, duplicate-email rejection → `409`), **login** (wrong credentials → `401`), **profile retrieval and update**, and a bookings endpoint. Passwords are never exposed in responses.

### Vehicle Service

Manages vehicles per user: register (unique vehicle number → duplicate gives `409`), retrieve, list by user, update, delete, and **simulated entry/exit** tracking with `entryTime`/`exitTime`. Entry/exit transitions are guarded (already-inside → `409`, not-inside exit → `409`) and protected with a **pessimistic write lock** so concurrent entries/exits serialize.

### Parking Service

Manages parking spaces and reservations:
- **Spaces**: create, retrieve, list, update, delete, status updates (simulated IoT), and search/filter by `city`, `zone`, and `available`.
- **Reservations**: create (space must be `AVAILABLE`, else `409`; `startTime < endTime`, else `400`), retrieve, list by user, cancel, and release. Creation uses a **pessimistic write lock** (`SELECT ... FOR UPDATE`) so two concurrent reservations on the same space serialize — exactly one succeeds.

### Payment Service

Processes billing for reservations against an in-process **mock payment gateway** (no external provider):
- `CASH` and `MOCK_WALLET` always succeed; `CARD` fails only for numbers ending in `0002` (e.g. `4000000000000002`).
- Card numbers are validated (13–19 digits + **Luhn** checksum; invalid → `400`) and **never stored** — only a masked form (`************1111`) is returned.
- Verifies the reservation exists in the Parking Service (missing → `404`; parking unavailable → `503`).
- Prevents duplicate charging: a second **successful** payment for the same reservation → `409` (failed attempts do not block retries).
- Generates a digital **receipt** for successful payments (`GET /api/payments/{id}/receipt`).

## Service Ports

| Service | Port | Direct URL | Gateway URL |
| --- | --- | --- | --- |
| Eureka Server | 8761 | `http://localhost:8761` | — |
| Config Server | 8888 | `http://localhost:8888` | — |
| API Gateway | 8080 | `http://localhost:8080` | — |
| User Service | 8081 | `http://localhost:8081` | `http://localhost:8080/api/users/**` |
| Vehicle Service | 8082 | `http://localhost:8082` | `http://localhost:8080/api/vehicles/**` |
| Parking Service | 8083 | `http://localhost:8083` | `http://localhost:8080/api/parking/**` |
| Payment Service | 8084 | `http://localhost:8084` | `http://localhost:8080/api/payments/**` |

## Database Design

All services share one **PostgreSQL database** (`smart_parking_db`). Each service owns its own set of tables (no table-name collisions); cross-service data (e.g. a user's vehicle, or a payment's reservation) is referenced by **ID only**, and ownership is enforced by the owning service.

| Service | Owns | Key entities / rules |
| --- | --- | --- |
| User Service | Users | `User` — `email` unique, `password` BCrypt-hashed, `role` DRIVER/OWNER |
| Vehicle Service | Vehicles | `Vehicle` — `vehicleNumber` unique, `status` OUTSIDE/INSIDE, `userId` reference to User Service |
| Parking Service | Spaces + Reservations | `ParkingSpace` — status AVAILABLE/RESERVED/OCCUPIED/MAINTENANCE; `Reservation` — status PENDING/CONFIRMED/CANCELLED/COMPLETED, references `userId`/`vehicleId`/`parkingSpaceId` |
| Payment Service | Payments | `Payment` — `reservationId`/`userId` references, `transactionId` unique, status PENDING/SUCCESS/FAILED; card data never persisted |

Schema is auto-managed by Hibernate (`spring.jpa.hibernate.ddl-auto: update` by default; tests use `create-drop` on in-memory H2).

## API Endpoints

All endpoints below are reached through the **API Gateway** at `http://localhost:8080`. See the full collection at [postman_collection.json](./postman_collection.json).

### User Service

| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/users` | Register a user | `201` |
| `POST` | `/api/users/login` | Log in | `200` |
| `GET` | `/api/users/{id}` | Get a user | `200` |
| `PUT` | `/api/users/{id}` | Update a user | `200` |
| `GET` | `/api/users/{id}/bookings` | Get a user's bookings | `200` |

### Vehicle Service

| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/vehicles` | Create a vehicle | `201` |
| `GET` | `/api/vehicles/{id}` | Get a vehicle | `200` |
| `GET` | `/api/vehicles/user/{userId}` | List a user's vehicles | `200` |
| `PUT` | `/api/vehicles/{id}` | Update a vehicle | `200` |
| `DELETE` | `/api/vehicles/{id}` | Delete a vehicle | `204` |
| `POST` | `/api/vehicles/{id}/entry` | Vehicle entry | `200` |
| `POST` | `/api/vehicles/{id}/exit` | Vehicle exit | `200` |

### Parking Service

| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/parking/spaces` | Create a space | `201` |
| `GET` | `/api/parking/spaces` | Search/list spaces (`?city=`, `?zone=`, `?available=`) | `200` |
| `GET` | `/api/parking/spaces/{id}` | Get a space | `200` |
| `PUT` | `/api/parking/spaces/{id}` | Update a space | `200` |
| `DELETE` | `/api/parking/spaces/{id}` | Delete a space | `204` |
| `PUT` | `/api/parking/spaces/{id}/status` | Update space status (simulated IoT) | `200` |

### Reservation (Parking Service)

| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/parking/reservations` | Create a reservation | `201` |
| `GET` | `/api/parking/reservations/{id}` | Get a reservation | `200` |
| `GET` | `/api/parking/reservations/user/{userId}` | List a user's reservations | `200` |
| `POST` | `/api/parking/reservations/{id}/cancel` | Cancel a reservation | `200` |
| `POST` | `/api/parking/reservations/{id}/release` | Release/complete a reservation | `200` |

### Payment Service

| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/payments` | Process a payment | `201` |
| `GET` | `/api/payments/{id}` | Get a payment | `200` |
| `GET` | `/api/payments/reservation/{reservationId}` | List payments for a reservation | `200` |
| `GET` | `/api/payments/user/{userId}` | List a user's payments | `200` |
| `GET` | `/api/payments/{id}/receipt` | Get the digital receipt | `200` |

## Running the Project

Prerequisites: **Java 17+** (JDK 26 is used on the dev machine) and **Maven** (wrapper provided). The services connect to PostgreSQL by default; set `DB_*` environment variables to point at real databases, or leave the dev defaults.

Start the services **in this exact order** — each service registers with Eureka on startup, so Eureka must be up first, and the Config Server serves configuration to everything else:

```bash
# 1. Eureka Server  (port 8761) - service registry
mvnw.cmd -pl eureka-server spring-boot:run

# 2. Config Server  (port 8888) - centralized configuration
mvnw.cmd -pl config-server spring-boot:run

# 3. User Service   (port 8081)
mvnw.cmd -pl user-service spring-boot:run

# 4. Vehicle Service (port 8082)
mvnw.cmd -pl vehicle-service spring-boot:run

# 5. Parking Service (port 8083)
mvnw.cmd -pl parking-service spring-boot:run

# 6. Payment Service (port 8084)
mvnw.cmd -pl payment-service spring-boot:run

# 7. API Gateway    (port 8080) - single entry point
mvnw.cmd -pl api-gateway spring-boot:run
```

> **Windows note:** use `mvnw.cmd`; on macOS/Linux use `./mvnw`.

Alternatively, run the built artifacts directly:

```bash
java -jar eureka-server/target/eureka-server-1.0.0-SNAPSHOT.jar
java -jar config-server/target/config-server-1.0.0-SNAPSHOT.jar
# ... and so on for each service
```

**Sanity check:** after all seven services are up, open `http://localhost:8761` — the dashboard should list `API-GATEWAY`, `USER-SERVICE`, `VEHICLE-SERVICE`, `PARKING-SERVICE`, `PAYMENT-SERVICE`, and `CONFIG-SERVER` as up/registered instances. Then call the gateway, e.g. `GET http://localhost:8080/api/parking/spaces`.

**Building everything:**

```bash
mvnw.cmd clean package    # or ./mvnw clean package
```

## Postman Testing

A complete Postman collection is provided:

- **[postman_collection.json](./postman_collection.json)**

Import it into Postman (**Import > Upload Files**). The collection contains **6 folders** with **39 requests**, all routed through the gateway at `http://localhost:8080` (collection variable `baseUrl`):

1. **User Service** — Register, Login, Get User, Update User.
2. **Vehicle Service** — Create Vehicle, Get Vehicle, Get User Vehicles, Update Vehicle, Vehicle Entry, Vehicle Exit.
3. **Parking Service** — Create Space, Get Spaces, Search by City, Search by Zone, Search Available, Update Space, Update Status.
4. **Reservation** — Create Reservation, Get Reservation, Get User Reservations, Cancel Reservation, Release Reservation.
5. **Payment Service** — Create Payment, Get Payment, Get Reservation Payment, Get User Payments, Get Receipt.
6. **Error Cases** — Duplicate User, Invalid Login, User Not Found, Vehicle Not Found, Duplicate Vehicle, Vehicle Already Inside, Vehicle Already Outside, Parking Space Not Found, Parking Space Already Reserved, Invalid Reservation Time, Duplicate Payment, Invalid Payment.

**Suggested flow** (with all services running): Register a user → Create a vehicle → Create a space → Create a reservation on that space → Create a payment for that reservation → Get the receipt. Several Error Cases intentionally depend on a prior success (e.g. duplicate user/vehicle/payment, already-inside) — their descriptions explain the prerequisite step.

## Eureka Dashboard

Once all services are running, the Eureka dashboard at `http://localhost:8761` shows every registered application instance. Add the real screenshot here:

![Eureka Dashboard](./docs/screenshots/eureka_dashboard.png)

To capture it: start all seven services in the order above, open `http://localhost:8761` in a browser, wait ~30 seconds for all instances to register, and take a full-page screenshot of the dashboard showing all applications in **UP** state (a screenshot guide is included in `docs/README.md`).

## Error Handling

All services return a **consistent JSON error body**:

```json
{
  "timestamp": "2026-08-14T10:30:04.123+05:30",
  "status": 409,
  "error": "Conflict",
  "path": "/api/users",
  "message": "Email is already registered"
}
```

The `message` field carries the human-readable reason. Status codes used across the system:

| Status | Meaning |
| --- | --- |
| `200` / `201` | Success (201 = resource created, e.g. user, vehicle, space, reservation, payment) |
| `400` | Bad request — invalid/malformed body, invalid enum, invalid reservation time, invalid card, wrong ID type |
| `401` | Unauthorized — invalid login credentials |
| `404` | Not found — user, vehicle, space, reservation, payment, or receipt missing |
| `409` | Conflict — duplicate email/vehicle number, space not available, illegal entry/exit, duplicate payment, wrong reservation state |
| `500` | Internal server error |
| `503` | Service unavailable — e.g. payment service cannot reach the parking service |

The API Gateway also returns this shape for gateway-level failures (404 no route, 503 no service instances).

## Business Flow

The end-to-end happy path:

```
User
 ↓
Gateway            (single entry point, port 8080, /api/**)
 ↓
Parking            (search & find an available space)
 ↓
Reservation        (reserve the space for a time window)
 ↓
Payment            (charge for the reservation via the mock gateway)
 ↓
Receipt            (digital receipt for the successful payment)
```

1. A **user** registers and logs in (`User Service`).
2. The **user** searches for an available space via the **Gateway** (`Parking Service`).
3. The **user** books the space — a **reservation** is created and the space becomes `RESERVED` (`Parking Service`).
4. The **user** pays — the **payment** service verifies the reservation, processes the transaction through the mock gateway, and prevents duplicate charging (`Payment Service`).
5. A **receipt** is generated for the successful payment (`Payment Service`).
6. On arrival/departure, the vehicle's **entry/exit** is tracked, and when the parking session ends the reservation can be **released**.

## Future Improvements

- **Real IoT integration** — replace simulated space status updates with real sensors (e.g. per-space ultrasonic/IR detectors publishing occupancy events).
- **Real payment gateway** — swap `MockPaymentGateway` for a real provider (Stripe / PayPal / PayHere) behind the same interface, with webhooks and idempotency keys.
- **JWT security** — add OAuth2/JWT authentication so the gateway can authenticate users and pass identity (e.g. `X-User-Id`) downstream, replacing plain login endpoints.
- **Docker** — containerize every service and the database with Dockerfiles and a Compose orchestration for one-command startup.
- **Kafka** — event-driven flows (e.g. `ReservationCreated`, `PaymentSucceeded`) for async notifications, reporting, and decoupling services.
- **Cloud deployment** — deploy on AWS/GCP/Azure (EKS/GKE) with managed PostgreSQL, CI/CD pipelines, and centralized logging/metrics (Prometheus/Grafana).

---

*Coursework project: microservices architecture in Java/Spring Boot. All backend business logic is covered by automated tests across every service.*
