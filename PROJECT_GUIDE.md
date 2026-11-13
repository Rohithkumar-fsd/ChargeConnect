# ChargeConnect — Project Guide & Implementation Status

ChargeConnect is a **Peer-to-Peer EV Charging Marketplace** built as a Spring Boot **Modular Monolith** backed by PostgreSQL (with PostGIS), Flyway, Spring Security (JWT), and Docker Compose (with Redis provisioned for upcoming booking locks/caching).

---

## 1. Tech Stack & Runtime Configuration

- **Language & Framework:** Java 17, Spring Boot `4.1.2`
- **Security:** Spring Security (Stateless JWT via `io.jsonwebtoken` `0.12.6`, BCrypt password hashing)
- **Persistence:** Spring Data JPA / Hibernate (`ddl-auto=none`, `open-in-view=false`)
- **Database & Migrations:** PostgreSQL 16 + PostGIS 3.7, Flyway (`classpath:db/migration`)
- **Server Port:** `8083` (`application.properties`)
- **JVM Timezone:** Forced to `UTC` in `pom.xml` (`spring-boot-maven-plugin` and `maven-surefire-plugin`)

---

## 2. Module & Package Structure (`com.example.chargeconnect`)

- **`auth`**
  - `controller.AuthController` — `/api/auth/register`, `/api/auth/login`, `/api/auth/me`
  - `service.AuthService`, `service.JwtService`, `service.CustomUserDetailsService`
  - `security.JwtAuthFilter`
  - `dto` — `RegisterRequest`, `LoginRequest`, `AuthResponse`
- **`user`**
  - `model` — `User`, `Role` (`DRIVER`, `HOST`, `ADMIN`)
  - `repo.UserRepository`
  - `controller.UserController` — `/api/users/me` profile, password change, self-assignable role addition
  - `controller.AdminUserController` — `/api/admin/users` listing, lookup, account suspension/activation
  - `service.UserService`
  - `dto` — `AccountStatus` (`ACTIVE`, `SUSPENDED`), `UserProfileResponse`, `UpdateProfileRequest`, `ChangePasswordRequest`, `AddRoleRequest`, `UpdateAccountStatusRequest`
- **`vehicle`**
  - `model` — `Vehicle`, `VehicleConnector`, `ConnectorType`
  - `repo.VehicleRepository`
  - `controller.VehicleController` — `/api/driver/vehicles` CRUD
  - `service.VehicleService`
  - `dto` — `VehicleRequest`, `VehiclePatchRequest`, `VehicleResponse`
- **`charger`**
  - `model` — `Charger`, `ChargerConnector`, `LocationType`, `PowerType`
  - `repo.ChargerRepository`
  - `controller.HostChargerController` — `/api/host/chargers` CRUD & availability toggle
  - `controller.AdminChargerController` — `/api/admin/chargers` review & approve/reject verification
  - `service.ChargerService`
  - `dto` — `AvailabilityStatus`, `VerificationStatus`, `ChargerRequest`, `ChargerPatchRequest`, `UpdateAvailabilityRequest`, `VerifyChargerRequest`, `ChargerResponse`
- **`discovery`**
  - `repo` — `DiscoveryRepository`, `DiscoveredChargerProjection`
  - `controller.DriverDiscoveryController` — `/api/driver/discovery/nearby`
  - `service.DiscoveryService`
  - `dto` — `DiscoveredChargerResponse`, `VehicleMatchedChargerResponse`
- **`booking`**
  - `model` — `Booking`, `BookingStatus` (`PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`, `EXPIRED`, `BLOCKED_BY_HOST`)
  - `repo` — `BookingRepository`
  - `controller.DriverBookingController` — `/api/driver/bookings/**`, `/api/driver/bookings/available-slots`
  - `controller.HostBookingController` — `/api/host/chargers/{chargerId}/bookings/**`, `/api/host/chargers/{chargerId}/block-slots`
  - `service.BookingService`, `service.BookingLockService`, `service.RedisBookingLockService`
  - `dto` — `CreateBookingRequest`, `CancelBookingRequest`, `HostBlockSlotRequest`, `BookingResponse`, `AvailableSlotsResponse`, `TimeSlot`
- **`config`**
  - `SecurityConfig` — stateless filter chain, RBAC route rules, JSON 401/403 handlers
  - `HealthController` — `/api/health` database connectivity check
