package com.example.demo.cinema.controller;

import com.example.demo.cinema.dto.SeatDTO;
import com.example.demo.cinema.service.SeatService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms/{roomId}/seats")
public class SeatController {

  private final SeatService seatService;

  public SeatController(SeatService seatService) {
    this.seatService = seatService;
  }

  @GetMapping
  public List<SeatDTO> listSeats(@PathVariable UUID roomId) {
    return seatService.listByRoom(roomId);
  }
}
