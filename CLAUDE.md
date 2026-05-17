# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Presencia is a Spring Boot 4.0.6 web application (Java 21) for attendance/presence management. It uses server-side rendering with Thymeleaf, JWT-based authentication via Spring Security, and Spring Data JPA for persistence.

## Build & Run Commands

```bash
./mvnw clean install          # Build the project
./mvnw spring-boot:run        # Run the application
./mvnw test                   # Run all tests
./mvnw test -Dtest=ClassName  # Run a single test class
./mvnw test -Dtest=ClassName#methodName  # Run a single test method
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Architecture

- **Web Layer**: Spring MVC controllers + Thymeleaf templates (`src/main/resources/templates/`)
- **Security**: Spring Security with JWT (jjwt 0.12.6) for authentication/authorization
- **Data Layer**: Spring Data JPA repositories with H2 (dev) and PostgreSQL (prod)
- **Validation**: Spring Validation (JSR-380) for bean validation
- **Excel Processing**: Apache POI (poi-ooxml 5.3.0) for reading/writing Excel files
- **Utilities**: Lombok for boilerplate reduction

## Key Technical Decisions

- **Spring Boot 4.0.6** with `spring-boot-starter-webmvc` (not reactive)
- **Thymeleaf** with `thymeleaf-extras-springsecurity6` for security-aware templates
- **Dual database**: H2 with console for development, PostgreSQL for production
- **JWT tokens** for stateless authentication (jjwt library, not Spring's built-in OAuth)
- **Lombok** configured as annotation processor in both compile and test phases

## Package Structure

Base package: `com.example.presencia`

Source: `src/main/java/com/example/presencia/`
Resources: `src/main/resources/` (application.properties, static/, templates/)
Tests: `src/test/java/com/example/presencia/`
