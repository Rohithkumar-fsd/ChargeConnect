package com.example.chargeconnect.auth.service;

import com.example.chargeconnect.user.dto.AccountStatus;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.user.repo.UserRepository;
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

