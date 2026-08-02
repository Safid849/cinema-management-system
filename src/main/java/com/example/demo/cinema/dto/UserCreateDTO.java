package com.example.demo.cinema.dto;

import java.time.LocalDate;

public record UserCreateDTO(
    String firstName,
    String lastName,
    LocalDate birthdate,
    String email,
    String password,
    String phone) {}