- **`exception`**
  - `GlobalExceptionHandler`, `ApiErrorResponse`, `DuplicateResourceException`, `ResourceNotFoundException`

---

## 3. Database Schema (Flyway Migrations)

Migrations live in `src/main/resources/db/migration`:

1. **`V1__enable_postgis.sql`**
   - Enables the `postgis` extension (`CREATE EXTENSION IF NOT EXISTS postgis;`).
2. **`V2__create_users.sql`**
   - `users` (`id`, `full_name`, `email` UNIQUE, `phone`, `password_hash`, `account_status` DEFAULT `'ACTIVE'`, `created_at`)
   - `user_roles` (`user_id` FK -> `users.id` ON DELETE CASCADE, `role`, PK `(user_id, role)`)
3. **`V3__create_vehicle_tables.sql`**
   - `vehicles` (`id`, `user_id` FK -> `users.id` ON DELETE CASCADE, `manufacturer`, `model`, `registration_number` UNIQUE, `battery_capacity_kwh > 0`, `max_ac_power_kw`, `max_dc_power_kw`, `created_at`, `updated_at`) + index `idx_vehicles_user_id`
   - `vehicle_connectors` (`id`, `vehicle_id` FK -> `vehicles.id` ON DELETE CASCADE, `connector_type`, UNIQUE `(vehicle_id, connector_type)`)
4. **`V4__update_users_for_user_management.sql`**
   - Adds `updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` to `users`
   - Adds `chk_users_account_status` (`ACTIVE`, `SUSPENDED`) and `chk_user_roles_role` (`DRIVER`, `HOST`, `ADMIN`)
5. **`V5__create_charger_tables.sql`**
   - `chargers` (`id`, `host_id` FK -> `users.id` ON DELETE CASCADE, `title`, `description`, `location_type`, address fields, `latitude`, `longitude`, `location GEOGRAPHY(Point, 4326)`, `power_type`, `max_power_kw > 0`, `price_per_kwh > 0`, `availability_status`, `verification_status`, `rejection_reason`, `verified_by` FK -> `users.id`, `verified_at`, `created_at`, `updated_at`)
   - `charger_connectors` (`id`, `charger_id` FK -> `chargers.id` ON DELETE CASCADE, `connector_type`, UNIQUE `(charger_id, connector_type)`)
   - Indexes: `idx_chargers_host_id`, `idx_chargers_verification_status`, and `GIST` spatial index `idx_chargers_location` on `chargers(location)`
   - Trigger `trg_sync_charger_location` (`sync_charger_location()`) automatically keeps `location` synced from `(longitude, latitude)` in SRID `4326` on every `INSERT` or coordinate `UPDATE`.
6. **`V6__create_booking_tables.sql`**
   - Adds operating hours to chargers: `open_time TIME NOT NULL DEFAULT '00:00:00'`, `close_time TIME NOT NULL DEFAULT '23:59:59'`
   - `bookings` (`id`, `booking_reference` UUID UNIQUE, `driver_id` FK -> `users.id` ON DELETE CASCADE, `charger_id` FK -> `chargers.id` ON DELETE CASCADE, `vehicle_id` FK -> `vehicles.id` ON DELETE SET NULL, `start_time`, `end_time`, `total_hours`, `price_per_kwh`, `estimated_kwh`, `total_price`, `status`, `cancellation_reason`, timestamps)
   - Constraints: `end_time > start_time`, `chk_booking_status` (`PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`, `EXPIRED`, `BLOCKED_BY_HOST`)
   - Indexes: `idx_bookings_driver_id`, `idx_bookings_charger_id`, `idx_bookings_charger_status_dates`, `idx_bookings_reference`

---

## 4. Implemented API Endpoints

### Public & Health
| Method | Path | Auth | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | Public | Checks app & PostgreSQL connectivity (`200` / `503`) |
| `POST` | `/api/auth/register` | Public | Registers a `DRIVER` or `HOST` and returns a JWT (`201`) |
| `POST` | `/api/auth/login` | Public | Authenticates email/password and returns a JWT (`200`) |
| `GET` | `/api/auth/me` | Authenticated | Simple check returning `"Logged in as: <email>"` |

