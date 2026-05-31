# Presencia Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete attendance management system with Spring Boot MVC (admin web) + REST API (Flutter mobile), including JWT auth, geolocation-based check-in, and data export.

**Architecture:** Single Spring Boot project with two entry points — MVC controllers for admin web (session auth) and REST API controllers for Flutter (JWT auth). Both share the same service layer, repositories, and entities.

**Tech Stack:** Spring Boot 4.0.6, Java 21, Spring Security (dual filter chains), jjwt 0.12.6, Thymeleaf, Spring Data JPA, H2 (dev), PostgreSQL (prod), Apache POI 5.3.0, Lombok.

---

## File Map

```
src/main/java/com/example/presencia/
├── model/
│   ├── enums/Role.java
│   ├── enums/AttendanceStatus.java
│   ├── User.java
│   ├── Department.java
│   └── Attendance.java
├── repository/
│   ├── UserRepository.java
│   ├── DepartmentRepository.java
│   └── AttendanceRepository.java
├── dto/
│   ├── request/LoginRequest.java
│   ├── request/CheckInRequest.java
│   ├── request/CheckOutRequest.java
│   ├── request/ChangePasswordRequest.java
│   └── response/AuthResponse.java
│   └── response/AttendanceResponse.java
│   └── response/ProfileResponse.java
│   └── response/DashboardStats.java
├── exception/
│   ├── ResourceNotFoundException.java
│   └── GlobalExceptionHandler.java
├── security/
│   ├── CustomUserDetailsService.java
│   ├── SecurityConfig.java
│   └── jwt/
│       ├── JwtTokenProvider.java
│       ├── JwtAuthenticationFilter.java
│       └── JwtAuthEntryPoint.java
├── service/
│   ├── UserService.java
│   ├── DepartmentService.java
│   ├── AttendanceService.java
│   ├── GeoLocationService.java
│   └── ExportService.java
├── controller/
│   ├── AuthController.java
│   ├── DashboardController.java
│   ├── EmployeeController.java
│   ├── DepartmentController.java
│   ├── AttendanceController.java
│   └── ExportController.java
├── api/
│   ├── AuthApiController.java
│   ├── AttendanceApiController.java
│   └── ProfileApiController.java
└── config/
    └── DataInitializer.java

src/main/resources/
├── application.properties
├── application-prod.properties
├── templates/
│   ├── layout.html
│   ├── login.html
│   ├── dashboard.html
│   ├── employees/list.html
│   ├── employees/form.html
│   ├── departments/list.html
│   ├── departments/form.html
│   ├── attendance/list.html
│   └── attendance/report.html
└── static/
    └── css/style.css
```

---

### Task 1: Dependencies and Configuration

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.properties`
- Create: `src/main/resources/application-prod.properties`

- [ ] **Step 1: Add JWT and POI dependencies to pom.xml**

Add inside `<dependencies>`:

```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Apache POI for Excel export -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.3.0</version>
</dependency>
```

- [ ] **Step 2: Configure application.properties for dev (H2)**

```properties
spring.application.name=presencia

# H2 Database (dev)
spring.datasource.url=jdbc:h2:mem:presenciadb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# JWT
app.jwt.secret=presencia-secret-key-change-in-production-must-be-at-least-256-bits-long
app.jwt.expiration=86400000
app.jwt.refresh-expiration=604800000

# Attendance
app.attendance.start-hour=08:00

# Thymeleaf
spring.thymeleaf.cache=false
```

- [ ] **Step 3: Create application-prod.properties**

```properties
# PostgreSQL (prod)
spring.datasource.url=jdbc:postgresql://localhost:5432/presenciadb
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.h2.console.enabled=false

app.jwt.secret=${JWT_SECRET}

spring.thymeleaf.cache=true
```

- [ ] **Step 4: Build to verify dependencies resolve**

Run: `mvnw.cmd clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/resources/application.properties src/main/resources/application-prod.properties
git commit -m "feat: add JWT, POI dependencies and configure H2/PostgreSQL profiles"
```

---

### Task 2: Enums and Entities

**Files:**
- Create: `src/main/java/com/example/presencia/model/enums/Role.java`
- Create: `src/main/java/com/example/presencia/model/enums/AttendanceStatus.java`
- Create: `src/main/java/com/example/presencia/model/Department.java`
- Create: `src/main/java/com/example/presencia/model/User.java`
- Create: `src/main/java/com/example/presencia/model/Attendance.java`

- [ ] **Step 1: Create Role enum**

```java
package com.example.presencia.model.enums;

public enum Role {
    ADMIN,
    EMPLOYEE
}
```

- [ ] **Step 2: Create AttendanceStatus enum**

```java
package com.example.presencia.model.enums;

public enum AttendanceStatus {
    PRESENT,
    LATE,
    ABSENT
}
```

- [ ] **Step 3: Create Department entity**

```java
package com.example.presencia.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @NotNull
    @Column(nullable = false)
    private Double latitude;

    @NotNull
    @Column(nullable = false)
    private Double longitude;

    @NotNull
    @Column(nullable = false)
    private Double radius;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 4: Create User entity**

```java
package com.example.presencia.model;

import com.example.presencia.model.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email
    @NotBlank
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @NotBlank
    @Column(nullable = false)
    private String firstName;

    @NotBlank
    @Column(nullable = false)
    private String lastName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Builder.Default
    private boolean active = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 5: Create Attendance entity**

```java
package com.example.presencia.model;

