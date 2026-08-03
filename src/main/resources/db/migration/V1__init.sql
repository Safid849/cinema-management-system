CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       first_name VARCHAR(255) NOT NULL,
                       last_name VARCHAR(255) NOT NULL,
                       birthdate DATE,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       phone VARCHAR(50),
                       role VARCHAR(50) NOT NULL
);

CREATE TABLE rooms (
                       id UUID PRIMARY KEY,
                       number VARCHAR(50) NOT NULL,
                       capacity INT NOT NULL
);

CREATE TABLE seats (
                       id UUID PRIMARY KEY,
                       number VARCHAR(50) NOT NULL,
                       room_id UUID NOT NULL REFERENCES rooms(id)
);

CREATE TABLE movies (
                        id UUID PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        description VARCHAR(2000),
                        duration NUMERIC(21, 0) NOT NULL
);

CREATE TABLE movie_genres (
                              movie_id UUID NOT NULL REFERENCES movies(id),
                              genre VARCHAR(50) NOT NULL
);

CREATE TABLE projections (
                             id UUID PRIMARY KEY,
                             movie_id UUID NOT NULL REFERENCES movies(id),
                             room_id UUID NOT NULL REFERENCES rooms(id),
                             datetime TIMESTAMP NOT NULL,
                             seat_price NUMERIC(10, 2) NOT NULL
);

CREATE TABLE reservations (
                              id UUID PRIMARY KEY,
                              user_id UUID NOT NULL REFERENCES users(id),
                              projection_id UUID NOT NULL REFERENCES projections(id),
                              created_at TIMESTAMP NOT NULL
);

CREATE TABLE reservation_seats (
                                   reservation_id UUID NOT NULL REFERENCES reservations(id),
                                   seat_id UUID NOT NULL REFERENCES seats(id),
                                   PRIMARY KEY (reservation_id, seat_id)
);