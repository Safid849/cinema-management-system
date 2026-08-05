package com.example.demo.cinema.mapper;

import com.example.demo.cinema.dto.MovieDTO;
import com.example.demo.cinema.entity.Movie;

public final class MovieMapper {

  private MovieMapper() {}

  public static MovieDTO toDTO(Movie movie) {
    return new MovieDTO(
        movie.getId(),
        movie.getTitle(),
        movie.getGenre(),
        movie.getDescription(),
        movie.getDuration());
  }
}