import com.example.presencia.model.enums.AttendanceStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    @Column(nullable = false)
    private LocalDateTime checkIn;

    private LocalDateTime checkOut;

    @Column(nullable = false)
    private Double checkInLatitude;

    @Column(nullable = false)
    private Double checkInLongitude;

    private Double checkOutLatitude;

    private Double checkOutLongitude;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    @NotNull
    @Column(nullable = false)
    private LocalDate date;

    private String notes;
}
```

- [ ] **Step 6: Build to verify entities compile**

Run: `mvnw.cmd clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/presencia/model/
git commit -m "feat: add domain entities (User, Department, Attendance) and enums"
```

---

### Task 3: Repositories

**Files:**
- Create: `src/main/java/com/example/presencia/repository/UserRepository.java`
- Create: `src/main/java/com/example/presencia/repository/DepartmentRepository.java`
- Create: `src/main/java/com/example/presencia/repository/AttendanceRepository.java`

- [ ] **Step 1: Create UserRepository**

```java
package com.example.presencia.repository;

import com.example.presencia.model.User;
import com.example.presencia.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByDepartmentId(Long departmentId);

    long countByActiveTrue();
}
```

- [ ] **Step 2: Create DepartmentRepository**

```java
package com.example.presencia.repository;

import com.example.presencia.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByName(String name);

    boolean existsByName(String name);
}
```

- [ ] **Step 3: Create AttendanceRepository**

```java
package com.example.presencia.repository;

import com.example.presencia.model.Attendance;
import com.example.presencia.model.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByUserIdAndDate(Long userId, LocalDate date);

    List<Attendance> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate start, LocalDate end);

    List<Attendance> findByDateOrderByCheckInDesc(LocalDate date);

    List<Attendance> findByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end);

    long countByDateAndStatus(LocalDate date, AttendanceStatus status);

    @Query("SELECT a FROM Attendance a WHERE a.user.department.id = :deptId AND a.date = :date")
    List<Attendance> findByDepartmentAndDate(@Param("deptId") Long departmentId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.date = :date")
    long countByDate(@Param("date") LocalDate date);
}
```

- [ ] **Step 4: Build to verify**

Run: `mvnw.cmd clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/presencia/repository/
git commit -m "feat: add JPA repositories with custom queries"
```

---

### Task 4: DTOs

**Files:**
- Create: `src/main/java/com/example/presencia/dto/request/LoginRequest.java`
- Create: `src/main/java/com/example/presencia/dto/request/CheckInRequest.java`
- Create: `src/main/java/com/example/presencia/dto/request/CheckOutRequest.java`
- Create: `src/main/java/com/example/presencia/dto/request/ChangePasswordRequest.java`
- Create: `src/main/java/com/example/presencia/dto/response/AuthResponse.java`
- Create: `src/main/java/com/example/presencia/dto/response/AttendanceResponse.java`
- Create: `src/main/java/com/example/presencia/dto/response/ProfileResponse.java`
- Create: `src/main/java/com/example/presencia/dto/response/DashboardStats.java`

- [ ] **Step 1: Create request DTOs**

`LoginRequest.java`:
```java
package com.example.presencia.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @Email @NotBlank
    private String email;
    @NotBlank
    private String password;
}
```

`CheckInRequest.java`:
```java
package com.example.presencia.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckInRequest {
    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;
}
```

`CheckOutRequest.java`:
```java
package com.example.presencia.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckOutRequest {
    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;
}
```

`ChangePasswordRequest.java`:
```java
package com.example.presencia.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank
    private String currentPassword;
    @NotBlank @Size(min = 6)
    private String newPassword;
}
```

- [ ] **Step 2: Create response DTOs**

`AuthResponse.java`:
```java
package com.example.presencia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}
```

`AttendanceResponse.java`:
```java
package com.example.presencia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class AttendanceResponse {
    private Long id;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private String status;
    private LocalDate date;
    private String notes;
}
```

`ProfileResponse.java`:
```java
package com.example.presencia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ProfileResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String departmentName;
}
```

`DashboardStats.java`:
```java
package com.example.presencia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DashboardStats {
    private long totalEmployees;
    private long presentToday;
    private long lateToday;
    private long absentToday;
}
```

- [ ] **Step 3: Build to verify**

Run: `mvnw.cmd clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/presencia/dto/
git commit -m "feat: add request/response DTOs"
```

---

### Task 5: Exception Handling

**Files:**
- Create: `src/main/java/com/example/presencia/exception/ResourceNotFoundException.java`
- Create: `src/main/java/com/example/presencia/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Create ResourceNotFoundException**

```java
package com.example.presencia.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: Create GlobalExceptionHandler**

```java
package com.example.presencia.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/presencia/exception/
git commit -m "feat: add exception handling (ResourceNotFoundException, GlobalExceptionHandler)"
```

---

### Task 6: Security — JWT Infrastructure

**Files:**
- Create: `src/main/java/com/example/presencia/security/jwt/JwtTokenProvider.java`
- Create: `src/main/java/com/example/presencia/security/jwt/JwtAuthenticationFilter.java`
- Create: `src/main/java/com/example/presencia/security/jwt/JwtAuthEntryPoint.java`
- Create: `src/main/java/com/example/presencia/security/CustomUserDetailsService.java`
- Create: `src/main/java/com/example/presencia/security/SecurityConfig.java`

- [ ] **Step 1: Create JwtTokenProvider**

```java
package com.example.presencia.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expiration;
    private final long refreshExpiration;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") long expiration,
            @Value("${app.jwt.refresh-expiration}") long refreshExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateToken(String email) {
        return buildToken(email, expiration);
    }

    public String generateRefreshToken(String email) {
        return buildToken(email, refreshExpiration);
    }

    private String buildToken(String subject, long expirationMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

- [ ] **Step 2: Create JwtAuthenticationFilter**

```java
package com.example.presencia.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmailFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 3: Create JwtAuthEntryPoint**

```java
package com.example.presencia.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        new ObjectMapper().writeValue(response.getOutputStream(),
                Map.of("error", "Non autorise", "message", authException.getMessage()));
    }
}
```

- [ ] **Step 4: Create CustomUserDetailsService**

```java
package com.example.presencia.security;

