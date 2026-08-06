package com.example.demo.cinema.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** {@code id} null means "create", otherwise the PUT updates that projection. */
public record ProjectionInputDTO(
    UUID id,
    @NotNull(message = "movieId is required") UUID movieId,
    @NotNull(message = "roomId is required") UUID roomId,
    @NotNull(message = "datetime is required") Instant datetime,
    @NotNull(message = "seatPrice is required") BigDecimal seatPrice) {}