### User Management
| Method | Path | Auth | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/users/me` | Authenticated | Returns full profile (`id`, `fullName`, `email`, `phone`, `accountStatus`, `roles`, timestamps) |
| `PATCH` | `/api/users/me` | Authenticated | Partially updates `fullName` / `phone` (blank phone clears it to `null`) |
| `POST` | `/api/users/me/change-password` | Authenticated | Verifies `currentPassword` and hashes `newPassword` (`204`) |
| `POST` | `/api/users/me/roles` | Authenticated | Adds `DRIVER` or `HOST` role (`ADMIN` self-assignment blocked) |
| `GET` | `/api/admin/users` | `ROLE_ADMIN` | Lists all users ordered by `createdAt DESC` |
| `GET` | `/api/admin/users/{userId}` | `ROLE_ADMIN` | Fetches a user profile by ID |
| `PATCH` | `/api/admin/users/{userId}/status` | `ROLE_ADMIN` | Sets `ACTIVE` or `SUSPENDED` (admins cannot suspend themselves) |

### Vehicle Management
All vehicle endpoints enforce both `ROLE_DRIVER` and row-level ownership (`vehicle.user_id == authenticated user's id`).

| Method | Path | Auth | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/driver/vehicles` | `ROLE_DRIVER` | Registers a vehicle + connectors (`201`) |
| `GET` | `/api/driver/vehicles` | `ROLE_DRIVER` | Lists the logged-in driver's vehicles (`createdAt DESC`) |
| `GET` | `/api/driver/vehicles/{vehicleId}` | `ROLE_DRIVER` | Gets one owned vehicle (`404` if missing or owned by another user) |
| `PATCH` | `/api/driver/vehicles/{vehicleId}` | `ROLE_DRIVER` | Partially updates vehicle details and/or replaces connectors |
| `DELETE` | `/api/driver/vehicles/{vehicleId}` | `ROLE_DRIVER` | Deletes an owned vehicle (`204`) |

Supported `ConnectorType` values (shared across vehicles and chargers): `TYPE_1`, `TYPE_2`, `CCS1`, `CCS2`, `CHADEMO`, `GB_T`, `BHARAT_AC_001`, `NACS`.

### Charger Management
Host endpoints enforce `ROLE_HOST` and row-level ownership (`charger.host_id == authenticated user's id`). Updating location/hardware specs on an already reviewed charger resets `verificationStatus` to `PENDING` (updating only `title`, `description`, or `pricePerKwh` keeps existing approval).

| Method | Path | Auth | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/host/chargers` | `ROLE_HOST` | Registers a charger (`AVAILABLE`, `PENDING` verification) (`201`) |
| `GET` | `/api/host/chargers` | `ROLE_HOST` | Lists all chargers owned by the logged-in host |
| `GET` | `/api/host/chargers/{chargerId}` | `ROLE_HOST` | Gets one owned charger (`404` if missing or owned by another host) |
| `PATCH` | `/api/host/chargers/{chargerId}` | `ROLE_HOST` | Updates charger details/location/connectors (smart re-verification) |
| `PATCH` | `/api/host/chargers/{chargerId}/availability` | `ROLE_HOST` | Sets `AVAILABLE`, `UNAVAILABLE`, or `MAINTENANCE` |
| `DELETE` | `/api/host/chargers/{chargerId}` | `ROLE_HOST` | Deletes an owned charger (`204`) |
| `GET` | `/api/admin/chargers` | `ROLE_ADMIN` | Lists all chargers (optional `?status=PENDING\|APPROVED\|REJECTED`) |
| `GET` | `/api/admin/chargers/{chargerId}` | `ROLE_ADMIN` | Inspects any charger by ID |
| `PATCH` | `/api/admin/chargers/{chargerId}/verification` | `ROLE_ADMIN` | `APPROVE` or `REJECT` (requires `rejectionReason` on reject) |

Supported Charger Enums:
- `LocationType`: `HOME`, `HOTEL`, `RESTAURANT`, `OFFICE`, `COMMERCIAL`, `OTHER`
- `PowerType`: `AC`, `DC`, `BOTH`
- `AvailabilityStatus`: `AVAILABLE`, `UNAVAILABLE`, `MAINTENANCE`
- `VerificationStatus`: `PENDING`, `APPROVED`, `REJECTED`

### Location & Discovery (MVP)
When the driver clicks "Find Nearby" on the map, the API finds approved and available charging stations near their current GPS coordinates that match their EV's connectors and power constraints.

| Method | Path | Auth | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/driver/discovery/nearby` | `ROLE_DRIVER` | Find nearby compatible chargers by query params (`vehicleId`, `latitude`, `longitude`, optional `radiusMeters`, `maxPricePerKwh`) |

