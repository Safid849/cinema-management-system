package com.example.demo.cinema.dto;

import com.example.demo.cinema.entity.Userrole;
import java.time.LocalDate;
import java.util.UUID;

public record UserResponseDTO(
    UUID id,
    String firstName,
    String lastName,
    LocalDate birthdate,
    String email,
    String phone,
    Userrole role) {}