import com.example.presencia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouve: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isActive(),
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
```

- [ ] **Step 5: Create SecurityConfig with dual filter chains**

```java
package com.example.presencia.security;

import com.example.presencia.security.jwt.JwtAuthEntryPoint;
import com.example.presencia.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Chain 1: API REST — JWT stateless
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthEntryPoint))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Chain 2: Web MVC — Session + form login
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/h2-console/**").permitAll()
                .anyRequest().hasRole("ADMIN")
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
```

- [ ] **Step 6: Build to verify**

Run: `mvnw.cmd clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/presencia/security/
git commit -m "feat: add Spring Security with dual filter chains (JWT for API, sessions for web)"
```

---

### Task 7: Services

**Files:**
- Create: `src/main/java/com/example/presencia/service/GeoLocationService.java`
- Create: `src/main/java/com/example/presencia/service/UserService.java`
- Create: `src/main/java/com/example/presencia/service/DepartmentService.java`
- Create: `src/main/java/com/example/presencia/service/AttendanceService.java`
- Create: `src/main/java/com/example/presencia/service/ExportService.java`

- [ ] **Step 1: Create GeoLocationService**

```java
package com.example.presencia.service;

import org.springframework.stereotype.Service;

@Service
public class GeoLocationService {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    /**
     * Calcule la distance en metres entre deux points GPS (formule Haversine).
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Verifie si la position est dans le rayon autorise.
     */
    public boolean isWithinRadius(double userLat, double userLon,
                                   double siteLat, double siteLon, double radiusMeters) {
        return calculateDistance(userLat, userLon, siteLat, siteLon) <= radiusMeters;
    }
}
```

- [ ] **Step 2: Create UserService**

```java
package com.example.presencia.service;

import com.example.presencia.exception.ResourceNotFoundException;
import com.example.presencia.model.User;
import com.example.presencia.model.enums.Role;
import com.example.presencia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findEmployees() {
        return userRepository.findByRole(Role.EMPLOYEE);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouve: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouve: " + email));
    }

    public User create(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email deja utilise: " + user.getEmail());
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User update(Long id, User updated) {
        User user = findById(id);
        user.setFirstName(updated.getFirstName());
        user.setLastName(updated.getLastName());
        user.setEmail(updated.getEmail());
        user.setRole(updated.getRole());
        user.setDepartment(updated.getDepartment());
        user.setActive(updated.isActive());
        return userRepository.save(user);
    }

    public void delete(Long id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mot de passe actuel incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public long countActiveEmployees() {
        return userRepository.countByActiveTrue();
    }
}
```

- [ ] **Step 3: Create DepartmentService**

```java
package com.example.presencia.service;

import com.example.presencia.exception.ResourceNotFoundException;
import com.example.presencia.model.Department;
import com.example.presencia.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public Department findById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departement non trouve: " + id));
    }

    public Department create(Department department) {
        if (departmentRepository.existsByName(department.getName())) {
            throw new IllegalArgumentException("Nom de departement deja utilise: " + department.getName());
        }
        return departmentRepository.save(department);
    }

    public Department update(Long id, Department updated) {
        Department dept = findById(id);
        dept.setName(updated.getName());
        dept.setDescription(updated.getDescription());
        dept.setLatitude(updated.getLatitude());
        dept.setLongitude(updated.getLongitude());
        dept.setRadius(updated.getRadius());
        return departmentRepository.save(dept);
    }

    public void delete(Long id) {
        Department dept = findById(id);
        departmentRepository.delete(dept);
    }
}
```

- [ ] **Step 4: Create AttendanceService**

```java
package com.example.presencia.service;

import com.example.presencia.dto.response.DashboardStats;
import com.example.presencia.exception.ResourceNotFoundException;
import com.example.presencia.model.Attendance;
import com.example.presencia.model.Department;
import com.example.presencia.model.User;
import com.example.presencia.model.enums.AttendanceStatus;
import com.example.presencia.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserService userService;
    private final GeoLocationService geoLocationService;

    @Value("${app.attendance.start-hour}")
    private String startHour;

    public Attendance checkIn(String email, Double latitude, Double longitude) {
        User user = userService.findByEmail(email);
        LocalDate today = LocalDate.now();

        if (attendanceRepository.findByUserIdAndDate(user.getId(), today).isPresent()) {
            throw new IllegalArgumentException("Vous avez deja pointe aujourd'hui");
        }

        Department dept = user.getDepartment();
        if (dept == null) {
            throw new IllegalArgumentException("Aucun departement assigne");
        }

        if (!geoLocationService.isWithinRadius(latitude, longitude,
                dept.getLatitude(), dept.getLongitude(), dept.getRadius())) {
            throw new IllegalArgumentException("Vous n'etes pas dans le perimetre autorise");
        }

        LocalTime limit = LocalTime.parse(startHour);
        AttendanceStatus status = LocalTime.now().isAfter(limit)
                ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;

        Attendance attendance = Attendance.builder()
                .user(user)
                .checkIn(LocalDateTime.now())
                .checkInLatitude(latitude)
                .checkInLongitude(longitude)
                .status(status)
                .date(today)
                .build();

        return attendanceRepository.save(attendance);
    }

    public Attendance checkOut(String email, Double latitude, Double longitude) {
        User user = userService.findByEmail(email);
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByUserIdAndDate(user.getId(), today)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun pointage trouve pour aujourd'hui"));

        if (attendance.getCheckOut() != null) {
            throw new IllegalArgumentException("Vous avez deja pointe la sortie");
        }

        attendance.setCheckOut(LocalDateTime.now());
        attendance.setCheckOutLatitude(latitude);
        attendance.setCheckOutLongitude(longitude);

        return attendanceRepository.save(attendance);
    }

    public Attendance getTodayAttendance(String email) {
        User user = userService.findByEmail(email);
        return attendanceRepository.findByUserIdAndDate(user.getId(), LocalDate.now())
                .orElse(null);
    }

    public List<Attendance> getHistory(String email, int month, int year) {
        User user = userService.findByEmail(email);
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return attendanceRepository.findByUserIdAndDateBetweenOrderByDateDesc(user.getId(), start, end);
    }

    public List<Attendance> findByDate(LocalDate date) {
        return attendanceRepository.findByDateOrderByCheckInDesc(date);
    }

    public List<Attendance> findByDateRange(LocalDate start, LocalDate end) {
        return attendanceRepository.findByDateBetweenOrderByDateDesc(start, end);
    }

    public DashboardStats getDashboardStats() {
        LocalDate today = LocalDate.now();
        return DashboardStats.builder()
                .totalEmployees(userService.countActiveEmployees())
                .presentToday(attendanceRepository.countByDateAndStatus(today, AttendanceStatus.PRESENT))
                .lateToday(attendanceRepository.countByDateAndStatus(today, AttendanceStatus.LATE))
                .absentToday(userService.countActiveEmployees()
                        - attendanceRepository.countByDate(today))
                .build();
    }
}
```

- [ ] **Step 5: Create ExportService**

```java
package com.example.presencia.service;

