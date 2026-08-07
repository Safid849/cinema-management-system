# Cinema management system

REST API for a cinema: catalogue of films, rooms and their seats, screenings, and
seat reservations, with JWT authentication and three roles.

Spring Boot 3.2 · Java 21 · PostgreSQL · Flyway · Gradle · POJA layout.

## Domain

```
Room 1 ──◆ 1..* Seat            a room is composed of its seats
Movie 1 ──── 0..* Projection    a film is shown by many screenings
Room  1 ──── 0..* Projection    a screening takes place in one room
Projection 1 ── 0..* Reservation
User  1 ──── 0..* Reservation
Reservation *──── * Seat        one seat is sold once per projection
```

`Genre`: THRILLER, ROMANCE, COMEDY, DRAMA, ACTION, SCI_FI, FANTASY, ANIMATION.
`Userrole`: CLIENT, EMPLOYEE, MANAGER.

## Running it

```bash
docker compose up --build
```

The compose file starts PostgreSQL, runs the Flyway migrations, and creates the
first MANAGER (`manager@cinema.local` / `changeit`) because public sign-up only
ever creates CLIENTs — without it nobody could grant a role. Change that
password, then set `CINEMA_BOOTSTRAP_ENABLED=false`.

Against your own database:

```bash
export DB_URL=postgresql://localhost:5432/cinema
export DB_USERNAME=cinema DB_PASSWORD=cinema
export JWT_SECRET=$(openssl rand -base64 48)   # at least 32 bytes once decoded
./gradlew bootRun
```

## Authentication

```bash
# 1. sign up (always a CLIENT)
curl -X POST localhost:8080/users/register -H 'Content-Type: application/json' \
  -d '{"firstName":"Ando","lastName":"Rakoto","email":"ando@cinema.mg","password":"password123"}'

# 2. log in
curl -X POST localhost:8080/users/login -H 'Content-Type: application/json' \
  -d '{"email":"ando@cinema.mg","password":"password123"}'
# -> {"token":"eyJ...","type":"Bearer","userId":"...","role":"CLIENT","expiresInMs":3600000}

# 3. call anything else
curl localhost:8080/users/me -H "Authorization: Bearer eyJ..."
```

The token is HS256, carries the email as subject plus a `role` and a `userId`
claim, and lives for one hour by default.

## Endpoints and who may call them

| Method | Route | CLIENT | EMPLOYEE | MANAGER | anonymous |
|---|---|---|---|---|---|
| POST | `/users/register` | 201 | 201 | 201 | 201 |
| POST | `/users/login` | 200 | 200 | 200 | 200 |
| GET | `/users` | 403 | 200 | 200 | 401 |
| GET | `/users/me` | 200 | 200 | 200 | 401 |
| GET | `/users/{id}` | 200 own / 403 other | 200 | 200 | 401 |
| PUT | `/users/{id}/role` | 403 | 403 | 200 | 401 |
| GET | `/movies`, `/movies/{id}` | 200 | 200 | 200 | 401 |
| PUT | `/movies` | 403 | 403 | 200 | 401 |
| GET | `/projections`, `/projections/{id}` | 200 | 200 | 200 | 200 |
| GET | `/projections/{id}/seats` | 200 | 200 | 200 | 200 |
| GET | `/projections/{id}/available-seats` | 200 | 200 | 200 | 200 |
| PUT | `/projections` | 403 | 403 | 200 | 401 |
| GET | `/rooms`, `/rooms/{id}`, `/rooms/{id}/seats` | 200 | 200 | 200 | 401 |
| PUT | `/rooms` | 403 | 403 | 200 | 401 |
| GET | `/reservations` | 403 | 200 | 200 | 401 |
| GET | `/reservations/me` | 200 | 200 | 200 | 401 |
| GET | `/reservations/{id}` | 200 own / 403 other | 200 | 200 | 401 |
| PUT | `/reservations` | 403 | 200 | 200 | 401 |
| DELETE | `/reservations/{id}` | 403 | 200 | 200 | 401 |

Every line of this table is asserted by `SecurityMatrixIT`.

The `PUT` routes are upserts: a payload without an `id` creates, a payload with
one updates. `PUT /rooms` is keyed on the room number instead, and creates the
room's seats along with it.

## Business rules enforced

- A seat is sold at most once per projection (checked in the service, and
  guaranteed by a unique constraint on `reservation_seats`).
- A seat can only be booked for a projection running in its own room.
- Two projections cannot overlap in the same room, film duration included.
- A film's duration must be strictly positive.
- Emails are trimmed and lower-cased before storage.
- A MANAGER cannot demote themselves out of the manager role.

## Errors

Every failure returns `{"code": "...", "message": "..."}`:

| Status | Code | When |
|---|---|---|
| 400 | `BAD_REQUEST` | invalid payload, malformed body, broken business precondition |
| 401 | `UNAUTHORIZED` | missing, expired or forged token, bad credentials |
| 403 | `FORBIDDEN` | authenticated but the role or the ownership does not allow it |
| 404 | `NOT_FOUND` | unknown id |
| 409 | `CONFLICT` | email taken, seat taken, overlapping projection |

## Tests

```bash
./gradlew test          # unit + integration, then the 80% coverage gate
```

Unit tests mock the repositories. Integration tests (`*IT`) boot the whole
application on a random port against a throwaway PostgreSQL started by
Testcontainers, and drive it over real HTTP with real tokens — **Docker must be
running**. Coverage is measured on `**/cinema/**` and must stay at or above 80%
of lines, or the build fails.
