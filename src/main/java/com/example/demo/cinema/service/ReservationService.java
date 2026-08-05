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
import com.example.demo.endpoint.rest.exception.NotFoundException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
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

  public ReservationDTO create(ReservationInputDTO input) {
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
    Set<Seat> seats = resolveSeats(input.seatIds());

    Reservation reservation =
        Reservation.builder()
            .user(user)
            .projection(projection)
            .seats(seats)
            .createdAt(Instant.now())
            .build();
    return ReservationMapper.toDTO(reservationRepository.save(reservation));
  }

  private Set<Seat> resolveSeats(Set<UUID> seatIds) {
    if (seatIds == null || seatIds.isEmpty()) {
      throw new BadRequestException("seatIds must not be empty");
    }
    Set<Seat> seats = new HashSet<>(seatRepository.findAllById(seatIds));
    if (seats.size() != seatIds.size()) {
      Set<UUID> foundIds = seats.stream().map(Seat::getId).collect(toSet());
      Set<UUID> missing = new HashSet<>(seatIds);
      missing.removeAll(foundIds);
      throw new NotFoundException("Seat(s) not found: " + missing);
    }
    return seats;
  }

  private Reservation getOrThrow(UUID id) {
    return reservationRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Reservation " + id));
  }
}
