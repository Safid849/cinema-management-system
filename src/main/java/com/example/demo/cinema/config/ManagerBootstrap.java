package com.example.demo.cinema.config;

import com.example.demo.cinema.entity.User;
import com.example.demo.cinema.entity.Userrole;
import com.example.demo.cinema.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Public sign-up always creates a CLIENT, so a fresh database would have no one able to grant
 * roles. Enabling {@code cinema.bootstrap.enabled} creates the very first MANAGER; it is a no-op
 * once that account exists.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "cinema.bootstrap.enabled", havingValue = "true")
public class ManagerBootstrap implements ApplicationRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final String email;
  private final String password;

  public ManagerBootstrap(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      @Value("${cinema.bootstrap.email}") String email,
      @Value("${cinema.bootstrap.password}") String password) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.email = email;
    this.password = password;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (userRepository.existsByEmail(email)) {
      log.info("bootstrap manager {} already exists, skipping", email);
      return;
    }

    userRepository.save(
        User.builder()
            .firstName("Cinema")
            .lastName("Manager")
            .email(email)
            .password(passwordEncoder.encode(password))
            .role(Userrole.MANAGER)
            .build());

    log.warn("bootstrap manager {} created - change its password immediately", email);
  }
}
