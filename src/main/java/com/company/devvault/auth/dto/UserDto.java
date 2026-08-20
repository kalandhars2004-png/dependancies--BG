package com.company.devvault.auth.dto;

import com.company.devvault.auth.entity.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class UserDto {

    private Long id;
    private String name;
    private String email;
    private UserRole role;
    private String profileImage;
    private String department;
    private boolean active;
    private Instant createdAt;
}