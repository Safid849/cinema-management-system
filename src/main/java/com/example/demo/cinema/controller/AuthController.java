package com.example.demo.cinema.controller;

import com.example.demo.cinema.dto.LoginRequestDTO;
import com.example.demo.cinema.dto.LoginResponseDTO;
import com.example.demo.cinema.dto.UserCreateDTO;
import com.example.demo.cinema.dto.UserResponseDTO;
import com.example.demo.cinema.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class AuthController {

  private final UserService userService;

  @PostMapping("/register")
  public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserCreateDTO input) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(input));
  }

  @PostMapping("/login")
  public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
    return userService.login(request.email(), request.password());
  }
}
