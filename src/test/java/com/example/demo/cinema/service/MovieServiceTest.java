package com.example.demo.cinema.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.demo.cinema.dto.MovieDTO;
import com.example.demo.cinema.dto.MovieInputDTO;
import com.example.demo.cinema.entity.Genre;
import com.example.demo.cinema.entity.Movie;
import com.example.demo.cinema.repository.MovieRepository;
import com.example.demo.cinema.support.TestFixtures;
import com.example.demo.endpoint.rest.exception.BadRequestException;
import com.example.demo.endpoint.rest.exception.NotFoundException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

  @Mock private MovieRepository movieRepository;

  private MovieService movieService;

  @BeforeEach
  void setUp() {
    movieService = new MovieService(movieRepository);
  }

  @Test
  void findAll_returnsMappedDtos() {
    Movie movie = TestFixtures.movie(UUID.randomUUID(), "Dune", Duration.ofMinutes(155));
    when(movieRepository.findAll()).thenReturn(List.of(movie));

    List<MovieDTO> result = movieService.findAll();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).title()).isEqualTo("Dune");
    assertThat(result.get(0).duration()).isEqualTo(Duration.ofMinutes(155));
  }

  @Test
  void findById_returnsDto() {
    UUID id = UUID.randomUUID();
    when(movieRepository.findById(id))
        .thenReturn(Optional.of(TestFixtures.movie(id, "Arrival", Duration.ofMinutes(116))));

    assertThat(movieService.findById(id).title()).isEqualTo("Arrival");
  }

  @Test
  void findById_throwsNotFound_whenAbsent() {
    UUID id = UUID.randomUUID();
    when(movieRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> movieService.findById(id)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void upsert_createsMovie_whenIdIsNull() {
    when(movieRepository.save(any(Movie.class)))
        .thenAnswer(
            call -> {
              Movie movie = call.getArgument(0);
              movie.setId(UUID.randomUUID());
              return movie;
            });

    MovieDTO result =
        movieService.upsert(
            new MovieInputDTO(
                null, "Interstellar", Set.of(Genre.SCI_FI), "Space", Duration.ofMinutes(169)));

    assertThat(result.id()).isNotNull();
    assertThat(result.title()).isEqualTo("Interstellar");
    assertThat(result.genre()).containsExactly(Genre.SCI_FI);
  }

  @Test
  void upsert_updatesExistingMovie_whenIdIsProvided() {
    UUID id = UUID.randomUUID();
    Movie existing = TestFixtures.movie(id, "Old title", Duration.ofMinutes(90));
    when(movieRepository.findById(id)).thenReturn(Optional.of(existing));
    when(movieRepository.save(any(Movie.class))).thenAnswer(call -> call.getArgument(0));

    MovieDTO result =
        movieService.upsert(
            new MovieInputDTO(
                id, "New title", Set.of(Genre.DRAMA), "Updated", Duration.ofMinutes(120)));

    assertThat(result.id()).isEqualTo(id);
    assertThat(result.title()).isEqualTo("New title");
    assertThat(existing.getGenre()).containsExactly(Genre.DRAMA);
  }

  @Test
  void upsert_throwsNotFound_whenUpdatingAnUnknownMovie() {
    UUID id = UUID.randomUUID();
    when(movieRepository.findById(id)).thenReturn(Optional.empty());
    MovieInputDTO input =
        new MovieInputDTO(id, "Ghost", Set.of(Genre.THRILLER), null, Duration.ofMinutes(100));

    assertThatThrownBy(() -> movieService.upsert(input)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void upsert_rejectsNonPositiveDuration() {
    MovieInputDTO input =
        new MovieInputDTO(null, "Zero", Set.of(Genre.COMEDY), null, Duration.ZERO);

    assertThatThrownBy(() -> movieService.upsert(input))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("duration");
  }

  @Test
  void genreEnum_exposesEveryValueOfTheSpecification() {
    assertThat(Genre.values()).hasSize(8);
    assertThat(Genre.valueOf("ANIMATION")).isEqualTo(Genre.ANIMATION);
  }
}
