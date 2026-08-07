package com.example.demo.cinema.service;

import com.example.demo.cinema.dto.LoginResponseDTO;
import com.example.demo.cinema.dto.UserCreateDTO;
import com.example.demo.cinema.dto.UserResponseDTO;
import com.example.demo.cinema.entity.User;
import com.example.demo.cinema.entity.Userrole;
import com.example.demo.cinema.mapper.UserMapper;
import com.example.demo.cinema.repository.UserRepository;
import com.example.demo.cinema.security.CinemaUserPrincipal;
import com.example.demo.cinema.security.JwtService;
import com.example.demo.endpoint.rest.exception.BadRequestException;
import com.example.demo.endpoint.rest.exception.EmailAlreadyExistsException;
import com.example.demo.endpoint.rest.exception.InvalidCredentialsException;
import com.example.demo.endpoint.rest.exception.NotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  @Transactional
  public UserResponseDTO register(UserCreateDTO input) {
    String email = normalise(input.email());

    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyExistsException("Email already in use: " + email);
    }

    User user =
        User.builder()
            .firstName(input.firstName())
            .lastName(input.lastName())
            .birthdate(input.birthdate())
            .email(email)
            .password(passwordEncoder.encode(input.password()))
            .phone(input.phone())
            .role(Userrole.CLIENT)
            .build();

    return UserMapper.toDTO(userRepository.save(user));
  }

  public LoginResponseDTO login(String email, String rawPassword) {
    User user =
        userRepository
            .findByEmail(normalise(email))
            .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

    if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
      throw new InvalidCredentialsException("Invalid email or password");
    }

    return LoginResponseDTO.bearer(
        jwtService.generate(user),
        user.getId(),
        user.getEmail(),
        user.getRole(),
        jwtService.getExpirationMs());
  }

  public List<UserResponseDTO> listUsers() {
    return userRepository.findAll().stream().map(UserMapper::toDTO).toList();
  }

  public UserResponseDTO getUserById(UUID id, CinemaUserPrincipal caller) {
    requireCaller(caller);
    User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User " + id));

    if (!user.getId().equals(caller.getId()) && !isStaff(caller)) {
      throw new AccessDeniedException("User " + id + " does not belong to " + caller.getId());
    }

    return UserMapper.toDTO(user);
  }

  /** Profile of whoever holds the bearer token. */
  public UserResponseDTO getCurrentUser(CinemaUserPrincipal caller) {
    requireCaller(caller);
    return UserMapper.toDTO(
        userRepository
            .findById(caller.getId())
            .orElseThrow(() -> new NotFoundException("User " + caller.getId())));
  }

  /** Only a MANAGER promotes or demotes, and never themselves out of the manager role. */
  @Transactional
  public UserResponseDTO updateRole(UUID id, Userrole role, CinemaUserPrincipal caller) {
    requireCaller(caller);
    if (caller.getRole() != Userrole.MANAGER) {
      throw new AccessDeniedException("Only a MANAGER can change a role");
    }
    if (role == null) {
      throw new BadRequestException("role is required");
    }

    User target =
        userRepository.findById(id).orElseThrow(() -> new NotFoundException("User " + id));

    if (target.getId().equals(caller.getId()) && role != Userrole.MANAGER) {
      throw new BadRequestException("A manager cannot demote themselves");
    }

    target.setRole(role);
    return UserMapper.toDTO(userRepository.save(target));
  }

  private static void requireCaller(CinemaUserPrincipal caller) {
    if (caller == null) {
      throw new AccessDeniedException("Authentication required");
    }
  }

  private static boolean isStaff(CinemaUserPrincipal caller) {
    return caller.getRole() == Userrole.EMPLOYEE || caller.getRole() == Userrole.MANAGER;
  }

  private static String normalise(String email) {
    return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
  }
}
