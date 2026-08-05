package com.example.demo.cinema.controller;

import com.example.demo.cinema.dto.ProjectionDTO;
import com.example.demo.cinema.dto.ProjectionInputDTO;
import com.example.demo.cinema.service.ProjectionService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

  @PutMapping("/projections")
  public ResponseEntity<ProjectionDTO> upsertProjection(@RequestBody ProjectionInputDTO input) {
    return new ResponseEntity<>(projectionService.create(input), HttpStatus.OK);
  }
}
