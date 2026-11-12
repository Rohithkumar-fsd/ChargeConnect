package com.example.chargeconnect.auth.security;

import com.example.chargeconnect.auth.service.CustomUserDetailsService;
import com.example.chargeconnect.auth.service.JwtService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userService;

    public JwtAuthFilter(
            JwtService jwtService,
            CustomUserDetailsService userService
    ) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtService.isValid(token)
                    && SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

                String email = jwtService.extractEmail(token);
                try {
                    UserDetails user = userService.loadUserByUsername(email);

                    // Reject suspended/disabled accounts even if their JWT has not expired yet.
                    if (user.isEnabled()) {
                        var authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user, null, user.getAuthorities()
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request)
                        );

                        SecurityContextHolder.getContext()
                                .setAuthentication(authentication);
                    }
                } catch (UsernameNotFoundException ignored) {
                    // Leave SecurityContext unauthenticated so SecurityConfig returns 401 JSON.
                }
            }
        }

        chain.doFilter(request, response);
    }
}

