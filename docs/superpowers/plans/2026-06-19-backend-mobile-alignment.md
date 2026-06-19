# Backend-Mobile Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adapt the Presencia Spring Boot backend API to match the Flutter mobile app's expected endpoints, field names, and data formats so the mobile can consume the API without any client-side changes (except adding GPS coordinates to check-in/check-out).

**Architecture:** Modify entities, enums, DTOs, API controllers, services, repositories, and Thymeleaf templates in-place. The web admin (Thymeleaf) must continue working after all changes.

**Tech Stack:** Spring Boot 4.0.6, Java 21, PostgreSQL, JWT (JJWT), Thymeleaf, Flutter/Dart (mobile GPS fix only)

---

### Task 1: Rename User entity fields and add new columns

**Files:**
- Modify: `src/main/java/com/example/presencia/model/User.java`

- [ ] **Step 1: Rename firstName/lastName and add matricule, photo, poste**

Replace the full content of `User.java` with:

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
    private String prenom;

    @NotBlank
    @Column(nullable = false)
    private String nom;

    @Column(unique = true)
    private String matricule;

    private String photo;

    private String poste;

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

- [ ] **Step 2: Verify the file compiles**

Run: `cd /c/Users/bmaha/IdeaProjects/presencia && ./mvnw compile -pl . -q 2>&1 | tail -5`
Expected: Compilation errors (expected — dependent classes still reference old field names). This confirms the entity itself is valid.

---

### Task 2: Update Role and AttendanceStatus enums

**Files:**
- Modify: `src/main/java/com/example/presencia/model/enums/Role.java`
- Modify: `src/main/java/com/example/presencia/model/enums/AttendanceStatus.java`

- [ ] **Step 1: Change EMPLOYEE to EMPLOYE in Role.java**

Replace the full content of `Role.java`:

```java
package com.example.presencia.model.enums;

public enum Role {
    ADMIN,
    EMPLOYE
}
```

- [ ] **Step 2: Change LATE to RETARD and add EN_CONGE in AttendanceStatus.java**

Replace the full content of `AttendanceStatus.java`:

```java
package com.example.presencia.model.enums;

public enum AttendanceStatus {
    PRESENT,
    RETARD,
    ABSENT,
    EN_CONGE
}
```

---

### Task 3: Update UserRepository to support matricule lookup

**Files:**
- Modify: `src/main/java/com/example/presencia/repository/UserRepository.java`

- [ ] **Step 1: Add findByMatricule method**

Replace the full content of `UserRepository.java`:

```java
package com.example.presencia.repository;

import com.example.presencia.model.User;
import com.example.presencia.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByMatricule(String matricule);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByDepartmentId(Long departmentId);

    long countByActiveTrue();
}
```

---

### Task 4: Update UserService for new field names and matricule support

**Files:**
- Modify: `src/main/java/com/example/presencia/service/UserService.java`

- [ ] **Step 1: Update UserService with renamed fields and new methods**

Replace the full content of `UserService.java`:

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
        return userRepository.findByRole(Role.EMPLOYE);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouve: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouve: " + email));
    }

    public User findByMatricule(String matricule) {
        return userRepository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouve: " + matricule));
    }

    /**
     * Find user by matricule or email. If identifier contains '@', search by email, otherwise by matricule.
     */
    public User findByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return findByEmail(identifier);
        }
        return findByMatricule(identifier);
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
        user.setPrenom(updated.getPrenom());
        user.setNom(updated.getNom());
        user.setEmail(updated.getEmail());
        user.setMatricule(updated.getMatricule());
        user.setPoste(updated.getPoste());
        user.setRole(updated.getRole());
        user.setDepartment(updated.getDepartment());
        user.setActive(updated.isActive());
        return userRepository.save(user);
    }

    public User updateProfile(String email, String nom, String prenom, String newEmail) {
        User user = findByEmail(email);
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setEmail(newEmail);
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

---

### Task 5: Update AttendanceService for renamed enum values

**Files:**
- Modify: `src/main/java/com/example/presencia/service/AttendanceService.java`

- [ ] **Step 1: Replace LATE with RETARD**

In `AttendanceService.java`, change the status determination line:

Replace:
```java
        AttendanceStatus status = LocalTime.now().isAfter(limit)
                ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
```

With:
```java
        AttendanceStatus status = LocalTime.now().isAfter(limit)
                ? AttendanceStatus.RETARD : AttendanceStatus.PRESENT;
```

---

### Task 6: Update DashboardStats and its usage for renamed enum

**Files:**
- Modify: `src/main/java/com/example/presencia/dto/response/DashboardStats.java`
- Modify: `src/main/java/com/example/presencia/service/AttendanceService.java` (getDashboardStats method)

- [ ] **Step 1: Rename lateToday to retardToday in DashboardStats**

Replace the full content of `DashboardStats.java`:

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
    private long retardToday;
    private long absentToday;
}
```

- [ ] **Step 2: Update getDashboardStats in AttendanceService**

Replace the `getDashboardStats` method in `AttendanceService.java`:

Replace:
```java
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
```

With:
```java
    public DashboardStats getDashboardStats() {
        LocalDate today = LocalDate.now();
        return DashboardStats.builder()
                .totalEmployees(userService.countActiveEmployees())
                .presentToday(attendanceRepository.countByDateAndStatus(today, AttendanceStatus.PRESENT))
                .retardToday(attendanceRepository.countByDateAndStatus(today, AttendanceStatus.RETARD))
                .absentToday(userService.countActiveEmployees()
                        - attendanceRepository.countByDate(today))
                .build();
    }
