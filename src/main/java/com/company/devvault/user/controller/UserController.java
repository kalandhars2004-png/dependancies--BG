package com.company.devvault.user.controller;

import com.company.devvault.artifact.dto.UpdateProfileRequest;
import com.company.devvault.auth.dto.UserDto;
import com.company.devvault.auth.entity.UserRole;
import com.company.devvault.auth.service.AuthService;
import com.company.devvault.common.response.ApiResponse;
import com.company.devvault.user.dto.CreateUserRequest;
import com.company.devvault.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<UserDto>> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(userService.listUsers(page, size));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDto> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success("User created", userService.createUser(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDto> get(@PathVariable Long id) {
        return ApiResponse.success(userService.getUser(id));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDto> changeRole(@PathVariable Long id, @RequestBody RoleRequest request) {
        return ApiResponse.success("Role updated", userService.updateRole(id, request.getRole()));
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDto> setActive(@PathVariable Long id, @RequestBody ActiveRequest request) {
        return ApiResponse.success("User status updated", userService.setActive(id, request.isActive()));
    }

    @PutMapping("/me")
    public ApiResponse<UserDto> updateProfile(@RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("Profile updated",
                userService.updateProfile(authService.getCurrentUserEntity(), request));
    }

    public static class RoleRequest {
        private UserRole role;

        public UserRole getRole() {
            return role;
        }

        public void setRole(UserRole role) {
            this.role = role;
        }
    }

    public static class ActiveRequest {
        private boolean active;

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}