import com.example.presencia.model.Attendance;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final AttendanceService attendanceService;

    public byte[] exportAttendanceToExcel(LocalDate start, LocalDate end) throws IOException {
        List<Attendance> attendances = attendanceService.findByDateRange(start, end);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Pointages");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // Header
            Row header = sheet.createRow(0);
            String[] columns = {"Date", "Employe", "Email", "Departement", "Entree", "Sortie", "Statut"};
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data
            int rowIdx = 1;
            for (Attendance a : attendances) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(a.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                row.createCell(1).setCellValue(a.getUser().getFirstName() + " " + a.getUser().getLastName());
                row.createCell(2).setCellValue(a.getUser().getEmail());
                row.createCell(3).setCellValue(
                        a.getUser().getDepartment() != null ? a.getUser().getDepartment().getName() : "");
                row.createCell(4).setCellValue(a.getCheckIn().format(dtf));
                row.createCell(5).setCellValue(a.getCheckOut() != null ? a.getCheckOut().format(dtf) : "");
                row.createCell(6).setCellValue(a.getStatus().name());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
```

- [ ] **Step 6: Build to verify**

Run: `mvnw.cmd clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/presencia/service/
git commit -m "feat: add service layer (User, Department, Attendance, GeoLocation, Export)"
```

---

### Task 8: API Controllers (REST for Flutter)

**Files:**
- Create: `src/main/java/com/example/presencia/api/AuthApiController.java`
- Create: `src/main/java/com/example/presencia/api/AttendanceApiController.java`
- Create: `src/main/java/com/example/presencia/api/ProfileApiController.java`

- [ ] **Step 1: Create AuthApiController**

```java
package com.example.presencia.api;

import com.example.presencia.dto.request.LoginRequest;
import com.example.presencia.dto.response.AuthResponse;
import com.example.presencia.model.User;
import com.example.presencia.security.jwt.JwtTokenProvider;
import com.example.presencia.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userService.findByEmail(request.getEmail());

        String token = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody java.util.Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(401).build();
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User user = userService.findByEmail(email);

        String newToken = jwtTokenProvider.generateToken(email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        return ResponseEntity.ok(AuthResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build());
    }
}
```

- [ ] **Step 2: Create AttendanceApiController**

```java
package com.example.presencia.api;

import com.example.presencia.dto.request.CheckInRequest;
import com.example.presencia.dto.request.CheckOutRequest;
import com.example.presencia.dto.response.AttendanceResponse;
import com.example.presencia.model.Attendance;
import com.example.presencia.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceApiController {

    private final AttendanceService attendanceService;

    @GetMapping("/today")
    public ResponseEntity<AttendanceResponse> today(@AuthenticationPrincipal UserDetails userDetails) {
        Attendance attendance = attendanceService.getTodayAttendance(userDetails.getUsername());
        if (attendance == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(toResponse(attendance));
    }

    @PostMapping("/check-in")
    public ResponseEntity<AttendanceResponse> checkIn(@AuthenticationPrincipal UserDetails userDetails,
                                                       @Valid @RequestBody CheckInRequest request) {
        Attendance attendance = attendanceService.checkIn(
                userDetails.getUsername(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(toResponse(attendance));
    }

    @PutMapping("/check-out")
    public ResponseEntity<AttendanceResponse> checkOut(@AuthenticationPrincipal UserDetails userDetails,
                                                        @Valid @RequestBody CheckOutRequest request) {
        Attendance attendance = attendanceService.checkOut(
                userDetails.getUsername(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(toResponse(attendance));
    }

    @GetMapping("/history")
    public ResponseEntity<List<AttendanceResponse>> history(@AuthenticationPrincipal UserDetails userDetails,
                                                             @RequestParam int month,
                                                             @RequestParam int year) {
        List<Attendance> list = attendanceService.getHistory(userDetails.getUsername(), month, year);
        return ResponseEntity.ok(list.stream().map(this::toResponse).toList());
    }

    private AttendanceResponse toResponse(Attendance a) {
        return AttendanceResponse.builder()
                .id(a.getId())
                .checkIn(a.getCheckIn())
                .checkOut(a.getCheckOut())
                .status(a.getStatus().name())
                .date(a.getDate())
                .notes(a.getNotes())
                .build();
    }
}
```

- [ ] **Step 3: Create ProfileApiController**

```java
package com.example.presencia.api;

import com.example.presencia.dto.request.ChangePasswordRequest;
import com.example.presencia.dto.response.ProfileResponse;
import com.example.presencia.model.User;
import com.example.presencia.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileApiController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .build());
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(),
                request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Mot de passe modifie avec succes"));
    }
}
```

- [ ] **Step 4: Build to verify**

Run: `mvnw.cmd clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/presencia/api/
git commit -m "feat: add REST API controllers (auth, attendance, profile) for Flutter"
```

---

### Task 9: MVC Controllers (Admin Web)

**Files:**
- Create: `src/main/java/com/example/presencia/controller/AuthController.java`
- Create: `src/main/java/com/example/presencia/controller/DashboardController.java`
- Create: `src/main/java/com/example/presencia/controller/EmployeeController.java`
- Create: `src/main/java/com/example/presencia/controller/DepartmentController.java`
- Create: `src/main/java/com/example/presencia/controller/AttendanceController.java`
- Create: `src/main/java/com/example/presencia/controller/ExportController.java`

- [ ] **Step 1: Create AuthController**

```java
package com.example.presencia.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
```

- [ ] **Step 2: Create DashboardController**

```java
package com.example.presencia.controller;

import com.example.presencia.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final AttendanceService attendanceService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", attendanceService.getDashboardStats());
        model.addAttribute("todayAttendances", attendanceService.findByDate(LocalDate.now()));
        return "dashboard";
    }
}
```

- [ ] **Step 3: Create EmployeeController**

```java
package com.example.presencia.controller;

import com.example.presencia.model.User;
import com.example.presencia.model.enums.Role;
import com.example.presencia.service.DepartmentService;
import com.example.presencia.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final UserService userService;
    private final DepartmentService departmentService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("employees", userService.findAll());
        return "employees/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("roles", Role.values());
        return "employees/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute User user, BindingResult result,
                          Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("departments", departmentService.findAll());
            model.addAttribute("roles", Role.values());
            return "employees/form";
        }
        userService.create(user);
        redirectAttributes.addFlashAttribute("success", "Employe cree avec succes");
        return "redirect:/employees";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.findById(id));
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("roles", Role.values());
        return "employees/form";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute User user,
                          BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("departments", departmentService.findAll());
            model.addAttribute("roles", Role.values());
            return "employees/form";
        }
        userService.update(id, user);
        redirectAttributes.addFlashAttribute("success", "Employe modifie avec succes");
        return "redirect:/employees";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Employe supprime avec succes");
        return "redirect:/employees";
    }
}
```

- [ ] **Step 4: Create DepartmentController**

```java
package com.example.presencia.controller;

