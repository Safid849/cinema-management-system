package com.example.demo.cinema.mapper;

import com.example.demo.cinema.dto.UserResponseDTO;
import com.example.demo.cinema.entity.User;

public class UserMapper {

  private UserMapper() {}

  public static UserResponseDTO toDTO(User user) {
    return new UserResponseDTO(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getBirthdate(),
        user.getEmail(),
        user.getPhone(),
        user.getRole());
  }
}
