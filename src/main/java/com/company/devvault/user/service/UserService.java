package com.company.devvault.user.service;

import com.company.devvault.artifact.dto.UpdateProfileRequest;
import com.company.devvault.audit.service.AuditService;
import com.company.devvault.auth.dto.UserDto;
import com.company.devvault.auth.entity.User;
import com.company.devvault.auth.entity.UserRole;
import com.company.devvault.auth.repository.UserRepository;
import com.company.devvault.common.exception.ApiException;
import com.company.devvault.user.dto.CreateUserRequest;
import com.company.devvault.user.mapper.UserMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("An account with email " + email + " already exists");
        }
        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : UserRole.DEVELOPER);
        user.setDepartment(request.getDepartment());
        user = userRepository.save(user);
        auditService.log("USER_CREATED", "User", String.valueOf(user.getId()), "Account created by administrator");
        return userMapper.toDto(user);
    }

    public Page<UserDto> listUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.findAll(pageable).map(userMapper::toDto);
    }

    public UserDto getUser(Long id) {
        return userMapper.toDto(getEntity(id));
    }

    @Transactional
    public UserDto updateRole(Long id, UserRole role) {
        User user = getEntity(id);
        user.setRole(role);
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto setActive(Long id, boolean active) {
        User user = getEntity(id);
        user.setActive(active);
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto updateProfile(User current, UpdateProfileRequest request) {
        if (request.getName() != null && !request.getName().isBlank()) {
            current.setName(request.getName().trim());
        }
        if (request.getDepartment() != null) {
            current.setDepartment(request.getDepartment().trim());
        }
        if (request.getProfileImage() != null) {
            current.setProfileImage(request.getProfileImage().trim());
        }
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null
                    || !passwordEncoder.matches(request.getCurrentPassword(), current.getPasswordHash())) {
                throw ApiException.badRequest("Current password is incorrect");
            }
            if (request.getNewPassword().length() < 8) {
                throw ApiException.badRequest("New password must be at least 8 characters");
            }
            current.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }
        return userMapper.toDto(userRepository.save(current));
    }

    private User getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found with id " + id));
    }
}