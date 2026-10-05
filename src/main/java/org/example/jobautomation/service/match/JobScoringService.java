package org.example.jobautomation.service.match;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.jobautomation.dto.JobListingDto;
import org.example.jobautomation.dto.JobMatchResultDto;
import org.example.jobautomation.entity.UserProfile;
import org.example.jobautomation.service.match.config.ScoringConfigProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobScoringService {

    private final JobTextNormalizer jobTextNormalizer;
    private final ExperienceExtractor experienceExtractor;
    private final ScoringConfigProperties scoringConfig;

    public JobMatchResultDto score(UserProfile profile, JobListingDto job) {
        List<String> reasons = new ArrayList<>();
        List<String> concerns = new ArrayList<>();

        // Preprocess job text for scoring
        String normalizedTitle = jobTextNormalizer.normalize(job.getTitle());
        String normalizedDescription = jobTextNormalizer.normalizeAndStripHtml(job.getDescription());
        String normalizedLocation = jobTextNormalizer.normalize(job.getLocation());

        // Calculate individual scores with new weights
        int roleScore = calculateRoleScore(profile, normalizedTitle, normalizedDescription, reasons, concerns);
        int skillScore = calculateSkillScore(profile, normalizedTitle + " " + normalizedDescription, reasons, concerns);
        int experienceScore = calculateExperienceScore(profile, normalizedDescription, reasons, concerns);
        int locationScore = calculateLocationScore(profile, normalizedLocation, reasons, concerns);
        int recencyScore = calculateRecencyScore(job.getPostedAt(), reasons);

        // Apply weights from config
        int weightedScore =
                (roleScore * scoringConfig.getWeights().getRoleMatch() / 25) +
                        (skillScore * scoringConfig.getWeights().getSkillsMatch() / 40) +
                        (experienceScore * scoringConfig.getWeights().getExperienceMatch() / 20) +
                        (locationScore * scoringConfig.getWeights().getLocationMatch() / 10) +
                        (recencyScore * scoringConfig.getWeights().getRecencyMatch() / 5);

        String matchLevel = determineLevel(weightedScore, roleScore, experienceScore);

        log.debug("Job: {} | Role: {} | Skills: {} | Exp: {} | Loc: {} | Recency: {} | Weighted: {}",
                job.getTitle(), roleScore, skillScore, experienceScore, locationScore, recencyScore, weightedScore);

        return buildResult(job, weightedScore, matchLevel, reasons, concerns);
    }

    private int calculateRoleScore(UserProfile profile, String title, String description,
                                   List<String> reasons, List<String> concerns) {
        int score = 0;
        String combined = title + " " + description;

        boolean targetRoleMatch = profile.getTargetRoles().stream()
                .map(jobTextNormalizer::normalize)
                .anyMatch(role -> combined.contains(role) || relatedRoleMatch(role, combined));

        boolean pastRoleMatch = profile.getPastRoles().stream()
                .map(jobTextNormalizer::normalize)
                .anyMatch(role -> combined.contains(role) || relatedRoleMatch(role, combined));

        if (targetRoleMatch) {
            score = 25;  // Max role score = weight value
            reasons.add("Matches target role preference");
        } else if (pastRoleMatch) {
            score = 15;  // Partial credit
            reasons.add("Matches past role experience");
        } else if (hasBackendEngineeringSignals(combined)) {
            score = 12;  // Weak signal
            reasons.add("Relevant backend/software engineering role");
        } else {
            concerns.add("Role appears weakly aligned with target roles");
        }

        return score;
    }

    private int calculateSkillScore(UserProfile profile, String text,
                                    List<String> reasons, List<String> concerns) {
        int matched = 0;
        int total = profile.getSkills().size();

        for (String skill : profile.getSkills()) {
            String normalizedSkill = jobTextNormalizer.normalize(skill);
            if (matchesSkill(normalizedSkill, text)) {
                matched++;
            }
        }

        // Scale to max 40 (weight value)
        int score = total > 0 ? (matched * 40) / total : 0;
        score = Math.min(score, 40);

        if (matched > 0) {
            reasons.add("Matched " + matched + "/" + total + " profile skill(s)");
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
            return 20;  // Max experience score = weight value
        }

        if (userYears + 1 >= requiredMinYears) {
            concerns.add("Slight experience gap");
            return 10;  // Partial credit
        }

        concerns.add("Experience requirement is higher than profile experience");
        return 0;
    }

    private int calculateLocationScore(UserProfile profile, String location,
                                       List<String> reasons, List<String> concerns) {
        if (profile.getPreferredLocation() == null || profile.getPreferredLocation().isBlank()) {
            return 5;  // Neutral if no preference
        }

        String preferred = jobTextNormalizer.normalize(profile.getPreferredLocation());

        if (location.contains(preferred)) {
            reasons.add("Location matches preferred location");
            return 10;  // Max location score = weight value
        }

        if (isRemote(location)) {
            reasons.add("Remote-friendly role");
            return 7;  // Partial credit
        }

        if (hasRegionMismatch(location)) {
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
        int veryRecentDays = scoringConfig.getRecency().getVeryRecentDays();
        int recentDays = scoringConfig.getRecency().getRecentDays();

        if (days <= veryRecentDays) {
            reasons.add("Recently posted job");
            return 5;  // Max recency score = weight value
        }
        if (days <= recentDays) {
            return 3;
        }
        return 1;
    }

    private String determineLevel(int score, int roleScore, int experienceScore) {
        if (roleScore == 0 || experienceScore == 0) {
            if (score < scoringConfig.getThresholds().getReject()) {
                return "REJECT";
            }
        }
        if (score >= scoringConfig.getThresholds().getHigh()) return "HIGH";
        if (score >= scoringConfig.getThresholds().getMedium()) return "MEDIUM";
        if (score >= scoringConfig.getThresholds().getLow()) return "LOW";
        return "REJECT";
    }

    private boolean hasBackendEngineeringSignals(String text) {
        return scoringConfig.getKeywordSignals()
                .getOrDefault("backend_engineering", Collections.emptyList())
                .stream()
                .anyMatch(text::contains);
    }

    private boolean relatedRoleMatch(String role, String text) {
        return scoringConfig.getRoleAliases()
                .getOrDefault(role, Collections.emptyList())
                .stream()
                .anyMatch(text::contains);
    }

    private boolean matchesSkill(String skill, String text) {
        // Direct match
        if (text.contains(skill)) {
            return true;
        }

        // Alias match from config
        return scoringConfig.getSkillAliases()
                .getOrDefault(skill, Collections.emptyList())
                .stream()
                .anyMatch(text::contains);
    }

    private boolean isRemote(String location) {
        return scoringConfig.getLocation()
                .getRemoteKeywords()
                .stream()
                .anyMatch(location::contains);
    }

    private boolean hasRegionMismatch(String location) {
        return scoringConfig.getLocation()
                .getRegionKeywords()
                .stream()
                .anyMatch(location::contains);
    }

    private JobMatchResultDto buildResult(JobListingDto job, int keywordScore, String matchLevel,
                                          List<String> reasons, List<String> concerns) {
        return new JobMatchResultDto(
                job,
                keywordScore,        // matchScore
                keywordScore,        // keywordScore
                null,                // aiScore (future)
                matchLevel,
                reasons,
                concerns
        );
    }
}