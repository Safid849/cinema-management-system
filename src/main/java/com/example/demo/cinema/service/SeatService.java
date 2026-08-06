package com.example.demo.cinema.service;

import com.example.demo.cinema.dto.SeatDTO;
import com.example.demo.cinema.mapper.SeatMapper;
import com.example.demo.cinema.repository.RoomRepository;
import com.example.demo.cinema.repository.SeatRepository;
import com.example.demo.endpoint.rest.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class SeatService {

  private final SeatRepository seatRepository;
  private final RoomRepository roomRepository;

  public List<SeatDTO> listByRoom(UUID roomId) {
    if (!roomRepository.existsById(roomId)) {
      throw new NotFoundException("Room " + roomId);
    }
    return seatRepository.findByRoomId(roomId).stream().map(SeatMapper::toDTO).toList();
  }
}
