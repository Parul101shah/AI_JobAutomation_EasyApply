package org.example.jobautomation.service.match;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.jobautomation.dto.JobDiscoveryResponse;
import org.example.jobautomation.dto.JobListingDto;
import org.example.jobautomation.dto.JobMatchRequest;
import org.example.jobautomation.dto.JobMatchResponseDto;
import org.example.jobautomation.dto.JobMatchResultDto;
import org.example.jobautomation.dto.KeywordScoreResultDto;
import org.example.jobautomation.entity.UserProfile;
import org.example.jobautomation.service.UserProfileService;
import org.example.jobautomation.service.discovery.JobDiscoveryService;
import org.example.jobautomation.service.match.config.ScoringConfigProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobMatchService {

    private final UserProfileService userProfileService;
    private final JobDiscoveryService jobDiscoveryService;
    private final CombinedJobScoringService combinedJobScoringService;
    private final ExecutorService jobScoringExecutor;
    private final ScoringConfigProperties scoringConfig;

    private record Scored(JobListingDto job, KeywordScoreResultDto keyword) {}

    public JobMatchResponseDto matchJobs(Long userId, JobMatchRequest request) {
        UserProfile profile = userProfileService.getProfile(userId);

        JobDiscoveryResponse discoveryResponse =
                jobDiscoveryService.discoverJobs(request.getJobSearchRequest());

        Integer minimumScore = request.getMinimumScore() != null ? request.getMinimumScore() : 30;

        // Phase 0: cheap keyword scoring for ALL jobs (no network), best first
        List<Scored> ranked = discoveryResponse.getJobs().stream()
                .map(job -> new Scored(job, combinedJobScoringService.keywordScore(profile, job)))
                .sorted(Comparator.comparing((Scored s) -> s.keyword().getTotalScore()).reversed())
                .toList();

        var blend = scoringConfig.getBlend();
        List<CompletableFuture<JobMatchResultDto>> futures = new ArrayList<>();

        for (int i = 0; i < ranked.size(); i++) {
            Scored s = ranked.get(i);
            boolean useAi = Boolean.TRUE.equals(blend.getAiEnabled())
                    && i < blend.getAiMaxCandidates()
                    && s.keyword().getTotalScore() >= blend.getAiMinKeywordScore();

            if (useAi) {
                // Phase 1: only the shortlist calls the LLM, in parallel
                futures.add(CompletableFuture
                        .supplyAsync(() -> combinedJobScoringService.scoreWithAi(profile, s.job(), s.keyword()),
                                jobScoringExecutor)
                        .exceptionally(ex -> {
                            log.warn("AI scoring failed for job {}: {}", s.job().getExternalJobId(), ex.toString());
                            return combinedJobScoringService.buildKeywordOnly(s.job(), s.keyword());
                        }));
            } else {
                futures.add(CompletableFuture.completedFuture(
                        combinedJobScoringService.buildKeywordOnly(s.job(), s.keyword())));
            }
        }

        // Phase 2: wait for all, then filter and sort
        List<JobMatchResultDto> results = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .filter(result -> Boolean.TRUE.equals(request.getIncludeRejected())
                        || result.getMatchScore() >= minimumScore)
                .sorted(Comparator.comparing(JobMatchResultDto::getMatchScore).reversed())
                .collect(Collectors.toList());

        if (request.getLimit() != null && request.getLimit() > 0 && request.getLimit() < results.size()) {
            results = new ArrayList<>(results.subList(0, request.getLimit()));
        }

        return new JobMatchResponseDto(
                userId,
                discoveryResponse.getTotalJobs(),
                results.size(),
                results
        );
    }
}