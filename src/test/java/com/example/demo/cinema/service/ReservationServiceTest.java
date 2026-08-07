package com.example.demo.cinema.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.cinema.dto.ReservationDTO;
import com.example.demo.cinema.dto.ReservationInputDTO;
import com.example.demo.cinema.entity.Movie;
import com.example.demo.cinema.entity.Projection;
import com.example.demo.cinema.entity.Reservation;
import com.example.demo.cinema.entity.Room;
import com.example.demo.cinema.entity.Seat;
import com.example.demo.cinema.entity.User;
import com.example.demo.cinema.entity.Userrole;
import com.example.demo.cinema.repository.ProjectionRepository;
import com.example.demo.cinema.repository.ReservationRepository;
import com.example.demo.cinema.repository.SeatRepository;
import com.example.demo.cinema.repository.UserRepository;
import com.example.demo.cinema.support.TestFixtures;
import com.example.demo.endpoint.rest.exception.ConflictException;
import com.example.demo.endpoint.rest.exception.NotFoundException;
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
class ReservationServiceTest {

  private static final Instant EVENING = Instant.parse("2026-09-10T18:00:00Z");

  @Mock private ReservationRepository reservationRepository;
  @Mock private ProjectionRepository projectionRepository;
  @Mock private UserRepository userRepository;
  @Mock private SeatRepository seatRepository;

  private ReservationService reservationService;

  private User client;
  private Room room;
  private Projection projection;
  private Seat a1;
  private Seat a2;

  @BeforeEach
  void setUp() {
    reservationService =
        new ReservationService(
            reservationRepository, projectionRepository, userRepository, seatRepository);

    client = TestFixtures.user(Userrole.CLIENT);
    room = TestFixtures.room(UUID.randomUUID(), "A", 10);
    Movie movie = TestFixtures.movie(UUID.randomUUID(), "Dune", Duration.ofMinutes(120));
    projection = TestFixtures.projection(UUID.randomUUID(), movie, room, EVENING);
    a1 = TestFixtures.seat(UUID.randomUUID(), "A1", room);
    a2 = TestFixtures.seat(UUID.randomUUID(), "A2", room);
  }

