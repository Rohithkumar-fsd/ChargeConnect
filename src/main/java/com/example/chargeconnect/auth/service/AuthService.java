package com.example.chargeconnect.auth.service;

import com.example.chargeconnect.auth.JwtService;
import com.example.chargeconnect.auth.dto.AuthResponse;
import com.example.chargeconnect.auth.dto.LoginRequest;
import com.example.chargeconnect.auth.dto.RegisterRequest;
import com.example.chargeconnect.user.*;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository repository,
            PasswordEncoder encoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.repository = repository;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        if (repository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email already registered"
            );
        }

        String normalizedPhone = request.phone() != null && !request.phone().isBlank()
                ? request.phone().trim()
                : null;

        User user = new User(
                request.fullName().trim(),
                normalizedEmail,
                normalizedPhone,
                encoder.encode(request.password())
        );

        user.getRoles().add(
                Role.valueOf(request.role().name())
        );

        repository.save(user);

        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(token, "Bearer", user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase().trim();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        request.password()
                )
        );

        String token = jwtService.generateToken(normalizedEmail);

        return new AuthResponse(
                token, "Bearer", normalizedEmail
        );
    }
}