The response matches the vehicle's connectors, calculates `effectivePowerKw` (the maximum rate the car can safely draw at that charger), and sorts chargers nearest-first (`distanceMeters`).

### Booking Management
Enforces 30-minute time-slot boundaries, host operating hours, connector compatibility, and a **Two-Layer Locking Strategy** (Layer 1: Redis distributed lock with 10-min TTL during checkout; Layer 2: PostgreSQL transactional overlap constraint) to physically eliminate double-booking race conditions.

| Method | Path | Auth | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/driver/bookings` | `ROLE_DRIVER` | Creates a new booking (`PENDING` with 10-min Redis lock) (`201`) |
| `POST` | `/api/driver/bookings/{bookingId}/confirm` | `ROLE_DRIVER` | Confirms payment / finalizes slot (`CONFIRMED`, releases checkout lock) |
| `POST` | `/api/driver/bookings/{bookingId}/cancel` | `ROLE_DRIVER` | Cancels an upcoming booking (`CANCELLED`) |
| `GET` | `/api/driver/bookings` | `ROLE_DRIVER` | Lists all bookings for the logged-in driver (`startTime DESC`) |
| `GET` | `/api/driver/bookings/{bookingId}` | `ROLE_DRIVER` | Fetches details of a specific driver booking |
| `GET` | `/api/driver/bookings/available-slots` | `ROLE_DRIVER` | Returns 30-minute calendar slots (`AVAILABLE` / `BUSY`) for a charger & date |
| `GET` | `/api/host/chargers/{chargerId}/bookings` | `ROLE_HOST` | Host views calendar of all bookings on their charger |
| `POST` | `/api/host/chargers/{chargerId}/block-slots` | `ROLE_HOST` | Host blocks a slot for personal EV charging (`BLOCKED_BY_HOST`) (`201`) |

---

## 5. How to Run & Test the Project Locally

### Prerequisites
- Java 17+
- Docker Desktop (for PostgreSQL/PostGIS and Redis)
- Postman (or `curl`)

### 1. Environment Variables
Copy `.env.example` to `.env` (already present in the repo) if needed:
- `POSTGRES_DB=chargeconnect`
- `POSTGRES_USER=chargeconnect`
- `POSTGRES_PASSWORD=chargeconnect`
- `POSTGRES_PORT=5433` (mapped to container `5432` to avoid clashing with local PostgreSQL)
- `REDIS_PORT=6379`
- Optional: `JWT_SECRET` (must be at least 32 UTF-8 bytes; defaults to a local dev value in `application.properties`)

### 2. Start PostgreSQL (PostGIS) & Redis
From the project root (`c:\Users\rohit\Downloads\chargeconnect`):
```powershell
docker compose up -d
docker compose ps
```

### 3. Run the Spring Boot Application
```powershell
.\mvnw.cmd spring-boot:run
```
Flyway automatically applies `V1`–`V6` on startup, and the API listens on `http://localhost:8083`.

### 4. Run Automated Tests
Unit and controller slice tests (do not require Docker):
```powershell
.\mvnw.cmd test "-Dtest=BookingServiceTest,DriverBookingControllerTest,HostBookingControllerTest,DiscoveryServiceTest,DriverDiscoveryControllerTest,ChargerServiceTest,HostChargerControllerTest,AdminChargerControllerTest,UserServiceTest,UserControllerTest,AdminUserControllerTest,VehicleServiceTest,VehicleControllerTest"
```
Full test suite (requires Docker Postgres running on port `5433` for `ChargeconnectApplicationTests`):
```powershell
.\mvnw.cmd test
```

### 5. Granting `ADMIN` Role for Testing `/api/admin/**`
Public registration only allows `DRIVER` or `HOST`. To promote an existing registered user (e.g., `admin@chargeconnect.com`) to `ADMIN` locally:
```powershell
docker exec -it chargeconnect-postgres psql -U chargeconnect -d chargeconnect -c "INSERT INTO user_roles (user_id, role) SELECT id, 'ADMIN' FROM users WHERE email = 'admin@chargeconnect.com' ON CONFLICT DO NOTHING;"
```
Then send that user's Bearer token in Postman (`Authorization → Bearer Token`).

