package org.example.jobautomation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="user_profiles")
@Data
@NoArgsConstructor
public class UserProfile {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    //Manual input
    private String fullName;
    @Column(unique = true)
    private String email;
    private String preferredLocation;
    private Integer minSalary;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_target_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private List<String> targetRoles = new ArrayList<>();

    // AI-extracted input
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_skills", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_past_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private List<String> pastRoles = new ArrayList<>();

    private Integer totalExperienceYears;
    private String education;

    @Column(columnDefinition = "TEXT")
    private String profileSummary;

    @Column(columnDefinition = "TEXT")
    private String resumeText;

    private String phone;
    private String linkedinUrl;
    private String currentLocation;
    private Boolean workAuthorized;        // legally authorized to work in target country
    private Boolean requiresSponsorship;
    private Integer noticePeriodDays;
    private Integer expectedSalary;

}