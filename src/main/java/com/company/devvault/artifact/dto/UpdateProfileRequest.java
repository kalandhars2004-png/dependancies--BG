package com.company.devvault.artifact.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {

    private String name;
    private String department;
    private String profileImage;
    private String currentPassword;
    private String newPassword;
}