import com.example.presencia.model.Department;
import com.example.presencia.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("departments", departmentService.findAll());
        return "departments/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("department", new Department());
        return "departments/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute Department department, BindingResult result,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "departments/form";
        }
        departmentService.create(department);
        redirectAttributes.addFlashAttribute("success", "Departement cree avec succes");
        return "redirect:/departments";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("department", departmentService.findById(id));
        return "departments/form";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute Department department,
                          BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "departments/form";
        }
        departmentService.update(id, department);
        redirectAttributes.addFlashAttribute("success", "Departement modifie avec succes");
        return "redirect:/departments";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        departmentService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Departement supprime avec succes");
        return "redirect:/departments";
    }
}
```

- [ ] **Step 5: Create AttendanceController**

```java
package com.example.presencia.controller;

import com.example.presencia.service.AttendanceService;
import com.example.presencia.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final DepartmentService departmentService;

    @GetMapping
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        Model model) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        model.addAttribute("attendances", attendanceService.findByDate(targetDate));
        model.addAttribute("selectedDate", targetDate);
        return "attendance/list";
    }

    @GetMapping("/report")
    public String report(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                          Model model) {
        LocalDate s = start != null ? start : LocalDate.now().withDayOfMonth(1);
        LocalDate e = end != null ? end : LocalDate.now();
        model.addAttribute("attendances", attendanceService.findByDateRange(s, e));
        model.addAttribute("startDate", s);
        model.addAttribute("endDate", e);
        model.addAttribute("departments", departmentService.findAll());
        return "attendance/report";
    }
}
```

- [ ] **Step 6: Create ExportController**

```java
package com.example.presencia.controller;

import com.example.presencia.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDate;

