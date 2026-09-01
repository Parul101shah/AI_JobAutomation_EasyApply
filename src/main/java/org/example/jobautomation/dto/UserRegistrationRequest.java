package org.example.jobautomation.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserRegistrationRequest {
    private String fullName;
    private String email;
    private String preferredLocation;
    private Integer minSalary;
    private List<String> targetRoles;

}
