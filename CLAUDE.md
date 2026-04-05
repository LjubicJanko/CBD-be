# CBD Order Tracker - Backend

Spring Boot 3.3.3 REST API (Java 17) for a custom order management system. Tracks orders through a manufacturing pipeline (design -> print -> sew -> ship), handles payments, user auth, and banners.

## Build & Run

```bash
./mvnw spring-boot:run          # run locally
./mvnw clean package             # build JAR
./mvnw clean package -DskipTests # build without tests
./mvnw test                      # run tests
```

Docker:
```bash
docker build -t cbd-order-tracker .
docker run -p 8080:8080 cbd-order-tracker
```

## Database

- MySQL 8 via Spring Data JPA
- Hibernate ddl-auto: `update` (schema auto-managed)
- Seed data in `src/main/resources/data.sql`
- Config in `src/main/resources/application.properties`

## Authentication

- JWT-based (jjwt 0.11.5), stateless sessions
- BCrypt password encoding
- Public endpoints: `/api/auth/**`, `/api/orderExtend/**`, `/api/orders/track/**`, `/api/banners/active/**`
- All other endpoints require `Authorization: Bearer <token>` header

## Roles & Privileges

Three roles: `admin`, `manufacturer`, `manager`. Admin has all privileges. Privileges control order status transitions and actions (e.g., `order-create`, `move-to-printing`, `payment-add`).

## Order Status Flow

```
PENDING -> DESIGN -> PRINT_READY -> PRINTING -> SEWING -> SHIP_READY -> SHIPPED -> DONE
```

Orders also have an execution status: `ACTIVE`, `PAUSED`, or `CANCELLED`. Soft-deleted via `deleted` flag.

## API Endpoints

| Prefix               | Controller                  | Auth     |
|-----------------------|-----------------------------|----------|
| `/api/auth`           | AuthenticationController    | Public   |
| `/api/orders`         | OrderController             | Required |
| `/api/orderExtend`    | OrderExtensionController    | Public   |
| `/api/profile`        | ProfileController           | Required |
| `/api/banners`        | BannersController           | Mixed    |

## Project Structure

```
src/main/java/cbd/order_tracker/
  config/          # Security, JWT filter, app config
  controller/      # REST controllers
  exceptions/      # Global exception handler, custom exceptions
  model/           # JPA entities, enums
    dto/           # Request/response DTOs
      request/     # Inbound DTOs
      response/    # Outbound DTOs
  repository/      # Spring Data JPA repositories
  service/         # Service interfaces
    impl/          # Service implementations
  util/            # Mappers (Order, User, Payment, Banner)
```

## Key Libraries

- **Lombok** - `@Data`, `@RequiredArgsConstructor` for boilerplate reduction
- **ModelMapper** - DTO-entity mapping
- **jjwt** - JWT token generation and validation

## Frontend

Separate repo, deployed on Vercel. CORS configured for `localhost:5173` and Vercel origins.

## Code Conventions

- Constructor injection (Lombok `@RequiredArgsConstructor` or explicit constructors)
- Service layer uses interface + impl pattern (e.g., `OrderService` / `OrderServiceImpl`)
- Mappers live in `util/` package
- Entities use Lombok `@Data`; some older entities (Role, User) have manual getters/setters
