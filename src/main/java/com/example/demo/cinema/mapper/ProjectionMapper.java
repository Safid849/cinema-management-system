package com.example.demo.cinema.mapper;

import com.example.demo.cinema.dto.ProjectionDTO;
import com.example.demo.cinema.entity.Projection;

public final class ProjectionMapper {

  private ProjectionMapper() {}

  public static ProjectionDTO toDTO(Projection projection) {
    return new ProjectionDTO(
        projection.getId(),
        projection.getMovie().getId(),
        projection.getRoom().getId(),
        projection.getDatetime(),
        projection.getSeatPrice());
  }
}
