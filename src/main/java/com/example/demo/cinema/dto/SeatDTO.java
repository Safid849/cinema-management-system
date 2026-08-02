package com.example.demo.cinema.dto;

import java.util.UUID;

public record SeatDTO(UUID id, String number, UUID roomId) {}
