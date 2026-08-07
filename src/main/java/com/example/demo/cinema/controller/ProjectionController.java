package com.example.demo.cinema.controller;

import com.example.demo.cinema.dto.ProjectionDTO;
import com.example.demo.cinema.dto.ProjectionInputDTO;
import com.example.demo.cinema.dto.SeatDTO;
import com.example.demo.cinema.service.ProjectionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ProjectionController {

  private final ProjectionService projectionService;

  @GetMapping("/projections")
  public List<ProjectionDTO> listProjections() {
    return projectionService.findAll();
  }

  @GetMapping("/projections/{id}")
  public ProjectionDTO getProjectionById(@PathVariable UUID id) {
    return projectionService.findById(id);
  }

  @GetMapping("/projections/{id}/seats")
  public List<SeatDTO> seatsOfProjection(@PathVariable UUID id) {
    return projectionService.seatsOfProjectionRoom(id);
  }

  @GetMapping("/projections/{id}/available-seats")
  public List<SeatDTO> availableSeats(@PathVariable UUID id) {
    return projectionService.findAvailableSeats(id);
  }

  @PutMapping("/projections")
  public ProjectionDTO upsertProjection(@Valid @RequestBody ProjectionInputDTO input) {
    return projectionService.upsert(input);
  }
}