```

---

### Task 7: Create new DTOs for mobile API responses

**Files:**
- Create: `src/main/java/com/example/presencia/dto/response/UserResponse.java`
- Create: `src/main/java/com/example/presencia/dto/response/MobileAuthResponse.java`
- Create: `src/main/java/com/example/presencia/dto/response/PointageResponse.java`
- Create: `src/main/java/com/example/presencia/dto/request/ProfileUpdateRequest.java`
- Modify: `src/main/java/com/example/presencia/dto/request/LoginRequest.java`

- [ ] **Step 1: Update LoginRequest to accept matricule**

Replace the full content of `LoginRequest.java`:

```java
package com.example.presencia.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String matricule;
    @NotBlank
    private String password;
}
```

- [ ] **Step 2: Create UserResponse DTO**

Create `src/main/java/com/example/presencia/dto/response/UserResponse.java`:

```java
package com.example.presencia.dto.response;

import com.example.presencia.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String matricule;
    private String role;
    private String photo;
    private String departement;
    private String poste;

    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .matricule(user.getMatricule())
                .role(user.getRole().name())
                .photo(user.getPhoto())
                .departement(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .poste(user.getPoste())
                .build();
    }
}
```

- [ ] **Step 3: Create MobileAuthResponse DTO**

Create `src/main/java/com/example/presencia/dto/response/MobileAuthResponse.java`:

```java
package com.example.presencia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class MobileAuthResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private UserResponse user;
}
```

- [ ] **Step 4: Create PointageResponse DTO**

Create `src/main/java/com/example/presencia/dto/response/PointageResponse.java`:

```java
package com.example.presencia.dto.response;

import com.example.presencia.model.Attendance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
@Builder
@AllArgsConstructor
public class PointageResponse {
    private Long id;
    private String date;
    private String heureEntree;
    private String heureSortie;
    private String statut;
    private Double latitude;
    private Double longitude;
    private String remarque;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public static PointageResponse fromAttendance(Attendance a) {
        return PointageResponse.builder()
                .id(a.getId())
                .date(a.getDate().toString())
                .heureEntree(a.getCheckIn() != null ? a.getCheckIn().format(TIME_FMT) : null)
                .heureSortie(a.getCheckOut() != null ? a.getCheckOut().format(TIME_FMT) : null)
                .statut(a.getStatus().name())
                .latitude(a.getCheckInLatitude())
                .longitude(a.getCheckInLongitude())
                .remarque(a.getNotes())
                .build();
    }
}
```

- [ ] **Step 5: Create ProfileUpdateRequest DTO**

Create `src/main/java/com/example/presencia/dto/request/ProfileUpdateRequest.java`:

```java
package com.example.presencia.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @NotBlank
    private String nom;
    @NotBlank
    private String prenom;
    @Email @NotBlank
    private String email;
}
```

- [ ] **Step 6: Commit DTOs**

```bash
git add src/main/java/com/example/presencia/dto/
git commit -m "feat: add mobile-compatible DTOs (UserResponse, MobileAuthResponse, PointageResponse, ProfileUpdateRequest)"
```

---

### Task 8: Rewrite AuthApiController for mobile compatibility

**Files:**
- Modify: `src/main/java/com/example/presencia/api/AuthApiController.java`

- [ ] **Step 1: Replace AuthApiController with mobile-compatible version**

Replace the full content of `AuthApiController.java`:

```java
package com.example.presencia.api;

