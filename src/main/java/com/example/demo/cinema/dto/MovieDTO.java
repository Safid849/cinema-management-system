package com.example.demo.cinema.dto;

import com.example.demo.cinema.entity.Genre;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

public record MovieDTO(
    UUID id, String title, Set<Genre> genre, String description, Duration duration) {}
