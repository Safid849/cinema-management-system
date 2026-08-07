package com.example.demo.cinema.security;

import com.example.demo.cinema.entity.User;
import com.example.demo.cinema.entity.Userrole;
import java.util.Collection;
import java.util.UUID;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class CinemaUserPrincipal implements UserDetails {

  @Getter private final UUID id;
  private final String email;
  private final String passwordHash;
  @Getter private final Userrole role;

  private CinemaUserPrincipal(UUID id, String email, String passwordHash, Userrole role) {
    this.id = id;
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = role;
  }

  public static CinemaUserPrincipal from(User user) {
    return new CinemaUserPrincipal(
        user.getId(), user.getEmail(), user.getPassword(), user.getRole());
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return GrantedAuthorityFactory.fromRole(role);
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