  @Test
  void findAll_returnsMappedDtos() {
    when(reservationRepository.findAll())
        .thenReturn(
            List.of(TestFixtures.reservation(UUID.randomUUID(), client, projection, a1, a2)));

    List<ReservationDTO> result = reservationService.findAll();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).seatIds()).containsExactlyInAnyOrder(a1.getId(), a2.getId());
    assertThat(result.get(0).totalPrice()).isEqualByComparingTo("24000.00");
  }

  @Test
  void findById_returnsDto() {
    UUID id = UUID.randomUUID();
    when(reservationRepository.findById(id))
        .thenReturn(Optional.of(TestFixtures.reservation(id, client, projection, a1)));

    ReservationDTO result = reservationService.findById(id);

    assertThat(result.id()).isEqualTo(id);
    assertThat(result.userId()).isEqualTo(client.getId());
  }

  @Test
  void findById_throwsNotFound_whenAbsent() {
    UUID id = UUID.randomUUID();
    when(reservationRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reservationService.findById(id)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void findByUser_returnsOnlyThatUsersReservations() {
    when(reservationRepository.findByUserId(client.getId()))
        .thenReturn(List.of(TestFixtures.reservation(UUID.randomUUID(), client, projection, a1)));

    assertThat(reservationService.findByUser(client.getId()))
        .singleElement()
        .satisfies(dto -> assertThat(dto.userId()).isEqualTo(client.getId()));
  }

  @Test
  void upsert_createsReservationAndStampsCreatedAt() {
    stubCreateDependencies();
    when(reservationRepository.findBookedSeatIds(projection.getId())).thenReturn(Set.of());
    when(reservationRepository.save(any(Reservation.class)))
        .thenAnswer(
            call -> {
              Reservation reservation = call.getArgument(0);
              reservation.setId(UUID.randomUUID());
              return reservation;
            });

    ReservationDTO result =
        reservationService.upsert(
            new ReservationInputDTO(
                null, client.getId(), projection.getId(), Set.of(a1.getId(), a2.getId())));

    assertThat(result.id()).isNotNull();
    assertThat(result.createdAt()).isNotNull();
    assertThat(result.seatIds()).containsExactlyInAnyOrder(a1.getId(), a2.getId());
  }

  @Test
  void upsert_throwsNotFound_whenUserIsUnknown() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());
    ReservationInputDTO input =
        new ReservationInputDTO(null, userId, projection.getId(), Set.of(a1.getId()));

    assertThatThrownBy(() -> reservationService.upsert(input))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("User");
  }

  @Test
  void upsert_throwsNotFound_whenProjectionIsUnknown() {
    when(userRepository.findById(client.getId())).thenReturn(Optional.of(client));
    when(projectionRepository.findById(projection.getId())).thenReturn(Optional.empty());
    ReservationInputDTO input =
        new ReservationInputDTO(null, client.getId(), projection.getId(), Set.of(a1.getId()));

    assertThatThrownBy(() -> reservationService.upsert(input))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Projection");
  }

  @Test
  void upsert_throwsNotFound_whenASeatDoesNotExist() {
    UUID ghostSeat = UUID.randomUUID();
    when(userRepository.findById(client.getId())).thenReturn(Optional.of(client));
    when(projectionRepository.findById(projection.getId())).thenReturn(Optional.of(projection));
    when(seatRepository.findAllById(Set.of(ghostSeat))).thenReturn(List.of());
    ReservationInputDTO input =
        new ReservationInputDTO(null, client.getId(), projection.getId(), Set.of(ghostSeat));

    assertThatThrownBy(() -> reservationService.upsert(input))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Seat(s) not found");
  }

  @Test
  void upsert_throwsConflict_whenASeatBelongsToAnotherRoom() {
    Room otherRoom = TestFixtures.room(UUID.randomUUID(), "B", 10);
    Seat foreignSeat = TestFixtures.seat(UUID.randomUUID(), "B1", otherRoom);

    when(userRepository.findById(client.getId())).thenReturn(Optional.of(client));
    when(projectionRepository.findById(projection.getId())).thenReturn(Optional.of(projection));
    when(seatRepository.findAllById(Set.of(foreignSeat.getId()))).thenReturn(List.of(foreignSeat));
    ReservationInputDTO input =
        new ReservationInputDTO(
            null, client.getId(), projection.getId(), Set.of(foreignSeat.getId()));

    assertThatThrownBy(() -> reservationService.upsert(input))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("do not belong to the room");
  }

  @Test
  void upsert_throwsConflict_whenASeatIsAlreadyBookedForThatProjection() {
    when(userRepository.findById(client.getId())).thenReturn(Optional.of(client));
    when(projectionRepository.findById(projection.getId())).thenReturn(Optional.of(projection));
    when(seatRepository.findAllById(Set.of(a1.getId()))).thenReturn(List.of(a1));
    when(reservationRepository.findBookedSeatIds(projection.getId()))
        .thenReturn(Set.of(a1.getId()));
    ReservationInputDTO input =
        new ReservationInputDTO(null, client.getId(), projection.getId(), Set.of(a1.getId()));

    assertThatThrownBy(() -> reservationService.upsert(input))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already booked");
  }

  @Test
  void upsert_letsAReservationKeepItsOwnSeatsWhenUpdated() {
    UUID reservationId = UUID.randomUUID();
    Reservation existing = TestFixtures.reservation(reservationId, client, projection, a1);

    when(userRepository.findById(client.getId())).thenReturn(Optional.of(client));
    when(projectionRepository.findById(projection.getId())).thenReturn(Optional.of(projection));
    when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(existing));
    when(seatRepository.findAllById(Set.of(a1.getId(), a2.getId()))).thenReturn(List.of(a1, a2));
    when(reservationRepository.findBookedSeatIds(projection.getId()))
        .thenReturn(Set.of(a1.getId()));
    when(reservationRepository.save(any(Reservation.class))).thenAnswer(c -> c.getArgument(0));

    ReservationDTO result =
        reservationService.upsert(
            new ReservationInputDTO(
                reservationId, client.getId(), projection.getId(), Set.of(a1.getId(), a2.getId())));

    assertThat(result.seatIds()).containsExactlyInAnyOrder(a1.getId(), a2.getId());
    assertThat(result.createdAt()).isEqualTo(Instant.parse("2026-01-15T10:00:00Z"));
  }

  @Test
  void delete_removesTheReservation() {
    UUID id = UUID.randomUUID();
    Reservation reservation = TestFixtures.reservation(id, client, projection, a1);
    when(reservationRepository.findById(id)).thenReturn(Optional.of(reservation));

    reservationService.delete(id);

    verify(reservationRepository).delete(reservation);
  }

  private void stubCreateDependencies() {
    when(userRepository.findById(client.getId())).thenReturn(Optional.of(client));
    when(projectionRepository.findById(projection.getId())).thenReturn(Optional.of(projection));
    when(seatRepository.findAllById(Set.of(a1.getId(), a2.getId()))).thenReturn(List.of(a1, a2));
  }
}
