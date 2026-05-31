# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Presencia is a Spring Boot 4.0.6 web application (Java 21) for employee attendance management. It combines:
- **Admin web interface** (MVC + Thymeleaf, session auth) for managing employees, departments, and viewing reports
- **REST API** (JWT auth) for the Flutter mobile app where employees clock in/out via GPS

## Build & Run Commands

```bash
./mvnw clean install          # Build the project
./mvnw spring-boot:run        # Run the application
./mvnw test                   # Run all tests
./mvnw test -Dtest=ClassName  # Run a single test class
./mvnw test -Dtest=ClassName#methodName  # Run a single test method
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Default Credentials (Dev)

- **Admin login**: `admin@presencia.com` / `admin123`
- **H2 Console**: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:presenciadb`, user: `sa`, no password)

## Architecture

- **Dual auth**: Spring Security with 2 filter chains — JWT stateless for `/api/**`, session-based form login for web
- **Jackson 3.x**: Spring Boot 4.0.6 uses `tools.jackson.core` package (NOT `com.fasterxml.jackson`)
- **Geolocation**: Haversine formula validates employee GPS position against department coordinates/radius
- **Export**: Apache POI for Excel export of attendance data

## Package Structure

Base package: `com.example.presencia`

| Package | Purpose |
|---------|---------|
| `model/` | JPA entities (User, Department, Attendance) + `enums/` (Role, AttendanceStatus) |
| `repository/` | Spring Data JPA repositories with custom queries |
| `service/` | Business logic: UserService, DepartmentService, AttendanceService, GeoLocationService, ExportService |
| `security/` | SecurityConfig (dual filter chains) + `jwt/` (JwtTokenProvider, JwtAuthenticationFilter, JwtAuthEntryPoint) |
| `controller/` | MVC controllers for admin web (Thymeleaf pages) |
| `api/` | REST controllers for Flutter mobile (AuthApi, AttendanceApi, ProfileApi) |
| `dto/` | `request/` and `response/` DTOs for API communication |
| `exception/` | GlobalExceptionHandler, ResourceNotFoundException |
| `config/` | DataInitializer (seeds admin user on startup) |

## API Endpoints (Flutter)

| Method | Endpoint | Auth |
|--------|----------|------|
| POST | `/api/auth/login` | Public |
| POST | `/api/auth/refresh` | Public |
| GET | `/api/attendance/today` | JWT |
| POST | `/api/attendance/check-in` | JWT |
| PUT | `/api/attendance/check-out` | JWT |
| GET | `/api/attendance/history?month=&year=` | JWT |
| GET | `/api/profile` | JWT |
| PUT | `/api/profile/password` | JWT |

## Key Configuration

- `app.jwt.secret` / `app.jwt.expiration` / `app.jwt.refresh-expiration` — JWT settings
- `app.attendance.start-hour` — Hour after which check-in is marked LATE (default: 08:00)
- `spring.mvc.hiddenmethod.filter.enabled=true` — Enables PUT/DELETE from HTML forms
- Production profile (`application-prod.properties`): PostgreSQL, env vars for credentials
