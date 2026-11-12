package com.example.chargeconnect.auth;

import com.example.chargeconnect.user.*;
import com.example.chargeconnect.user.User;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repository;

    public CustomUserDetailsService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(
                        user.getRoles().stream()
                                .map(role -> "ROLE_" + role.name())
                                .toArray(String[]::new)
                )
                .disabled(user.getAccountStatus() != AccountStatus.ACTIVE)
                .build();
    }
}