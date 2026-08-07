package com.example.demo.cinema.controller;

import com.example.demo.cinema.dto.UpdateUserRoleDTO;
import com.example.demo.cinema.dto.UserResponseDTO;
import com.example.demo.cinema.security.CinemaUserPrincipal;
import com.example.demo.cinema.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping
  @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
  public List<UserResponseDTO> listUsers() {
    return userService.listUsers();
  }

  @GetMapping("/me")
  public UserResponseDTO getCurrentUser(@AuthenticationPrincipal CinemaUserPrincipal caller) {
    return userService.getCurrentUser(caller);
  }

  @GetMapping("/{id}")
  public UserResponseDTO getUserById(
      @PathVariable UUID id, @AuthenticationPrincipal CinemaUserPrincipal caller) {
    return userService.getUserById(id, caller);
  }

  @PutMapping("/{id}/role")
  @PreAuthorize("hasRole('MANAGER')")
  public UserResponseDTO updateRole(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateUserRoleDTO input,
      @AuthenticationPrincipal CinemaUserPrincipal caller) {
    return userService.updateRole(id, input.role(), caller);
  }
}
