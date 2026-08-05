package com.example.demo.cinema.controller;

import com.example.demo.cinema.dto.MovieDTO;
import com.example.demo.cinema.dto.MovieInputDTO;
import com.example.demo.cinema.service.MovieService;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class MovieController {

  private final MovieService movieService;

  @GetMapping("/movies")
  public List<MovieDTO> listMovies() {
    return movieService.findAll();
  }

  @GetMapping("/movies/{id}")
  public MovieDTO getMovieById(@PathVariable UUID id) {
    return movieService.findById(id);
  }

  @PutMapping("/movies")
  public ResponseEntity<MovieDTO> upsertMovie(@RequestBody MovieInputDTO input) {
    return new ResponseEntity<>(movieService.create(input), HttpStatus.OK);
  }
}