@Controller
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/attendance/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) throws IOException {

        byte[] excelBytes = exportService.exportAttendanceToExcel(start, end);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pointages.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
```

- [ ] **Step 7: Build to verify**

Run: `mvnw.cmd clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/example/presencia/controller/
git commit -m "feat: add MVC controllers (dashboard, employees, departments, attendance, export)"
```

---

### Task 10: Thymeleaf Templates

**Files:**
- Create: `src/main/resources/templates/layout.html`
- Create: `src/main/resources/templates/login.html`
- Create: `src/main/resources/templates/dashboard.html`
- Create: `src/main/resources/templates/employees/list.html`
- Create: `src/main/resources/templates/employees/form.html`
- Create: `src/main/resources/templates/departments/list.html`
- Create: `src/main/resources/templates/departments/form.html`
- Create: `src/main/resources/templates/attendance/list.html`
- Create: `src/main/resources/templates/attendance/report.html`
- Create: `src/main/resources/static/css/style.css`

- [ ] **Step 1: Create layout.html (base template with Thymeleaf layout dialect alternative using fragments)**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:fragment="head(title)">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${title}">Presencia</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <link rel="stylesheet" th:href="@{/css/style.css}">
</head>
<body>
<nav th:fragment="navbar" class="navbar navbar-expand-lg navbar-dark bg-dark">
    <div class="container-fluid">
        <a class="navbar-brand" th:href="@{/dashboard}">Presencia</a>
        <div class="collapse navbar-collapse">
            <ul class="navbar-nav me-auto">
                <li class="nav-item">
                    <a class="nav-link" th:href="@{/dashboard}">Dashboard</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" th:href="@{/employees}">Employes</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" th:href="@{/departments}">Departements</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" th:href="@{/attendance}">Pointages</a>
                </li>
            </ul>
            <form th:action="@{/logout}" method="post" class="d-flex">
                <span class="navbar-text me-3" sec:authentication="name">User</span>
                <button type="submit" class="btn btn-outline-light btn-sm">Deconnexion</button>
            </form>
        </div>
    </div>
</nav>
</body>
</html>
```

- [ ] **Step 2: Create login.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{layout :: head('Connexion - Presencia')}"></head>
<body class="bg-light">
<div class="container">
    <div class="row justify-content-center mt-5">
        <div class="col-md-4">
            <div class="card shadow">
                <div class="card-body">
                    <h3 class="card-title text-center mb-4">Presencia</h3>
                    <div th:if="${param.error}" class="alert alert-danger">Email ou mot de passe incorrect</div>
                    <div th:if="${param.logout}" class="alert alert-success">Deconnexion reussie</div>
                    <form th:action="@{/login}" method="post">
                        <div class="mb-3">
                            <label for="username" class="form-label">Email</label>
                            <input type="email" class="form-control" id="username" name="username" required>
                        </div>
                        <div class="mb-3">
                            <label for="password" class="form-label">Mot de passe</label>
                            <input type="password" class="form-control" id="password" name="password" required>
                        </div>
                        <button type="submit" class="btn btn-primary w-100">Se connecter</button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
```

- [ ] **Step 3: Create dashboard.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:replace="~{layout :: head('Dashboard - Presencia')}"></head>
<body>
<nav th:replace="~{layout :: navbar}"></nav>
<div class="container-fluid mt-4">
    <h2>Tableau de bord</h2>
    <div class="row mt-3">
        <div class="col-md-3">
            <div class="card text-white bg-primary">
                <div class="card-body">
                    <h5 class="card-title">Total Employes</h5>
                    <h2 th:text="${stats.totalEmployees}">0</h2>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card text-white bg-success">
                <div class="card-body">
                    <h5 class="card-title">Presents</h5>
                    <h2 th:text="${stats.presentToday}">0</h2>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card text-white bg-warning">
                <div class="card-body">
                    <h5 class="card-title">En retard</h5>
                    <h2 th:text="${stats.lateToday}">0</h2>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card text-white bg-danger">
                <div class="card-body">
                    <h5 class="card-title">Absents</h5>
                    <h2 th:text="${stats.absentToday}">0</h2>
                </div>
            </div>
        </div>
    </div>

    <div class="card mt-4">
        <div class="card-header">Derniers pointages aujourd'hui</div>
        <div class="card-body">
            <table class="table table-striped">
                <thead>
                    <tr>
                        <th>Employe</th>
                        <th>Departement</th>
                        <th>Entree</th>
                        <th>Sortie</th>
                        <th>Statut</th>
                    </tr>
                </thead>
                <tbody>
                    <tr th:each="a : ${todayAttendances}">
                        <td th:text="${a.user.firstName + ' ' + a.user.lastName}"></td>
                        <td th:text="${a.user.department != null ? a.user.department.name : '-'}"></td>
                        <td th:text="${#temporals.format(a.checkIn, 'HH:mm')}"></td>
                        <td th:text="${a.checkOut != null ? #temporals.format(a.checkOut, 'HH:mm') : '-'}"></td>
                        <td>
                            <span class="badge"
                                  th:classappend="${a.status.name() == 'PRESENT' ? 'bg-success' : (a.status.name() == 'LATE' ? 'bg-warning' : 'bg-danger')}"
                                  th:text="${a.status}"></span>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

- [ ] **Step 4: Create employees/list.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:replace="~{layout :: head('Employes - Presencia')}"></head>
<body>
<nav th:replace="~{layout :: navbar}"></nav>
<div class="container-fluid mt-4">
    <div class="d-flex justify-content-between align-items-center">
        <h2>Employes</h2>
        <a th:href="@{/employees/new}" class="btn btn-primary">Nouvel employe</a>
    </div>
    <div th:if="${success}" class="alert alert-success mt-2" th:text="${success}"></div>
    <table class="table table-striped mt-3">
        <thead>
            <tr>
                <th>Nom</th>
                <th>Email</th>
                <th>Role</th>
                <th>Departement</th>
                <th>Actif</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="e : ${employees}">
                <td th:text="${e.firstName + ' ' + e.lastName}"></td>
                <td th:text="${e.email}"></td>
                <td th:text="${e.role}"></td>
                <td th:text="${e.department != null ? e.department.name : '-'}"></td>
                <td><span class="badge" th:classappend="${e.active ? 'bg-success' : 'bg-secondary'}" th:text="${e.active ? 'Oui' : 'Non'}"></span></td>
                <td>
                    <a th:href="@{/employees/{id}/edit(id=${e.id})}" class="btn btn-sm btn-warning">Modifier</a>
                    <form th:action="@{/employees/{id}(id=${e.id})}" method="post" style="display:inline">
                        <input type="hidden" name="_method" value="DELETE">
                        <button type="submit" class="btn btn-sm btn-danger" onclick="return confirm('Confirmer la suppression ?')">Supprimer</button>
                    </form>
                </td>
            </tr>
        </tbody>
    </table>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

- [ ] **Step 5: Create employees/form.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:replace="~{layout :: head('Employe - Presencia')}"></head>
<body>
<nav th:replace="~{layout :: navbar}"></nav>
<div class="container mt-4">
    <h2 th:text="${user.id != null ? 'Modifier employe' : 'Nouvel employe'}"></h2>
    <form th:action="${user.id != null ? '/employees/' + user.id : '/employees'}"
          th:method="${user.id != null ? 'put' : 'post'}"
          th:object="${user}" class="mt-3">
        <div class="row">
            <div class="col-md-6 mb-3">
                <label class="form-label">Prenom</label>
                <input type="text" class="form-control" th:field="*{firstName}" required>
            </div>
            <div class="col-md-6 mb-3">
                <label class="form-label">Nom</label>
                <input type="text" class="form-control" th:field="*{lastName}" required>
            </div>
        </div>
        <div class="mb-3">
            <label class="form-label">Email</label>
            <input type="email" class="form-control" th:field="*{email}" required>
        </div>
        <div class="mb-3" th:if="${user.id == null}">
            <label class="form-label">Mot de passe</label>
            <input type="password" class="form-control" th:field="*{password}" required>
        </div>
        <div class="row">
            <div class="col-md-6 mb-3">
                <label class="form-label">Role</label>
                <select class="form-select" th:field="*{role}">
                    <option th:each="r : ${roles}" th:value="${r}" th:text="${r}"></option>
                </select>
            </div>
            <div class="col-md-6 mb-3">
                <label class="form-label">Departement</label>
                <select class="form-select" th:field="*{department.id}">
                    <option value="">-- Aucun --</option>
                    <option th:each="d : ${departments}" th:value="${d.id}" th:text="${d.name}"></option>
                </select>
            </div>
        </div>
        <div class="mb-3 form-check" th:if="${user.id != null}">
            <input type="checkbox" class="form-check-input" th:field="*{active}">
            <label class="form-check-label">Actif</label>
        </div>
        <button type="submit" class="btn btn-primary">Enregistrer</button>
        <a th:href="@{/employees}" class="btn btn-secondary">Annuler</a>
    </form>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

- [ ] **Step 6: Create departments/list.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:replace="~{layout :: head('Departements - Presencia')}"></head>
<body>
<nav th:replace="~{layout :: navbar}"></nav>
<div class="container-fluid mt-4">
    <div class="d-flex justify-content-between align-items-center">
        <h2>Departements</h2>
        <a th:href="@{/departments/new}" class="btn btn-primary">Nouveau departement</a>
    </div>
    <div th:if="${success}" class="alert alert-success mt-2" th:text="${success}"></div>
    <table class="table table-striped mt-3">
        <thead>
            <tr>
                <th>Nom</th>
                <th>Description</th>
                <th>Latitude</th>
                <th>Longitude</th>
                <th>Rayon (m)</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="d : ${departments}">
                <td th:text="${d.name}"></td>
                <td th:text="${d.description}"></td>
                <td th:text="${d.latitude}"></td>
                <td th:text="${d.longitude}"></td>
                <td th:text="${d.radius}"></td>
                <td>
                    <a th:href="@{/departments/{id}/edit(id=${d.id})}" class="btn btn-sm btn-warning">Modifier</a>
                    <form th:action="@{/departments/{id}(id=${d.id})}" method="post" style="display:inline">
                        <input type="hidden" name="_method" value="DELETE">
                        <button type="submit" class="btn btn-sm btn-danger" onclick="return confirm('Confirmer la suppression ?')">Supprimer</button>
                    </form>
                </td>
            </tr>
        </tbody>
    </table>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

- [ ] **Step 7: Create departments/form.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:replace="~{layout :: head('Departement - Presencia')}"></head>
<body>
<nav th:replace="~{layout :: navbar}"></nav>
<div class="container mt-4">
    <h2 th:text="${department.id != null ? 'Modifier departement' : 'Nouveau departement'}"></h2>
    <form th:action="${department.id != null ? '/departments/' + department.id : '/departments'}"
          th:method="${department.id != null ? 'put' : 'post'}"
          th:object="${department}" class="mt-3">
        <div class="mb-3">
            <label class="form-label">Nom</label>
            <input type="text" class="form-control" th:field="*{name}" required>
        </div>
        <div class="mb-3">
            <label class="form-label">Description</label>
            <textarea class="form-control" th:field="*{description}" rows="3"></textarea>
        </div>
        <div class="row">
            <div class="col-md-4 mb-3">
                <label class="form-label">Latitude</label>
                <input type="number" step="any" class="form-control" th:field="*{latitude}" required>
            </div>
            <div class="col-md-4 mb-3">
                <label class="form-label">Longitude</label>
                <input type="number" step="any" class="form-control" th:field="*{longitude}" required>
            </div>
            <div class="col-md-4 mb-3">
                <label class="form-label">Rayon (metres)</label>
                <input type="number" step="any" class="form-control" th:field="*{radius}" required>
            </div>
        </div>
        <button type="submit" class="btn btn-primary">Enregistrer</button>
        <a th:href="@{/departments}" class="btn btn-secondary">Annuler</a>
    </form>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

- [ ] **Step 8: Create attendance/list.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:replace="~{layout :: head('Pointages - Presencia')}"></head>
<body>
<nav th:replace="~{layout :: navbar}"></nav>
<div class="container-fluid mt-4">
    <h2>Pointages</h2>
    <form class="row g-3 mt-2" method="get" th:action="@{/attendance}">
        <div class="col-auto">
            <input type="date" class="form-control" name="date" th:value="${selectedDate}">
        </div>
        <div class="col-auto">
            <button type="submit" class="btn btn-primary">Filtrer</button>
        </div>
        <div class="col-auto">
            <a th:href="@{/attendance/report}" class="btn btn-outline-secondary">Rapport</a>
        </div>
    </form>
    <table class="table table-striped mt-3">
        <thead>
            <tr>
                <th>Employe</th>
                <th>Departement</th>
                <th>Entree</th>
                <th>Sortie</th>
                <th>Statut</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="a : ${attendances}">
                <td th:text="${a.user.firstName + ' ' + a.user.lastName}"></td>
                <td th:text="${a.user.department != null ? a.user.department.name : '-'}"></td>
                <td th:text="${#temporals.format(a.checkIn, 'HH:mm')}"></td>
                <td th:text="${a.checkOut != null ? #temporals.format(a.checkOut, 'HH:mm') : '-'}"></td>
                <td>
                    <span class="badge"
                          th:classappend="${a.status.name() == 'PRESENT' ? 'bg-success' : (a.status.name() == 'LATE' ? 'bg-warning' : 'bg-danger')}"
                          th:text="${a.status}"></span>
                </td>
            </tr>
        </tbody>
    </table>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

- [ ] **Step 9: Create attendance/report.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:replace="~{layout :: head('Rapport - Presencia')}"></head>
<body>
<nav th:replace="~{layout :: navbar}"></nav>
<div class="container-fluid mt-4">
    <h2>Rapport de pointage</h2>
    <form class="row g-3 mt-2" method="get" th:action="@{/attendance/report}">
        <div class="col-auto">
            <label class="form-label">Du</label>
            <input type="date" class="form-control" name="start" th:value="${startDate}">
        </div>
        <div class="col-auto">
            <label class="form-label">Au</label>
            <input type="date" class="form-control" name="end" th:value="${endDate}">
        </div>
        <div class="col-auto align-self-end">
            <button type="submit" class="btn btn-primary">Filtrer</button>
        </div>
        <div class="col-auto align-self-end">
            <a th:href="@{/export/attendance/excel(start=${startDate}, end=${endDate})}" class="btn btn-success">
                <i class="bi bi-file-earmark-excel"></i> Export Excel
            </a>
        </div>
    </form>
    <table class="table table-striped mt-3">
        <thead>
            <tr>
                <th>Date</th>
                <th>Employe</th>
                <th>Departement</th>
                <th>Entree</th>
                <th>Sortie</th>
                <th>Statut</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="a : ${attendances}">
                <td th:text="${#temporals.format(a.date, 'dd/MM/yyyy')}"></td>
                <td th:text="${a.user.firstName + ' ' + a.user.lastName}"></td>
                <td th:text="${a.user.department != null ? a.user.department.name : '-'}"></td>
                <td th:text="${#temporals.format(a.checkIn, 'HH:mm')}"></td>
                <td th:text="${a.checkOut != null ? #temporals.format(a.checkOut, 'HH:mm') : '-'}"></td>
                <td>
                    <span class="badge"
                          th:classappend="${a.status.name() == 'PRESENT' ? 'bg-success' : (a.status.name() == 'LATE' ? 'bg-warning' : 'bg-danger')}"
                          th:text="${a.status}"></span>
                </td>
            </tr>
        </tbody>
    </table>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

- [ ] **Step 10: Create style.css**

```css
body {
    background-color: #f8f9fa;
}

.card {
    border: none;
    border-radius: 10px;
}

.navbar-brand {
    font-weight: bold;
    font-size: 1.4rem;
}
```

- [ ] **Step 11: Build to verify**

Run: `mvnw.cmd clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 12: Commit**

```bash
git add src/main/resources/templates/ src/main/resources/static/
git commit -m "feat: add Thymeleaf templates (login, dashboard, employees, departments, attendance, report)"
```

---

### Task 11: Data Initializer and HiddenHttpMethodFilter

**Files:**
- Create: `src/main/java/com/example/presencia/config/DataInitializer.java`

- [ ] **Step 1: Create DataInitializer to seed an admin user on startup**

```java
package com.example.presencia.config;

import com.example.presencia.model.Department;
import com.example.presencia.model.User;
import com.example.presencia.model.enums.Role;
import com.example.presencia.repository.DepartmentRepository;
import com.example.presencia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            Department dept = departmentRepository.save(Department.builder()
                    .name("Siege")
                    .description("Bureau principal")
                    .latitude(5.3600)
                    .longitude(-4.0083)
                    .radius(200.0)
                    .build());

            userRepository.save(User.builder()
                    .email("admin@presencia.com")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("Admin")
                    .lastName("Presencia")
                    .role(Role.ADMIN)
                    .department(dept)
                    .active(true)
                    .build());
        }
    }
}
```

- [ ] **Step 2: Add HiddenHttpMethodFilter config to application.properties**

Append to `application.properties`:
```properties

# Enable PUT/DELETE from HTML forms
spring.mvc.hiddenmethod.filter.enabled=true
```

- [ ] **Step 3: Run the application to verify everything boots**

Run: `mvnw.cmd spring-boot:run`
Expected: Application starts on port 8080, navigate to http://localhost:8080/login, login with `admin@presencia.com` / `admin123`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/presencia/config/ src/main/resources/application.properties
git commit -m "feat: add DataInitializer (admin seed) and enable HiddenHttpMethodFilter"
```

---

### Task 12: Update CLAUDE.md

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Update CLAUDE.md to reflect the implemented architecture**

Add the package structure, API endpoints summary, and default credentials to CLAUDE.md so future contributors have context.

- [ ] **Step 2: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: update CLAUDE.md with implemented architecture details"
```
