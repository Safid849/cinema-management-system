package com.example.demo.cinema.dto;

import com.example.demo.cinema.entity.Genre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/** {@code id} null means "create", otherwise the PUT updates that movie. */
public record MovieInputDTO(
    UUID id,
    @NotBlank(message = "title is required") String title,
    Set<Genre> genre,
    String description,
    @NotNull(message = "duration is required") Duration duration) {}
