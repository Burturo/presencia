# Presencia - Design Spec

## Overview

Systeme de pointage d'entreprise compose de :
- **Application web** (Spring Boot MVC + Thymeleaf) pour l'administration
- **API REST** (dans le meme projet) pour l'application mobile Flutter
- **Authentification** : Sessions pour le web, JWT pour le mobile

## Entites

### User
| Champ | Type | Contraintes |
|-------|------|-------------|
| id | Long | PK, auto-generated |
| email | String | unique, not null |
| password | String | BCrypt hash |
| firstName | String | not null |
| lastName | String | not null |
| role | Enum(ADMIN, EMPLOYEE) | not null |
| department | Department | ManyToOne |
| active | boolean | default true |
| createdAt | LocalDateTime | auto |
| updatedAt | LocalDateTime | auto |

### Department
| Champ | Type | Contraintes |
|-------|------|-------------|
| id | Long | PK, auto-generated |
| name | String | unique, not null |
| description | String | nullable |
| latitude | Double | not null |
| longitude | Double | not null |
| radius | Double | rayon autorise en metres |
| createdAt | LocalDateTime | auto |

### Attendance
| Champ | Type | Contraintes |
|-------|------|-------------|
| id | Long | PK, auto-generated |
| user | User | ManyToOne, not null |
| checkIn | LocalDateTime | not null |
| checkOut | LocalDateTime | nullable |
| checkInLatitude | Double | not null |
| checkInLongitude | Double | not null |
| checkOutLatitude | Double | nullable |
| checkOutLongitude | Double | nullable |
| status | Enum(PRESENT, LATE, ABSENT) | not null |
| date | LocalDate | not null, pour requetes par jour |
| notes | String | nullable |

### Enums
- **Role** : `ADMIN`, `EMPLOYEE`
- **AttendanceStatus** : `PRESENT`, `LATE`, `ABSENT`

## Package Structure

```
com.example.presencia/
├── model/           Entites JPA + enums
├── repository/      Spring Data JPA repositories
├── service/         Logique metier (partagee web + api)
├── controller/      MVC Thymeleaf (sessions, admin)
├── api/             REST endpoints pour Flutter (JWT)
├── security/        SecurityConfig + JWT (filter, provider, entry point)
│   └── jwt/
├── dto/
│   ├── request/     LoginRequest, CheckInRequest, etc.
│   └── response/    AuthResponse, AttendanceResponse, etc.
├── config/          Configuration applicative
└── exception/       GlobalExceptionHandler, ResourceNotFoundException
```

## API REST (Flutter)

### Authentification
| Methode | Endpoint | Description |
|---------|----------|-------------|
| POST | /api/auth/login | Login → { token, refreshToken, user } |
| POST | /api/auth/refresh | Refresh token |

### Pointage
| Methode | Endpoint | Description |
|---------|----------|-------------|
| GET | /api/attendance/today | Pointage du jour (employe connecte) |
| POST | /api/attendance/check-in | Pointer entree { latitude, longitude } |
| PUT | /api/attendance/check-out | Pointer sortie { latitude, longitude } |
| GET | /api/attendance/history | Historique ?month=&year= |

### Profil
| Methode | Endpoint | Description |
|---------|----------|-------------|
| GET | /api/profile | Infos employe connecte |
| PUT | /api/profile/password | Changement mot de passe |

### Logique de geolocalisation
1. Flutter envoie latitude + longitude + token JWT
2. Le serveur recupere le Department de l'employe
3. GeoLocationService calcule la distance (formule Haversine)
4. Si distance <= rayon autorise → pointage enregistre
5. Sinon → erreur 400

### Logique de status
- Check-in avant l'heure de debut → `PRESENT`
- Check-in apres l'heure de debut → `LATE`
- Pas de check-in en fin de journee → `ABSENT`

## Routes MVC Web (Admin)

| Methode | Route | Description |
|---------|-------|-------------|
| GET | /login | Page de connexion |
| GET | /dashboard | Tableau de bord |
| GET | /employees | Liste employes |
| GET | /employees/new | Formulaire creation |
| POST | /employees | Creer employe |
| GET | /employees/{id}/edit | Formulaire edition |
| PUT | /employees/{id} | Modifier employe |
| DELETE | /employees/{id} | Supprimer employe |
| GET | /departments | Liste departements |
| GET | /departments/new | Formulaire creation |
| POST | /departments | Creer departement |
| GET | /departments/{id}/edit | Formulaire edition |
| PUT | /departments/{id} | Modifier departement |
| DELETE | /departments/{id} | Supprimer departement |
| GET | /attendance | Liste pointages (filtres) |
| GET | /attendance/report | Rapport detaille |
| GET | /export/attendance/excel | Export Excel (Apache POI) |
| GET | /export/attendance/pdf | Export PDF |

## Securite

Deux filter chains Spring Security :

**Chain 1 — API REST (ordre 1, prioritaire)**
- Matcher : `/api/**`
- Stateless (pas de session)
- JwtAuthenticationFilter extrait et valide le token
- JwtAuthEntryPoint renvoie 401 JSON en cas d'echec

**Chain 2 — Web MVC (ordre 2)**
- Matcher : `/**`
- Form login vers `/login`
- Session cookie classique
- Redirect vers `/dashboard` apres login

Les deux chains partagent le meme `CustomUserDetailsService`.

## Dashboard Admin

- Compteurs : presents / absents / en retard aujourd'hui
- Graphique de presence semaine/mois
- Derniers pointages en temps reel
- Stats par departement

## Dependencies a ajouter au pom.xml

- `io.jsonwebtoken:jjwt-api:0.12.6` + `jjwt-impl` + `jjwt-jackson` (JWT)
- `org.apache.poi:poi-ooxml:5.3.0` (export Excel)

## Configuration

`application.properties` :
- `app.jwt.secret` — cle secrete JWT
- `app.jwt.expiration` — duree de validite du token (ms)
- `app.jwt.refresh-expiration` — duree du refresh token (ms)
- `app.attendance.start-hour` — heure de debut de journee (ex: 08:00)
- H2 console activee en dev
- PostgreSQL configure en prod (profil `prod`)
