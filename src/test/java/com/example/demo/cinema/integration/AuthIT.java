package com.example.demo.cinema.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.cinema.dto.LoginRequestDTO;
import com.example.demo.cinema.dto.LoginResponseDTO;
import com.example.demo.cinema.dto.UpdateUserRoleDTO;
import com.example.demo.cinema.dto.UserResponseDTO;
import com.example.demo.cinema.entity.User;
import com.example.demo.cinema.entity.Userrole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Sign-up, login, and the JWT-driven user management on top of them. */
class AuthIT extends CinemaIT {

  @Test
  void register_createsAClientAndNeverLeaksThePassword() {
    ResponseEntity<String> response =
        rest.postForEntity(url("/users/register"), signUp("newbie@cinema.mg"), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).contains("CLIENT").doesNotContain("password");
    assertThat(userRepository.findByEmail("newbie@cinema.mg")).isPresent();
  }

  @Test
  void register_normalisesTheEmailAndRefusesADuplicate() {
    rest.postForEntity(url("/users/register"), signUp("Mixed@Cinema.MG"), String.class);

    ResponseEntity<String> duplicate =
        rest.postForEntity(url("/users/register"), signUp("mixed@cinema.mg"), String.class);

    assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(userRepository.findByEmail("mixed@cinema.mg")).isPresent();
  }

  @Test
  void register_rejectsAnInvalidPayload() {
    ResponseEntity<String> response =
        rest.postForEntity(url("/users/register"), signUp("not-an-email"), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void login_returnsABearerTokenCarryingTheRole() {
    User manager = persistUser(Userrole.MANAGER);

    ResponseEntity<LoginResponseDTO> response =
        rest.postForEntity(
            url("/users/login"),
            new LoginRequestDTO(manager.getEmail(), PASSWORD),
            LoginResponseDTO.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().type()).isEqualTo("Bearer");
    assertThat(response.getBody().role()).isEqualTo(Userrole.MANAGER);
    assertThat(response.getBody().userId()).isEqualTo(manager.getId());
    assertThat(response.getBody().token()).isNotBlank();
  }

  @Test
  void login_returns401OnABadPassword() {
    User client = persistUser(Userrole.CLIENT);

    ResponseEntity<String> response =
        rest.postForEntity(
            url("/users/login"),
            new LoginRequestDTO(client.getEmail(), "wrong-password"),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void protectedRoute_returns401WithoutAToken() {
    assertThat(get("/users/me", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void protectedRoute_returns401OnATamperedToken() {
    String token = tokenOf(Userrole.CLIENT);

    assertThat(get("/users/me", token + "tampered").getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void me_returnsTheProfileBehindTheToken() {
    User client = persistUser(Userrole.CLIENT);

    ResponseEntity<UserResponseDTO> response =
        call(HttpMethod.GET, "/users/me", tokenOf(client), null, UserResponseDTO.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().email()).isEqualTo(client.getEmail());
    assertThat(response.getBody().role()).isEqualTo(Userrole.CLIENT);
  }

  @Test
  void listUsers_isStaffOnly() {
    persistUser(Userrole.CLIENT);

    assertThat(get("/users", tokenOf(Userrole.CLIENT)).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(get("/users", tokenOf(Userrole.EMPLOYEE)).getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(get("/users", tokenOf(Userrole.MANAGER)).getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void getUserById_letsAClientReadThemselvesButNobodyElse() {
    User client = persistUser(Userrole.CLIENT);
    User other = persistUser(Userrole.CLIENT);
    String token = tokenOf(client);

    assertThat(get("/users/" + client.getId(), token).getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(get("/users/" + other.getId(), token).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void updateRole_isManagerOnlyAndTakesEffectOnTheNextToken() {
    User client = persistUser(Userrole.CLIENT);
    User manager = persistUser(Userrole.MANAGER);

    assertThat(
            put(
                    "/users/" + client.getId() + "/role",
                    tokenOf(Userrole.EMPLOYEE),
                    new UpdateUserRoleDTO(Userrole.MANAGER))
                .getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<String> promoted =
        put(
            "/users/" + client.getId() + "/role",
            tokenOf(manager),
            new UpdateUserRoleDTO(Userrole.EMPLOYEE));

    assertThat(promoted.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(userRepository.findById(client.getId()).orElseThrow().getRole())
        .isEqualTo(Userrole.EMPLOYEE);
    assertThat(get("/reservations", tokenOf(client)).getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void updateRole_refusesAManagerDemotingThemselves() {
    User manager = persistUser(Userrole.MANAGER);

    ResponseEntity<String> response =
        put(
            "/users/" + manager.getId() + "/role",
            tokenOf(manager),
            new UpdateUserRoleDTO(Userrole.CLIENT));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void updateRole_returns404OnAnUnknownUser() {
    ResponseEntity<String> response =
        put(
            "/users/" + UUID.randomUUID() + "/role",
            tokenOf(Userrole.MANAGER),
            new UpdateUserRoleDTO(Userrole.EMPLOYEE));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}
