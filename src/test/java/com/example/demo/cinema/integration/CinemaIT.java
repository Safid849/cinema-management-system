package com.example.demo.cinema.integration;

import com.example.demo.cinema.dto.LoginRequestDTO;
import com.example.demo.cinema.dto.LoginResponseDTO;
import com.example.demo.cinema.dto.UserCreateDTO;
import com.example.demo.cinema.entity.User;
import com.example.demo.cinema.entity.Userrole;
import com.example.demo.cinema.repository.MovieRepository;
import com.example.demo.cinema.repository.ProjectionRepository;
import com.example.demo.cinema.repository.ReservationRepository;
import com.example.demo.cinema.repository.RoomRepository;
import com.example.demo.cinema.repository.SeatRepository;
import com.example.demo.cinema.repository.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for the end-to-end tests: a real PostgreSQL migrated by Flyway, the real security
 * filter chain, and real HTTP calls carrying real JWTs. The container is static so every IT in the
 * run shares one database; each test wipes the tables it touches first.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class CinemaIT {

  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16-alpine")
          .withDatabaseName("cinema")
          .withUsername("cinema")
          .withPassword("cinema");

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("aws.s3.bucket", () -> "dummy-bucket");
    registry.add("aws.ses.source", () -> "dummy-ses-source");
  }

  @Value("${local.server.port}")
  protected int port;

  @Autowired protected TestRestTemplate rest;
  @Autowired protected UserRepository userRepository;
  @Autowired protected RoomRepository roomRepository;
  @Autowired protected SeatRepository seatRepository;
  @Autowired protected MovieRepository movieRepository;
  @Autowired protected ProjectionRepository projectionRepository;
  @Autowired protected ReservationRepository reservationRepository;
  @Autowired protected PasswordEncoder passwordEncoder;

  protected static final String PASSWORD = "password123";

  @BeforeEach
  void resetDatabase() {
    reservationRepository.deleteAll();
    projectionRepository.deleteAll();
    seatRepository.deleteAll();
    roomRepository.deleteAll();
    movieRepository.deleteAll();
    userRepository.deleteAll();
  }

  // --- fixtures -------------------------------------------------------------

  /** Creates a user straight in the database with the role we need, bypassing public sign-up. */
  protected User persistUser(Userrole role) {
    return userRepository.save(
        User.builder()
            .firstName("Ando")
            .lastName("Rakoto")
            .birthdate(LocalDate.of(1995, 4, 12))
            .email(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@cinema.mg")
            .password(passwordEncoder.encode(PASSWORD))
            .phone("+261340000000")
            .role(role)
            .build());
  }

  protected UserCreateDTO signUp(String email) {
    return new UserCreateDTO(
        "Ando", "Rakoto", LocalDate.of(1995, 4, 12), email, PASSWORD, "+261340000000");
  }

  // --- http helpers ---------------------------------------------------------

  protected String tokenOf(User user) {
    LoginResponseDTO login =
        rest.postForObject(
            url("/users/login"),
            new LoginRequestDTO(user.getEmail(), PASSWORD),
            LoginResponseDTO.class);
    return login.token();
  }

  protected String tokenOf(Userrole role) {
    return tokenOf(persistUser(role));
  }

  protected String url(String path) {
    return "http://localhost:" + port + path;
  }

  protected HttpHeaders bearer(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return headers;
  }

  protected <T> ResponseEntity<T> call(
      HttpMethod method, String path, String token, Object body, Class<T> responseType) {
    return rest.exchange(url(path), method, new HttpEntity<>(body, bearer(token)), responseType);
  }

  protected ResponseEntity<String> get(String path, String token) {
    return call(HttpMethod.GET, path, token, null, String.class);
  }

  protected ResponseEntity<String> put(String path, String token, Object body) {
    return call(HttpMethod.PUT, path, token, body, String.class);
  }
}
