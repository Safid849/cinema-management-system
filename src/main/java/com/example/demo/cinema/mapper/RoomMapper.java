package com.example.demo.cinema.mapper;

import com.example.demo.cinema.dto.RoomDTO;
import com.example.demo.cinema.entity.Room;

public class RoomMapper {

  private RoomMapper() {}

  public static RoomDTO toDTO(Room room) {
    return new RoomDTO(room.getId(), room.getNumber(), room.getCapacity());
  }
}
