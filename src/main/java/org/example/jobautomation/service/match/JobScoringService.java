package org.example.jobautomation.service.match;

import lombok.RequiredArgsConstructor;
import org.example.jobautomation.dto.JobListingDto;
import org.example.jobautomation.dto.JobMatchResultDto;
import org.example.jobautomation.entity.UserProfile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobScoringService {

    private final CombinedJobScoringService combinedJobScoringService;

    public JobMatchResultDto score(UserProfile profile, JobListingDto job) {
        return combinedJobScoringService.score(profile, job);
    }
}
