# SmartHR API

Backend REST API for SmartHR, an HR management system. It covers employees and their accounts, attendance (clock in / clock out), leave requests and approvals, onboarding tasks, probation tracking, performance evaluations, and company / department / role setup.

## Tech stack

- Java 21
- Spring Boot 4.1 (Web MVC, Data JPA, Security, Actuator)
- PostgreSQL
- JWT authentication (jjwt 0.12)
- Cloudinary for file uploads (leave request attachments)
- Telegram bot integration (announcements)
- springdoc-openapi for Swagger UI
- Lombok
- Maven (wrapper included)

## Getting started

### Prerequisites

- JDK 21
- A running PostgreSQL database

### Configuration

Settings are in `src/main/resources/application.yml`, with the `qa` profile in `application-qa.yml`. Override them with environment variables:

| Variable | Purpose | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL of the database | `jdbc:postgresql://localhost:5432/postgres` |
| `SPRING_DATASOURCE_USERNAME` | Database user | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | set this yourself |
| `DB_POOL_SIZE` / `DB_POOL_MIN_IDLE` | Connection pool size | `5` / `1` |
| `PORT` | HTTP port | `8081` |
| `APP_JWT_SECRET` | Secret used to sign JWTs (at least 32 bytes) | built-in development key |
| `APP_JWT_EXPIRATION_MS` | Access token lifetime | `86400000` (24 hours) |
| `APP_JWT_REFRESH_EXPIRATION_MS` | Refresh token lifetime | `604800000` (7 days) |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | Cloudinary credentials for uploads | placeholders |
| `TELEGRAM_BOT_TOKEN` | Telegram bot token | placeholder |

> **Production:** always set `APP_JWT_SECRET` and the database password. The built-in defaults are for local development only.

The schema is created and updated automatically by Hibernate (`ddl-auto: update`); there are no migration scripts.

### Run locally

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8081`.

### Build and run with Docker

```bash
docker build -t smarthr-api .
docker run -p 8080:8080 -e PORT=8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<db> \
  -e SPRING_DATASOURCE_USERNAME=<user> \
  -e SPRING_DATASOURCE_PASSWORD=<password> \
  -e APP_JWT_SECRET=<secret> \
  smarthr-api
```

The image exposes port 8080, so set `PORT=8080` as shown.

### Tests

```bash
./mvnw test
```

## API documentation

With the app running:

- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- Health check: `http://localhost:8081/actuator/health`

### Postman collection

`SmartHr.postman_collection.json` in the project root has ready-made requests, grouped into **Admin** and **User** folders.

1. In Postman, choose **Import** and select `SmartHr.postman_collection.json`.
2. The collection variable `baseUrl` points to the deployed API. To test locally, change it to `http://localhost:8081`.
3. Run a **Login** request, then paste the returned `token` into the collection's `adminToken` variable or into the request's **Authorization → Bearer Token**.

## Authentication

1. Create the first admin account with `POST /api/auth/init-admin`.
2. Log in with `POST /api/auth/login` to get an access token and a refresh token.
3. Send the access token on every request: `Authorization: Bearer <token>`.
4. When the access token expires the API returns `401`; exchange the refresh token for a new pair with `POST /api/auth/refresh`.

Roles: `ADMIN`, `HR`, `LINE_MANAGER` and `USER`. Each endpoint's allowed roles are enforced in `SecurityConfig` and with `@PreAuthorize` on the controllers.

## Response format

Most endpoints wrap their result like this:

```json
{
  "success": true,
  "message": "Success",
  "data": { }
}
```

On error, `success` is `false`, `message` explains why and `data` is `null`. Status codes: `400` invalid input, `401` not logged in or bad credentials, `403` wrong role, `404` not found, `409` conflict.

List endpoints are paged and accept `page` (from 0), `size`, `sortBy` (an entity field name) and `sortDir` (`asc` or `desc`).

## Endpoints

### Auth: `/api/auth`

| Method | Path | Description |
|---|---|---|
| POST | `/login` | Log in with username and password |
| POST | `/refresh` | Exchange a refresh token for new tokens |
| POST | `/init-admin` | Create an admin account |
| GET | `/me` (also `/profile`, `/info`) | Current user's profile, including today's clock-in state |
| POST | `/change-password` | Change the current user's password |

### Employees: `/api/v1/employees`

