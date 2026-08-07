package com.example.demo.cinema.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.demo.cinema.dto.ProjectionDTO;
import com.example.demo.cinema.dto.ProjectionInputDTO;
import com.example.demo.cinema.dto.SeatDTO;
import com.example.demo.cinema.entity.Movie;
import com.example.demo.cinema.entity.Projection;
import com.example.demo.cinema.entity.Room;
import com.example.demo.cinema.entity.Seat;
import com.example.demo.cinema.repository.MovieRepository;
import com.example.demo.cinema.repository.ProjectionRepository;
import com.example.demo.cinema.repository.ReservationRepository;
import com.example.demo.cinema.repository.RoomRepository;
import com.example.demo.cinema.repository.SeatRepository;
import com.example.demo.cinema.support.TestFixtures;
import com.example.demo.endpoint.rest.exception.ConflictException;
import com.example.demo.endpoint.rest.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectionServiceTest {

  private static final Instant EVENING = Instant.parse("2026-09-10T18:00:00Z");

  @Mock private ProjectionRepository projectionRepository;
  @Mock private MovieRepository movieRepository;
  @Mock private RoomRepository roomRepository;
  @Mock private SeatRepository seatRepository;
  @Mock private ReservationRepository reservationRepository;

  private ProjectionService projectionService;

  private final UUID movieId = UUID.randomUUID();
  private final UUID roomId = UUID.randomUUID();
  private Movie movie;
  private Room room;

  @BeforeEach
  void setUp() {
    projectionService =
        new ProjectionService(
            projectionRepository,
            movieRepository,
            roomRepository,
            seatRepository,
            reservationRepository);
    movie = TestFixtures.movie(movieId, "Dune", Duration.ofMinutes(120));
    room = TestFixtures.room(roomId, "A", 3);
  }

  @Test
  void findAll_returnsMappedDtos() {
    when(projectionRepository.findAll())
        .thenReturn(List.of(TestFixtures.projection(UUID.randomUUID(), movie, room, EVENING)));

    List<ProjectionDTO> result = projectionService.findAll();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).movieId()).isEqualTo(movieId);
    assertThat(result.get(0).roomId()).isEqualTo(roomId);
    assertThat(result.get(0).datetime()).isEqualTo(EVENING);
  }

  @Test
  void findById_returnsDto() {
    UUID id = UUID.randomUUID();
    when(projectionRepository.findById(id))
        .thenReturn(Optional.of(TestFixtures.projection(id, movie, room, EVENING)));

    assertThat(projectionService.findById(id).id()).isEqualTo(id);
  }

  @Test
  void findById_throwsNotFound_whenAbsent() {
    UUID id = UUID.randomUUID();
    when(projectionRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> projectionService.findById(id)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void upsert_createsProjection() {
    when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(projectionRepository.findByRoomId(roomId)).thenReturn(List.of());
    when(projectionRepository.save(any(Projection.class)))
        .thenAnswer(
            call -> {
              Projection projection = call.getArgument(0);
              projection.setId(UUID.randomUUID());
              return projection;
            });

    ProjectionDTO result =
        projectionService.upsert(
            new ProjectionInputDTO(null, movieId, roomId, EVENING, new BigDecimal("10000")));

    assertThat(result.id()).isNotNull();
    assertThat(result.seatPrice()).isEqualByComparingTo("10000");
  }

  @Test
  void upsert_throwsNotFound_whenMovieIsUnknown() {
    when(movieRepository.findById(movieId)).thenReturn(Optional.empty());
    ProjectionInputDTO input =
        new ProjectionInputDTO(null, movieId, roomId, EVENING, BigDecimal.TEN);

    assertThatThrownBy(() -> projectionService.upsert(input))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Movie");
  }

  @Test
  void upsert_throwsNotFound_whenRoomIsUnknown() {
    when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
    when(roomRepository.findById(roomId)).thenReturn(Optional.empty());
    ProjectionInputDTO input =
        new ProjectionInputDTO(null, movieId, roomId, EVENING, BigDecimal.TEN);

    assertThatThrownBy(() -> projectionService.upsert(input))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Room");
  }

  @Test
  void upsert_throwsConflict_whenTheRoomIsAlreadyBusyAtThatTime() {
    when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(projectionRepository.findByRoomId(roomId))
        .thenReturn(List.of(TestFixtures.projection(UUID.randomUUID(), movie, room, EVENING)));

    ProjectionInputDTO input =
        new ProjectionInputDTO(
            null, movieId, roomId, EVENING.plus(Duration.ofMinutes(30)), BigDecimal.TEN);

    assertThatThrownBy(() -> projectionService.upsert(input))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("overlapping");
  }

  @Test
  void upsert_acceptsAProjectionStartingAfterThePreviousOneEnds() {
    when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(projectionRepository.findByRoomId(roomId))
        .thenReturn(List.of(TestFixtures.projection(UUID.randomUUID(), movie, room, EVENING)));
    when(projectionRepository.save(any(Projection.class))).thenAnswer(call -> call.getArgument(0));

    ProjectionDTO result =
        projectionService.upsert(
            new ProjectionInputDTO(
                null, movieId, roomId, EVENING.plus(Duration.ofHours(3)), new BigDecimal("15000")));

    assertThat(result.datetime()).isEqualTo(EVENING.plus(Duration.ofHours(3)));
  }

  @Test
  void upsert_updatesExistingProjection_withoutClashingWithItself() {
    UUID id = UUID.randomUUID();
    Projection existing = TestFixtures.projection(id, movie, room, EVENING);
    when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
    when(projectionRepository.findById(id)).thenReturn(Optional.of(existing));
    when(projectionRepository.findByRoomId(roomId)).thenReturn(List.of(existing));
    when(projectionRepository.save(any(Projection.class))).thenAnswer(call -> call.getArgument(0));

    ProjectionDTO result =
        projectionService.upsert(
            new ProjectionInputDTO(id, movieId, roomId, EVENING, new BigDecimal("20000")));

    assertThat(result.id()).isEqualTo(id);
    assertThat(result.seatPrice()).isEqualByComparingTo("20000");
  }

  @Test
  void findAvailableSeats_excludesSeatsAlreadySold() {
    UUID projectionId = UUID.randomUUID();
    Seat a1 = TestFixtures.seat(UUID.randomUUID(), "A1", room);
    Seat a2 = TestFixtures.seat(UUID.randomUUID(), "A2", room);
    Seat a3 = TestFixtures.seat(UUID.randomUUID(), "A3", room);

    when(projectionRepository.findById(projectionId))
        .thenReturn(Optional.of(TestFixtures.projection(projectionId, movie, room, EVENING)));
    when(reservationRepository.findBookedSeatIds(projectionId)).thenReturn(Set.of(a2.getId()));
    when(seatRepository.findByRoomIdOrderByNumberAsc(roomId)).thenReturn(List.of(a1, a2, a3));

    List<SeatDTO> available = projectionService.findAvailableSeats(projectionId);

    assertThat(available).extracting(SeatDTO::number).containsExactly("A1", "A3");
  }

  @Test
  void seatsOfProjectionRoom_returnsEveryPhysicalSeat() {
    UUID projectionId = UUID.randomUUID();
    when(projectionRepository.findById(projectionId))
        .thenReturn(Optional.of(TestFixtures.projection(projectionId, movie, room, EVENING)));
    when(seatRepository.findByRoomIdOrderByNumberAsc(roomId))
        .thenReturn(List.of(TestFixtures.seat(UUID.randomUUID(), "A1", room)));

    assertThat(projectionService.seatsOfProjectionRoom(projectionId)).hasSize(1);
  }
}
