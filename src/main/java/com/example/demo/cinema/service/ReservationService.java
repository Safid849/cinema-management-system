package com.example.demo.cinema.service;

import static java.util.stream.Collectors.toSet;

import com.example.demo.cinema.dto.ReservationDTO;
import com.example.demo.cinema.dto.ReservationInputDTO;
import com.example.demo.cinema.entity.Projection;
import com.example.demo.cinema.entity.Reservation;
import com.example.demo.cinema.entity.Seat;
import com.example.demo.cinema.entity.User;
import com.example.demo.cinema.mapper.ReservationMapper;
import com.example.demo.cinema.repository.ProjectionRepository;
import com.example.demo.cinema.repository.ReservationRepository;
import com.example.demo.cinema.repository.SeatRepository;
import com.example.demo.cinema.repository.UserRepository;
import com.example.demo.endpoint.rest.exception.BadRequestException;
import com.example.demo.endpoint.rest.exception.ConflictException;
import com.example.demo.endpoint.rest.exception.NotFoundException;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

  private final ReservationRepository reservationRepository;
  private final ProjectionRepository projectionRepository;
  private final UserRepository userRepository;
  private final SeatRepository seatRepository;

  public List<ReservationDTO> findAll() {
    return reservationRepository.findAll().stream().map(ReservationMapper::toDTO).toList();
  }

  public ReservationDTO findById(UUID id) {
    return ReservationMapper.toDTO(getOrThrow(id));
  }

  public List<ReservationDTO> findByUser(UUID userId) {
    return reservationRepository.findByUserId(userId).stream()
        .map(ReservationMapper::toDTO)
        .toList();
  }

  /** PUT semantics: create with no id, update the named reservation otherwise. */
  @Transactional
  public ReservationDTO upsert(ReservationInputDTO input) {
    if (input.userId() == null) {
      throw new BadRequestException("userId is required");
    }
    User user =
        userRepository
            .findById(input.userId())
            .orElseThrow(() -> new NotFoundException("User " + input.userId()));
    Projection projection =
        projectionRepository
            .findById(input.projectionId())
            .orElseThrow(() -> new NotFoundException("Projection " + input.projectionId()));

    Reservation reservation = input.id() == null ? new Reservation() : getOrThrow(input.id());

    Set<Seat> seats = resolveSeats(input.seatIds());
    requireSeatsOfRoom(seats, projection);
    requireSeatsFree(seats, projection, reservation);

    reservation.setUser(user);
    reservation.setProjection(projection);
    reservation.setSeats(new LinkedHashSet<>(seats));
    if (reservation.getCreatedAt() == null) {
      reservation.setCreatedAt(Instant.now());
    }

    return ReservationMapper.toDTO(reservationRepository.save(reservation));
  }

  @Transactional
  public void delete(UUID id) {
    reservationRepository.delete(getOrThrow(id));
  }

  private Set<Seat> resolveSeats(Set<UUID> seatIds) {
    if (seatIds == null || seatIds.isEmpty()) {
      throw new BadRequestException("seatIds must not be empty");
    }
    Set<Seat> seats = new LinkedHashSet<>(seatRepository.findAllById(seatIds));
    if (seats.size() != seatIds.size()) {
      Set<UUID> foundIds = seats.stream().map(Seat::getId).collect(toSet());
      Set<UUID> missing = new HashSet<>(seatIds);
      missing.removeAll(foundIds);
      throw new NotFoundException("Seat(s) not found: " + missing);
    }
    return seats;
  }

  /** You cannot book a seat that is physically in another room than the one showing the film. */
  private static void requireSeatsOfRoom(Set<Seat> seats, Projection projection) {
    UUID roomId = projection.getRoom().getId();
    Set<UUID> foreign =
        seats.stream()
            .filter(seat -> !roomId.equals(seat.getRoom().getId()))
            .map(Seat::getId)
            .collect(toSet());

    if (!foreign.isEmpty()) {
      throw new ConflictException(
          "Seat(s) " + foreign + " do not belong to the room of projection " + projection.getId());
    }
  }

  /**
   * One seat is sold once per projection. Seats the reservation being updated already holds stay
   * legal, otherwise editing a reservation would clash with itself.
   */
  private void requireSeatsFree(Set<Seat> seats, Projection projection, Reservation reservation) {
    Set<UUID> booked = new HashSet<>(reservationRepository.findBookedSeatIds(projection.getId()));
    if (reservation.getSeats() != null) {
      booked.removeAll(reservation.getSeats().stream().map(Seat::getId).collect(toSet()));
    }

    Set<UUID> taken = seats.stream().map(Seat::getId).filter(booked::contains).collect(toSet());

    if (!taken.isEmpty()) {
      throw new ConflictException(
          "Seat(s) " + taken + " are already booked for projection " + projection.getId());
    }
  }

  private Reservation getOrThrow(UUID id) {
    return reservationRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Reservation " + id));
  }
}