| Method | Path | Description | Roles |
|---|---|---|---|
| GET | `/` | List employees | any logged-in user |
| GET | `/{id}` | Get one employee | any logged-in user |
| POST | `/` | Create an employee (default onboarding tasks are assigned) | ADMIN, HR |
| PUT | `/{id}` | Update an employee | ADMIN, HR |
| DELETE | `/{id}` | Delete an employee | ADMIN, HR |
| GET | `/employment-statuses` | List employment statuses | any logged-in user |
| GET | `/probation` | Employees on probation, with start date, end date, remaining days and manager | ADMIN, HR |
| GET | `/probation/summary` | Counts: on probation, ending within 14 days, completed this quarter, pending review | ADMIN, HR |

### Attendance: `/api/v1/attendance`

| Method | Path | Description |
|---|---|---|
| POST | `/clock-in` | Clock in (once per day) |
| POST | `/clock-out` | Clock out |
| GET | `/history/{employeeId}` | Attendance history; `startDate`/`endDate` default to the current month |

### Leave requests: `/api/v1/leave-requests`

| Method | Path | Description | Roles |
|---|---|---|---|
| POST | `/` | Submit a leave request (JSON, or multipart with a `file` attachment) | USER, ADMIN |
| GET | `/my-requests` | Current user's leave requests | USER |
| GET | `/balance` | Current user's remaining leave per leave type | USER, ADMIN, LINE_MANAGER |
| GET | `/pending-approvals` | Requests waiting for a decision (a line manager sees their direct reports) | ADMIN, LINE_MANAGER |
| PUT | `/{id}/status` | Approve or reject a request | ADMIN, LINE_MANAGER |
| GET | `/` | All leave requests | ADMIN |
| GET | `/statuses` | List leave statuses | USER, ADMIN, LINE_MANAGER |
| GET / POST | `/leave-types` | List / create leave types | list: all; create: ADMIN |

### Evaluations

| Method | Path | Description | Roles |
|---|---|---|---|
| POST | `/api/v1/evaluations` | Submit a performance evaluation for an employee | ADMIN, LINE_MANAGER |
| GET | `/api/v1/evaluation-criteria` | Criteria used to build the evaluation form | USER, ADMIN |

### Onboarding

| Method | Path | Description | Roles |
|---|---|---|---|
| GET | `/api/v1/onboarding-tasks/employee/{employeeId}` | An employee's onboarding tasks | USER, ADMIN |
| PATCH | `/api/v1/onboarding-tasks/{taskId}/complete?completed=true` | Tick or untick a task | USER, ADMIN |
| GET / POST | `/api/v1/default-onboarding-tasks` | List / create default task templates (`activeOnly` filter) | list: USER, ADMIN; create: ADMIN |
| GET / PUT / DELETE | `/api/v1/default-onboarding-tasks/{id}` | Get / update / delete a template | get: USER, ADMIN; change: ADMIN |

### Organisation setup

`/api/v1/companies`, `/api/v1/departments` and `/api/v1/roles` each support list (`GET /`), get (`GET /{id}`), create (`POST /`), update (`PUT /{id}`) and delete (`DELETE /{id}`). Reading is open to USER and ADMIN; changes require ADMIN. Roles can also be looked up by name with `GET /api/v1/roles/name/{name}`.

### Announcements: `/api/v1/announcements`

| Method | Path | Description | Roles |
|---|---|---|---|
| GET | `/` | List announcements (paged) | USER, ADMIN |
| GET | `/{id}` | Get one announcement | USER, ADMIN |
| POST | `/` | Create an announcement | ADMIN |
| PUT | `/{id}` | Update an announcement | ADMIN |
| DELETE | `/{id}` | Delete an announcement | ADMIN |
| POST | `/{id}/push` | Post an existing announcement to the Telegram channel | ADMIN |

### Dashboard

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/dashboard` | Aggregated HR statistics |

## Project structure

```
src/main/java/com/smarthr/smarthr/
├── config/        Security setup and startup data initializers
├── controller/    REST endpoints
├── entity/        JPA entities
├── enumeration/   Enums (employment status, leave status, evaluation status, ...)
├── exception/     Custom exceptions and the global exception handler
├── repository/    Spring Data JPA repositories
├── request/       Request bodies
├── response/      Response bodies
├── security/      JWT provider, filter and entry point
└── service/       Business logic
```
