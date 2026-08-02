package com.example.demo.cinema.dto;

import com.example.demo.cinema.entity.Genre;
import java.time.Duration;
import java.util.Set;

public record MovieInputDTO(
    String title, Set<Genre> genre, String description, Duration duration) {}
