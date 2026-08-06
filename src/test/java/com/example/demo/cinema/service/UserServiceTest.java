package com.example.demo.cinema.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.example.demo.cinema.dto.LoginResponseDTO;
import com.example.demo.cinema.dto.UserCreateDTO;
import com.example.demo.cinema.dto.UserResponseDTO;
import com.example.demo.cinema.entity.User;
import com.example.demo.cinema.entity.Userrole;
import com.example.demo.cinema.repository.UserRepository;
import com.example.demo.cinema.security.CinemaUserPrincipal;
import com.example.demo.cinema.security.JwtService;
import com.example.demo.cinema.support.TestFixtures;
import com.example.demo.endpoint.rest.exception.BadRequestException;
import com.example.demo.endpoint.rest.exception.EmailAlreadyExistsException;
import com.example.demo.endpoint.rest.exception.InvalidCredentialsException;
import com.example.demo.endpoint.rest.exception.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, passwordEncoder, jwtService);
  }

  private static UserCreateDTO signUp(String email) {
    return new UserCreateDTO(
        "Ando", "Rakoto", LocalDate.of(1995, 4, 12), email, "password123", "+261340000000");
  }

  @Test
  void register_createsAClientWithAHashedPasswordAndANormalisedEmail() {
    when(userRepository.existsByEmail("ando@cinema.mg")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            call -> {
              User user = call.getArgument(0);
              user.setId(UUID.randomUUID());
              return user;
            });

    UserResponseDTO result = userService.register(signUp("  Ando@Cinema.MG "));

    assertThat(result.email()).isEqualTo("ando@cinema.mg");
    assertThat(result.role()).isEqualTo(Userrole.CLIENT);
  }

  @Test
  void register_throwsConflict_whenEmailIsAlreadyTaken() {
    when(userRepository.existsByEmail("ando@cinema.mg")).thenReturn(true);
    UserCreateDTO input = signUp("ando@cinema.mg");

    assertThatThrownBy(() -> userService.register(input))
        .isInstanceOf(EmailAlreadyExistsException.class);
  }

  @Test
  void login_returnsABearerTokenWithTheUserRole() {
    User user = TestFixtures.user(Userrole.MANAGER);
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);
    when(jwtService.generate(user)).thenReturn("jwt-token");
    when(jwtService.getExpirationMs()).thenReturn(3_600_000L);

    LoginResponseDTO result = userService.login(user.getEmail(), "password123");

    assertThat(result.token()).isEqualTo("jwt-token");
    assertThat(result.type()).isEqualTo("Bearer");
    assertThat(result.role()).isEqualTo(Userrole.MANAGER);
    assertThat(result.userId()).isEqualTo(user.getId());
    assertThat(result.expiresInMs()).isEqualTo(3_600_000L);
  }

  @Test
  void login_throwsUnauthorized_whenEmailIsUnknown() {
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.login("ghost@cinema.mg", "whatever"))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void login_throwsUnauthorized_whenPasswordDoesNotMatch() {
    User user = TestFixtures.user(Userrole.CLIENT);
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);
    String email = user.getEmail();

    assertThatThrownBy(() -> userService.login(email, "wrong"))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void listUsers_returnsEveryUserWithoutPassword() {
    when(userRepository.findAll())
        .thenReturn(
            List.of(TestFixtures.user(Userrole.CLIENT), TestFixtures.user(Userrole.MANAGER)));

    List<UserResponseDTO> result = userService.listUsers();

    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(UserResponseDTO::role)
        .contains(Userrole.CLIENT, Userrole.MANAGER);
  }

  @Test
  void getUserById_allowsAClientToReadTheirOwnProfile() {
    UUID id = UUID.randomUUID();
    User user = TestFixtures.user(id, Userrole.CLIENT);
    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    UserResponseDTO result = userService.getUserById(id, CinemaUserPrincipal.from(user));

    assertThat(result.id()).isEqualTo(id);
  }

  @Test
  void getUserById_deniesAClientReadingSomeoneElse() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id))
        .thenReturn(Optional.of(TestFixtures.user(id, Userrole.CLIENT)));
    CinemaUserPrincipal other = TestFixtures.principal(Userrole.CLIENT);

    assertThatThrownBy(() -> userService.getUserById(id, other))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void getUserById_allowsStaffToReadAnyone() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id))
        .thenReturn(Optional.of(TestFixtures.user(id, Userrole.CLIENT)));

    assertThat(userService.getUserById(id, TestFixtures.principal(Userrole.EMPLOYEE)).id())
        .isEqualTo(id);
  }

  @Test
  void getUserById_throwsNotFound_whenAbsent() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id)).thenReturn(Optional.empty());
    CinemaUserPrincipal manager = TestFixtures.principal(Userrole.MANAGER);

    assertThatThrownBy(() -> userService.getUserById(id, manager))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void getCurrentUser_returnsTheCallerProfile() {
    User user = TestFixtures.user(Userrole.CLIENT);
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    assertThat(userService.getCurrentUser(CinemaUserPrincipal.from(user)).email())
        .isEqualTo(user.getEmail());
  }

  @Test
  void getCurrentUser_deniesAnonymousCallers() {
    assertThatThrownBy(() -> userService.getCurrentUser(null))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void updateRole_promotesAClientToEmployee() {
    UUID id = UUID.randomUUID();
    User target = TestFixtures.user(id, Userrole.CLIENT);
    when(userRepository.findById(id)).thenReturn(Optional.of(target));
    when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

    UserResponseDTO result =
        userService.updateRole(id, Userrole.EMPLOYEE, TestFixtures.principal(Userrole.MANAGER));

    assertThat(result.role()).isEqualTo(Userrole.EMPLOYEE);
  }

  @Test
  void updateRole_deniesNonManagers() {
    UUID id = UUID.randomUUID();
    CinemaUserPrincipal employee = TestFixtures.principal(Userrole.EMPLOYEE);

    assertThatThrownBy(() -> userService.updateRole(id, Userrole.MANAGER, employee))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void updateRole_refusesAManagerDemotingThemselves() {
    UUID id = UUID.randomUUID();
    User manager = TestFixtures.user(id, Userrole.MANAGER);
    when(userRepository.findById(id)).thenReturn(Optional.of(manager));
    CinemaUserPrincipal caller = CinemaUserPrincipal.from(manager);

    assertThatThrownBy(() -> userService.updateRole(id, Userrole.CLIENT, caller))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("demote");
  }

  @Test
  void updateRole_rejectsANullRole() {
    UUID id = UUID.randomUUID();
    CinemaUserPrincipal manager = TestFixtures.principal(Userrole.MANAGER);

    assertThatThrownBy(() -> userService.updateRole(id, null, manager))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void userroleEnum_containsTheThreeRolesOfTheSpecification() {
    assertThat(Userrole.values())
        .containsExactly(Userrole.CLIENT, Userrole.EMPLOYEE, Userrole.MANAGER);
  }
}
