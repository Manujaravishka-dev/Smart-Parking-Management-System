# Documentation & Screenshots

This directory holds design/architecture documentation and screenshots for the Smart Parking Management System.

## Screenshots

- `screenshots/eureka_dashboard.png` — the Eureka dashboard after all services are running (referenced by the project README).

## How to capture the real Eureka dashboard screenshot

Do **not** use a fake/generated image. Take the screenshot from the running system:

1. Make sure **PostgreSQL** is available (or run the services with H2-in-test only — the live services expect PostgreSQL via the `DB_*` environment variables).
2. Start every service **in order** (see the project README, "Running the Project"):
   1. `mvnw.cmd -pl eureka-server spring-boot:run`
   2. `mvnw.cmd -pl config-server spring-boot:run`
   3. `mvnw.cmd -pl user-service spring-boot:run`
   4. `mvnw.cmd -pl vehicle-service spring-boot:run`
   5. `mvnw.cmd -pl parking-service spring-boot:run`
   6. `mvnw.cmd -pl payment-service spring-boot:run`
   7. `mvnw.cmd -pl api-gateway spring-boot:run`
3. Open a browser and go to **`http://localhost:8761`**.
4. Wait ~30–60 seconds for all instances to register (the "last 1000 since startup" area shows a heartbeat interval, and instances appear in the application table).
5. Confirm the dashboard lists **all** applications as **UP**:
   - `API-GATEWAY`
   - `CONFIG-SERVER`
   - `USER-SERVICE`
   - `VEHICLE-SERVICE`
   - `PARKING-SERVICE`
   - `PAYMENT-SERVICE`
6. Take a full-page screenshot (Windows: `Win + Shift + S` for a region, or browser screenshot tools; the whole "System Status / General Info / Instances currently registered with Eureka" section should be visible).
7. Save the image as **`docs/screenshots/eureka_dashboard.png`** (overwrite the current placeholder path referenced in the README).

The README renders it automatically via `![Eureka Dashboard](./docs/screenshots/eureka_dashboard.png)`.
