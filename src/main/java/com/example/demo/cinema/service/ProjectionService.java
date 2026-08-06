package com.example.demo.cinema.service;

import com.example.demo.cinema.dto.ProjectionDTO;
import com.example.demo.cinema.dto.ProjectionInputDTO;
import com.example.demo.cinema.dto.SeatDTO;
import com.example.demo.cinema.entity.Movie;
import com.example.demo.cinema.entity.Projection;
import com.example.demo.cinema.entity.Room;
import com.example.demo.cinema.mapper.ProjectionMapper;
import com.example.demo.cinema.mapper.SeatMapper;
import com.example.demo.cinema.repository.MovieRepository;
import com.example.demo.cinema.repository.ProjectionRepository;
import com.example.demo.cinema.repository.ReservationRepository;
import com.example.demo.cinema.repository.RoomRepository;
import com.example.demo.cinema.repository.SeatRepository;
import com.example.demo.endpoint.rest.exception.ConflictException;
import com.example.demo.endpoint.rest.exception.NotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class ProjectionService {

  private final ProjectionRepository projectionRepository;
  private final MovieRepository movieRepository;
  private final RoomRepository roomRepository;
  private final SeatRepository seatRepository;
  private final ReservationRepository reservationRepository;

  public List<ProjectionDTO> findAll() {
    return projectionRepository.findAll().stream().map(ProjectionMapper::toDTO).toList();
  }

  public ProjectionDTO findById(UUID id) {
    return ProjectionMapper.toDTO(getOrThrow(id));
  }

  /** PUT semantics: create with no id, update the named projection otherwise. */
  @Transactional
  public ProjectionDTO upsert(ProjectionInputDTO input) {
    Movie movie =
        movieRepository
            .findById(input.movieId())
            .orElseThrow(() -> new NotFoundException("Movie " + input.movieId()));
    Room room =
        roomRepository
            .findById(input.roomId())
            .orElseThrow(() -> new NotFoundException("Room " + input.roomId()));

    Projection projection = input.id() == null ? new Projection() : getOrThrow(input.id());
    requireFreeSlot(room, movie, input.datetime(), input.id());

    projection.setMovie(movie);
    projection.setRoom(room);
    projection.setDatetime(input.datetime());
    projection.setSeatPrice(input.seatPrice());

    return ProjectionMapper.toDTO(projectionRepository.save(projection));
  }

  /** Every physical seat of the room the projection runs in. */
  public List<SeatDTO> seatsOfProjectionRoom(UUID projectionId) {
    Projection projection = getOrThrow(projectionId);
    return seatRepository.findByRoomIdOrderByNumberAsc(projection.getRoom().getId()).stream()
        .map(SeatMapper::toDTO)
        .toList();
  }

  /** Same list, minus the seats already sold for this very projection. */
  public List<SeatDTO> findAvailableSeats(UUID projectionId) {
    Projection projection = getOrThrow(projectionId);
    Set<UUID> booked = reservationRepository.findBookedSeatIds(projectionId);

    return seatRepository.findByRoomIdOrderByNumberAsc(projection.getRoom().getId()).stream()
        .filter(seat -> !booked.contains(seat.getId()))
        .map(SeatMapper::toDTO)
        .toList();
  }

  /**
   * A room can only host one film at a time: the new [start, start + duration) window must not
   * intersect any other projection already scheduled in that room.
   */
  private void requireFreeSlot(Room room, Movie movie, Instant start, UUID selfId) {
    Instant end = start.plus(movie.getDuration());

    boolean clashes =
        projectionRepository.findByRoomId(room.getId()).stream()
            .filter(other -> !Objects.equals(other.getId(), selfId))
            .anyMatch(other -> overlaps(start, end, other));

    if (clashes) {
      throw new ConflictException(
          "Room " + room.getNumber() + " already has an overlapping projection at " + start);
    }
  }

  private static boolean overlaps(Instant start, Instant end, Projection other) {
    Duration otherDuration = other.getMovie().getDuration();
    Instant otherStart = other.getDatetime();
    Instant otherEnd = otherStart.plus(otherDuration);
    return start.isBefore(otherEnd) && otherStart.isBefore(end);
  }

  private Projection getOrThrow(UUID id) {
    return projectionRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Projection " + id));
  }
}
