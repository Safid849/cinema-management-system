package com.example.demo.cinema.service;

import com.example.demo.cinema.dto.MovieDTO;
import com.example.demo.cinema.dto.MovieInputDTO;
import com.example.demo.cinema.entity.Movie;
import com.example.demo.cinema.mapper.MovieMapper;
import com.example.demo.cinema.repository.MovieRepository;
import com.example.demo.endpoint.rest.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MovieService {

  private final MovieRepository movieRepository;

  public List<MovieDTO> findAll() {
    return movieRepository.findAll().stream().map(MovieMapper::toDTO).toList();
  }

  public MovieDTO findById(UUID id) {
    return MovieMapper.toDTO(getOrThrow(id));
  }

  public MovieDTO create(MovieInputDTO input) {
    Movie movie =
        Movie.builder()
            .title(input.title())
            .genre(input.genre())
            .description(input.description())
            .duration(input.duration())
            .build();
    return MovieMapper.toDTO(movieRepository.save(movie));
  }

  private Movie getOrThrow(UUID id) {
    return movieRepository.findById(id).orElseThrow(() -> new NotFoundException("Movie " + id));
  }
}
