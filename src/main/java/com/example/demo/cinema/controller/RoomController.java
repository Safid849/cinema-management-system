package com.example.demo.cinema.controller;

import com.example.demo.cinema.dto.RoomDTO;
import com.example.demo.cinema.dto.RoomInputDTO;
import com.example.demo.cinema.service.RoomService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
public class RoomController {

  private final RoomService roomService;

  public RoomController(RoomService roomService) {
    this.roomService = roomService;
  }

  @GetMapping
  public List<RoomDTO> listRooms() {
    return roomService.listRooms();
  }

  @GetMapping("/{id}")
  public RoomDTO getRoomById(@PathVariable UUID id) {
    return roomService.getRoomById(id);
  }

  @PutMapping
  @PreAuthorize("hasRole('MANAGER')")
  public RoomDTO upsert(@RequestBody RoomInputDTO input) {
    return roomService.upsert(input);
  }
}
