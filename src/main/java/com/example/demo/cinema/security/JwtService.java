package com.example.demo.cinema.security;

import static io.jsonwebtoken.Jwts.SIG.HS256;

import com.example.demo.cinema.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Issues and reads the HS256 tokens the API authenticates with. The subject carries the email, and
 * two extra claims carry the role and the user id so the filter never has to guess.
 */
@Slf4j
@Component
public class JwtService {

  static final String ROLE_CLAIM = "role";
  static final String USER_ID_CLAIM = "userId";

  private final SecretKey signingKey;

  @Getter private final long expirationMs;

  public JwtService(
      @Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expirationMs) {
    byte[] keyBytes = Decoders.BASE64.decode(secret);

    if (keyBytes.length < 32) {
      throw new IllegalArgumentException(
          "the jwt key decoded have to do at least 256 bits (32 octets).");
    }

    this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    this.expirationMs = expirationMs;
  }

  public String generate(User user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(user.getEmail())
        .claim(ROLE_CLAIM, user.getRole().name())
        .claim(USER_ID_CLAIM, String.valueOf(user.getId()))
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusMillis(expirationMs)))
        .signWith(signingKey, HS256)
        .compact();
  }

  public String extractEmail(String token) {
    return parseClaims(token).getSubject();
  }

  public String extractRole(String token) {
    return parseClaims(token).get(ROLE_CLAIM, String.class);
  }

  public UUID extractUserId(String token) {
    String raw = parseClaims(token).get(USER_ID_CLAIM, String.class);
    return raw == null ? null : UUID.fromString(raw);
  }

  public boolean isValid(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      log.debug("invalid jwt : {}", e.getMessage());
      return false;
    }
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }
}
