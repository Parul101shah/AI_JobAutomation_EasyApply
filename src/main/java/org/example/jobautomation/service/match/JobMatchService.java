package org.example.jobautomation.service.match;

import lombok.RequiredArgsConstructor;
import org.example.jobautomation.dto.JobDiscoveryResponse;
import org.example.jobautomation.dto.JobMatchRequest;
import org.example.jobautomation.dto.JobMatchResponseDto;
import org.example.jobautomation.dto.JobMatchResultDto;
import org.example.jobautomation.entity.UserProfile;
import org.example.jobautomation.service.UserProfileService;
import org.example.jobautomation.service.discovery.JobDiscoveryService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobMatchService {

    private final UserProfileService userProfileService;
    private final JobDiscoveryService jobDiscoveryService;
    private final JobScoringService jobScoringService;

    public JobMatchResponseDto matchJobs(Long userId, JobMatchRequest request) {
        UserProfile profile = userProfileService.getProfile(userId);

        JobDiscoveryResponse discoveryResponse =
                jobDiscoveryService.discoverJobs(request.getJobSearchRequest());

        List<JobMatchResultDto> results = discoveryResponse.getJobs().stream()
                .map(job -> jobScoringService.score(profile, job))
                .filter(result -> Boolean.TRUE.equals(request.getIncludeRejected()) || !"REJECT".equals(result.getMatchLevel()))
                .sorted(Comparator.comparing(JobMatchResultDto::getMatchScore).reversed())
                .collect(Collectors.toList());

        if (request.getLimit() != null && request.getLimit() > 0 && request.getLimit() < results.size()) {
            results = results.subList(0, request.getLimit());
        }

        return new JobMatchResponseDto(
                userId,
                discoveryResponse.getTotalJobs(),
                results.size(),
                results
        );
    }
}
