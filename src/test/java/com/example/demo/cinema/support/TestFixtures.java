package com.example.demo.cinema.support;

import com.example.demo.cinema.entity.Genre;
import com.example.demo.cinema.entity.Movie;
import com.example.demo.cinema.entity.Projection;
import com.example.demo.cinema.entity.Reservation;
import com.example.demo.cinema.entity.Room;
import com.example.demo.cinema.entity.Seat;
import com.example.demo.cinema.entity.User;
import com.example.demo.cinema.entity.Userrole;
import com.example.demo.cinema.security.CinemaUserPrincipal;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Small builders shared by the tests so each test only spells out what it actually asserts. */
public final class TestFixtures {

  private TestFixtures() {}

  public static User user(Userrole role) {
    return user(UUID.randomUUID(), role);
  }

  public static User user(UUID id, Userrole role) {
    return User.builder()
        .id(id)
        .firstName("Ando")
        .lastName("Rakoto")
        .birthdate(LocalDate.of(1995, 4, 12))
        .email(role.name().toLowerCase() + "-" + id + "@cinema.mg")
        .password("$2a$10$hashed")
        .phone("+261340000000")
        .role(role)
        .build();
  }

  public static CinemaUserPrincipal principal(Userrole role) {
    return CinemaUserPrincipal.from(user(role));
  }

  public static CinemaUserPrincipal principal(UUID id, Userrole role) {
    return CinemaUserPrincipal.from(user(id, role));
  }

  public static Room room(UUID id, String number, int capacity) {
    return Room.builder().id(id).number(number).capacity(capacity).build();
  }

  public static Seat seat(UUID id, String number, Room room) {
    return Seat.builder().id(id).number(number).room(room).build();
  }

  public static Movie movie(UUID id, String title, Duration duration) {
    Set<Genre> genres = new LinkedHashSet<>(List.of(Genre.ACTION));
    return Movie.builder()
        .id(id)
        .title(title)
        .genre(genres)
        .description("A description")
        .duration(duration)
        .build();
  }

  public static Projection projection(UUID id, Movie movie, Room room, Instant datetime) {
    return Projection.builder()
        .id(id)
        .movie(movie)
        .room(room)
        .datetime(datetime)
        .seatPrice(new BigDecimal("12000.00"))
        .build();
  }

  public static Reservation reservation(UUID id, User user, Projection projection, Seat... seats) {
    return Reservation.builder()
        .id(id)
        .user(user)
        .projection(projection)
        .seats(new LinkedHashSet<>(List.of(seats)))
        .createdAt(Instant.parse("2026-01-15T10:00:00Z"))
        .build();
  }
}
