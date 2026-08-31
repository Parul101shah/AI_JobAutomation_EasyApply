package org.example.jobautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExtractedProfile {
    private List<String> skills;
    private List<String> roles;
    private int totalExperienceYears;
    private String education;
    private String profileSummary;
}
//DTO for AI response