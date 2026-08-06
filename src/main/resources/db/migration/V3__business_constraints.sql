-- ---------------------------------------------------------------------------
-- V3: business invariants the service layer already enforces, restated where
-- they really belong. A bug in a service, a manual SQL fix or a concurrent
-- request cannot leave the database in a state the domain forbids.
-- ---------------------------------------------------------------------------

-- A room without seats is not a room, and a negative capacity is meaningless.
ALTER TABLE rooms DROP CONSTRAINT IF EXISTS ck_rooms_capacity_positive;
ALTER TABLE rooms ADD CONSTRAINT ck_rooms_capacity_positive CHECK (capacity > 0);

-- Duration is stored as a numeric count of nanoseconds by Hibernate.
ALTER TABLE movies DROP CONSTRAINT IF EXISTS ck_movies_duration_positive;
ALTER TABLE movies ADD CONSTRAINT ck_movies_duration_positive CHECK (duration > 0);

-- A free screening is legitimate, a negative price is not.
ALTER TABLE projections DROP CONSTRAINT IF EXISTS ck_projections_seat_price_positive;
ALTER TABLE projections
  ADD CONSTRAINT ck_projections_seat_price_positive CHECK (seat_price >= 0);

-- Only the three roles of the specification.
ALTER TABLE users DROP CONSTRAINT IF EXISTS ck_users_role;
ALTER TABLE users
  ADD CONSTRAINT ck_users_role CHECK (role IN ('CLIENT', 'EMPLOYEE', 'MANAGER'));

-- Only the eight genres of the specification.
ALTER TABLE movie_genres DROP CONSTRAINT IF EXISTS ck_movie_genres_genre;
ALTER TABLE movie_genres
  ADD CONSTRAINT ck_movie_genres_genre CHECK (
    genre IN ('THRILLER', 'ROMANCE', 'COMEDY', 'DRAMA',
              'ACTION', 'SCI_FI', 'FANTASY', 'ANIMATION'));

-- Emails are normalised to lower case before they are stored, so a
-- case-insensitive duplicate can never sneak in through a direct insert.
UPDATE users SET email = lower(email) WHERE email <> lower(email);

ALTER TABLE users DROP CONSTRAINT IF EXISTS ck_users_email_lowercase;
ALTER TABLE users ADD CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email));

-- A movie belongs to its genres: dropping the movie drops the rows.
ALTER TABLE movie_genres DROP CONSTRAINT IF EXISTS movie_genres_movie_id_fkey;
ALTER TABLE movie_genres
  ADD CONSTRAINT movie_genres_movie_id_fkey
  FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE;

-- Seats are a composition of the room (the diamond in the class diagram):
-- destroying the room destroys its seats.
ALTER TABLE seats DROP CONSTRAINT IF EXISTS seats_room_id_fkey;
ALTER TABLE seats
  ADD CONSTRAINT seats_room_id_fkey
  FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE;

-- A reservation is meaningless without its projection.
ALTER TABLE reservations DROP CONSTRAINT IF EXISTS reservations_projection_id_fkey;
ALTER TABLE reservations
  ADD CONSTRAINT reservations_projection_id_fkey
  FOREIGN KEY (projection_id) REFERENCES projections (id) ON DELETE CASCADE;

-- A seat that no longer exists cannot stay attached to a reservation, otherwise
-- dropping a room fails on an opaque foreign key error.
ALTER TABLE reservation_seats DROP CONSTRAINT IF EXISTS reservation_seats_seat_id_fkey;
ALTER TABLE reservation_seats
  ADD CONSTRAINT reservation_seats_seat_id_fkey
  FOREIGN KEY (seat_id) REFERENCES seats (id) ON DELETE CASCADE;

-- Listing "what is on tonight" is the most frequent read of the whole API.
CREATE INDEX IF NOT EXISTS idx_projections_datetime_room
  ON projections (datetime, room_id);
