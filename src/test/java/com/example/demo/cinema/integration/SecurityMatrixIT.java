package com.example.demo.cinema.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.cinema.dto.MovieInputDTO;
import com.example.demo.cinema.dto.ProjectionInputDTO;
import com.example.demo.cinema.dto.ReservationInputDTO;
import com.example.demo.cinema.entity.Genre;
import com.example.demo.cinema.entity.Movie;
import com.example.demo.cinema.entity.Projection;
import com.example.demo.cinema.entity.Reservation;
import com.example.demo.cinema.entity.Room;
import com.example.demo.cinema.entity.Seat;
import com.example.demo.cinema.entity.User;
import com.example.demo.cinema.entity.Userrole;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * One test per line of the specification's EXAMPLES block: who gets 200 and who gets 403 on each
 * route, over real HTTP with real tokens.
 */
class SecurityMatrixIT extends CinemaIT {

  private static final Instant EVENING = Instant.parse("2026-09-10T18:00:00Z");

  // --- PUT /movies: 403 for CLIENT and EMPLOYEE, 200 for MANAGER -----------

  @Test
  void putMovies_isManagerOnly() {
    MovieInputDTO body =
        new MovieInputDTO(null, "Dune", Set.of(Genre.SCI_FI), "Arrakis", Duration.ofMinutes(155));

    assertThat(put("/movies", tokenOf(Userrole.CLIENT), body).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(put("/movies", tokenOf(Userrole.EMPLOYEE), body).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(put("/movies", tokenOf(Userrole.MANAGER), body).getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  // --- PUT /projections: 403 for CLIENT and EMPLOYEE, 200 for MANAGER ------

  @Test
  void putProjections_isManagerOnly() {
    Movie movie = persistMovie();
    Room room = persistRoom("A", 3);
    ProjectionInputDTO body =
        new ProjectionInputDTO(
            null, movie.getId(), room.getId(), EVENING, new BigDecimal("12000.00"));

    assertThat(put("/projections", tokenOf(Userrole.CLIENT), body).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(put("/projections", tokenOf(Userrole.EMPLOYEE), body).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(put("/projections", tokenOf(Userrole.MANAGER), body).getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  // --- GET /projections: 200 for everyone, token or not --------------------

  @Test
  void getProjections_isPublic() {
    assertThat(get("/projections", null).getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(get("/projections", tokenOf(Userrole.CLIENT)).getStatusCode())
        .isEqualTo(HttpStatus.OK);
    assertThat(get("/projections", tokenOf(Userrole.MANAGER)).getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  // --- GET /reservations: 403 for CLIENT, 200 for staff --------------------

  @Test
  void getReservations_isStaffOnly() {
    assertThat(get("/reservations", tokenOf(Userrole.CLIENT)).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(get("/reservations", tokenOf(Userrole.EMPLOYEE)).getStatusCode())
        .isEqualTo(HttpStatus.OK);
    assertThat(get("/reservations", tokenOf(Userrole.MANAGER)).getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  // --- PUT /reservations: 403 for CLIENT, 200 for staff --------------------

  @Test
  void putReservations_isStaffOnly() {
    User owner = persistUser(Userrole.CLIENT);
    Projection projection = persistProjection(persistMovie(), persistRoom("A", 3));
    List<Seat> seats = seatRepository.findByRoomIdOrderByNumberAsc(projection.getRoom().getId());

    assertThat(
            put(
                    "/reservations",
                    tokenOf(Userrole.CLIENT),
                    new ReservationInputDTO(
                        null, owner.getId(), projection.getId(), Set.of(seats.get(0).getId())))
                .getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);

    assertThat(
            put(
                    "/reservations",
                    tokenOf(Userrole.EMPLOYEE),
                    new ReservationInputDTO(
                        null, owner.getId(), projection.getId(), Set.of(seats.get(0).getId())))
                .getStatusCode())
        .isEqualTo(HttpStatus.OK);

    assertThat(
            put(
                    "/reservations",
                    tokenOf(Userrole.MANAGER),
                    new ReservationInputDTO(
                        null, owner.getId(), projection.getId(), Set.of(seats.get(1).getId())))
                .getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  // --- GET /reservations/{id}: owner or staff only -------------------------

  @Test
  void getReservationById_followsOwnership() {
    User owner = persistUser(Userrole.CLIENT);
    User stranger = persistUser(Userrole.CLIENT);
    Reservation reservation = persistReservation(owner);
    String path = "/reservations/" + reservation.getId();

    assertThat(get(path, tokenOf(owner)).getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(get(path, tokenOf(stranger)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(get(path, tokenOf(Userrole.EMPLOYEE)).getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(get(path, tokenOf(Userrole.MANAGER)).getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  // --- PUT /rooms: 403 for CLIENT and EMPLOYEE, 200 for MANAGER ------------

  @Test
  void putRooms_isManagerOnly() {
    var body = new com.example.demo.cinema.dto.RoomInputDTO("Z", 5);

    assertThat(put("/rooms", tokenOf(Userrole.CLIENT), body).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(put("/rooms", tokenOf(Userrole.EMPLOYEE), body).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(put("/rooms", tokenOf(Userrole.MANAGER), body).getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  // --- fixtures -------------------------------------------------------------

  private Movie persistMovie() {
    return movieRepository.save(
        Movie.builder()
            .title("Dune")
            .genre(new LinkedHashSet<>(List.of(Genre.SCI_FI)))
            .description("Arrakis")
            .duration(Duration.ofMinutes(120))
            .build());
  }

  private Room persistRoom(String number, int capacity) {
    Room room = roomRepository.save(Room.builder().number(number).capacity(capacity).build());
    for (int i = 1; i <= capacity; i++) {
      seatRepository.save(Seat.builder().number(String.format("%03d", i)).room(room).build());
    }
    return room;
  }

  private Projection persistProjection(Movie movie, Room room) {
    return projectionRepository.save(
        Projection.builder()
            .movie(movie)
            .room(room)
            .datetime(EVENING)
            .seatPrice(new BigDecimal("12000.00"))
            .build());
  }

  private Reservation persistReservation(User owner) {
    Projection projection = persistProjection(persistMovie(), persistRoom("A", 3));
    Seat seat = seatRepository.findByRoomIdOrderByNumberAsc(projection.getRoom().getId()).get(0);

    return reservationRepository.save(
        Reservation.builder()
            .user(owner)
            .projection(projection)
            .seats(new LinkedHashSet<>(Set.of(seat)))
            .createdAt(Instant.now())
            .build());
  }
}
