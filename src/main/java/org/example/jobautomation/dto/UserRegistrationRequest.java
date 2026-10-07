package org.example.jobautomation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
public class UserRegistrationRequest {
    private String fullName;
    @NotBlank @Email
    private String email;
    private String preferredLocation;
    private Integer minSalary;
    private List<String> targetRoles;

}