---

## 6. How the Authentication, Filters & Role Validation Flow Works

ChargeConnect uses **stateless JWT authentication** — the server stores no HTTP session in memory.

### A. Registration (`POST /api/auth/register`)
1. `AuthController` validates `RegisterRequest` (`@Valid` checks non-blank name, valid email, password length 8–72, role `DRIVER` or `HOST`).
2. `AuthService` normalizes the email (`toLowerCase().trim()`), checks `UserRepository.existsByEmail(...)`, hashes the raw password with `BCryptPasswordEncoder`, saves the `User` + `Role` to PostgreSQL, and calls `JwtService.generateToken(email)`.
3. Returns `201 Created` with `{ "token": "<jwt>", "tokenType": "Bearer", "email": "..." }`.

### B. Login (`POST /api/auth/login`)
1. `AuthController` passes `LoginRequest` to `AuthService.login(...)`.
2. `AuthService` calls `authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password))`.
3. Spring Security's `DaoAuthenticationProvider` calls `CustomUserDetailsService.loadUserByUsername(email)` to fetch the user from PostgreSQL, verifies the raw password against `password_hash` via `BCryptPasswordEncoder`, and rejects `SUSPENDED` accounts (`DisabledException` → `403`).
4. If valid, `JwtService` signs a JWT (HMAC-SHA, `sub = email`, 1-hour expiry) and returns `AuthResponse`.

### C. Request Lifecycle & Filter Pipeline (`Postman → Service → DB`)
```text
Postman HTTP Request (Authorization: Bearer <token>)
   │
   ▼
Embedded Tomcat (Port 8083)
   │
   ▼
Spring Security FilterChainProxy (SecurityConfig)
   ├── 1. SecurityContextHolderFilter  (starts empty — STATELESS)
   ├── 2. JwtAuthFilter                (OncePerRequestFilter, registered before UsernamePasswordAuthenticationFilter)
   │       ├── validates JWT signature & expiry (JwtService)
   │       ├── loads fresh UserDetails + roles from PostgreSQL (CustomUserDetailsService)
   │       └── if user.isEnabled(), stores Authentication + ROLE_* authorities in SecurityContextHolder
   ├── 3. ExceptionTranslationFilter   (catches 401/403 from AuthorizationFilter and writes JSON responses)
   └── 4. AuthorizationFilter          (matches route rules top-to-bottom: /api/admin/**, /api/host/**, /api/driver/**)
   │
   ▼ (only if authorized)
DispatcherServlet (Spring MVC)
   ├── Jackson deserializes JSON body
   ├── @Valid runs Bean Validation (400 with fieldErrors via GlobalExceptionHandler)
   ▼
Controller (injects Authentication from SecurityContextHolder)
   │
   ▼
Service (@Transactional — enforces ownership & business rules using authentication.getName())
   │
   ▼
Repository / PostgreSQL
```

Key details:
1. **Why `ROLE_` prefix matters:** `CustomUserDetailsService` maps `Role.ADMIN` to `"ROLE_ADMIN"`. In `SecurityConfig`, `.hasRole("ADMIN")` automatically checks for `"ROLE_ADMIN"`.
2. **Fresh DB lookup on every request:** Loading `UserDetails` via `CustomUserDetailsService` inside `JwtAuthFilter` ensures newly added roles (`POST /api/users/me/roles`) and admin suspensions (`PATCH /api/admin/users/{id}/status`) take effect immediately on the next request.
3. **Why `SecurityConfig` has its own 401/403 JSON handlers:** Authentication and role checks run inside the Security Filter Chain *before* `DispatcherServlet`, so `@RestControllerAdvice` (`GlobalExceptionHandler`) does not catch filter-level `401`/`403` errors.
4. **Two-layer defense:** `SecurityConfig` checks coarse-grained roles (`/api/driver/**`, `/api/host/**`, `/api/admin/**`), while Services enforce row-level ownership (`findByIdAndUserId`, `findByIdAndHostId`) using `authentication.getName()` from the verified JWT.

---

## 7. Next Modules (Planned)

- **Payment Management** (Razorpay integration, checkout order creation, signature verification, refunds, host payouts)
- **Charging Sessions** (QR check-in, host confirmation, real-time start/end tracking, energy consumption)
