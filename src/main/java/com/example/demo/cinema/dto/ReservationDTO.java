package com.example.demo.cinema.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ReservationDTO(
    UUID id, UUID userId, UUID projectionId, Set<UUID> seatIds, Instant createdAt) {}
