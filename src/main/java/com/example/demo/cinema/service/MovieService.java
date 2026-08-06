package com.example.demo.cinema.service;

import com.example.demo.cinema.dto.MovieDTO;
import com.example.demo.cinema.dto.MovieInputDTO;
import com.example.demo.cinema.entity.Genre;
import com.example.demo.cinema.entity.Movie;
import com.example.demo.cinema.mapper.MovieMapper;
import com.example.demo.cinema.repository.MovieRepository;
import com.example.demo.endpoint.rest.exception.BadRequestException;
import com.example.demo.endpoint.rest.exception.NotFoundException;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

  private final MovieRepository movieRepository;

  public List<MovieDTO> findAll() {
    return movieRepository.findAll().stream().map(MovieMapper::toDTO).toList();
  }

  public MovieDTO findById(UUID id) {
    return MovieMapper.toDTO(getOrThrow(id));
  }

  /** PUT semantics: create when the payload carries no id, update the named movie otherwise. */
  @Transactional
  public MovieDTO upsert(MovieInputDTO input) {
    requirePositiveDuration(input.duration());

    Movie movie = input.id() == null ? new Movie() : getOrThrow(input.id());
    movie.setTitle(input.title());
    replaceGenres(movie, input.genre());
    movie.setDescription(input.description());
    movie.setDuration(input.duration());

    return MovieMapper.toDTO(movieRepository.save(movie));
  }

  /**
   * Hibernate tracks the identity of an {@code @ElementCollection}, so the existing set is emptied
   * and refilled rather than swapped for a brand new one.
   */
  private static void replaceGenres(Movie movie, Set<Genre> genres) {
    Set<Genre> wanted = genres == null ? Set.of() : genres;
    if (movie.getGenre() == null) {
      movie.setGenre(new LinkedHashSet<>(wanted));
      return;
    }
    movie.getGenre().clear();
    movie.getGenre().addAll(wanted);
  }

  private static void requirePositiveDuration(Duration duration) {
    if (duration == null || duration.isZero() || duration.isNegative()) {
      throw new BadRequestException("duration must be strictly positive");
    }
  }

  private Movie getOrThrow(UUID id) {
    return movieRepository.findById(id).orElseThrow(() -> new NotFoundException("Movie " + id));
  }
}
