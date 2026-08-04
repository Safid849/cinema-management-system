package com.example.demo.cinema.security;

import com.example.demo.cinema.entity.Userrole;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class GrantedAuthorityFactory {

    private static final String ROLE_PREFIX = "ROLE_";

    private GrantedAuthorityFactory() {}

    public static List<GrantedAuthority> fromRole(Userrole role) {
        return List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role.name()));
    }
}
