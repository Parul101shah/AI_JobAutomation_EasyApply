package org.example.jobautomation.service.resume;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.jobautomation.dto.JobListingDto;
import org.example.jobautomation.dto.TailoredResume;
import org.example.jobautomation.entity.*;
import org.example.jobautomation.repository.*;
import org.example.jobautomation.service.UserProfileService;
import org.example.jobautomation.service.notification.MatchEmailService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeGenerationService {

    private final ResumeConfirmationRepository confirmationRepo;
    private final GeneratedResumeRepository resumeRepo;
    private final UserProfileService userProfileService;
    private final ResumeTailoringService tailoringService;
    private final ResumePdfBuilder pdfBuilder;
    private final MatchEmailService emailService;
    private final ObjectMapper objectMapper;
    private final ExecutorService jobScoringExecutor;

    /** Validates token, marks CONFIRMED, runs generation in the background. */
    public String confirm(String token) {
        // Atomic: only ONE request can change PENDING -> CONFIRMED
        int updated = confirmationRepo.claim(token, Instant.now());
        if (updated == 0) {
            // invalid token, expired, or already used (same message for all)
            return "This link is invalid, expired, or already used.";
        }

        ResumeConfirmation c = confirmationRepo.findByToken(token).orElseThrow();
        jobScoringExecutor.submit(() -> generate(c.getId()));
        return "Thanks! Your resume for " + c.getJobTitle() + " at " + c.getCompany()
                + " is being generated and will be emailed to you shortly.";
    }

    public void generate(Long confirmationId) {
        ResumeConfirmation c = confirmationRepo.findById(confirmationId).orElseThrow();
        try {
            UserProfile user = userProfileService.getProfile(c.getUserId());
            JobListingDto job = objectMapper.readValue(c.getJobSnapshotJson(), JobListingDto.class);

            TailoredResume tailored = tailoringService.tailor(user, job);
            byte[] pdf = pdfBuilder.build(tailored);

            GeneratedResume g = resumeRepo
                    .findByUserIdAndSourceAndExternalJobId(c.getUserId(), c.getSource(), c.getExternalJobId())
                    .orElseGet(GeneratedResume::new);
            g.setUserId(c.getUserId());
            g.setSource(c.getSource());
            g.setExternalJobId(c.getExternalJobId());
            g.setJobTitle(c.getJobTitle());
            g.setCompany(c.getCompany());
            g.setTailoredJson(objectMapper.writeValueAsString(tailored));
            g.setPdf(pdf);
            g.setCreatedAt(Instant.now());
            resumeRepo.save(g);

            c.setStatus(ResumeConfirmationStatus.COMPLETED);
            confirmationRepo.save(c);
            emailService.sendResume(user, c.getJobTitle(), c.getCompany(), pdf);
        } catch (Exception ex) {
            log.error("Resume generation failed for confirmation {}", confirmationId, ex);
            c.setStatus(ResumeConfirmationStatus.FAILED);
            confirmationRepo.save(c);
        }
    }
}