import com.example.presencia.dto.request.LoginRequest;
import com.example.presencia.dto.response.MobileAuthResponse;
import com.example.presencia.dto.response.UserResponse;
import com.example.presencia.model.User;
import com.example.presencia.security.jwt.JwtTokenProvider;
import com.example.presencia.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<MobileAuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // Resolve the user by matricule or email
        User user = userService.findByIdentifier(request.getMatricule());

        // Authenticate using email (Spring Security username)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword()));

        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return ResponseEntity.ok(MobileAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(UserResponse.fromUser(user))
                .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<MobileAuthResponse> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(401).build();
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User user = userService.findByEmail(email);

        String newAccessToken = jwtTokenProvider.generateToken(email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        return ResponseEntity.ok(MobileAuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .user(UserResponse.fromUser(user))
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        // JWT is stateless — nothing to invalidate server-side
        return ResponseEntity.ok(Map.of("message", "Deconnexion reussie"));
    }
}
```

---

### Task 9: Rename AttendanceApiController to PointageApiController

**Files:**
- Delete: `src/main/java/com/example/presencia/api/AttendanceApiController.java`
- Create: `src/main/java/com/example/presencia/api/PointageApiController.java`

- [ ] **Step 1: Create PointageApiController with mobile-compatible paths and response format**

Delete `AttendanceApiController.java` and create `PointageApiController.java`:

```java
package com.example.presencia.api;

import com.example.presencia.dto.request.CheckInRequest;
import com.example.presencia.dto.request.CheckOutRequest;
import com.example.presencia.dto.response.PointageResponse;
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
@RequestMapping("/api/pointage")
@RequiredArgsConstructor
public class PointageApiController {

    private final AttendanceService attendanceService;

    @GetMapping("/today")
    public ResponseEntity<PointageResponse> today(@AuthenticationPrincipal UserDetails userDetails) {
        Attendance attendance = attendanceService.getTodayAttendance(userDetails.getUsername());
        if (attendance == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(PointageResponse.fromAttendance(attendance));
    }

    @PostMapping("/check-in")
    public ResponseEntity<PointageResponse> checkIn(@AuthenticationPrincipal UserDetails userDetails,
                                                     @Valid @RequestBody CheckInRequest request) {
        Attendance attendance = attendanceService.checkIn(
                userDetails.getUsername(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(PointageResponse.fromAttendance(attendance));
    }

    @PostMapping("/check-out")
    public ResponseEntity<PointageResponse> checkOut(@AuthenticationPrincipal UserDetails userDetails,
                                                      @Valid @RequestBody CheckOutRequest request) {
        Attendance attendance = attendanceService.checkOut(
                userDetails.getUsername(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(PointageResponse.fromAttendance(attendance));
    }

    @GetMapping("/historique")
    public ResponseEntity<List<PointageResponse>> historique(@AuthenticationPrincipal UserDetails userDetails,
                                                              @RequestParam int mois,
                                                              @RequestParam int annee) {
        List<Attendance> list = attendanceService.getHistory(userDetails.getUsername(), mois, annee);
        return ResponseEntity.ok(list.stream().map(PointageResponse::fromAttendance).toList());
    }
}
```

Note: check-out is now `POST` (not `PUT`) to match what the mobile sends.

---

### Task 10: Rename ProfileApiController to EmployeApiController

**Files:**
- Delete: `src/main/java/com/example/presencia/api/ProfileApiController.java`
- Create: `src/main/java/com/example/presencia/api/EmployeApiController.java`

- [ ] **Step 1: Create EmployeApiController with mobile-compatible paths**

Delete `ProfileApiController.java` and create `EmployeApiController.java`:

```java
package com.example.presencia.api;

import com.example.presencia.dto.request.ChangePasswordRequest;
import com.example.presencia.dto.request.ProfileUpdateRequest;
import com.example.presencia.dto.response.UserResponse;
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
@RequestMapping("/api/employe")
@RequiredArgsConstructor
public class EmployeApiController {

    private final UserService userService;

    @GetMapping("/profil")
    public ResponseEntity<UserResponse> getProfil(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    @PutMapping("/profil/update")
    public ResponseEntity<UserResponse> updateProfil(@AuthenticationPrincipal UserDetails userDetails,
                                                      @Valid @RequestBody ProfileUpdateRequest request) {
        User user = userService.updateProfile(
                userDetails.getUsername(), request.getNom(), request.getPrenom(), request.getEmail());
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    @PutMapping("/profil/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(),
                request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Mot de passe modifie avec succes"));
    }
}
```

---

### Task 11: Update DataInitializer for new field names

**Files:**
- Modify: `src/main/java/com/example/presencia/config/DataInitializer.java`

- [ ] **Step 1: Use new field names and add matricule**

Replace the full content of `DataInitializer.java`:

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
                    .prenom("Admin")
                    .nom("Presencia")
                    .matricule("ADM001")
                    .role(Role.ADMIN)
                    .department(dept)
                    .active(true)
                    .build());
        }
    }
}
```

---

### Task 12: Update ExportService for renamed fields

**Files:**
- Modify: `src/main/java/com/example/presencia/service/ExportService.java`

- [ ] **Step 1: Replace firstName/lastName with prenom/nom**

In `ExportService.java`, replace:
```java
                row.createCell(1).setCellValue(a.getUser().getFirstName() + " " + a.getUser().getLastName());
```

With:
```java
                row.createCell(1).setCellValue(a.getUser().getPrenom() + " " + a.getUser().getNom());
```

---

### Task 13: Update Thymeleaf templates for new field names and enum values

**Files:**
- Modify: `src/main/resources/templates/employees/list.html`
- Modify: `src/main/resources/templates/employees/form.html`
- Modify: `src/main/resources/templates/attendance/list.html`
- Modify: `src/main/resources/templates/attendance/report.html`
- Modify: `src/main/resources/templates/dashboard.html`

- [ ] **Step 1: Update employees/list.html**

In `employees/list.html`, replace:
```html
                <td th:text="${e.firstName + ' ' + e.lastName}"></td>
```

With:
```html
                <td th:text="${e.prenom + ' ' + e.nom}"></td>
```

- [ ] **Step 2: Update employees/form.html**

In `employees/form.html`, replace:
```html
                <input type="text" class="form-control" th:field="*{firstName}" required>
```

With:
```html
                <input type="text" class="form-control" th:field="*{prenom}" required>
```

And replace:
```html
                <input type="text" class="form-control" th:field="*{lastName}" required>
```

With:
```html
                <input type="text" class="form-control" th:field="*{nom}" required>
```

- [ ] **Step 3: Update attendance/list.html**

In `attendance/list.html`, replace:
```html
                <td th:text="${a.user.firstName + ' ' + a.user.lastName}"></td>
```

With:
```html
                <td th:text="${a.user.prenom + ' ' + a.user.nom}"></td>
```

And replace:
```html
                          th:classappend="${a.status.name() == 'PRESENT' ? 'bg-success' : (a.status.name() == 'LATE' ? 'bg-warning' : 'bg-danger')}"
```

With:
```html
                          th:classappend="${a.status.name() == 'PRESENT' ? 'bg-success' : (a.status.name() == 'RETARD' ? 'bg-warning' : 'bg-danger')}"
```

- [ ] **Step 4: Update attendance/report.html**

In `attendance/report.html`, replace:
```html
                <td th:text="${a.user.firstName + ' ' + a.user.lastName}"></td>
```

With:
```html
                <td th:text="${a.user.prenom + ' ' + a.user.nom}"></td>
```

And replace:
```html
                          th:classappend="${a.status.name() == 'PRESENT' ? 'bg-success' : (a.status.name() == 'LATE' ? 'bg-warning' : 'bg-danger')}"
```

With:
```html
                          th:classappend="${a.status.name() == 'PRESENT' ? 'bg-success' : (a.status.name() == 'RETARD' ? 'bg-warning' : 'bg-danger')}"
```

- [ ] **Step 5: Update dashboard.html**

In `dashboard.html`, replace:
```html
                        <td th:text="${a.user.firstName + ' ' + a.user.lastName}"></td>
```

With:
```html
                        <td th:text="${a.user.prenom + ' ' + a.user.nom}"></td>
```

And replace:
```html
                                  th:classappend="${a.status.name() == 'PRESENT' ? 'bg-success' : (a.status.name() == 'LATE' ? 'bg-warning' : 'bg-danger')}"
```

With:
```html
                                  th:classappend="${a.status.name() == 'PRESENT' ? 'bg-success' : (a.status.name() == 'RETARD' ? 'bg-warning' : 'bg-danger')}"
```

And replace:
```html
                    <h2 th:text="${stats.lateToday}">0</h2>
```

With:
```html
                    <h2 th:text="${stats.retardToday}">0</h2>
```

---

### Task 14: Update EmployeeController for new field names

**Files:**
- Modify: `src/main/java/com/example/presencia/controller/EmployeeController.java`

- [ ] **Step 1: No code changes needed**

The `EmployeeController` passes `User` objects directly from the service and uses `Role.values()`. Since we renamed the fields in the `User` entity and the enum value in `Role`, this controller will work without changes. The Thymeleaf form binds with `th:field="*{prenom}"` and `th:field="*{nom}"` which matches the new entity field names.

Verify by checking that the controller does not reference `firstName` or `lastName` directly (it doesn't — it delegates to `UserService`).

---

### Task 15: Change server port to 8080

**Files:**
- Modify: `src/main/resources/application.properties`

- [ ] **Step 1: Change port from 8086 to 8080**

In `application.properties`, replace:
```properties
server.port=8086
```

With:
```properties
server.port=8080
```

---

### Task 16: Update mobile PointageController to send GPS coordinates

**Files:**
- Modify: `/c/Users/bmaha/IdeaProjects/presencia-mobile/lib/controllers/pointage_controller.dart`

- [ ] **Step 1: Add geolocator import and send GPS in check-in/check-out**

Replace the full content of `pointage_controller.dart`:

```dart
import 'package:get/get.dart';
import 'package:geolocator/geolocator.dart';
import '../models/pointage_model.dart';
import '../services/api_service.dart';
import '../components/app_snakbar.dart';
import '../configs/api_config.dart';

class PointageController extends GetxController {
  final _api = Get.find<ApiService>();

  final isLoading       = false.obs;
  final isCheckedIn     = false.obs;
  final pointageAujourdhui = Rxn<PointageModel>();
  final heureEntree     = ''.obs;
  final heureSortie     = ''.obs;

  @override
  void onInit() {
    super.onInit();
    fetchStatutAujourdhui();
  }

  Future<Position> _getPosition() async {
    LocationPermission permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        throw Exception('Permission de localisation refusee.');
      }
    }
    if (permission == LocationPermission.deniedForever) {
      throw Exception('Permission de localisation refusee definitivement. Activez-la dans les parametres.');
    }
    return await Geolocator.getCurrentPosition(
      locationSettings: const LocationSettings(accuracy: LocationAccuracy.high),
    );
  }

  Future<void> fetchStatutAujourdhui() async {
    try {
      isLoading(true);
      final data = await _api.get('${ApiConfig.pointageById}/today');
      final pointage = PointageModel.fromJson(data);
      pointageAujourdhui(pointage);
      isCheckedIn(pointage.heureSortie == null && pointage.heureEntree != null);
      heureEntree(pointage.heureEntree ?? '--:--');
      heureSortie(pointage.heureSortie ?? '--:--');
    } catch (e) {
      // Pas encore pointe aujourd'hui — etat initial normal
      isCheckedIn(false);
    } finally {
      isLoading(false);
    }
  }

  Future<void> checkIn() async {
    try {
      isLoading(true);
      final position = await _getPosition();
      final data = await _api.post(ApiConfig.checkIn, {
        'latitude': position.latitude,
        'longitude': position.longitude,
      });
      final pointage = PointageModel.fromJson(data);
      pointageAujourdhui(pointage);
      isCheckedIn(true);
      heureEntree(pointage.heureEntree ?? '--:--');
      AppSnackbar.show(
        message: 'Entree enregistree a ${pointage.heureEntree}',
        type: SnackType.success,
      );
    } catch (e) {
      AppSnackbar.show(message: e.toString(), type: SnackType.error);
    } finally {
      isLoading(false);
    }
  }

  Future<void> checkOut() async {
    try {
      isLoading(true);
      final position = await _getPosition();
      final data = await _api.post(ApiConfig.checkOut, {
        'latitude': position.latitude,
        'longitude': position.longitude,
      });
      final pointage = PointageModel.fromJson(data);
      pointageAujourdhui(pointage);
      isCheckedIn(false);
      heureSortie(pointage.heureSortie ?? '--:--');
      AppSnackbar.show(
        message: 'Sortie enregistree a ${pointage.heureSortie}',
        type: SnackType.success,
      );
    } catch (e) {
      AppSnackbar.show(message: e.toString(), type: SnackType.error);
    } finally {
      isLoading(false);
    }
  }
}
```

---

### Task 17: Delete old API controllers and commit all backend changes

**Files:**
- Delete: `src/main/java/com/example/presencia/api/AttendanceApiController.java`
- Delete: `src/main/java/com/example/presencia/api/ProfileApiController.java`
- Delete: `src/main/java/com/example/presencia/dto/response/AuthResponse.java` (replaced by MobileAuthResponse)
- Delete: `src/main/java/com/example/presencia/dto/response/AttendanceResponse.java` (replaced by PointageResponse)
- Delete: `src/main/java/com/example/presencia/dto/response/ProfileResponse.java` (replaced by UserResponse)

- [ ] **Step 1: Delete old files**

```bash
cd /c/Users/bmaha/IdeaProjects/presencia
rm src/main/java/com/example/presencia/api/AttendanceApiController.java
rm src/main/java/com/example/presencia/api/ProfileApiController.java
rm src/main/java/com/example/presencia/dto/response/AuthResponse.java
rm src/main/java/com/example/presencia/dto/response/AttendanceResponse.java
rm src/main/java/com/example/presencia/dto/response/ProfileResponse.java
```

- [ ] **Step 2: Verify backend compiles**

```bash
cd /c/Users/bmaha/IdeaProjects/presencia && ./mvnw compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit all backend changes**

```bash
cd /c/Users/bmaha/IdeaProjects/presencia
git add -A
git commit -m "feat: align backend API with mobile app expectations

- Rename User fields: firstName->prenom, lastName->nom, add matricule/photo/poste
- Rename enums: EMPLOYEE->EMPLOYE, LATE->RETARD, add EN_CONGE
- Rename API paths: /attendance->pointage, /profile->/employe/profil
- New response format: MobileAuthResponse with nested user object
- Add /api/auth/logout endpoint
- Change check-out from PUT to POST
- Change history params from month/year to mois/annee
- Add profile update endpoint PUT /api/employe/profil/update
- Update Thymeleaf templates for new field names
- Change server port from 8086 to 8080"
```

---

### Task 18: Commit mobile GPS changes

**Files:**
- Modified: `/c/Users/bmaha/IdeaProjects/presencia-mobile/lib/controllers/pointage_controller.dart`

- [ ] **Step 1: Commit mobile changes**

```bash
cd /c/Users/bmaha/IdeaProjects/presencia-mobile
git add lib/controllers/pointage_controller.dart
git commit -m "feat: send GPS coordinates on check-in/check-out"
```

---

### Task 19: Database migration — rename existing columns (if DB already has data)

**Note:** This task is only needed if the PostgreSQL database already has data with the old column names (`first_name`, `last_name`). If starting fresh, Hibernate `ddl-auto=update` will create the new columns automatically. For an existing database:

- [ ] **Step 1: Run SQL migration**

Connect to PostgreSQL and run:

```sql
-- Rename existing columns
ALTER TABLE users RENAME COLUMN first_name TO prenom;
ALTER TABLE users RENAME COLUMN last_name TO nom;

-- Add new columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS matricule VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS photo VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS poste VARCHAR(255);

-- Update enum values in attendance status
UPDATE attendances SET status = 'RETARD' WHERE status = 'LATE';

-- Update enum values in user roles
UPDATE users SET role = 'EMPLOYE' WHERE role = 'EMPLOYEE';
```

- [ ] **Step 2: Verify data integrity**

```sql
SELECT id, prenom, nom, email, matricule, role FROM users;
SELECT id, status FROM attendances WHERE status IN ('RETARD', 'PRESENT', 'ABSENT');
```

Expected: All rows display correctly with new column names and enum values.
