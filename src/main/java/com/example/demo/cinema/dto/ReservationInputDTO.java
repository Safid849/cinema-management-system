package com.example.demo.cinema.dto;

import java.util.Set;
import java.util.UUID;

public record ReservationInputDTO(UUID userId, UUID projectionId, Set<UUID> seatIds) {}
