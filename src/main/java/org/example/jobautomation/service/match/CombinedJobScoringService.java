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

    public KeywordScoreResultDto keywordScore(UserProfile profile, JobListingDto job) {
        return keywordJobScoringService.score(profile, job);
    }

    /** Keyword-only result: used for jobs that didn't make the AI shortlist. */
    public JobMatchResultDto buildKeywordOnly(JobListingDto job, KeywordScoreResultDto kw) {
        return build(job, kw, null);
    }

    /** Calls the LLM, then blends. Slow, so call it only for shortlisted jobs. */
    public JobMatchResultDto scoreWithAi(UserProfile profile, JobListingDto job, KeywordScoreResultDto kw) {
        return build(job, kw, aiJobScoringService.score(profile, job));
    }

    private JobMatchResultDto build(JobListingDto job, KeywordScoreResultDto kw, AiScoreResultDto ai) {
        int keywordScore = safe(kw.getTotalScore());
        boolean aiUsed = ai != null && !"UNKNOWN".equals(ai.getMatchLevel()); // fallback = AI failed
        int aiScore = aiUsed ? safe(ai.getScore()) : 0;

        int finalScore = aiUsed ? combine(keywordScore, aiScore) : keywordScore;

        List<String> reasons = new ArrayList<>();
        List<String> concerns = new ArrayList<>();
        if (kw.getReasons() != null) reasons.addAll(kw.getReasons());
        if (kw.getConcerns() != null) concerns.addAll(kw.getConcerns());
        if (aiUsed) {
            if (ai.getReasons() != null) reasons.addAll(ai.getReasons());
            if (ai.getConcerns() != null) concerns.addAll(ai.getConcerns());
        }

        return new JobMatchResultDto(
                job, finalScore, keywordScore, aiScore,
                aiUsed ? ai.getSummary() : "AI scoring skipped (keyword score only)",
                determineLevel(finalScore), reasons, concerns);
    }

    private int combine(int keywordScore, int aiScore) {
        var blend = scoringConfig.getBlend();
        int total = blend.getKeywordWeight() + blend.getAiWeight();
        return (keywordScore * blend.getKeywordWeight() + aiScore * blend.getAiWeight()) / total;
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