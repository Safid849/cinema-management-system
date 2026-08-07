package com.example.demo.cinema.service;

import com.example.demo.cinema.dto.RoomDTO;
import com.example.demo.cinema.dto.RoomInputDTO;
import com.example.demo.cinema.entity.Room;
import com.example.demo.cinema.entity.Seat;
import com.example.demo.cinema.mapper.RoomMapper;
import com.example.demo.cinema.repository.RoomRepository;
import com.example.demo.endpoint.rest.exception.BadRequestException;
import com.example.demo.endpoint.rest.exception.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

  private final RoomRepository roomRepository;

  public List<RoomDTO> listRooms() {
    return roomRepository.findAll().stream().map(RoomMapper::toDTO).toList();
  }

  public RoomDTO getRoomById(UUID id) {
    return RoomMapper.toDTO(getOrThrow(id));
  }

  @Transactional
  public RoomDTO upsert(RoomInputDTO input) {
    if (input.capacity() <= 0) {
      throw new BadRequestException("capacity must be strictly positive");
    }

    Room room =
        roomRepository
            .findByNumber(input.number())
            .orElseGet(
                () -> Room.builder().number(input.number()).seats(new ArrayList<>()).build());

    room.setCapacity(input.capacity());
    fillSeats(room, input.capacity());

    return RoomMapper.toDTO(roomRepository.save(room));
  }

  private static void fillSeats(Room room, int capacity) {
    if (room.getSeats() == null) {
      room.setSeats(new ArrayList<>());
    }
    for (int number = room.getSeats().size() + 1; number <= capacity; number++) {
      room.getSeats().add(Seat.builder().number(String.format("%03d", number)).room(room).build());
    }
  }

  private Room getOrThrow(UUID id) {
    return roomRepository.findById(id).orElseThrow(() -> new NotFoundException("Room " + id));
  }
}
