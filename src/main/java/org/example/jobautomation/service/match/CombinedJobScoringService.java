package org.example.jobautomation.service.match;

import lombok.RequiredArgsConstructor;
import org.example.jobautomation.dto.AiScoreResultDto;
import org.example.jobautomation.dto.JobListingDto;
import org.example.jobautomation.dto.JobMatchResultDto;
import org.example.jobautomation.dto.KeywordScoreResultDto;
import org.example.jobautomation.entity.UserProfile;
import org.example.jobautomation.service.match.config.ScoringConfigProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CombinedJobScoringService {

    private final KeywordJobScoringService keywordJobScoringService;
    private final AiJobScoringService aiJobScoringService;
    private final ScoringConfigProperties scoringConfig;

    public JobMatchResultDto score(UserProfile profile, JobListingDto job) {
        KeywordScoreResultDto keywordResult = keywordJobScoringService.score(profile, job);
        AiScoreResultDto aiResult = aiJobScoringService.score(profile, job);

        int keywordScore = safe(keywordResult.getTotalScore());
        int aiScore = safe(aiResult.getScore());

        int finalScore = combine(keywordScore, aiScore);
        String matchLevel = determineLevel(finalScore);

        List<String> reasons = new ArrayList<>();
        List<String> concerns = new ArrayList<>();

        if (keywordResult.getReasons() != null) {
            reasons.addAll(keywordResult.getReasons());
        }
        if (aiResult.getReasons() != null) {
            reasons.addAll(aiResult.getReasons());
        }

        if (keywordResult.getConcerns() != null) {
            concerns.addAll(keywordResult.getConcerns());
        }
        if (aiResult.getConcerns() != null) {
            concerns.addAll(aiResult.getConcerns());
        }

        return new JobMatchResultDto(
                job,
                finalScore,
                keywordScore,
                aiScore,
                aiResult.getSummary(),
                matchLevel,
                reasons,
                concerns
        );
    }

    private int combine(int keywordScore, int aiScore) {
        int keywordWeight = 70;
        int aiWeight = 30;
        return (keywordScore * keywordWeight + aiScore * aiWeight) / 100;
    }

    private String determineLevel(int score) {
        if (score >= scoringConfig.getThresholds().getHigh()) {
            return "HIGH";
        }
        if (score >= scoringConfig.getThresholds().getMedium()) {
            return "MEDIUM";
        }
        if (score >= scoringConfig.getThresholds().getLow()) {
            return "LOW";
        }
        return "REJECT";
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
