package com.example.demo.cinema.mapper;

import com.example.demo.cinema.dto.SeatDTO;
import com.example.demo.cinema.entity.Seat;

public class SeatMapper {

  private SeatMapper() {}

  public static SeatDTO toDTO(Seat seat) {
    return new SeatDTO(seat.getId(), seat.getNumber(), seat.getRoom().getId());
  }
}
