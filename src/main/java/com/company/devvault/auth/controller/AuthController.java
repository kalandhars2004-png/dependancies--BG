package com.company.devvault.auth.controller;

import com.company.devvault.auth.dto.LoginRequest;
import com.company.devvault.auth.dto.UserDto;
import com.company.devvault.auth.security.JwtCookieFilter;
import com.company.devvault.auth.service.AuthService;
import com.company.devvault.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${devvault.cookie.same-site}")
    private String cookieSameSite;

    @Value("${devvault.cookie.secure}")
    private boolean cookieSecure;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserDto>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        String token = authService.login(request);
        ResponseCookie cookie = buildCookie(token, Duration.ofDays(1));
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        UserDto user = authService.currentUser(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Login successful", user));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        ResponseCookie cookie = buildCookie("", Duration.ZERO);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(ApiResponse.success("Logged out", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me() {
        return ResponseEntity.ok(ApiResponse.success(authService.currentUser()));
    }

    private ResponseCookie buildCookie(String token, Duration maxAge) {
        return ResponseCookie.from(JwtCookieFilter.COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}