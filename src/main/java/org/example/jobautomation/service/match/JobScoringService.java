package org.example.jobautomation.service.match;

import lombok.RequiredArgsConstructor;
import org.example.jobautomation.dto.JobListingDto;
import org.example.jobautomation.dto.JobMatchResultDto;
import org.example.jobautomation.entity.UserProfile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobScoringService {

    private final JobTextNormalizer jobTextNormalizer;
    private final ExperienceExtractor experienceExtractor;

    public JobMatchResultDto score(UserProfile profile, JobListingDto job) {
        List<String> reasons = new ArrayList<>();
        List<String> concerns = new ArrayList<>();

        String normalizedTitle = jobTextNormalizer.normalize(job.getTitle());
        String normalizedDescription = jobTextNormalizer.normalizeAndStripHtml(job.getDescription());
        String normalizedLocation = jobTextNormalizer.normalize(job.getLocation());

        int score = 0;

        int roleScore = calculateRoleScore(profile, normalizedTitle, normalizedDescription, reasons, concerns);
        int skillScore = calculateSkillScore(profile, normalizedTitle + " " + normalizedDescription, reasons, concerns);
        int experienceScore = calculateExperienceScore(profile, normalizedDescription, reasons, concerns);
        int locationScore = calculateLocationScore(profile, normalizedLocation, reasons, concerns);
        int recencyScore = calculateRecencyScore(job.getPostedAt(), reasons);

        score = roleScore + skillScore + experienceScore + locationScore + recencyScore;

        String matchLevel = determineLevel(score, roleScore, experienceScore);

        return new JobMatchResultDto(job, score, matchLevel, reasons, concerns);
    }

    private int calculateRoleScore(UserProfile profile, String title, String description,
                                   List<String> reasons, List<String> concerns) {
        int score = 0;
        String combined = title + " " + description;

        boolean targetRoleMatch = profile.getTargetRoles().stream()
                .map(String::toLowerCase)
                .anyMatch(role -> combined.contains(role)
                        || relatedRoleMatch(role, combined));

        boolean pastRoleMatch = profile.getPastRoles().stream()
                .map(String::toLowerCase)
                .anyMatch(role -> combined.contains(role)
                        || relatedRoleMatch(role, combined));

        if (targetRoleMatch) {
            score += 35;
            reasons.add("Matches target role preference");
        } else if (pastRoleMatch) {
            score += 20;
            reasons.add("Matches past role experience");
        } else if (containsBackendEngineeringSignals(combined)) {
            score += 18;
            reasons.add("Relevant backend/software engineering role");
        } else {
            concerns.add("Role appears weakly aligned with target roles");
        }

        return score;
    }

    private int calculateSkillScore(UserProfile profile, String text,
                                    List<String> reasons, List<String> concerns) {
        int matched = 0;
        for (String skill : profile.getSkills()) {
            String normalizedSkill = skill.toLowerCase().trim();
            if (text.contains(normalizedSkill) || skillAliasMatch(normalizedSkill, text)) {
                matched++;
            }
        }

        int score = Math.min(matched * 8, 30);

        if (matched > 0) {
            reasons.add("Matched " + matched + " profile skill(s)");
        } else {
            concerns.add("No clear skill overlap found");
        }

        return score;
    }

    private int calculateExperienceScore(UserProfile profile, String description,
                                         List<String> reasons, List<String> concerns) {
        Integer requiredMinYears = experienceExtractor.extractMinimumYears(description);
        Integer userYears = profile.getTotalExperienceYears();

        if (requiredMinYears == null || userYears == null) {
            return 10;
        }

        if (userYears >= requiredMinYears) {
            reasons.add("Experience requirement appears compatible");
            return 20;
        }

        if (userYears + 1 >= requiredMinYears) {
            concerns.add("Slight experience gap");
            return 10;
        }

        concerns.add("Experience requirement is higher than profile experience");
        return 0;
    }

    private int calculateLocationScore(UserProfile profile, String location,
                                       List<String> reasons, List<String> concerns) {
        if (profile.getPreferredLocation() == null || profile.getPreferredLocation().isBlank()) {
            return 5;
        }

        String preferred = profile.getPreferredLocation().toLowerCase().trim();

        if (location.contains(preferred)) {
            reasons.add("Location matches preferred location");
            return 10;
        }

        if (location.contains("remote") && !location.contains("us remote") && !location.contains("remote in the us")) {
            reasons.add("Remote-friendly role");
            return 7;
        }

        if (location.contains("us") || location.contains("canada")) {
            concerns.add("Location appears region-locked and may not match preferred geography");
            return 1;
        }

        concerns.add("Location does not match preferred location");
        return 0;
    }

    private int calculateRecencyScore(Instant postedAt, List<String> reasons) {
        if (postedAt == null) {
            return 0;
        }

        long days = ChronoUnit.DAYS.between(postedAt, Instant.now());

        if (days <= 7) {
            reasons.add("Recently posted job");
            return 5;
        }
        if (days <= 30) {
            return 3;
        }
        return 1;
    }

    private String determineLevel(int score, int roleScore, int experienceScore) {
        if (roleScore == 0 || experienceScore == 0) {
            if (score < 40) {
                return "REJECT";
            }
        }
        if (score >= 70) return "HIGH";
        if (score >= 50) return "MEDIUM";
        if (score >= 30) return "LOW";
        return "REJECT";
    }

    private boolean containsBackendEngineeringSignals(String text) {
        return text.contains("backend")
                || text.contains("software engineer")
                || text.contains("java")
                || text.contains("spring")
                || text.contains("api")
                || text.contains("distributed systems");
    }

    private boolean relatedRoleMatch(String role, String text) {
        if (role.contains("java developer")) {
            return text.contains("backend engineer") || text.contains("software engineer") || text.contains("java");
        }
        if (role.contains("spring boot developer")) {
            return text.contains("backend engineer") || text.contains("spring") || text.contains("java");
        }
        return false;
    }

    private boolean skillAliasMatch(String skill, String text) {
        return switch (skill) {
            case "spring boot" -> text.contains("spring");
            case "postgresql" -> text.contains("postgres");
            default -> false;
        };
    }
}
