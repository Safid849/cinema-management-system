package com.example.demo.cinema.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProjectionInputDTO(
    UUID movieId, UUID roomId, Instant datetime, BigDecimal seatPrice) {}
