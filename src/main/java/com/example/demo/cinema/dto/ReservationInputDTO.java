package com.example.demo.cinema.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

/** {@code id} null means "create", otherwise the PUT updates that reservation. */
public record ReservationInputDTO(
    UUID id,
    UUID userId,
    @NotNull(message = "projectionId is required") UUID projectionId,
    @NotEmpty(message = "seatIds must not be empty") Set<UUID> seatIds) {}
