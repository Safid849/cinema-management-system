package com.example.demo.cinema.mapper;

import static java.util.stream.Collectors.toSet;

import com.example.demo.cinema.dto.ReservationDTO;
import com.example.demo.cinema.entity.Reservation;
import com.example.demo.cinema.entity.Seat;

public final class ReservationMapper {

  private ReservationMapper() {}

  public static ReservationDTO toDTO(Reservation reservation) {
    return new ReservationDTO(
        reservation.getId(),
        reservation.getUser().getId(),
        reservation.getProjection().getId(),
        reservation.getSeats().stream().map(Seat::getId).collect(toSet()),
        reservation.getCreatedAt());
  }
}
