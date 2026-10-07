package org.example.jobautomation.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.jobautomation.dto.*;
import org.example.jobautomation.entity.ResumeConfirmation;
import org.example.jobautomation.entity.UserProfile;
import org.example.jobautomation.repository.ResumeConfirmationRepository;
import org.example.jobautomation.service.UserProfileService;
import org.example.jobautomation.service.match.JobMatchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchNotificationService {

    private final UserProfileService userProfileService;
    private final JobMatchService jobMatchService;
    private final ResumeConfirmationRepository confirmationRepo;
    private final MatchEmailService emailService;
    private final ObjectMapper objectMapper;
    @Value("${app.confirmation-days:7}") private int confirmationDays;

    /** Returns number of HIGH jobs emailed. */
    public int notifyHighMatches(Long userId, JobMatchRequest request) {
        UserProfile user = userProfileService.getProfile(userId);
        if (!StringUtils.hasText(user.getEmail())) {
            throw new IllegalStateException("User " + userId + " has no email");
        }

        JobMatchResponseDto result = jobMatchService.matchJobs(userId, request);
        List<HighMatchDto> toSend = new ArrayList<>();

        for (JobMatchResultDto m : result.getMatches()) {
            if (!"HIGH".equals(m.getMatchLevel())) continue;
            JobListingDto job = m.getJob();

            if (confirmationRepo.existsByUserIdAndSourceAndExternalJobId(
                    userId, job.getSource(), job.getExternalJobId())) {
                continue; // already emailed before
            }

            ResumeConfirmation c = new ResumeConfirmation();
            c.setToken(UUID.randomUUID().toString());
            c.setUserId(userId);
            c.setSource(job.getSource());
            c.setExternalJobId(job.getExternalJobId());
            c.setJobTitle(job.getTitle());
            c.setCompany(job.getCompany());
            c.setJobSnapshotJson(toJson(job));
            c.setExpiresAt(Instant.now().plus(confirmationDays, ChronoUnit.DAYS));
            confirmationRepo.save(c);

            toSend.add(new HighMatchDto(c.getToken(), job.getTitle(), job.getCompany(),
                    job.getLocation(), job.getJobUrl(),
                    m.getMatchScore(), m.getKeywordScore(), m.getAiScore(), m.getAiSummary()));
        }

        if (!toSend.isEmpty()) emailService.sendHighMatches(user, toSend);
        log.info("Emailed {} HIGH matches to user {}", toSend.size(), userId);
        return toSend.size();
    }

    private String toJson(JobListingDto job) {
        try { return objectMapper.writeValueAsString(job); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}

