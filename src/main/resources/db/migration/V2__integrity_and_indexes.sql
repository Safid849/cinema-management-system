-- Query performance on the foreign keys the API filters on.
CREATE INDEX IF NOT EXISTS idx_seats_room_id ON seats (room_id);
CREATE INDEX IF NOT EXISTS idx_projections_room_id ON projections (room_id);
CREATE INDEX IF NOT EXISTS idx_projections_movie_id ON projections (movie_id);
CREATE INDEX IF NOT EXISTS idx_projections_datetime ON projections (datetime);
CREATE INDEX IF NOT EXISTS idx_reservations_user_id ON reservations (user_id);
CREATE INDEX IF NOT EXISTS idx_reservations_projection_id ON reservations (projection_id);

-- A seat number is unique inside a given room.
ALTER TABLE seats DROP CONSTRAINT IF EXISTS uq_seats_room_number;
ALTER TABLE seats ADD CONSTRAINT uq_seats_room_number UNIQUE (room_id, number);

-- A room number is unique across the cinema.
ALTER TABLE rooms DROP CONSTRAINT IF EXISTS uq_rooms_number;
ALTER TABLE rooms ADD CONSTRAINT uq_rooms_number UNIQUE (number);

-- Two projections cannot start at the exact same time in the same room.
ALTER TABLE projections DROP CONSTRAINT IF EXISTS uq_projection_room_datetime;
ALTER TABLE projections ADD CONSTRAINT uq_projection_room_datetime UNIQUE (room_id, datetime);

-- ---------------------------------------------------------------------------
-- Double-booking guard.
--
-- The service layer already refuses a seat that is taken, but a concurrent
-- request could slip between the check and the insert. `projection_id` is
-- denormalised onto the join table and filled by a trigger (JPA only writes
-- reservation_id / seat_id), which lets the database enforce the invariant:
-- one seat can only be sold once per projection.
-- ---------------------------------------------------------------------------
ALTER TABLE reservation_seats ADD COLUMN IF NOT EXISTS projection_id UUID;

UPDATE reservation_seats rs
SET projection_id = r.projection_id
FROM reservations r
WHERE r.id = rs.reservation_id
  AND rs.projection_id IS NULL;

CREATE OR REPLACE FUNCTION fill_reservation_seat_projection()
RETURNS TRIGGER AS $$
BEGIN
  SELECT projection_id INTO NEW.projection_id
  FROM reservations
  WHERE id = NEW.reservation_id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_fill_reservation_seat_projection ON reservation_seats;
CREATE TRIGGER trg_fill_reservation_seat_projection
BEFORE INSERT OR UPDATE ON reservation_seats
FOR EACH ROW EXECUTE FUNCTION fill_reservation_seat_projection();

ALTER TABLE reservation_seats DROP CONSTRAINT IF EXISTS uq_seat_per_projection;
ALTER TABLE reservation_seats ADD CONSTRAINT uq_seat_per_projection UNIQUE (projection_id, seat_id);

-- Removing a reservation must free its seats.
ALTER TABLE reservation_seats DROP CONSTRAINT IF EXISTS reservation_seats_reservation_id_fkey;
ALTER TABLE reservation_seats
  ADD CONSTRAINT reservation_seats_reservation_id_fkey
  FOREIGN KEY (reservation_id) REFERENCES reservations (id) ON DELETE CASCADE;
