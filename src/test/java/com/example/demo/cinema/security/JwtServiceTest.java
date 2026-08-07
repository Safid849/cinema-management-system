package com.example.demo.cinema.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.cinema.entity.User;
import com.example.demo.cinema.entity.Userrole;
import com.example.demo.cinema.support.TestFixtures;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  /** Base64 of a 46-character secret, i.e. well above the 256 bits HS256 requires. */
  private static final String SECRET =
      "dGVzdF9zZWNyZXRfa2V5X2Zvcl9kZXZlbG9wbWVudF9vbmx5XzEyMzQ1Njc4OTA=";

  private static final String OTHER_SECRET =
      "YW5vdGhlcl9zZWNyZXRfa2V5X2Zvcl9kZXZlbG9wbWVudF8xMjM0NTY3ODkwMTI=";

  private final JwtService jwtService = new JwtService(SECRET, 3_600_000L);

  @Test
  void constructor_rejectsASecretShorterThan256Bits() {
    assertThatThrownBy(() -> new JwtService("dG9vc2hvcnQ=", 1000L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("256 bits");
  }

  @Test
  void generate_producesATokenCarryingEmailRoleAndUserId() {
    User user = TestFixtures.user(Userrole.MANAGER);

    String token = jwtService.generate(user);

    assertThat(jwtService.isValid(token)).isTrue();
    assertThat(jwtService.extractEmail(token)).isEqualTo(user.getEmail());
    assertThat(jwtService.extractRole(token)).isEqualTo("MANAGER");
    assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
  }

  @Test
  void getExpirationMs_exposesTheConfiguredLifetime() {
    assertThat(jwtService.getExpirationMs()).isEqualTo(3_600_000L);
  }

  @Test
  void isValid_rejectsGarbage() {
    assertThat(jwtService.isValid("not-a-jwt")).isFalse();
    assertThat(jwtService.isValid("")).isFalse();
  }

  @Test
  void isValid_rejectsAnExpiredToken() {
    JwtService expiringService = new JwtService(SECRET, -1_000L);
    String token = expiringService.generate(TestFixtures.user(Userrole.CLIENT));

    assertThat(expiringService.isValid(token)).isFalse();
  }

  @Test
  void isValid_rejectsATokenSignedWithAnotherKey() {
    String foreignToken =
        new JwtService(OTHER_SECRET, 3_600_000L).generate(TestFixtures.user(Userrole.CLIENT));

    assertThat(jwtService.isValid(foreignToken)).isFalse();
  }
}
