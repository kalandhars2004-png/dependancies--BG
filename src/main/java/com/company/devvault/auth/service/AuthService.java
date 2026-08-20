package com.company.devvault.auth.service;

import com.company.devvault.audit.service.AuditService;
import com.company.devvault.auth.dto.LoginRequest;
import com.company.devvault.auth.dto.UserDto;
import com.company.devvault.auth.entity.User;
import com.company.devvault.auth.entity.UserRole;
import com.company.devvault.auth.repository.UserRepository;
import com.company.devvault.auth.security.JwtService;
import com.company.devvault.common.exception.ApiException;
import com.company.devvault.common.util.SecurityUtils;
import com.company.devvault.user.mapper.UserMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
                       JwtService jwtService, AuditService auditService, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.userMapper = userMapper;
    }

    @Transactional
    public String login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (BadCredentialsException ex) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        auditService.log("LOGIN", "User", email, null);
        return jwtService.generateToken(userDetails);
    }

    public UserDto currentUser() {
        String email = SecurityUtils.currentUsername();
        return currentUser(email);
    }

    public UserDto currentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));
        return userMapper.toDto(user);
    }

    public User getCurrentUserEntity() {
        String email = SecurityUtils.currentUsername();
        if (email == null) {
            throw ApiException.unauthorized("Authentication required");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));
    }

    @Transactional
    public User resolvePublishActor(String developerName, String developerEmail) {
        if (SecurityUtils.isAuthenticated()) {
            return getCurrentUserEntity();
        }
        if (developerEmail == null || developerEmail.isBlank()) {
            throw ApiException.badRequest("Developer email is required to publish without login");
        }
        String email = developerEmail.trim().toLowerCase();
        return userRepository.findByEmail(email).orElseGet(() -> {
            String name = (developerName == null || developerName.isBlank())
                    ? email.substring(0, email.indexOf('@') < 0 ? email.length() : email.indexOf('@'))
                    : developerName.trim();
            User user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
            user.setRole(UserRole.DEVELOPER);
            User saved = userRepository.save(user);
            auditService.log("USER_AUTO_CREATED", saved, "User", email, null);
            return saved;
        });
    }
}