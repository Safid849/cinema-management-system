package com.example.demo.cinema.dto;

import com.example.demo.cinema.entity.Userrole;
import java.util.UUID;

public record LoginResponseDTO(
    String token, String type, UUID userId, String email, Userrole role, long expiresInMs) {

  public static LoginResponseDTO bearer(
      String token, UUID userId, String email, Userrole role, long expiresInMs) {
    return new LoginResponseDTO(token, "Bearer", userId, email, role, expiresInMs);
  }
}
