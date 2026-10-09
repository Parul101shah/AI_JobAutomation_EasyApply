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
    private String phone;
    private String linkedinUrl;
    private String currentLocation;
    private Boolean workAuthorized;        // legally authorized to work in target country
    private Boolean requiresSponsorship;
    private Integer noticePeriodDays;
    private Integer expectedSalary;

}
