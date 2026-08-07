package com.example.demo.cinema.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UserCreateDTO(
    @NotBlank(message = "firstName is required") String firstName,
    @NotBlank(message = "lastName is required") String lastName,
    LocalDate birthdate,
    @NotBlank(message = "email is required") @Email(message = "email must be valid") String email,
    @NotBlank(message = "password is required")
        @Size(min = 8, message = "password must be at least 8 characters")
        String password,
    String phone) {}
