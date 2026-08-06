package com.example.demo.cinema.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.cinema.dto.MovieDTO;
import com.example.demo.cinema.dto.MovieInputDTO;
import com.example.demo.cinema.dto.ProjectionDTO;
import com.example.demo.cinema.dto.ProjectionInputDTO;
import com.example.demo.cinema.dto.ReservationDTO;
import com.example.demo.cinema.dto.ReservationInputDTO;
import com.example.demo.cinema.dto.RoomDTO;
import com.example.demo.cinema.dto.RoomInputDTO;
import com.example.demo.cinema.dto.SeatDTO;
import com.example.demo.cinema.entity.Genre;
import com.example.demo.cinema.entity.User;
import com.example.demo.cinema.entity.Userrole;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** The whole business chain: room, film, projection, seats, booking, and the rules around it. */
class ReservationFlowIT extends CinemaIT {

  private static final Instant EVENING = Instant.parse("2026-09-10T18:00:00Z");

  @Test
  void managerBuildsTheCatalogue_thenAnEmployeeSellsSeats() {
    String manager = tokenOf(Userrole.MANAGER);
    String employee = tokenOf(Userrole.EMPLOYEE);
    User client = persistUser(Userrole.CLIENT);

    RoomDTO room = createRoom(manager, "A", 3);
    assertThat(room.capacity()).isEqualTo(3);

    // The room is a composition of seats: creating it must have created them too.
    List<SeatDTO> seats = seatsOfRoom(manager, room);
    assertThat(seats).hasSize(3);

    MovieDTO movie = createMovie(manager, "Dune", Duration.ofMinutes(120));
    ProjectionDTO projection = createProjection(manager, movie, room, EVENING);

    ResponseEntity<ReservationDTO> booking =
        call(
            HttpMethod.PUT,
            "/reservations",
            employee,
            new ReservationInputDTO(
                null,
                client.getId(),
                projection.id(),
                Set.of(seats.get(0).id(), seats.get(1).id())),
            ReservationDTO.class);

    assertThat(booking.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(booking.getBody().seatIds()).hasSize(2);
    assertThat(booking.getBody().totalPrice()).isEqualByComparingTo("24000.00");
    assertThat(booking.getBody().createdAt()).isNotNull();

    // Sold seats disappear from the availability list.
    assertThat(availableSeats(projection)).hasSize(1);

    // The client can read their own booking, and only that one.
    assertThat(get("/reservations/" + booking.getBody().id(), tokenOf(client)).getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  @Test
  void aSeatCannotBeSoldTwiceForTheSameProjection() {
    String manager = tokenOf(Userrole.MANAGER);
    User client = persistUser(Userrole.CLIENT);
    RoomDTO room = createRoom(manager, "B", 2);
    ProjectionDTO projection =
        createProjection(
            manager, createMovie(manager, "Arrival", Duration.ofMinutes(116)), room, EVENING);
    SeatDTO seat = seatsOfRoom(manager, room).get(0);

    ReservationInputDTO body =
        new ReservationInputDTO(null, client.getId(), projection.id(), Set.of(seat.id()));

    assertThat(put("/reservations", manager, body).getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(put("/reservations", manager, body).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void aSeatFromAnotherRoomIsRefused() {
    String manager = tokenOf(Userrole.MANAGER);
    User client = persistUser(Userrole.CLIENT);
    RoomDTO showing = createRoom(manager, "C", 2);
    RoomDTO elsewhere = createRoom(manager, "D", 2);
    ProjectionDTO projection =
        createProjection(
            manager, createMovie(manager, "Her", Duration.ofMinutes(126)), showing, EVENING);

    ResponseEntity<String> response =
        put(
            "/reservations",
            manager,
            new ReservationInputDTO(
                null,
                client.getId(),
                projection.id(),
                Set.of(seatsOfRoom(manager, elsewhere).get(0).id())));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void aRoomCannotHostTwoOverlappingProjections() {
    String manager = tokenOf(Userrole.MANAGER);
    RoomDTO room = createRoom(manager, "E", 2);
    MovieDTO movie = createMovie(manager, "Dune", Duration.ofMinutes(120));
    createProjection(manager, movie, room, EVENING);

    ResponseEntity<String> clash =
        put(
            "/projections",
            manager,
            new ProjectionInputDTO(
                null,
                movie.id(),
                room.id(),
                EVENING.plus(Duration.ofMinutes(30)),
                new BigDecimal("12000.00")));

    assertThat(clash.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    ResponseEntity<String> later =
        put(
            "/projections",
            manager,
            new ProjectionInputDTO(
                null,
                movie.id(),
                room.id(),
                EVENING.plus(Duration.ofHours(3)),
                new BigDecimal("12000.00")));

    assertThat(later.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void putMovie_withAnIdUpdatesInsteadOfCreatingADuplicate() {
    String manager = tokenOf(Userrole.MANAGER);
    MovieDTO created = createMovie(manager, "Old title", Duration.ofMinutes(90));

    ResponseEntity<MovieDTO> updated =
        call(
            HttpMethod.PUT,
            "/movies",
            manager,
            new MovieInputDTO(
                created.id(),
                "New title",
                Set.of(Genre.DRAMA),
                "Rewritten",
                Duration.ofMinutes(95)),
            MovieDTO.class);

    assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updated.getBody().id()).isEqualTo(created.id());
    assertThat(updated.getBody().title()).isEqualTo("New title");
    assertThat(movieRepository.count()).isEqualTo(1);
  }

  @Test
  void putMovie_rejectsAZeroDuration() {
    ResponseEntity<String> response =
        put(
            "/movies",
            tokenOf(Userrole.MANAGER),
            new MovieInputDTO(null, "Zero", Set.of(Genre.COMEDY), null, Duration.ZERO));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void reservationsMe_returnsOnlyTheCallersBookings() {
    String manager = tokenOf(Userrole.MANAGER);
    User mine = persistUser(Userrole.CLIENT);
    User theirs = persistUser(Userrole.CLIENT);
    RoomDTO room = createRoom(manager, "F", 3);
    ProjectionDTO projection =
        createProjection(
            manager, createMovie(manager, "Dune", Duration.ofMinutes(120)), room, EVENING);
    List<SeatDTO> seats = seatsOfRoom(manager, room);

    put(
        "/reservations",
        manager,
        new ReservationInputDTO(null, mine.getId(), projection.id(), Set.of(seats.get(0).id())));
    put(
        "/reservations",
        manager,
        new ReservationInputDTO(null, theirs.getId(), projection.id(), Set.of(seats.get(1).id())));

    ResponseEntity<List<ReservationDTO>> response =
        rest.exchange(
            url("/reservations/me"),
            HttpMethod.GET,
            new HttpEntity<>(bearer(tokenOf(mine))),
            new ParameterizedTypeReference<List<ReservationDTO>>() {});

    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody().get(0).userId()).isEqualTo(mine.getId());
  }

  // --- helpers --------------------------------------------------------------

  private RoomDTO createRoom(String token, String number, int capacity) {
    return call(HttpMethod.PUT, "/rooms", token, new RoomInputDTO(number, capacity), RoomDTO.class)
        .getBody();
  }

  private MovieDTO createMovie(String token, String title, Duration duration) {
    return call(
            HttpMethod.PUT,
            "/movies",
            token,
            new MovieInputDTO(null, title, Set.of(Genre.SCI_FI), "A description", duration),
            MovieDTO.class)
        .getBody();
  }

  private ProjectionDTO createProjection(
      String token, MovieDTO movie, RoomDTO room, Instant datetime) {
    return call(
            HttpMethod.PUT,
            "/projections",
            token,
            new ProjectionInputDTO(
                null, movie.id(), room.id(), datetime, new BigDecimal("12000.00")),
            ProjectionDTO.class)
        .getBody();
  }

  private List<SeatDTO> seatsOfRoom(String token, RoomDTO room) {
    return rest.exchange(
            url("/rooms/" + room.id() + "/seats"),
            HttpMethod.GET,
            new HttpEntity<>(bearer(token)),
            new ParameterizedTypeReference<List<SeatDTO>>() {})
        .getBody();
  }

  private List<SeatDTO> availableSeats(ProjectionDTO projection) {
    return rest.exchange(
            url("/projections/" + projection.id() + "/available-seats"),
            HttpMethod.GET,
            new HttpEntity<>(bearer(null)),
            new ParameterizedTypeReference<List<SeatDTO>>() {})
        .getBody();
  }
}
