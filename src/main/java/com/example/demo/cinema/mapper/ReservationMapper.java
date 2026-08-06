package com.example.demo.cinema.mapper;

import static java.util.stream.Collectors.toSet;

import com.example.demo.cinema.dto.ReservationDTO;
import com.example.demo.cinema.entity.Reservation;
import com.example.demo.cinema.entity.Seat;
import java.math.BigDecimal;

public final class ReservationMapper {

  private ReservationMapper() {}

  public static ReservationDTO toDTO(Reservation reservation) {
    int seatCount = reservation.getSeats() == null ? 0 : reservation.getSeats().size();
    BigDecimal totalPrice =
        reservation.getProjection().getSeatPrice().multiply(BigDecimal.valueOf(seatCount));

    return new ReservationDTO(
        reservation.getId(),
        reservation.getUser().getId(),
        reservation.getProjection().getId(),
        reservation.getSeats() == null
            ? java.util.Set.of()
            : reservation.getSeats().stream().map(Seat::getId).collect(toSet()),
        totalPrice,
        reservation.getCreatedAt());
  }
}
