package com.example.demo.cinema.dto;

import com.example.demo.cinema.entity.Userrole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleDTO(@NotNull(message = "role is required") Userrole role) {}
